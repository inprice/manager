package io.inprice.manager.scheduled;

import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.manager.dao.CompanyDao;
import io.inprice.manager.helpers.Global;

/**
 * Stops companies whose statuses are either FREE or COUPONED and subs renewal date expired.
 * Please note that stopping a regular subscriber is subject to another stopper see #SubscribedCompanyStopper
 * 
 * @since 2020-10-25
 * @author mdpinar
 */
public class FreeCompanyStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FreeCompanyStopper.class);

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

          List<Long> expiredCompanyIdList = 
            companyDao.findExpiredFreeCompanyIdList(
              Arrays.asList(
                CompanyStatus.FREE.name(),
                CompanyStatus.COUPONED.name()
              )
            );

          int affected = 0;

          if (expiredCompanyIdList != null && expiredCompanyIdList.size() > 0) {
            for (Long companyId: expiredCompanyIdList) {
              boolean isOK = companyDao.stopCompany(companyId);
              if (isOK) {
                isOK = companyDao.insertCompanyStatusHistory(companyId, CompanyStatus.STOPPED.name());
              }
              if (isOK) affected++;
            }
          }

          if (affected > 0) {
            log.info("{} free company in total stopped!", affected);
          } else {
            log.info("No free company to be stopped was found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
