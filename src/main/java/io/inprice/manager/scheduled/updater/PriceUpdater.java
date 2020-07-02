package io.inprice.manager.scheduled.updater;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.models.ProductPrice;
import io.inprice.common.utils.DateUtils;
import io.inprice.manager.config.Props;
import io.inprice.manager.helpers.Global;
import io.inprice.manager.helpers.RedisClient;
import io.inprice.manager.repository.ProductRepository;
import io.inprice.manager.scheduled.Task;

public class PriceUpdater implements Task {

  private static final String NAME = "Product Price Updater";

  private static final Logger log = LoggerFactory.getLogger(PriceUpdater.class);
  private static final Database db = Beans.getSingleton(Database.class);
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

    Connection con = null;

    try {
      Global.setTaskRunningStatus(getClass().getSimpleName(), true);

      con = db.getTransactionalConnection();

      log.info(NAME + " is triggered.");
      StringBuilder zeroizedSB = new StringBuilder("0");
      List<ProductPrice> ppList = new ArrayList<>();

      while (!RedisClient.isPriceChangingSetEmpty()) {
        Long productId = RedisClient.pollPriceChanging();
        if (productId != null) {
          ProductPrice pi = repository.getProductCompetitors(con, productId);
          if (pi != null) {
            ppList.add(pi);
          } else { // product and product_price relation must be removed since there is no price info
            zeroizedSB.append(",");
            zeroizedSB.append(productId);
          }
          log.info(NAME + " is completed for Id: {}", productId);
        }
      }

      if (ppList.size() > 0) {
        boolean result = repository.updatePrice(con, ppList, zeroizedSB.toString());
        if (result) {
          log.info("Prices of {} products have been updated!", ppList.size());
        } else {
          log.warn("An error occurred during updating products' prices!");
        }
      } else if (zeroizedSB.length() > 1) {
        boolean result = repository.updatePrice(con, null, zeroizedSB.toString());
        if (result) {
          log.info("{} products' price info is zeroized.", StringUtils.countMatches(zeroizedSB.toString(), ","));
        } else {
          log.warn("An error occurred during zeroizing products' price info!");
        }
      } else {
        log.info("No product price is updated!");
      }

    } catch (Exception e) {
      if (con != null)
        db.rollback(con);
      log.error("Failed to update product price!", e);
    } finally {
      if (con != null)
        db.close(con);
      Global.setTaskRunningStatus(getClass().getSimpleName(), false);
    }
  }

}
