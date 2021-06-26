package io.inprice.manager.scheduled.notifier;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.models.Alarm;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.AlarmDao;
import io.inprice.manager.email.EmailSender;
import io.inprice.manager.helpers.Global;

/**
 * Checks alarm table to find alarmed groups and links then send mails
 * 
 * @since 2021-06-21
 * @author mdpinar
 */
public class AlarmNotifier implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(AlarmNotifier.class);

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
        AlarmDao alarmDao = handle.attach(AlarmDao.class);
        
        List<Alarm> list = alarmDao.findTobeNotifiedLit();
        List<Long> idList = new ArrayList<>(list.size());

        if (CollectionUtils.isNotEmpty(list)) {
        	
        	Long lastAccountId = list.get(0).getAccountId();

        	List<Alarm> alarms = new ArrayList<>();
        	for (Alarm alarm: list) {
        		
        		idList.add(alarm.getId());
        		
        		if (lastAccountId.equals(alarm.getAccountId())) {
        			alarms.add(alarm);
        		} else {
        			sendEmail(list);
            	list.clear();
        		}
        	}
          
          if (list.size() > 0) {
      			sendEmail(list);
          }
          
          alarmDao.setNotified(idList);
          log.info("{} alarm notified!", idList.size());
        }

      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }

    } finally {
      Global.stopTask(clazz);
    }
  }
  
  private void sendEmail(List<Alarm> alarms) {
  	//String format = alarms.get(0).getCurrencyFormat();
  	//if (StringUtils.isBlank(format)) format = "#,###0.00";
  	String format = "#,###0.00";
  	DecimalFormat df = new DecimalFormat(format);
  	
  	String tableHeader = null;
  	String tableRow = null;
 
		try {
			tableHeader = IOUtils.resourceToString("/templates/alarm/table-header.html", Charset.defaultCharset());
			tableRow = IOUtils.resourceToString("/templates/alarm/table-row.html", Charset.defaultCharset());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
  	StringBuilder sb = new StringBuilder(tableHeader);
  	for (Alarm alarm: alarms) {
  		Map<String, String> dataMap = new HashMap<>();
  		dataMap.put("topic", alarm.getTopic().name().substring(0, 1));
  		dataMap.put("name", alarm.getName());
  		dataMap.put("status", alarm.getLastStatus());
  		dataMap.put("amount", df.format(alarm.getLastAmount()));
  		dataMap.put("time", DateUtils.formatTimeStandart(alarm.getUpdatedAt()));

  		StringSubstitutor st = new StringSubstitutor(dataMap);
  		sb.append(st.replace(tableRow));
  	}
  	sb.append("</table>");
  	
    Map<String, Object> mailMap = new HashMap<>(3);
    mailMap.put("user", alarms.get(0).getUsername());
    mailMap.put("table", sb.toString());
    
  	EmailSender.send(
			EmailData.builder()
  			.template(EmailTemplate.ALARM_NOTIFICATION)
  			.from(Props.APP_EMAIL_SENDER)
  			.to(alarms.get(0).getEmail())
  			.subject("The entities you want to be notified when they change. ")
  			.data(mailMap)
  		.build()	
		);
  }

}
