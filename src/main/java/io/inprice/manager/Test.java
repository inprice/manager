package io.inprice.manager;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Test {

	public static void main(String[] args) {
		String s = java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(java.time.ZoneId.of("Europe/Berlin")).format(new java.util.Date().toInstant());
  	
  	System.out.println(s);
	}
	
}
