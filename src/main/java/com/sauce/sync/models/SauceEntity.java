package com.sauce.sync.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.primitives.Ints;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.condition.IfNotNull;
import com.sauce.sync.utils.SyncIdUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by sauce on 4/19/15.
 */
@Entity
public class SauceEntity {
    //indexed fields and IDs
    @Id private long id;
    @Parent private Key<SauceUser> userKey;
    @Index(IfNotNull.class) private String type;
    @Index private long lastSyncedId;
    @Index(IfNotNull.class) private Map<String, Object> indexedData;
    //we use a timestamp so the reaper can query > deleted at and we only index when its not null to save 2 writes per entitiy that isnt deleted
    @Index(IfNotNull.class) private Long deletedAt;

    //unindexed fields stored in datastore
    //@Serialize
    private Map<String, Object> unindexedData;
    private long lastSyncedTime;

    //Ignored in datastore
    @Ignore private List<Reference> references;
    @Ignore private boolean deleted = false;
    @Ignore private Map<String, Object> data;
    @Ignore private Set<String> indexes;
    @Ignore private Set<String> ignored;

    //static stuff isnt serialized
    public static final String lastSyncedIdColumn = "lastSyncedId";
    public static final String deletedColumn = "deletedAt";
    public static final String indexedDataPrefix = "indexedData" + '.';
    private static final Logger log = Logger.getLogger(SauceEntity.class.getName());

    //public endpoints API
    public String getSyncId() {
        return lastSyncedId + "." + lastSyncedTime;
    }

    public void setSyncId(String syncId) {
        lastSyncedTime = SyncIdUtils.getLastSyncedTime(syncId);
        lastSyncedId = SyncIdUtils.getLastSyncedId(syncId);
    }

    public boolean isDeleted() {
        if(deletedAt != null && deletedAt > 0) {
            deleted = true;
        }
        return deleted;
    }

    public void setData(Map<String, Object> data) {
        log.info("data:" + data);
        this.data = data;
        dataToIndexedAndUnindexed();
    }

    public void setIgnored(Set<String> ignored) {
        this.ignored = ignored;
        dataToIndexedAndUnindexed();
    }

    public Set<String> getIgnored() {
        return ignored;
    }

    public void setIndexes(Set<String> indexes) {
        this.indexes = indexes;
        log.info("indexes:" + indexes);
        dataToIndexedAndUnindexed();
    }

    public void setDeleted(boolean deleted) {
        if(deleted) {
            deletedAt = System.currentTimeMillis();
        }
        this.deleted = deleted;
    }

    public String getEntityId() {
        return getKey()==null? null : getKey().toWebSafeString();
    }

