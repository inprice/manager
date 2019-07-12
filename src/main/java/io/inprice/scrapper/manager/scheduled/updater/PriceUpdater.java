package io.inprice.scrapper.manager.scheduled.updater;

import io.inprice.scrapper.common.info.ProductPriceInfo;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.models.Product;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.repository.Links;
import io.inprice.scrapper.manager.repository.Products;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.math.BigDecimal;
import java.util.List;

public class PriceUpdater implements Task {

    private static final Logger log = new Logger(PriceUpdater.class);

    private final String crontab;

    public PriceUpdater(String crontab) {
        this.crontab = crontab;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        synchronized (log) {
            if (Global.isTaskRunning(getClass().getSimpleName())) {
                log.warn("Price Updater is already triggered and hasn't finished yet!");
                return;
            }
        }

        Global.setTaskRunningStatus(getClass().getSimpleName(), true);

        log.info("Product Price Updater is triggered");
        int counter = 0;

        while (! RedisClient.isPriceChangingSetEmpty()) {
            ProductPriceInfo ppi = RedisClient.pollPriceChanging();
            BigDecimal basePrice = ppi.getPrice();

            int position = 4; //average
            String minSeller = "NA";
            String maxSeller = "NA";
            BigDecimal minPrice = basePrice;
            BigDecimal avgPrice = basePrice;
            BigDecimal maxPrice = basePrice;

            final List<Link> linkList = Links.findActiveSellerPriceList(ppi.getProductId());

            final int count = linkList.size();
            if (count > 0) {
                minSeller = linkList.get(0).getSeller();
                maxSeller = linkList.get(count - 1).getSeller();
                minPrice = linkList.get(0).getPrice();
                maxPrice = linkList.get(count - 1).getPrice();

                BigDecimal total = BigDecimal.ZERO;
                for (Link link : linkList) {
                    total.add(link.getPrice());
                }

                avgPrice = basePrice;
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = total.divide(BigDecimal.valueOf(count));
                }

                if (basePrice.compareTo(minPrice) < 0) {            //the lowest
                    position = 1;
                } else if (basePrice.compareTo(minPrice) == 0) {    //lower
                    position = 2;
                } else if (basePrice.compareTo(avgPrice) < 0) {     //between lower and average
                    position = 3;
                } else if (basePrice.compareTo(maxPrice) < 0) {     //between average and higher
                    position = 5;
                } else if (basePrice.compareTo(maxPrice) == 0) {    //higher
                    position = 6;
                } else if (basePrice.compareTo(maxPrice) > 0) {     //the highest
                    position = 7;
                }
            }
            Products.updatePrice(ppi, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice);

            counter++;
            log.info("Product Price Updater is completed for Product: %d", ppi.getProductId());
        }

        if (counter > 0) {
            log.info("Prices of %d products have been updated!", counter);
        } else {
            log.info("No product price is updated!");
        }

        Global.setTaskRunningStatus(getClass().getSimpleName(), false);
    }

    @Override
    public Trigger getTrigger() {
        return TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule(crontab)
            )
        .build();
    }

    @Override
    public JobDetail getJobDetail() {
        return JobBuilder.newJob(getClass()).build();
    }

}
