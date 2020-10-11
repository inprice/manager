package io.inprice.manager.scheduled;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.dao.MemberDao;
import io.inprice.manager.helpers.Global;

public class MemberRemover implements Task {

  private static final Logger log = LoggerFactory.getLogger(MemberRemover.class);

  private final String clazz = getClass().getSimpleName();

  @Override
  public TimePeriod getTimePeriod() {
    return DateUtils.parseTimePeriod(Props.TIMING_FOR_CLEANING_MEMBERS());
  }

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
        handle.inTransaction(transaction -> {
          MemberDao memberDao = transaction.attach(MemberDao.class);
          boolean isOK = memberDao.permenantlyDelete(UserStatus.DELETED.name());
          return isOK;
        });
      }
      
    } finally {
      log.info(clazz + " is completed.");
      Global.stopTask(clazz);
    }
  }
}
