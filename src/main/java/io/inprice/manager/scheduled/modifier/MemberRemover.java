package io.inprice.manager.scheduled.modifier;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.manager.dao.AccountDao;
import io.inprice.manager.dao.MembershipDao;
import io.inprice.manager.helpers.Global;

/**
 * Permanently deletes the members marked as deleted.
 * To prevent abuse, member deletion is done after a while from the time of the request.
 * 
 * @since 202-10-07
 * @author mdpinar
 */
public class MemberRemover implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(MemberRemover.class);

  private final String clazz = getClass().getSimpleName();

  @Override
  public void run() {
    if (Global.isTaskRunning(clazz)) {
      logger.warn(clazz + " is already triggered!");
      return;
    }

    try {
      Global.startTask(clazz);
      logger.info(clazz + " is triggered.");

      try (Handle handle = Database.getHandle()) {
      	MembershipDao membershipDao = handle.attach(MembershipDao.class);

      	Map<Long, Integer> accountInfoMap = membershipDao.findAccountInfoOfDeletedMembers();
      	if (MapUtils.isNotEmpty(accountInfoMap)) {
        	handle.begin();

        	AccountDao accountDao = handle.attach(AccountDao.class);
        	
        	int userCount = 0;
        	for (Entry<Long, Integer> entry: accountInfoMap.entrySet()) {
        		userCount += entry.getValue();
        		accountDao.decreaseUserCount(entry.getKey(), entry.getValue());
        	}
      		
          boolean isOK = membershipDao.deletePermenantly();
          if (isOK) {
          	handle.commit();
            logger.info("{} member(s) in total are permanently DELETED!", userCount);
          } else {
          	handle.rollback();
            logger.info("No deleted member found!");
          }
      	}

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
