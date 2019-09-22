package io.inprice.scrapper.manager.scheduled.updater;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Properties;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.info.ProductLinks;
import io.inprice.scrapper.manager.repository.ProductRepository;
import io.inprice.scrapper.manager.scheduled.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class PriceUpdater implements Task {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdater.class);
    private static final ProductRepository repository = Beans.getSingleton(ProductRepository.class);
    private static final Properties props = Beans.getSingleton(Properties.class);

    @Override
    public TimePeriod getTimePeriod() {
        return DateUtils.parseTimePeriod(props.getTP_ProductPriceUpdate());
    }

    @Override
    public void run() {
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

                    List<ProductLinks> prodLinks = repository.getProductLinks(productId);
                    if (prodLinks.size() > 0) {

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

                        repository.updatePrice(productId, basePrice, position, minSeller, maxSeller, minPrice, avgPrice, maxPrice);
                        counter++;
                    }
                    log.info("Product Price Updater is completed for Product: {}", productId);
                }
            }

            if (counter > 0) {
                log.info("Prices of {} products have been updated!", counter);
            } else {
                log.info("No product price is updated!");
            }

        } finally {
            Global.setTaskRunningStatus(getClass().getSimpleName(), false);
        }
    }
}
