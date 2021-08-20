package io.inprice.manager.scheduled.modifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.User;
import io.inprice.manager.dao.AccountDao;
import io.inprice.manager.dao.SubscriptionDao;
import io.inprice.manager.dao.UserDao;
import io.inprice.manager.email.EmailSender;
import io.inprice.manager.helpers.Global;

/**
 * Stops accounts whose statuses are either FREE or COUPONED and subs renewal date expired.
 * Please note that stopping a regular subscriber is subject to another stopper see #SubscribedAccountStopper
 * 
 * @since 2020-10-25
 * @author mdpinar
 */
public class FreeAccountStopper implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(FreeAccountStopper.class);

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
      	handle.begin();

        AccountDao accountDao = handle.attach(AccountDao.class);
        List<Account> expiredAccountList = 
          accountDao.findExpiredFreeAccountList(
            Arrays.asList(
              AccountStatus.FREE.name(),
              AccountStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (CollectionUtils.isNotEmpty(expiredAccountList)) {
          UserDao userDao = handle.attach(UserDao.class);
          SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

          for (Account account: expiredAccountList) {
            boolean isOK = subscriptionDao.terminate(account.getId(), AccountStatus.STOPPED.name());
            if (isOK) {

              AccountTrans trans = new AccountTrans();
              trans.setAccountId(account.getId());
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("End of period!"));

              if (AccountStatus.FREE.equals(account.getStatus()))
                trans.setEvent(SubsEvent.FREE_USE_STOPPED);
              else
                trans.setEvent(SubsEvent.COUPON_USE_STOPPED);
    
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = accountDao.insertStatusHistory(account.getId(), AccountStatus.STOPPED);
              }
            }

            if (isOK) {
              User user = userDao.findById(account.getId());
              String accountName = StringUtils.isNotBlank(account.getTitle()) ? account.getTitle() : account.getName();

              Map<String, Object> mailMap = Map.of(
              	"user", user.getEmail(),
              	"account", accountName
          		);

              EmailSender.send(
          			EmailData.builder()
            			.template(EmailTemplate.FREE_ACCOUNT_STOPPED)
            			.to(user.getEmail())
            			.subject("Your inprice subscription is stopped.")
            			.data(mailMap)
            		.build()	
      				);

              affected++;
            }
          }
        }

        if (affected > 0) {
          logger.info("{} free account in total stopped!", affected);
        } else {
          logger.info("No free account to be stopped was found!");
        }
        
        if (affected > 0)
        	handle.commit();
        else
        	handle.rollback();

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }

    } finally {
      Global.stopTask(clazz);
    }
  }

}