    public void setEntityId(String entityId) {
        Key<SauceEntity> key = Key.create(entityId);
        this.id = key.getId();
        this.userKey = key.getParent();
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setReferences(List<Reference> references) { this.references = references; }
    public List<Reference> getReferences() { return references; }

    public Set<String> getIndexes() {
        if(indexes != null) {
            return indexes;
        }
        //on queries we need to infer the data from the @Ignore the indexed data fields
        Set<String> inferredIndexes = new HashSet<String>();
        if(indexedData != null) {
            inferredIndexes.addAll(indexedData.keySet());
        }
        return inferredIndexes;
    }

    public Map<String, Object> getData() {
        if(data != null) {
            return data;
        }
        log.info("indexedData: " + indexedData);
        log.info("unindexedData: " + unindexedData);

        //on queries we need to infer the data from the @Ignore indexed and unindexed data fields
        Map<String, Object> newData = new HashMap<String,Object>();
        if(indexedData != null) {
            newData.putAll(indexedData);
        }
        if(unindexedData != null) {
            newData.putAll(unindexedData);
        }
        log.info("data:" + newData);
        return newData;
    }

    //private endpoints api
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedId() { return lastSyncedId; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setLastSyncedId(long lastSyncedId) { this.lastSyncedId = lastSyncedId; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setLastSyncedTime(long lastSyncedTime) { this.lastSyncedTime = lastSyncedTime; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedTime() { return lastSyncedId; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setIndexedData(Map<String, Object> indexedData) { this.indexedData = indexedData; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Map<String, Object> getIndexedData(){ return indexedData; };

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Map<String, Object> getUnindexedData() { return unindexedData; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setUnindexedData(Map<String, Object> unindexedData) { this.unindexedData = unindexedData; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setUserKey(Key<SauceUser> userKey) { this.userKey = userKey;}

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<SauceEntity> getKey() {
        if(userKey == null || id == 0) {
            return null;
        }
        return Key.create(userKey, SauceEntity.class, id);
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getId() {
        return id;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setId(long id) { this.id = id; }

    /* helper gets called by setIndexedData and setUnindexedData needs to be idempotent since
       setIndexedData, setData, setIndexes, and setIgnored all set this property, should probably
       see if I can do this in a slightly cleaner way
     */
    private void dataToIndexedAndUnindexed() {
        if(data == null) {
            //we cant do anything if data is null
            return;
        }
        if(indexes == null) {
            indexes = new HashSet<String>();
        }

        if (!indexes.isEmpty() && indexedData == null) {
            indexedData = new HashMap<String, Object>();
        }
        Map<String, Object> dataShallowCopy = new HashMap<String, Object>(data);
        log.info("shallow:" + dataShallowCopy);

        if(ignored != null) {
            for(String ignore: ignored) {
                dataShallowCopy.remove(ignore);
            }
        }

        if (indexes != null) {
            for (String index : indexes) {
                Object removedValue = dataShallowCopy.remove(index);
                if (removedValue != null) {
                    indexedData.put(index, removedValue);
                }
            }
        }

        if(!data.isEmpty()) {
            unindexedData = new HashMap<String,Object>(dataShallowCopy);
        }
    }


    /*
        So the problem here is two fold: first, Google Datastore upcasts all ints into a 64 bit number
        when storing them and second, since Cloud Endpoints sends data over the wire as json,
        all 64 bit numbers are turned into strings since [js loses precision after 2^53]()
        see https://cloud.google.com/appengine/docs/java/datastore/entities#Java_Properties_and_value_types
        and https://code.google.com/p/googleappengine/issues/detail?id=9173
    */
    @OnLoad
    public void onLoad() {
        fixLongsThatWillBeUpcastedToJSONStrings(indexedData);
        fixLongsThatWillBeUpcastedToJSONStrings(unindexedData);
    }

    private void fixLongsThatWillBeUpcastedToJSONStrings(Map<String, Object> map) {
        if(map == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(value instanceof Long ) {
                try {
                    int intValue = Ints.checkedCast((Long) value);
                    entry.setValue(intValue);
                } catch (IllegalArgumentException e) {
                    //do nothing, the long is actually a long, GAE is gonna cast this to a string in
                    //the response, since JSON can only support 2^53 bit integers
                }
            } else if(value instanceof List) {
                //check for lists that contain numbers
                List list = (List)value;
                for(int c = 0; c < list.size(); c++) {
                    if(list.get(c) instanceof Long) {
                        try {
                            int intValue = Ints.checkedCast((Long) list.get(c));
                            list.set(c, intValue);
                        } catch (IllegalArgumentException e) {
                            //do nothing, the long is actually a long, GAE is gonna cast this to a string in
                            //the response, since JSON can only support 2^53 bit integers
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "SauceEntity{" +
                "id=" + id +
                ", userKey=" + userKey +
                ", deleted=" + deleted +
                ", lastSyncedId=" + lastSyncedId +
                ", indexedData=" + indexedData +
                ", unindexedData=" + unindexedData +
                ", references=" + references +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SauceEntity that = (SauceEntity) o;

        if (id != that.id) return false;
        return lastSyncedId == that.lastSyncedId;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (lastSyncedId ^ (lastSyncedId >>> 32));
        return result;
    }

}
