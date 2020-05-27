package io.inprice.scrapper.manager.scheduled.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Props;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.LinkRepository;
import io.inprice.scrapper.manager.scheduled.Task;

public class LinkCleaner implements Task {

  private static final String NAME = "Link Cleaner for Imported Products";

  private static final Logger log = LoggerFactory.getLogger(LinkCleaner.class);
  private static final LinkRepository repository = Beans.getSingleton(LinkRepository.class);

  @Override
  public TimePeriod getTimePeriod() {
    return DateUtils.parseTimePeriod(Props.TIMING_FOR_CLEANING_LINKS());
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
      //TODO: silinen member lar icin uyarlanacak!
      // repository.deleteImportedProductsLinks();
      log.info(NAME + " is completed.");

    } finally {
      Global.setTaskRunningStatus(getClass().getSimpleName(), false);
    }
  }
}
