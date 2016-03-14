/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.sauce.sync;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.KeyRange;
import com.googlecode.objectify.Work;
import com.sauce.sync.conflicts.ConflictResolverFactory;
import com.sauce.sync.conflicts.Resolution;
import com.sauce.sync.models.Conflict;
import com.sauce.sync.models.Reference;
import com.sauce.sync.models.SauceEntity;
import com.sauce.sync.models.SauceUser;
import com.sauce.sync.exceptions.SyncConflictException;
import com.sauce.sync.json.OAuthTokenInfoResponse;
import com.sauce.sync.requests.QueryRequest;
import com.sauce.sync.requests.SaveRequest;
import com.sauce.sync.requests.SyncRequest;
import com.sauce.sync.responses.QueryResponse;
import com.sauce.sync.responses.SaveResponse;
import com.sauce.sync.responses.SyncResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import static com.sauce.sync.OfyService.ofy;

/**
 * google cloud endpoint Api class
 */
@Api(
        name = "sauce",
        description = "Sauce Sync lightweight BaaS, used to sync per-user data",
        documentationLink = "https://sauce-sync.appspot.com",
        version = "v1",
        namespace = @ApiNamespace(ownerDomain = "backend.sync.sauce.com", ownerName = "backend.sync.sauce.com", packagePath = ""),
        scopes = {
                Constants.EMAIL_SCOPE,
                Constants.PLUS_ME_SCOPE,
                Constants.PLUS_LOGIN_SCOPE
        },
        clientIds = {
                Constants.WEB_CLIENT_ID,
                Constants.ANDROID_CLIENT_ID,
                Constants.IOS_CLIENT_ID,
                com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID // so you can use the google api explorer
        },
        audiences = {Constants.ANDROID_AUDIENCE}
)
public class SauceSyncEndpoint {

    private static final String GCM_API_KEY = System.getProperty("gcm.api.key");
    private static final Logger log = Logger.getLogger(SauceSyncEndpoint.class.getName());


    @ApiMethod(name = "userInfo", path = "userinfo", httpMethod = ApiMethod.HttpMethod.GET)
    public SauceUser getUserInfo(User user, HttpServletRequest request) throws OAuthRequestException, IOException {
        final String userId = authenticate(user, request);
        SauceUser sauceUser = registerOrGetUser(userId, user.getEmail());
        return sauceUser;
    }

