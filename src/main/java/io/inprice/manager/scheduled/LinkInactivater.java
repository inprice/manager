package io.inprice.manager.scheduled;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.manager.dao.CompanyDao;
import io.inprice.manager.helpers.Global;

public class LinkInactivater implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(LinkInactivater.class);

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
        handle.inTransaction(transactional -> {
          CompanyDao companyDao = transactional.attach(CompanyDao.class);
          int affected = companyDao.inactivateLinks();
          if (affected > 0) {
            log.info("{} link(s) in total inactivated!", affected);
          } else {
            log.info("No link to inactivate was found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
