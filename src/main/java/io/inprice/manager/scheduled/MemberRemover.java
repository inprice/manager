package io.inprice.manager.scheduled;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserStatus;
import io.inprice.manager.dao.MemberDao;
import io.inprice.manager.helpers.Global;

public class MemberRemover implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(MemberRemover.class);

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
        handle.inTransaction(transaction -> {
          MemberDao memberDao = transaction.attach(MemberDao.class);
          int affected = memberDao.permenantlyDelete(UserStatus.DELETED.name());
          if (affected > 0) {
            log.info("{} member(s) in total set to be DELETED!", affected);
          } else {
            log.info("No deleted member found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