    @ApiMethod(name = "sync", path = "sync", httpMethod = ApiMethod.HttpMethod.POST)
    public SyncResponse sync(final SyncRequest syncRequest, final User user, final HttpServletRequest request) throws OAuthRequestException, SyncConflictException, IOException {
        final String userId = authenticate(user, request);
        syncRequest.validateRequest();


        //in order to make the transaction idempotent we generate the IDs outside of a transaction
        //this will also allow me to implement the 'refs' feature
        List<SauceEntity> entitiesThatNeedIds = new ArrayList<SauceEntity>();
        //first find all entities that are null and generateIds for them

        for(SauceEntity entity: syncRequest.getObjectsToSync()) {
            if(entity.getId() == 0) {
                entitiesThatNeedIds.add(entity);
            }
        }

        log.info("entitiesThatNeedIds:" + entitiesThatNeedIds);
        if(entitiesThatNeedIds.size()> 0) {
            KeyRange<SauceEntity> keys = ofy().factory().allocateIds(SauceEntity.class, entitiesThatNeedIds.size());
            Iterator<Key<SauceEntity>>  keysIterator = keys.iterator();
            for (SauceEntity entity : entitiesThatNeedIds) {
                entity.setId(keysIterator.next().getId());
            }
        }
        log.info("entities with ids now:" + entitiesThatNeedIds);

        SyncResponse syncResponse = ofy().transact(new Work<SyncResponse>() {
            public SyncResponse run() {

                //first check to see if we are too far out of sync
                SauceUser sauceUser = registerOrGetUser(userId, user.getEmail());
                if (!isUserInSync(sauceUser, syncRequest.getLastSyncedTime())) {
                    //the user is out of sync, they have to blow away their cache and resync

                    com.googlecode.objectify.cmd.Query<SauceEntity> outOfSyncQuery
                            = ofy().load().type(SauceEntity.class).ancestor(sauceUser.getKey());

                    //if you're selectively syncing, we have to only return the entities you're interested in
                    if (syncRequest.getDeltaQuery() != null) {
                        outOfSyncQuery = mutateObjectifyObjectBySyncQuery(outOfSyncQuery, syncRequest.getDeltaQuery().getFilters());
                    }

                    List<SauceEntity> outOfSyncEntities = outOfSyncQuery.list();

                    /*Lists.newArrayList will resolve the proxy by iterating through the proxy objects
                      if we don't do this we get a horrible serialization error which results in a 503 from GAE
                      https://groups.google.com/forum/#!topic/objectify-appengine/q61rUzwrufI */
                    return SyncResponse.getTooFarOutOfSyncResponse(Lists.newArrayList(outOfSyncEntities), sauceUser.getSyncId());
                }

                List<SauceEntity> entitiesToSave = null;

                //then check for conflits, if there are any return the conflict response
                if (!syncRequest.getObjectsToSync().isEmpty()) {
                    Optional<Resolution> conflictResponse = checkForConflicts(syncRequest);

                    //todo redo this
                    if (conflictResponse.isPresent() && !conflictResponse.get().resolved()) {
                        return SyncResponse.getConflictSyncResponse(conflictResponse.get().getUnresolvedConflicts(), sauceUser.getSyncId());
                    } else if (conflictResponse.isPresent()) {
                        entitiesToSave = conflictResponse.get().getResolvedConflicts();
                    } else {
                        entitiesToSave = syncRequest.getObjectsToSync();
                    }
                    log.info("entitiesToSave:" + entitiesToSave);
                    long newSyncedTime = System.currentTimeMillis();
                    long newLastSyncedId = sauceUser.incrementLastSyncedId(newSyncedTime);
                    //get and set new lastSyncedId
                    for (SauceEntity objectToSave : entitiesToSave) {
                        objectToSave.setLastSyncedId(newLastSyncedId);
                        objectToSave.setLastSyncedTime(newSyncedTime);
                        objectToSave.setUserKey(sauceUser.getKey());
                    }
                    //now that we have the user key and no conflicts we need resolve references
                    resolveSauceEntityReferences(entitiesToSave);

                    Map<Key<SauceEntity>, SauceEntity> entitiesStored = ofy().save().entities(entitiesToSave).now();
                    ofy().save().entities(sauceUser).now(); //save the updated sync id
                } else {
                    // sync only, not saving any entities. Carry on
                }


                com.googlecode.objectify.cmd.Query<SauceEntity> deltaQuery
                        = ofy().load().type(SauceEntity.class).ancestor(sauceUser.getKey());

                // selective sync feature
                if (syncRequest.getDeltaQuery() != null) {
                    deltaQuery = mutateObjectifyObjectBySyncQuery(deltaQuery, syncRequest.getDeltaQuery().getFilters());
                }

                deltaQuery = deltaQuery.filter(SauceEntity.lastSyncedIdColumn + " >", syncRequest.getLastSyncedId());

                List<SauceEntity> delta = deltaQuery.list();
                //queries are run frozen in time, so these entities might get returned from 'saved' and 'delta' responses
                //so we need to prune entities that came in the request.
                if (entitiesToSave != null) {
                    delta.removeAll(entitiesToSave);
                }
                log.info("delta: " + delta);
                /*Lists.newArrayList will resolve the proxy by iterating through the proxy objects
                  if we dont do this we get a horrible serialization error which results in a 503 from GAE
                  https://groups.google.com/forum/#!topic/objectify-appengine/q61rUzwrufI */
                SyncResponse response = SyncResponse.getSuccessSyncResponse(entitiesToSave, Lists.newArrayList(delta), sauceUser.getSyncId());
                return response;
            }
        });

        //no need to fire GCM on empty syncs
        if (!syncRequest.getObjectsToSync().isEmpty()) {
            Networker.sendGCMSync(userId, syncRequest.getSenderId());
        }
        return syncResponse;
    }

    @ApiMethod(name = "save", path = "save", httpMethod = ApiMethod.HttpMethod.POST)  //should never throw a sync conflict exception though
    public SaveResponse save(SaveRequest saveRequest, User user, HttpServletRequest request) throws OAuthRequestException, IOException, SyncConflictException {
        SyncResponse response = sync(saveRequest.toSyncRequest(), user, request);
        return SaveResponse.syncResponseToSavedResponse(response);
    }

    @ApiMethod(name = "query", path = "query", httpMethod = ApiMethod.HttpMethod.POST)
    public QueryResponse query(QueryRequest query, User user, HttpServletRequest request) throws OAuthRequestException, IOException {
        final String userId = authenticate(user, request);
        SauceUser sauceUser = registerOrGetUser(userId, user.getEmail());
        log.info("query" + query);

        com.googlecode.objectify.cmd.Query<SauceEntity> objectifyQuery = ofy().load().type(SauceEntity.class).ancestor(sauceUser.getKey());
        objectifyQuery = mutateObjectifyObjectBySyncQuery(objectifyQuery, query.getFilters());

        log.info("ofy query:" + objectifyQuery);
        List<SauceEntity> results = objectifyQuery.list();
        log.info("results:" + results);
        QueryResponse result = new QueryResponse(results);
        return result;
    }

