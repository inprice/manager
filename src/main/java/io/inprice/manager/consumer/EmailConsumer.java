package io.inprice.manager.consumer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.text.StringSubstitutor;
import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import io.inprice.common.config.SysProps;
import io.inprice.common.info.EmailData;
import io.inprice.manager.config.Props;
import io.inprice.manager.helpers.CssInliner;
import io.inprice.manager.helpers.RedisClient;

/**
 * Designed to manage all the sending emails around the platform
 * 
 * @author mdpinar
 * @since 2020-06-20
 */
public class EmailConsumer {

  private static final Logger logger = LoggerFactory.getLogger(EmailConsumer.class);

  private static RTopic topic;
  private static ExecutorService tPool;

  public static void start() {
  	tPool = Executors.newFixedThreadPool(SysProps.TPOOL_EMAIL_CONSUMER_CAPACITY);

  	topic = RedisClient.createTopic(SysProps.REDIS_SENDING_EMAILS_TOPIC);
    topic.addListener(EmailData.class, (channel, emailData) -> {

      tPool.submit(new Runnable() {

        @Override
        public void run() {
      		StringSubstitutor st = new StringSubstitutor(emailData.getData());
      		String content = st.replace(CssInliner.inlinedEmailTemplate(emailData.getTemplate()));

      		Email emailFrom = new Email(emailData.getFrom());
      		Email emailTo = new Email(emailData.getTo());
      		Content emailContent = new Content("text/html", content);

      		Mail mail = new Mail(emailFrom, emailData.getSubject(), emailTo, emailContent);

      		SendGrid sg = new SendGrid(Props.API_KEYS_SENDGRID);
      		Request request = new Request();
      		try {
      			request.setMethod(Method.POST);
      			request.setEndpoint("mail/send");
      			request.setBody(mail.build());
      			sg.api(request);
      			logger.info("Email sent to: {}", emailTo.getEmail());
      		} catch (IOException e) {
      			logger.error("Failed to send email, to: {}, body: {}", emailData.getFrom(), content, e);
      		}
        }

      });

    });
  }

  public static void stop() {
    try {
      topic.removeAllListeners();
      tPool.shutdown();
      tPool.awaitTermination(SysProps.WAITING_TIME_FOR_TERMINATION, TimeUnit.SECONDS);
    } catch (InterruptedException e) { }
  }
  
}
