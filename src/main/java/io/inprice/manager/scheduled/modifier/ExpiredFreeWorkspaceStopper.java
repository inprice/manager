package io.inprice.manager.scheduled.modifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.models.User;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.WorkspaceDao;
import io.inprice.manager.dao.SubscriptionDao;
import io.inprice.manager.dao.UserDao;
import io.inprice.manager.helpers.EmailSender;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Stops workspaces whose statuses are either FREE or VOUCHERED and subs renewal date expired.
 * Please note that stopping a regular subscriber is subject to another stopper see #SubscribedWorkspaceStopper
 * 
 * @since 2020-10-25
 * @author mdpinar
 */
public class ExpiredFreeWorkspaceStopper implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExpiredFreeWorkspaceStopper.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.EXPIRED_FREE_WORKSPACE_STOPPER;
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
      	handle.begin();

        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        List<Workspace> expiredWorkspaceList = 
          workspaceDao.findExpiredFreeWorkspaceList(
            Arrays.asList(
              WorkspaceStatus.FREE.name(),
              WorkspaceStatus.VOUCHERED.name()
            )
          );

        int affected = 0;

        if (CollectionUtils.isNotEmpty(expiredWorkspaceList)) {
          UserDao userDao = handle.attach(UserDao.class);
          SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

          for (Workspace workspace: expiredWorkspaceList) {
            boolean isOK = subscriptionDao.terminate(workspace.getId(), WorkspaceStatus.STOPPED.name());
            if (isOK) {

              WorkspaceTrans trans = new WorkspaceTrans();
              trans.setWorkspaceId(workspace.getId());
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("End of period!"));

              if (WorkspaceStatus.FREE.equals(workspace.getStatus()))
                trans.setEvent(SubsEvent.FREE_USE_STOPPED);
              else
                trans.setEvent(SubsEvent.VOUCHER_USE_STOPPED);
    
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = workspaceDao.insertStatusHistory(workspace.getId(), WorkspaceStatus.STOPPED);
              }
            }

            if (isOK) {
              User user = userDao.findById(workspace.getId());
              String workspaceName = StringUtils.isNotBlank(workspace.getTitle()) ? workspace.getTitle() : workspace.getName();

              Map<String, Object> mailMap = Map.of(
              	"fullName", user.getFullName(),
              	"workspaceName", workspaceName
          		);

              EmailSender.send(
          			EmailData.builder()
            			.template(EmailTemplate.FREE_WORKSPACE_STOPPED)
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
          logger.info("{} free workspace in total stopped!", affected);
        } else {
          logger.info("No free workspace to be stopped was found!");
        }
        
        if (affected > 0)
        	handle.commit();
        else
        	handle.rollback();

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }

    } finally {
      TaskManager.stopTask(clazz);
    }
  }

}
