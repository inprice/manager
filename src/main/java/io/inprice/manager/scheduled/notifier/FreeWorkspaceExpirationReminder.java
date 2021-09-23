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
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.WorkspaceDao;
import io.inprice.manager.dao.UserDao;
import io.inprice.manager.helpers.EmailSender;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Sends emails to the workspaces whose statuses are either FREE or CREDITED 
 * and there is less than four days to renewal date.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class FreeWorkspaceExpirationReminder implements Task {

  private static final Logger logger = LoggerFactory.getLogger(FreeWorkspaceExpirationReminder.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.FREE_WORKSPACE_EXPIRATION_REMINDER;
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
        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);

        List<Workspace> aboutToExpiredWorkspaceList = 
          workspaceDao.findAboutToExpiredFreeWorkspaceList(
            Arrays.asList(
              WorkspaceStatus.FREE.name(),
              WorkspaceStatus.CREDITED.name()
            )
          );

        int affected = 0;

        if (CollectionUtils.isNotEmpty(aboutToExpiredWorkspaceList)) {
          UserDao userDao = handle.attach(UserDao.class);

          for (Workspace workspace: aboutToExpiredWorkspaceList) {
            User user = userDao.findById(workspace.getAdminId());

            Map<String, Object> mailMap = Map.of(
            	"user", user.getName(),
            	"model", workspace.getStatus(),
            	"days", DateUtils.findDayDiff(workspace.getSubsRenewalAt(), new Date()),
            	"subsRenewalAt", DateUtils.formatReverseDate(workspace.getSubsRenewalAt())
          	);

            EmailSender.send(
        			EmailData.builder()
          			.template(EmailTemplate.FREE_WORKSPACE_REMINDER)
          			.to(user.getEmail())
          			.subject("Your subscription is about to end.")
          			.data(mailMap)
          		.build()	
    				);

            affected++;
          }
        }

        if (affected > 0) {
          logger.info("Reminder emails sent to {} workspaces which are using free or a credit!", affected);
        } else {
          logger.info("No remainder sent to free or credited workspaces!");
        }
      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      TaskManager.stopTask(clazz);
    }
  }

}
