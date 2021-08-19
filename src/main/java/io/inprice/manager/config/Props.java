package io.inprice.manager.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.JsonConverter;

/**
 * since 2021-08-18
 * @author mdpinar
 *
 */
public class Props {

  private static final Logger logger = LoggerFactory.getLogger(Props.class);
	
  private static Config config;

  public static synchronized Config getConfig() {
  	if (config == null) {
  		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
  		try {
  			String text = new String(classloader.getResourceAsStream("config.json").readAllBytes());
  			config = JsonConverter.fromJson(text, Config.class);
  		} catch (IOException e) {
  			logger.error("Failed to load config file", e);
  		}
  	}
		return config;
	}
  
}
