package io.inprice.manager;

import java.math.BigDecimal;

public class Test {
  
  public static void main(String[] args) {
    BigDecimal total = new BigDecimal(0);
    total = total.add(new BigDecimal(18.9));
    BigDecimal result=total.divide(BigDecimal.valueOf(1), 2, BigDecimal.ROUND_HALF_UP);
    System.out.println(result);
  }

}