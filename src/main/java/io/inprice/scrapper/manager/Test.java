package io.inprice.scrapper.manager;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Test {
  
  private static final BigDecimal BigDecimal_AHUNDRED = new BigDecimal(100);

  public static void main(String[] args) {
    BigDecimal first = new BigDecimal(2800);
    BigDecimal second= new BigDecimal(2790);
    BigDecimal result= second.divide(first, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE).multiply(BigDecimal_AHUNDRED).setScale(2);
    System.out.println(result);
  }

}