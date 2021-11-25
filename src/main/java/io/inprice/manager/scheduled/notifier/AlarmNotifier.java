package io.inprice.manager.scheduled.notifier;

import java.math.BigDecimal;
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

import io.inprice.common.config.SchedulerDef;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AlarmTopic;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.models.Alarm;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.AlarmDao;
import io.inprice.manager.helpers.EmailSender;
import io.inprice.manager.scheduled.Task;
import io.inprice.manager.scheduled.TaskManager;

/**
 * Checks alarm table to find alarmed products and links
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
        
        List<Alarm> list = alarmDao.findTobeAlarmedList();
        List<Long> productIdList = new ArrayList<>(list.size());
        List<Long> linkIdList = new ArrayList<>(list.size());

        if (CollectionUtils.isNotEmpty(list)) {
        	
        	Long lastWorkspaceId = list.get(0).getWorkspaceId();

        	List<Alarm> alarms = new ArrayList<>();
        	for (Alarm alarm: list) {

        		if (AlarmTopic.PRODUCT.equals(alarm.getTopic())) {
        			productIdList.add(alarm.getEntityId());
        		} else {
        			linkIdList.add(alarm.getEntityId());
        		}
        		
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

          handle.begin();
          if (CollectionUtils.isNotEmpty(productIdList)) alarmDao.setAlarmsOFF("product", productIdList);
          if (CollectionUtils.isNotEmpty(linkIdList)) alarmDao.setAlarmsOFF("link", linkIdList);
          handle.commit();
          logger.info("{} product and {} link alarm(s) notified!", productIdList.size(), linkIdList.size());
        }

      } catch (Exception e) {
        logger.error("Failed to trigger " + clazz , e);
      }

    } finally {
      TaskManager.stopTask(clazz);
    }
  }
  
  private void sendEmail(List<Alarm> alarms) {
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
  		Map<String, String> dataMap = new HashMap<>(7);
  		dataMap.put("entitySku", alarm.getEntitySku());
  		dataMap.put("entityName", alarm.getEntityName());
  		dataMap.put("alarmName", alarm.getName());
  		dataMap.put("when", alarm.getSubjectWhen().name());
  		dataMap.put("position", alarm.getEntityPosition().name());
  		dataMap.put("amount", df.format(findAmount(alarm)));
  		
  		StringSubstitutor st = new StringSubstitutor(dataMap);
  		sb.append(st.replace(tableRow));
  	}
  	sb.append("</table>");
  	
    Map<String, Object> mailMap = Map.of(
    	"fullName", alarms.get(0).getFullName(),
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
  
  private BigDecimal findAmount(Alarm alarm) {
		BigDecimal amount = alarm.getEntityPrice();
		if (AlarmTopic.PRODUCT.equals(alarm.getTopic())) {
			switch (alarm.getSubject()) {
				case MINIMUM: {
					amount = alarm.getEntityMinPrice();
					break;
				}
				case AVERAGE: {
					amount = alarm.getEntityAvgPrice();
					break;
				}
				case MAXIMUM: {
					amount = alarm.getEntityMaxPrice();
					break;
				}
				default: break;
			}
			
		}
		return amount;
  }

}
