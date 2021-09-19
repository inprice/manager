package io.inprice.manager;

import java.util.Map;

import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.manager.helpers.EmailSender;

public class EmailTest {
	
  public static void main(String[] args) {
  	final String email = "dumlupinar01@gmail.com";
  	final String APP_WEB_URL = "https://inprice.io";
  	final String APP_EMAIL_SENDER = System.getenv().getOrDefault("APP_EMAIL_SENDER", "support@inprice.io");

  	Map<String, Object> mailMap = Map.of(
	  	"workspace", "Deneme firması",
	  	"user", "Mahmut Bey",
	  	"plan", "Professional Plan",
	  	"subsRenewalAt", "2021-04-12 18:10:21",
	    "invoiceUrl", APP_WEB_URL + "/sdlkjadda/sadfkjsadfasf/sfmsadkfklsad/sdlkalksdfs/sadflklmasdlfas-ssdf/ssdfsdf/gdfdfgew/wrewerwer.pdf"
    );

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