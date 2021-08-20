package io.inprice.manager.config;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.common.helpers.JsonConverter;

/**
 * If finds, opens config.json on root first
 * else will search for it in classpath!
 * 
 * since 2021-08-18
 * @author mdpinar
 */
public class Props {

  private static final Logger logger = LoggerFactory.getLogger(Props.class);
	
  private static Config config;

  public static synchronized Config getConfig() {
  	if (config == null) {

  		String text = null;
  		try {

  			//checks if there is a config file on root
  			File file = new File("./config.json");
  			if (file.exists()) {
  				text = new String(Files.readAllBytes(Paths.get("./config.json")));
  			}

  			//if not, then search for it in classpath!
  			if (text == null) {
	    		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
	    		try (InputStream is = classloader.getResourceAsStream("config.json")) {
	    			if (is != null) text = new String(is.readAllBytes());
	    		}
  			}

  		} catch (Exception e) {
  			logger.error("Failed to load config file", e);
  		}
  		if (text != null) {
  			config = JsonConverter.fromJson(text, Config.class);
  		} else {
  			logger.error("There is no config.json neither in classpath nor root!");
  			java.lang.System.exit(-1);
  		}
  	}
		return config;
	}
  
}
