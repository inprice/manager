package io.inprice.scrapper.manager.info;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PriceUpdate {

  private Long productId;
  private BigDecimal basePrice;
  private Integer position;
  private String minPlatform;
  private String minSeller;
  private BigDecimal minPrice;
  private BigDecimal avgPrice;
  private String maxPlatform;
  private String maxSeller;
  private BigDecimal maxPrice;
  private Integer linksCount;
  private Long companyId;

}