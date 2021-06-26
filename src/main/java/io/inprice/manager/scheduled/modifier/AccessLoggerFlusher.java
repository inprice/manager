package io.inprice.manager.scheduled.modifier;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.models.AccessLog;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;

/**
 * Persists access logs to db
 * 
 * @since 2021-05-19
 * @author mdpinar
 */
public class AccessLoggerFlusher implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(AccessLoggerFlusher.class);

  private final String clazz = getClass().getSimpleName();

  @Override
  public void run() {
    if (Global.isTaskRunning(clazz)) {
      log.warn(clazz + " is already triggered!");
      return;
    }

    try {
      Global.startTask(clazz);

      log.info(clazz + " is triggered.");
      try (Handle handle = Database.getHandle()) {
      	handle.begin();
      	
        PreparedBatch batch = 
      		handle.prepareBatch(
    				"insert into access_log (user_id, user_email, user_role, account_id, account_name, ip, agent, path, path_ext, method, req_body, res_body, status, elapsed, slow, created_at) " +
    				"values (:log.userId, :log.userEmail, :log.userRole, :log.accountId, :log.accountName, :log.ip, :log.agent, :log.path, :log.pathExt, :log.method, :log.reqBody, :log.resBody, " +
    				":log.status, :log.elapsed, :log.slow, :log.createdAt)"
					);
        int remaining = 0;
        while (! RedisClient.accessLogQueue.isEmpty()) {
        	AccessLog log = RedisClient.accessLogQueue.poll();
        	if (log != null) {
            batch.bindBean("log", log).add();
            remaining++;
            if (remaining >= 100) {
            	batch.execute();
            	remaining = 0;
            	Thread.sleep(500);
            }
        	}
        }
        if (batch.size() > 0) batch.execute();

        handle.commit();

      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
