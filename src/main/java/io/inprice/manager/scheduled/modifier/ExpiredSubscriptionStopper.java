package io.inprice.manager.scheduled.modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SchedulerDef;
import io.inprice.manager.config.Props;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Stops SUBSCRIBED workspaces after four days later from their subs renewal date expired.
 * Normally, StripeService in api project will handle this properly. 
 * However, a communication problem with stripe may occur and we do not want to miss an expired workspace.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class ExpiredSubscriptionStopper implements Task {

  private static final Logger logger = LoggerFactory.getLogger(ExpiredSubscriptionStopper.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.EXPIRED_SUBSCRIPTION_STOPPER;
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
      
      /*

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        List<WorkspaceInfo> expiredWorkspaceList = workspaceDao.findExpiredSubscriberWorkspaceList();
        int affected = 0;

        if (CollectionUtils.isNotEmpty(expiredWorkspaceList)) {
          for (WorkspaceInfo accinfo: expiredWorkspaceList) {

            //we need to cancel stripe first
            try {
              Subscription subscription = Subscription.retrieve(accinfo.getCustId());
              Subscription subsResult = subscription.cancel();
              if (subsResult != null && subsResult.getStatus().equals("canceled")) {
                logger.info("Stopping subscription: {} stopped!", accinfo.getName());
              } else if (subsResult != null) {
                logger.warn("Stopping subscription: Unexpected subs status: {}", subsResult.getStatus());
              } else {
                logger.error("Stopping subscription: subsResult is null!");
              }
            } catch (Exception e) {
              logger.error("Stopping subscription: failed " + accinfo.getName(), e);
            }

            SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

            //then workspace can be cancellable
            boolean isOK = subscriptionDao.terminate(accinfo.getId(), WorkspaceStatus.STOPPED.name());

            WorkspaceTrans trans = new WorkspaceTrans();
            trans.setWorkspaceId(accinfo.getId());
            trans.setEvent(SubsEvent.SUBSCRIPTION_STOPPED);
            trans.setSuccessful(Boolean.TRUE);
            trans.setDescription(("Stopped! Final payment failed."));

            isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
            if (isOK) {
              isOK = workspaceDao.insertStatusHistory(accinfo.getId(), WorkspaceStatus.STOPPED.name());
            }

            if (isOK) {
              String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STOPPED, Map.of("fullName", accinfo.getFullName()));
              emailSender.send(Props.APP_EMAIL_SENDER, "The last notification for your subscription to inprice.", accinfo.getEmail(), message);

              affected++;
            }
          }
        }

        if (affected > 0) {
          logger.info("{} subscribed workspace in total stopped!", affected);
        } else {
          logger.info("No subscribed workspace to be stopped was found!");
        }

        if (affected > 0)
        	handle.commit();
        else
        	handle.rollback();

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }
      */
    } finally {
      TaskManager.stopTask(clazz);
    }
  }

}
