package io.inprice.manager.scheduled;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.manager.dao.CompanyDao;
import io.inprice.manager.helpers.Global;

/**
 * Stops SUBSCRIBED companies after four days later from their subs renewal date expired.
 * Normally, StripeService in api project will handle this properly. 
 * However, a communication problem with stripe may occur and we do not want to miss an expired company.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class SubscribedCompanyStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SubscribedCompanyStopper.class);

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

          List<Long> expiredCompanyIdList = companyDao.findExpiredSubscribedCompanyIdList();
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
            log.info("{} subscribed company in total stopped!", affected);
          } else {
            log.info("No subscribed company to be stopped was found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
