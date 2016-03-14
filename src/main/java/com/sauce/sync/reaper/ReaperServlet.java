package com.sauce.sync.reaper;

import com.googlecode.objectify.Key;
import com.sauce.sync.Constants;
import com.sauce.sync.models.SauceEntity;

import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.sauce.sync.OfyService.ofy;


/**
 * Created by sauce on 10/23/15.
 */
public class ReaperServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(ReaperServlet.class.getName());

    private static final int ITEMS_DELETED_AT_A_TIME = 1000; //items deleted at a time, the reaper servlet will spin until there are no more items to delete, or if it times out


    /* this is the task that the reaper cron calls every day, deletes every object older than
     * Constants.REAPER_DELAY (default 7 days) with a splay  (Constants.REAPER_SPLAY)
     * just incase the clocks are off between instances */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        log.info("reaper servlet");
        String reaperHeader = req.getHeader("X-AppEngine-TaskName");
        if(reaperHeader == null) {
            resp.setStatus(403);
            return;
        }
        log.info("reaper servlet task id:" + reaperHeader);
        long reaperTime = System.currentTimeMillis() - Constants.REAPER_DELAY + Constants.REAPER_SPLAY; // add the splay to allow for some tolerances
        log.info("reaperTime: " + reaperTime);
        List<Key<SauceEntity>> entitiesToDelete;
        do {
            entitiesToDelete = ofy().load().type(SauceEntity.class).filter(SauceEntity.deletedColumn + " <", reaperTime).limit(ITEMS_DELETED_AT_A_TIME).keys().list();
            log.info("deleting " +entitiesToDelete.size() + " entities... checking for more");
            ofy().delete().keys(entitiesToDelete).now();
        } while(entitiesToDelete.size() > 0);

        // there is some eventual consistency issues with the do while block,
        // the queries are not strongly consistent so they may rerun, but trying to delete the same records
        // shouldnt cause any harm so I'm not going to bother fixing it

        log.info("successful reaping, no more entities returning 200");
        return;
    }

}
