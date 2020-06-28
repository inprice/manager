package io.inprice.scrapper.manager.scheduled.publisher;

import java.util.List;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.RabbitMQ;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.JsonConverter;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.meta.CompetitorStatus;
import io.inprice.scrapper.common.models.Competitor;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Props;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.CompetitorRepository;
import io.inprice.scrapper.manager.scheduled.Task;

/**
 * This is a class containing common functions used by all the publishers. Since
 * quartz needs to create publisher classes through their default (without args)
 * constructors, in each publisher, please add a default constructor referencing
 * this class's constructors.
 *
 * @author mdpinar
 */
public abstract class AbstractCompetitorPublisher implements Task {

  private static final Logger log = LoggerFactory.getLogger("CompetitorHandlerTask");
  static final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);

  abstract CompetitorStatus getStatus();
  abstract String getTimePeriodStatement();
  abstract String getMQRoutingKey();

  @Override
  public void run() {
    if (Global.isTaskRunning(getStatus().name())) {
      log.warn("{} competitor handler is already triggered and hasn't finished yet!", getStatus());
      return;
    }

    try {
      long startTime = System.currentTimeMillis();
      Global.setTaskRunningStatus(getStatus().name(), true);

      int counter = 0;
      List<Competitor> competitors = getCompetitors();

      while (competitors.size() > 0) {
        counter += competitors.size();

        handleCompetitors(competitors);
        setLastCheckTime(competitors);

        if (competitors.size() >= Props.DB_FETCH_LIMIT()) {
          try {
            Thread.sleep(Props.WAITING_TIME_FOR_FETCHING_COMPETITORS());
          } catch (InterruptedException ignored) {
          }
          competitors = getCompetitors();
        } else {
          competitors.clear();
        }
      }

      if (counter > 0) {
        log.info("{} competitor(s) is handled successfully. Number: {}, Time: {}", getStatus(),
            counter, (System.currentTimeMillis() - startTime));
      }

    } catch (Exception e) {
      log.error(String.format("Failed to completed %s task!", getStatus()), e);
    } finally {
      Global.setTaskRunningStatus(getStatus().name(), false);
    }

  }

  @Override
  public TimePeriod getTimePeriod() {
    return DateUtils.parseTimePeriod(this.getTimePeriodStatement());
  }

  void handleCompetitors(List<Competitor> competitorList) {
    Channel channel = RabbitMQ.openChannel();
    try {
      for (Competitor competitor : competitorList) {
        RabbitMQ.publishCompetitor(channel, getMQRoutingKey(), JsonConverter.toJson(competitor));
      }
    } catch (Exception e) {
      log.error(String.format("Failed to handle competitors!", getStatus()), e);
    }
    RabbitMQ.closeChannel(channel);
  }

  List<Competitor> getCompetitors() {
    return competitorRepository.getCompetitors(getStatus());
  }

  boolean isIncreaseRetry() {
    return false;
  }

  private void setLastCheckTime(List<Competitor> competitorList) {
    StringBuilder sb = new StringBuilder();
    for (Competitor competitor : competitorList) {
      if (sb.length() > 0)
        sb.append(",");
      sb.append(competitor.getId());
    }
    competitorRepository.setLastCheckTime(sb.toString(), isIncreaseRetry());
  }

}
