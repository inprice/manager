package io.inprice.scrapper.manager.scheduled.updater;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.info.TimePeriod;
import io.inprice.scrapper.common.models.ProductPrice;
import io.inprice.scrapper.common.utils.DateUtils;
import io.inprice.scrapper.manager.config.Props;
import io.inprice.scrapper.manager.helpers.Global;
import io.inprice.scrapper.manager.helpers.RedisClient;
import io.inprice.scrapper.manager.info.ProductLinks;
import io.inprice.scrapper.manager.repository.ProductRepository;
import io.inprice.scrapper.manager.scheduled.Task;

public class PriceUpdater implements Task {

  private static final String NAME = "Product Price Updater";
  private static final BigDecimal BigDecimal_AHUNDRED = new BigDecimal(100);

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
      StringBuilder zeroizedSB = new StringBuilder("0");
      List<ProductPrice> ppList = new ArrayList<>();

      while (!RedisClient.isPriceChangingSetEmpty()) {
        Long productId = RedisClient.pollPriceChanging();
        if (productId != null) {

          List<ProductLinks> prodLinks = repository.getProductLinks(productId);
          if (prodLinks.size() > 0) {

            ProductLinks plFirst = prodLinks.get(0);
            ProductLinks plLast = prodLinks.get(prodLinks.size() - 1);

            ProductPrice pi = new ProductPrice();
            pi.setProductId(plFirst.getProductId());
            pi.setPrice(plFirst.getProductPrice());
            pi.setCompetitors(prodLinks.size());
            pi.setCompanyId(plFirst.getCompanyId());
            pi.setMinPlatform(plFirst.getSiteName());
            pi.setMinSeller(plFirst.getSeller());
            pi.setMinPrice(plFirst.getLinkPrice());
            pi.setAvgPrice(plFirst.getProductPrice());
            pi.setMaxPlatform(plLast.getSiteName());
            pi.setMaxSeller(plLast.getSeller());
            pi.setMaxPrice(plLast.getLinkPrice());
            pi.setSuggestedPrice(plFirst.getProductPrice());

            //finding total, ranking and rankingWith
            int ranking = 0;
            int rankingWith = 0;
            BigDecimal total = BigDecimal.ZERO;
            for (ProductLinks pl : prodLinks) {
              total = total.add(pl.getLinkPrice());
              if (pl.getProductPrice().compareTo(pl.getLinkPrice()) <= 0) {
                ranking = pl.getRanking();
              }
              if (pl.getProductPrice().compareTo(pl.getLinkPrice()) == 0) {
                rankingWith++;
              }
            }
            if (ranking == 0) {
              ranking = plLast.getRanking() + 1;
            }
            pi.setRanking(ranking);
            pi.setRankingWith(rankingWith);

            // finding avg price
            if (total.compareTo(BigDecimal.ZERO) > 0) {
              pi.setAvgPrice(total.divide(BigDecimal.valueOf(prodLinks.size()), 2, BigDecimal.ROUND_HALF_UP));
            }

            // setting diffs
            pi.setMinDiff(findDiff(pi.getPrice(), plFirst.getLinkPrice()));
            pi.setAvgDiff(findDiff(pi.getPrice(), pi.getAvgPrice()));
            pi.setMaxDiff(findDiff(pi.getPrice(), plLast.getLinkPrice()));

            //finding position
            pi.setPosition(3);// average
            BigDecimal basePrice = plFirst.getProductPrice();

            if (basePrice.compareTo(pi.getMinPrice()) <= 0) {
              pi.setPosition(1);
              pi.setMinPlatform("Yours");
              pi.setMinSeller("You");
              pi.setMinPrice(plFirst.getProductPrice());
              pi.setMinDiff(BigDecimal.ZERO);
            } else if (basePrice.compareTo(pi.getAvgPrice()) < 0) {
              pi.setPosition(2);
            } else if (basePrice.compareTo(pi.getMaxPrice()) < 0) {
              pi.setPosition(4);
            } else {
              pi.setPosition(5);
              pi.setMaxPlatform("Yours");
              pi.setMaxSeller("You");
              pi.setMaxPrice(plLast.getProductPrice());
              pi.setMaxDiff(BigDecimal.ZERO);
            }

            ppList.add(pi);
          } else { // product and product_price relation must be removed since there is no price info
            zeroizedSB.append(",");
            zeroizedSB.append(productId);
          }
          log.info(NAME + " is completed for Id: {}", productId);
        }
      }

      if (ppList.size() > 0) {
        boolean result = repository.updatePrice(ppList, zeroizedSB.toString());
        if (result) {
          log.info("Prices of {} products have been updated!", ppList.size());
        } else {
          log.warn("An error occurred during updating products' prices!");
        }
      } else if (zeroizedSB.length() > 1) {
        boolean result = repository.updatePrice(null, zeroizedSB.toString());
        if (result) {
          log.info("{} products' price info is zeroized.", StringUtils.countMatches(zeroizedSB.toString(), ","));
        } else {
          log.warn("An error occurred during zeroizing products' price info!");
        }
      } else {
        log.info("No product price is updated!");
      }

    } catch (Exception e) {
      log.error("Failed to update product price!", e);
    } finally {
      Global.setTaskRunningStatus(getClass().getSimpleName(), false);
    }
  }

  private BigDecimal findDiff(BigDecimal first, BigDecimal second) {
    BigDecimal result = BigDecimal_AHUNDRED;

    if (first.compareTo(BigDecimal.ZERO) > 0 && second.compareTo(BigDecimal.ZERO) > 0) {
      result = second.divide(first, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(BigDecimal_AHUNDRED).setScale(2);
    }

    return result;
  }

}
