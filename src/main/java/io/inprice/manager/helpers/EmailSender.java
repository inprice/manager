package io.inprice.manager.helpers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

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

		Email emailFrom = new Email(emailData.getFrom());
		Email emailTo = new Email(emailData.getTo());
		Content emailContent = new Content("text/html", content);

		Mail mail = new Mail(emailFrom, emailData.getSubject(), emailTo, emailContent);
		
		String from = StringUtils.defaultIfEmpty(emailData.getFrom(), Props.getConfig().MAIL.SENDER);

		SendGrid sg = new SendGrid(Props.getConfig().MAIL.PASSWORD);
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
      Response response = sg.api(request);
      if (response.getStatusCode() >= 400) {
      	logger.warn("Problem sending email, to: {}, status: {}, body: {}", from, response.getStatusCode(), response.getBody());
      } else {
      	logger.info("Email sent to: {}", emailTo.getEmail());
      }
		} catch (IOException e) {
			logger.error("Failed to send email, to: {}, body: {}", from, content, e);
		}
	}
	
}
