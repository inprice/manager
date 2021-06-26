package io.inprice.manager.scheduled.notifier;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.inprice.manager.email.EmailSender;
import io.inprice.manager.helpers.Global;

/**
 * Sends emails to the accounts whose statuses are either FREE or COUPONED 
 * and there is less than four days to renewal date.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class FreeAccountsExpirationReminder implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FreeAccountsExpirationReminder.class);

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
        AccountDao accountDao = handle.attach(AccountDao.class);

        List<Account> aboutToExpiredAccountList = 
          accountDao.findAboutToExpiredFreeAccountList(
            Arrays.asList(
              AccountStatus.FREE.name(),
              AccountStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (aboutToExpiredAccountList != null && aboutToExpiredAccountList.size() > 0) {
          UserDao userDao = handle.attach(UserDao.class);

          for (Account account: aboutToExpiredAccountList) {
            User user = userDao.findById(account.getAdminId());

            Map<String, Object> mailMap = new HashMap<>(4);
            mailMap.put("user", user.getName());
            mailMap.put("model", account.getStatus());
            mailMap.put("days", DateUtils.findDayDiff(account.getSubsRenewalAt(), new Date()));
            mailMap.put("subsRenewalAt", DateUtils.formatReverseDate(account.getSubsRenewalAt()));

            EmailSender.send(
        			EmailData.builder()
          			.template(EmailTemplate.FREE_ACCOUNT_REMINDER)
          			.from(Props.APP_EMAIL_SENDER)
          			.to(user.getEmail())
          			.subject("Your subscription is about to end.")
          			.data(mailMap)
          		.build()	
    				);

            affected++;
          }
        }

        if (affected > 0) {
          log.info("Reminder emails sent to {} accounts which are using free or a coupon!", affected);
        } else {
          log.info("No remainder sent to free or couponed accounts!");
        }
      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
