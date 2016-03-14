package com.sauce.sync.reaper;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by sauce on 10/27/15. The ReaperCronServlet takes requests
 * from Google's cron job (https://cloud.google.com/appengine/docs/java/config/cron) and adds a task
 * to reap old data
 */
public class ReaperCronServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(ReaperCronServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Enumeration e = req.getHeaderNames();
        while(e.hasMoreElements()) {
            String param = (String)e.nextElement();
            log.info("header:" + param + req.getHeader(param));
        }

        String cronHeader = req.getHeader("X-Appengine-Cron");
        if(cronHeader == null || !cronHeader.trim().equals("true")) {
            resp.setStatus(403);
            return;
        }

        Queue queue = QueueFactory.getQueue("reaper");
        queue.add(TaskOptions.Builder.withUrl("/reaper"));

    }
}