package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.common.config.Config;
import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.repository.Products;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductPriceUpdateTask implements Task {

    private static final Logger log = new Logger(ProductPriceUpdateTask.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (Global.isProductUpdaterRunning) {
            log.warn("Product Price Updater is triggered but a previous task hasn't finished yet!");
            return;
        }

        synchronized (log) {
            Global.isProductUpdaterRunning = true;
        }

        log.info("Product Price Updater is starting...");

        while (! RedisClient.isProductPriceInfoSetEmpty()) {
            ProductPriceInfo ppi = RedisClient.pollProductPriceInfo();
            if (ppi == null) break;

            final List<Link> linkList = Links.getSellerPriceList(ppi);
            final int count = linkList.size();
            if (count > 0) {
                String minSeller = linkList.get(0).getSeller();
                String maxSeller = linkList.get(count-1).getSeller();
                BigDecimal minPrice = linkList.get(0).getPrice();
                BigDecimal maxPrice = linkList.get(count-1).getPrice();

                BigDecimal total = BigDecimal.ZERO;
                for (Link link: linkList) {
                    total.add(link.getPrice());
                }

                BigDecimal avgPrice = ppi.getPrice();
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = total.divide(BigDecimal.valueOf(count));
                }

                int position = 3; //average

                if (ppi.getPrice().compareTo(minPrice) <= 0) {
                    position = 1; //the lowest
                } else if (ppi.getPrice().compareTo(avgPrice) < 0) {
                    position = 2; //lower
                } else if (ppi.getPrice().compareTo(avgPrice) == 0) {
                    //average
                } else if (ppi.getPrice().compareTo(maxPrice) < 0) {
                    position = 4; //higher
                } else if (ppi.getPrice().compareTo(maxPrice) >= 0) {
                    position = 5; //the highest
                }

                Products.updatePrice(ppi, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice);
            }

        }

        Global.isProductUpdaterRunning = false;

        log.info("Product Price Updater is completed.");
    }

    @Override
    public Trigger getTrigger() {
        return TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(Config.CRONTAB_FOR_PRODUCT_PRICE_UPDATE)
            )
        .build();
    }

    @Override
    public JobDetail getJobDetail() {
        return JobBuilder.newJob(getClass()).build();
    }

}
