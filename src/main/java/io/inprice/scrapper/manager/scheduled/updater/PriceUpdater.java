package io.inprice.scrapper.manager.scheduled.updater;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Props;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.info.PriceUpdate;
import io.inprice.scrapper.manager.info.ProductLinks;
import io.inprice.scrapper.manager.repository.ProductRepository;
import io.inprice.scrapper.manager.scheduled.Task;

public class PriceUpdater implements Task {

  private static final String NAME = "Product Price Updater";

  private static final Logger log = LoggerFactory.getLogger(PriceUpdater.class);
  private static final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

  @Override
  public TimePeriod getTimePeriod() {
    return DateUtils.parseTimePeriod(Props.TIMING_FOR_UPDATING_PRODUCT_PRICES());
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
      int counter = 0;

      while (!RedisClient.isPriceChangingSetEmpty()) {
        Long productId = RedisClient.pollPriceChanging();
        if (productId != null) {

          List<ProductLinks> prodLinks = repository.getProductLinks(productId);
          if (prodLinks.size() > 0) {

            ProductLinks plMin = prodLinks.get(0);
            ProductLinks plMax = prodLinks.get(prodLinks.size() - 1);

            PriceUpdate pu = new PriceUpdate();
            pu.setBasePrice(plMin.getProductPrice());
            pu.setCompanyId(plMin.getCompanyId());
            pu.setLinksCount(prodLinks.size() - 1);
            pu.setMinPlatform(plMin.getSiteName());
            pu.setMinSeller(plMin.getSeller());
            pu.setMinPrice(plMin.getLinkPrice());
            pu.setAvgPrice(plMin.getProductPrice());
            pu.setMaxPlatform(plMax.getSiteName());
            pu.setMaxSeller(plMax.getSeller());
            pu.setMaxPrice(plMax.getLinkPrice());

            BigDecimal total = BigDecimal.ZERO;
            for (ProductLinks pl : prodLinks) {
              total = total.add(pl.getLinkPrice());
            }

            if (total.compareTo(BigDecimal.ZERO) > 0) {
              pu.setAvgPrice(total.divide(BigDecimal.valueOf(prodLinks.size()), 2, BigDecimal.ROUND_HALF_UP));
            }

            pu.setPosition(3);// average
            BigDecimal basePrice = plMin.getProductPrice();

            if (basePrice.compareTo(pu.getMinPrice()) <= 0) {
              pu.setPosition(1);
              pu.setMinSeller("You");
            } else if (basePrice.compareTo(pu.getAvgPrice()) < 0) {
              pu.setPosition(2);
            } else if (basePrice.compareTo(pu.getAvgPrice()) > 0 && basePrice.compareTo(pu.getMaxPrice()) < 0) {
              pu.setPosition(4);
            } else if (basePrice.compareTo(pu.getMaxPrice()) >= 0) {
              pu.setPosition(5);
              pu.setMaxSeller("You");
            }

            repository.updatePrice(pu);
            counter++;
          }
          log.info(NAME + " is completed for Id: {}", productId);
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
