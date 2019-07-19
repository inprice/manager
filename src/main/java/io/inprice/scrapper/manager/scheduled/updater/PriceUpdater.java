package io.inprice.scrapper.manager.scheduled.updater;

import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.manager.config.Config;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.info.ProductLinks;
import io.inprice.scrapper.manager.repository.Products;
import io.inprice.scrapper.manager.scheduled.Task;
import org.quartz.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PriceUpdater implements Task {

    private static final Logger log = new Logger(PriceUpdater.class);

    private final String crontab = Config.CRONTAB_FOR_PRODUCT_PRICE_UPDATE;;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        if (Global.isTaskRunning(getClass().getSimpleName())) {
            log.warn("Price Updater is already triggered and hasn't finished yet!");
            return;
        }

        try {
            Global.setTaskRunningStatus(getClass().getSimpleName(), true);

            log.info("Product Price Updater is triggered");
            int counter = 0;

            while (!RedisClient.isPriceChangingSetEmpty()) {
                Long productId = RedisClient.pollPriceChanging();
                if (productId != null) {

                    List<ProductLinks> prodLinks = Products.getProductLinks(productId);
                    if (prodLinks != null && prodLinks.size() > 0) {

                        ProductLinks plMin = prodLinks.get(0);
                        ProductLinks plMax = prodLinks.get(prodLinks.size() - 1);

                        String minSeller = plMin.getSiteName() + "/" + plMin.getSeller();
                        String maxSeller = plMax.getSiteName() + "/" + plMax.getSeller();

                        BigDecimal minPrice = plMin.getLinkPrice();
                        BigDecimal avgPrice = plMin.getPrice();
                        BigDecimal maxPrice = plMax.getLinkPrice();

                        BigDecimal total = BigDecimal.ZERO;
                        for (ProductLinks pl : prodLinks) {
                            total = total.add(pl.getLinkPrice());
                        }

                        if (total.compareTo(BigDecimal.ZERO) > 0) {
                            avgPrice = total.divide(BigDecimal.valueOf(prodLinks.size()), 2, BigDecimal.ROUND_HALF_UP);
                        }

                        int position = 4;//average
                        BigDecimal basePrice = plMin.getPrice();

                        if (basePrice.compareTo(minPrice) < 0) {            //the lowest
                            position = 1;
                            minSeller = "You";
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
                            maxSeller = "You";
                        }

                        Products.updatePrice(productId, basePrice, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice);
                        counter++;
                    }
                    log.info("Product Price Updater is completed for Product: %d", productId);
                }
            }

            if (counter > 0) {
                log.info("Prices of %d products have been updated!", counter);
            } else {
                log.info("No product price is updated!");
            }

        } finally {
            Global.setTaskRunningStatus(getClass().getSimpleName(), false);
        }
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
