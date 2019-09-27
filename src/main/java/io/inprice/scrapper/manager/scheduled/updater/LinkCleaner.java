package io.inprice.scrapper.manager.scheduled.updater;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.repository.LinkRepository;
import io.inprice.scrapper.manager.scheduled.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkCleaner implements Task {

    private static final String NAME = "Link Cleaner for Imported Products";

    private static final Logger log = LoggerFactory.getLogger(LinkCleaner.class);
    private static final LinkRepository repository = Beans.getSingleton(LinkRepository.class);
    private static final Properties props = Beans.getSingleton(Properties.class);

    @Override
    public TimePeriod getTimePeriod() {
        return DateUtils.parseTimePeriod(props.getTP_CleanLinks());
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
            repository.deleteImportedProductsLinks();
            log.info(NAME + " is completed.");

        } finally {
            Global.setTaskRunningStatus(getClass().getSimpleName(), false);
        }
    }
}
