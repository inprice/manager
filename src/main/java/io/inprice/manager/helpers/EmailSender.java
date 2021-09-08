package io.inprice.manager.helpers;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.manager.config.Props;

public class EmailSender {

  private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
  
  private static Map<EmailTemplate, String> contentCacheMap = new HashMap<>(EmailTemplate.values().length);

  private static String header;
  private static String footer;
  
  static {
		try {
			header = IOUtils.resourceToString("/templates/fragment/header.html", Charset.defaultCharset());
			footer = IOUtils.resourceToString("/templates/fragment/footer.html", Charset.defaultCharset());
		} catch (Exception e) {
			logger.error("Fragments not found!", e);
		}
  }
	
	public static void send(EmailData emailData) {

		if (emailData.getTo().equals("demo@inprice.io")) {
			logger.info("Data for demo mail" + StringUtils.join(emailData.getData()));
			return;
		}

		String body = contentCacheMap.get(emailData.getTemplate());
		if (body == null) {
			try {
				body = IOUtils.resourceToString("/templates/" + emailData.getTemplate().getFileName(), Charset.defaultCharset());
				contentCacheMap.put(emailData.getTemplate(), body);
			} catch (Exception e) {
				logger.error("File not found!", e);
				return;
			}
		}
		
		//standard stylings
		emailData.getData().put("table-border", "border: 1px solid #ccc;"); 

		emailData.getData()
			.put("normal-button-style", 
  				"display: block; " +
  				"width: 200px; " +
  				"height: 25px; " +
  				"background: #4E9CAF; " +
  				"padding: 5px; " +
  				"text-align: center; " +
  				"border-radius: 3px; " +
  				"color: white; " +
  				"font-weight: bold; " +
  				"font-size: bold; " +
  				"line-height: 25px; " +
  				"font-size: 18px; " +
  				"text-decoration: none;"
  			);

		StringSubstitutor st = new StringSubstitutor(emailData.getData());
		String content = 
			st.replace(
				header +
				body +
				footer
			);

    Properties prop = System.getProperties();
    prop.put("mail.smtp.host", Props.getConfig().MAIL.HOST);
    prop.put("mail.smtp.port", Props.getConfig().MAIL.PORT);
    prop.put("mail.smtp.auth", Props.getConfig().MAIL.AUTH);
    prop.put("mail.smtp.starttls.enable", Props.getConfig().MAIL.TLS_ENABLED);
    
    Session session = Session.getDefaultInstance(prop, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(Props.getConfig().MAIL.USERNAME, Props.getConfig().MAIL.PASSWORD);
      }
    });

    String from = StringUtils.defaultIfBlank(emailData.getFrom(), Props.getConfig().MAIL.DEFAULT_SENDER);
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailData.getTo()));
			message.setSubject(emailData.getSubject());
			message.setContent(content, "text/html");

			Transport.send(message);
		} catch (MessagingException e) {
			logger.error("Failed to send email, to: {}, body: {}", from, content, e);
		}		
	}
	
}
