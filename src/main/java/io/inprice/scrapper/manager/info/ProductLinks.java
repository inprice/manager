package io.inprice.scrapper.manager.info;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductLinks implements Serializable {

  private static final long serialVersionUID = 373006989454188752L;

  private Long id;
  private BigDecimal price;
  private Long linkId;
  private BigDecimal linkPrice;
  private String seller;
  private String siteName;

}
