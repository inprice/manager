package io.inprice.scrapper.manager.scheduled.task;

import io.inprice.scrapper.manager.config.Config;
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

public class ProductPriceUpdater implements Task {

    private static final Logger log = new Logger(ProductPriceUpdater.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (Global.isProductUpdaterRunning) {
            log.warn("Product Price Updater is triggered but a previous publishers hasn't finished yet!");
            return;
        }

        synchronized (log) {
            Global.isProductUpdaterRunning = true;
        }

        log.info("Product Price Updater is starting...");

        while (! RedisClient.isProductPriceInfoSetEmpty() && Global.isApplicationRunning) {
            ProductPriceInfo ppi = RedisClient.pollProductPriceInfo();
            if (ppi == null) break;

            BigDecimal basePrice = ppi.getPrice();
            if (basePrice == null) {
                Product product = Products.findById(ppi.getProductId());
                if (product != null) {
                    basePrice = product.getPrice();
                } else {
                    basePrice = BigDecimal.ZERO;
                }
            }

            int position = 4; //average
            String minSeller = "NA";
            String maxSeller = "NA";
            BigDecimal minPrice = basePrice;
            BigDecimal avgPrice = basePrice;
            BigDecimal maxPrice = basePrice;

            final List<Link> linkList = Links.findActiveSellerPriceList(ppi);
            final int count = linkList.size();
            if (count > 0) {
                minSeller = linkList.get(0).getSeller();
                maxSeller = linkList.get(count-1).getSeller();
                minPrice = linkList.get(0).getPrice();
                maxPrice = linkList.get(count-1).getPrice();

                BigDecimal total = BigDecimal.ZERO;
                for (Link link: linkList) {
                    total.add(link.getPrice());
                }

                avgPrice = basePrice;
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    avgPrice = total.divide(BigDecimal.valueOf(count));
                }

                if (basePrice.compareTo(minPrice) < 0) {
                    position = 1; //the lowest
                } else if (basePrice.compareTo(minPrice) == 0) {
                    position = 2; //lower
                } else if (basePrice.compareTo(avgPrice) < 0) {
                    position = 3; //between lower and average
                } else if (basePrice.compareTo(maxPrice) < 0) {
                    position = 5; //between average and higher
                } else if (basePrice.compareTo(maxPrice) == 0) {
                    position = 6; //higher
                } else if (basePrice.compareTo(maxPrice) > 0) {
                    position = 7; //the highest
                }
            }
            Products.updatePrice(ppi, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice);
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
