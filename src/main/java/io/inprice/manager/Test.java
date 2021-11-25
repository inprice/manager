package io.inprice.manager;

import java.util.HashMap;
import java.util.Map;

public class Test {

	public static void main(String[] args) {
		Map<String, Long> brandMap = new HashMap<>();
		addNewValues(brandMap);
		System.out.println(brandMap.size());
	}

	private static void addNewValues(Map<String, Long> brandMap) {
		brandMap.put("Ahmet", 1L);
		brandMap.put("Mehmet", 2L);
		brandMap.put("Hasan", 3L);
	}

}
