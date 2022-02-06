package io.inprice.manager;

import java.util.Random;

import org.apache.commons.lang3.StringUtils;

public class Test {
	
	private static final String P_INDICATOR = "-p-";

	public static void main1(String[] args) {
		String[] urls = {
				"https://www.trendyol.com/arnica/pika-et14410-toz-torbasiz-elektrikli-supurge-mor-2016st00146-p-367708?boutiqueId=588046&merchantId=107151",
				"https://www.trendyol.com/hoover/hleh10a2tcex-17-10-kg-wi-fi-bluetooth-baglantili-isi-pompali-kurutma-makinesi-p-151149212?boutiqueId=590567&merchantId=4513",
				"https://www.trendyol.com/pirelli/225-65r16c-112r-carrier-winter-t01-p-191056352?boutiqueId=590969&merchantId=205656"
		};
		for (String url : urls) {
			System.out.println(getSeller(url));
		}
	}

  private static String getSeller(String url) {
  	int start = url.indexOf(P_INDICATOR);
  	if (start > 0) {
  		int stop = url.indexOf("?", start);
	    if (stop > 0) {
	      return url.substring(start+P_INDICATOR.length(), stop);
	    }
  	}
    return "NA";
  }
	
	public static void main(String[] args) {
		System.out.println(maskSellerName("el"));
		System.out.println(maskSellerName("bir"));
		System.out.println(maskSellerName("ayakkabı"));
		System.out.println(maskSellerName("mustafa"));
		System.out.println(maskSellerName("HandyMan42"));
	}

	private static String maskSellerName(String str) {
    if (StringUtils.isBlank(str) || str.trim().length() == 1) return str;

    int limit = str.length()/2;
    return str.substring(0, limit+1) + "**";
	}

}
