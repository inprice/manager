package io.inprice.manager.scheduled.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Beans;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.repository.MembershipRepository;
import io.inprice.manager.scheduled.Task;

public class MemebershipRemover implements Task {

  private static final String NAME = "competitor Cleaner for Imported Products";

  private static final Logger log = LoggerFactory.getLogger(MemebershipRemover.class);
  private static final MembershipRepository repository = Beans.getSingleton(MembershipRepository.class);

  @Override
  public TimePeriod getTimePeriod() {
    return DateUtils.parseTimePeriod(Props.TIMING_FOR_CLEANING_COMPETITORS());
  }

  @Override
  public void run() {
    if (Global.isTaskRunning(getClass().getSimpleName())) {
      log.warn(NAME + " is already triggered and hasn't finished yet!");
      return;
    }

    try {
      Global.setTaskRunningStatus(getClass().getSimpleName(), true);

      log.info(NAME + " is triggered.");
      repository.deletePermanently();
      log.info(NAME + " is completed.");

    } finally {
      Global.setTaskRunningStatus(getClass().getSimpleName(), false);
    }
  }
}
