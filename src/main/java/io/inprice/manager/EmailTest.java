package io.inprice.manager;

import java.util.HashMap;
import java.util.Map;

import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.manager.email.EmailSender;

public class EmailTest {
	
  public static void main(String[] args) {
  	final String email = "dumlupinar01@gmail.com";
  	final String APP_WEB_URL = "https://inprice.io";
  	final String APP_EMAIL_SENDER = System.getenv().getOrDefault("APP_EMAIL_SENDER", "support@inprice.io");

  	Map<String, Object> mailMap = new HashMap<>(5);
  	mailMap.put("account", "Deneme firması");
  	mailMap.put("user", "Mahmut Bey");
  	mailMap.put("plan", "Professional Plan");
  	mailMap.put("subsRenewalAt", "2021-04-12 18:10:21");
    mailMap.put("invoiceUrl", APP_WEB_URL + "/sdlkjadda/sadfkjsadfasf/sfmsadkfklsad/sdlkalksdfs/sadflklmasdlfas-ssdf/ssdfsdf/gdfdfgew/wrewerwer.pdf");

  	EmailSender.send(
			EmailData.builder()
  			.template(EmailTemplate.SUBSCRIPTION_STARTED)
  			.from(APP_EMAIL_SENDER)
  			.to(email)
  			.subject("About your invitation for Deneme firması at inprice.io")
  			.data(mailMap)
  		.build()	
		);
  }

}