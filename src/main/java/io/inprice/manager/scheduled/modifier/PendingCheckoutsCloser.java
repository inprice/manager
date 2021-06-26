package io.inprice.manager.scheduled.modifier;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.manager.dao.CheckoutDao;
import io.inprice.manager.helpers.Global;

/**
 * Sets the checkouts EXPIRED that have been in the PENDING state for more than two hours.
 * 
 * @since 2020-12-11
 * @author mdpinar
 */
public class PendingCheckoutsCloser implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(PendingCheckoutsCloser.class);
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
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);

        int affected = checkoutDao.expirePendings();
        if (affected > 0) {
          log.info("{} PENDING checkout(s) are set to EXPIRED!", affected);
        } else {
          log.info("No PENDING checkout to be EXPIRED was found!");
        }
      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
