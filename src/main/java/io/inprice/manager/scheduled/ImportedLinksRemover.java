package io.inprice.manager.scheduled;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LinkStatus;
import io.inprice.manager.dao.LinkDao;
import io.inprice.manager.helpers.Global;

/**
 * Delete all the links added in to link table for importing urls
 * Those links are either in Imported status or failed three times.
 * 
 * @since 202-11-15
 * @author mdpinar
 */
public class ImportedLinksRemover implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ImportedLinksRemover.class);

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
        handle.inTransaction(transactional -> {
          LinkDao linkDao = transactional.attach(LinkDao.class);
          int affected = linkDao.deleteImportedLinks(LinkStatus.IMPORTED.name(), 3);
          if (affected > 0) {
            log.info("{} imported link(s) cleaned from link table!", affected);
          } else {
            log.info("No imported link to be deleted found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
