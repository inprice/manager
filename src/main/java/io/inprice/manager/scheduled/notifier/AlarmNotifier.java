package io.inprice.manager.scheduled.notifier;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.models.Alarm;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.AlarmDao;
import io.inprice.manager.helpers.EmailSender;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Checks alarm table to find alarmed products and links then send mails
 * 
 * @since 2021-06-21
 * @author mdpinar
 */
public class AlarmNotifier implements Task {

  private static final Logger logger = LoggerFactory.getLogger(AlarmNotifier.class);
  private final String clazz = getClass().getSimpleName();

  @Override
  public SchedulerDef getScheduler() {
  	return Props.getConfig().SCHEDULERS.ALARM_NOTIFIER;
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
        AlarmDao alarmDao = handle.attach(AlarmDao.class);
        
        List<Alarm> list = alarmDao.findTobeNotifiedLit();
        List<Long> idList = new ArrayList<>(list.size());

        if (CollectionUtils.isNotEmpty(list)) {
        	
        	Long lastWorkspaceId = list.get(0).getWorkspaceId();

        	List<Alarm> alarms = new ArrayList<>();
        	for (Alarm alarm: list) {
        		
        		idList.add(alarm.getId());
        		
        		if (lastWorkspaceId.equals(alarm.getWorkspaceId())) {
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
          logger.info("{} alarm notified!", idList.size());
        }

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }

    } finally {
      TaskManager.stopTask(clazz);
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
  		Map<String, String> dataMap = Map.of(
  			"topic", alarm.getTopic().name().substring(0, 1),
  			"name", alarm.getName(),
  			"status", alarm.getLastStatus(),
  			"amount", df.format(alarm.getLastAmount()),
  			"time", DateUtils.formatTimeStandart(alarm.getUpdatedAt())
			);

  		StringSubstitutor st = new StringSubstitutor(dataMap);
  		sb.append(st.replace(tableRow));
  	}
  	sb.append("</table>");
  	
    Map<String, Object> mailMap = Map.of(
    	"user", alarms.get(0).getUsername(),
    	"table", sb.toString()
		);
    
  	EmailSender.send(
			EmailData.builder()
  			.template(EmailTemplate.ALARM_NOTIFICATION)
  			.to(alarms.get(0).getEmail())
  			.subject("The entities you want to be notified when they change. ")
  			.data(mailMap)
  		.build()	
		);
  }

}
