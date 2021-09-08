package io.inprice.manager.scheduled.notifier;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.models.Account;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.AccountDao;
import io.inprice.manager.dao.UserDao;
import io.inprice.manager.helpers.EmailSender;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Sends emails to the accounts whose statuses are either FREE or COUPONED 
 * and there is less than four days to renewal date.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class FreeAccountExpirationReminder implements Task {

  private static final Logger logger = LoggerFactory.getLogger(FreeAccountExpirationReminder.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.FREE_ACCOUNT_EXPIRATION_REMINDER;
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
        AccountDao accountDao = handle.attach(AccountDao.class);

        List<Account> aboutToExpiredAccountList = 
          accountDao.findAboutToExpiredFreeAccountList(
            Arrays.asList(
              AccountStatus.FREE.name(),
              AccountStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (CollectionUtils.isNotEmpty(aboutToExpiredAccountList)) {
          UserDao userDao = handle.attach(UserDao.class);

          for (Account account: aboutToExpiredAccountList) {
            User user = userDao.findById(account.getAdminId());

            Map<String, Object> mailMap = Map.of(
            	"user", user.getName(),
            	"model", account.getStatus(),
            	"days", DateUtils.findDayDiff(account.getSubsRenewalAt(), new Date()),
            	"subsRenewalAt", DateUtils.formatReverseDate(account.getSubsRenewalAt())
          	);

            EmailSender.send(
        			EmailData.builder()
          			.template(EmailTemplate.FREE_ACCOUNT_REMINDER)
          			.to(user.getEmail())
          			.subject("Your subscription is about to end.")
          			.data(mailMap)
          		.build()	
    				);

            affected++;
          }
        }

        if (affected > 0) {
          logger.info("Reminder emails sent to {} accounts which are using free or a coupon!", affected);
        } else {
          logger.info("No remainder sent to free or couponed accounts!");
        }
      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      TaskManager.stopTask(clazz);
    }
  }

}
