package io.inprice.manager.scheduled.modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.manager.helpers.Global;

/**
 * Stops SUBSCRIBED accounts after four days later from their subs renewal date expired.
 * Normally, StripeService in api project will handle this properly. 
 * However, a communication problem with stripe may occur and we do not want to miss an expired account.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class SubscribedAccountStopper implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(SubscribedAccountStopper.class);
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
      
      /*

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        AccountDao accountDao = handle.attach(AccountDao.class);
        List<AccountInfo> expiredAccountList = accountDao.findExpiredSubscriberAccountList();
        int affected = 0;

        if (expiredAccountList != null && expiredAccountList.size() > 0) {
          for (AccountInfo accinfo: expiredAccountList) {

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

            //then account can be cancellable
            boolean isOK = subscriptionDao.terminate(accinfo.getId(), AccountStatus.STOPPED.name());

            AccountTrans trans = new AccountTrans();
            trans.setAccountId(accinfo.getId());
            trans.setEvent(SubsEvent.SUBSCRIPTION_STOPPED);
            trans.setSuccessful(Boolean.TRUE);
            trans.setDescription(("Stopped! Final payment failed."));

            isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
            if (isOK) {
              isOK = accountDao.insertStatusHistory(accinfo.getId(), AccountStatus.STOPPED.name());
            }

            if (isOK) {
              Map<String, Object> dataMap = new HashMap<>(1);
              dataMap.put("user", accinfo.getEmail());
              String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STOPPED, dataMap);
              emailSender.send(Props.APP_EMAIL_SENDER, "The last notification for your subscription to inprice.", accinfo.getEmail(), message);

              affected++;
            }
          }
        }

        if (affected > 0) {
          logger.info("{} subscribed account in total stopped!", affected);
        } else {
          logger.info("No subscribed account to be stopped was found!");
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
      Global.stopTask(clazz);
    }
  }

}