    @ApiMethod(name = "purge", path = "purge", httpMethod = ApiMethod.HttpMethod.DELETE)
    public void purge(final User user, HttpServletRequest request) throws OAuthRequestException, IOException {
        final String userId = authenticate(user, request);

        ofy().transact(new Work<Void>() {
            public Void run() {
                SauceUser sauceUser = registerOrGetUser(userId, user.getEmail());
                ofy().delete().keys(ofy().load().ancestor(sauceUser.getKey()).keys().list());
                return null;
            }
        });
    }


    private com.googlecode.objectify.cmd.Query<SauceEntity> mutateObjectifyObjectBySyncQuery(com.googlecode.objectify.cmd.Query<SauceEntity> objectifyQuery, List<QueryRequest.Filter> filters) {
        for(QueryRequest.Filter filter: filters) {
            log.info("filter object:" + filter.getValue().getClass());
            objectifyQuery = objectifyQuery.filter(filter.getField() + ' ' + filter.getOperator(), filter.getValue());
        }
        return objectifyQuery;
    }

    private void resolveSauceEntityReferences(List<SauceEntity> objectsToSync) {
        for(SauceEntity entity: objectsToSync) {
            List<Reference> refs = entity.getReferences();
            Set<String> indexes = entity.getIndexes();
            if(refs != null && !refs.isEmpty()){
                for(Reference ref: refs) {
                    String refId = objectsToSync.get(ref.getArrayIndex()).getEntityId();
                    if(indexes.contains(ref.getKey())) {
                        entity.getIndexedData().put(ref.getKey(), refId);
                    } else {
                        entity.getUnindexedData().put(ref.getKey(), refId);
                    }
                    //always put in data so it shows up on the returned object
                    entity.getData().put(ref.getKey(), refId);
                }
            }
        }
    }


    private String authenticate(User user, HttpServletRequest request) throws OAuthRequestException, IOException {
        if (user == null) {
            throw new OAuthRequestException("Invalid oauth credentials");
        }
        return hackUserId(user, request);
    }

    /*
        Infamous bug https://code.google.com/p/googleappengine/issues/detail?id=8848
        this method is a workaround that goes to google's oauth token api and grabs the
        google plus id
    */
    private String hackUserId(User user, HttpServletRequest request) throws IOException {
        String authHeader = request.getHeader("Authorization");
        log.info("authToken:" + authHeader);
        OAuthTokenInfoResponse response = Networker.getTokenInfo(user, authHeader);
        return response.getUserId();
    }


    private Optional<Resolution> checkForConflicts(SyncRequest syncRequest) {
        //get entities from datastore and validate that there are no conflicts
        List<Key<SauceEntity>> serverKeysToLoad = new ArrayList<Key<SauceEntity>>();
        List<SauceEntity> clientSideConflictCheck = new ArrayList<SauceEntity>();
        for (SauceEntity keyExtractor : syncRequest.getObjectsToSync()) {
            if (keyExtractor.getKey() != null) {
                serverKeysToLoad.add(keyExtractor.getKey());
                clientSideConflictCheck.add(keyExtractor);
            }
        }

        if(!serverKeysToLoad.isEmpty()) {
            log.info("serverKeysToLoad:" + serverKeysToLoad);
            List<Conflict> conflicts = new ArrayList<Conflict>();
            Map<Key<SauceEntity>, SauceEntity> checkForConflicts = ofy().load().keys(serverKeysToLoad);
            for (SauceEntity clientEntity : clientSideConflictCheck) {
                SauceEntity serverEntity = checkForConflicts.get(clientEntity.getKey());
                log.info("clientEntity: " + clientEntity + " serverEntity: " + serverEntity);
                //check for null, if the server entity is null it means the reaper may have cleaned it up a while ago
                //so if the server is null, we dont want to treat it as a conflict, we should just let it get synced
                if (serverEntity != null && !clientEntity.equals(serverEntity)) {
                    conflicts.add(new Conflict(clientEntity, serverEntity));
                }
            }

            if (!conflicts.isEmpty()) {
                log.info("conflicts found:" + conflicts);
                Resolution resolution = ConflictResolverFactory.getResolverAndResolveConflicts(syncRequest.getConflictResolutionType(), conflicts);
                return Optional.of(resolution);
            }
        }
        //there were no conflicts
        return Optional.absent();
    }

    private SauceUser registerOrGetUser(String userId, String email) {
        Key<SauceUser> sauceUserKey = Key.create(SauceUser.class, userId);
        SauceUser sauceUser = ofy().load().key(sauceUserKey).now();

        //user is not registered yet, go ahead and write it
        if (sauceUser == null) {
            SauceUser user = new SauceUser(userId, email);
            ofy().save().entity(user).now();
            return user;
        }
        return sauceUser;
    }

    private boolean isUserInSync(SauceUser sauceUser, long clientSyncedTime) {
        if(sauceUser.getLastSyncedTime() - clientSyncedTime >= Constants.REAPER_DELAY - Constants.REAPER_SPLAY) {
            return false;
        }
        return true;
    }
}
