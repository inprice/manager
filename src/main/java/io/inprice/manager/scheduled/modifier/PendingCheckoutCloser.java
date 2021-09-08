package io.inprice.manager.scheduled.modifier;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.CheckoutDao;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Sets the checkouts EXPIRED that have been in the PENDING state for more than two hours.
 * 
 * @since 2020-12-11
 * @author mdpinar
 */
public class PendingCheckoutCloser implements Task {

  private static final Logger logger = LoggerFactory.getLogger(PendingCheckoutCloser.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.PENDING_CHECKOUT_CLOSER;
  }

  @Override
  public void run() {
    if (TaskManager.isTaskRunning(clazz)) {
      logger.warn(clazz + " is already triggered!");
      return;
    }

    try {
      TaskManager.startTask(clazz);

      logger.info(clazz + " is triggered.");
      try (Handle handle = Database.getHandle()) {
        CheckoutDao checkoutDao = handle.attach(CheckoutDao.class);

        int affected = checkoutDao.expirePendings();
        if (affected > 0) {
          logger.info("{} PENDING checkout(s) are set to EXPIRED!", affected);
        } else {
          logger.info("No PENDING checkout to be EXPIRED was found!");
        }
      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      TaskManager.stopTask(clazz);
    }
  }

}
