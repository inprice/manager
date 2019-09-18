package io.inprice.scrapper.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Properties {

	private static final Logger log = LoggerFactory.getLogger(Properties.class);

	private final java.util.Properties prop;

	Properties() {
		prop = new java.util.Properties();

		try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				log.error("Unable to find config.properties in class path!");
				return;
			}
			prop.load(input);
		} catch (IOException e) {
			log.error("Failed to load config.properties", e);
		}
	}

	public boolean isRunningForTests() {
		String runningAt = prop.getProperty("app.running-at", "prod");
		return runningAt.equals("test");
	}

	public String getDB_Host() {
		return prop.getProperty("db.host", "localhost");
	}

	public int getDB_Port() {
		return getOrDefault("db.port", 3306);
	}

	public String getDB_Properties() {
		return prop.getProperty("db.properties", "characterEncoding=utf8");
	}

	public String getDB_Database() {
		return prop.getProperty("db.database", "inprice");
	}

	public String getDB_Username() {
		return prop.getProperty("db.username", "root");
	}

	public String getDB_Password() {
		return prop.getProperty("db.password", "1234");
	}

	public int getDB_FetchLimit() {
		return getOrDefault("db.fetch-limit", 100);
	}

	public String getRedis_Host() {
		return prop.getProperty("redis.host", "localhost");
	}

	public int getRedis_Port() {
		return getOrDefault("redis.port", 6379);
	}

	public String getRedis_Password() {
		return prop.getProperty("redis.password", null);
	}

	public String getMQ_Host() {
		return prop.getProperty("mq.host", "localhost");
	}

	public int getMQ_Port() {
		return getOrDefault("mq.port", 5672);
	}

	public String getMQ_Username() {
		return prop.getProperty("mq.username", "guest");
	}

	public String getMQ_Password() {
		return prop.getProperty("mq.password", "guest");
	}

	public String getMQ_LinkExchange() {
		return prop.getProperty("mq.exchange.link", "links");
	}

	public String getMQ_ChangeExchange() {
		return prop.getProperty("mq.exchange.change", "changes");
	}

	public String getRoutingKey_NewLinks() {
		return prop.getProperty("routingKey.for.new-links", "new-links");
	}

	/*
	 * the difference between AVAILABLE_LINKS_QUEUE and TOBE_AVAILABLE_LINKS_QUEUE is that
	 * AVAILABLE_LINKS are already in AVAILABLE status
	 * TOBE_AVAILABLE_LINKS are in different status and are about to switch to AVAILABLE.
	 */
	public String getRoutingKey_AvailableLinks() {
		return prop.getProperty("routingKey.for.available-links", "available-links");
	}

	public String getRoutingKey_FailedLinks() {
		return prop.getProperty("routingKey.for.failed-links", "failed-links");
	}

	public String getRoutingKey_StatusChange() {
		return getQueue_StatusChange();
	}

	public String getQueue_PriceChange() {
		return prop.getProperty("queue.of.price-change", "price-change");
	}

	public String getQueue_DeletedLinks() {
		return prop.getProperty("queue.of.deleted-links", "deleted-links");
	}

	public String getQueue_StatusChange() {
		return prop.getProperty("queue.of.status-change", "status-change");
	}

	public String getQueue_TobeAvailableLinks() {
		return prop.getProperty("queue.of.tobe-available-links", "tobe-available-links");
	}

	public int getWTF_AwaitTermination() {
		return getOrDefault("wtf.await-termination", 30000);
	}

	public int getWTF_GettingLinksFromDB() {
		return getOrDefault("wtf.getting-links-from-db", 3000);
	}

	public int getRL_FailedLinksG1() {
		return getOrDefault("rl.failed-links.g1", 3);
	}

	public int getRL_FailedLinksG2() {
		return getOrDefault("rl.failed-links.g2", 5);
	}

	public int getRL_FailedLinksG3() {
		return getOrDefault("rl.failed-links.g3", 10);
	}

	public String getTP_NewLinks() {
		return prop.getProperty("tp.new-links", "3M");
	}

	public String getTP_SocketErrors() {
		return prop.getProperty("tp.socket-errors", "8M");
	}

	public String getTP_ResumedLinks() {
		return prop.getProperty("tp.resumed-links", "13M");
	}

	public String getTP_RenewedLinks() {
		return prop.getProperty("tp.renewed-links", "27M");
	}

	public String getTP_ImplementedLinks() {
		return prop.getProperty("tp.implemented-links", "33M");
	}

	public String getTP_ProductPriceUpdate() {
		return prop.getProperty("tp.product-price-update", "1H");
	}

	public String getTP_NetworkErrors() {
		return prop.getProperty("tp.network-errors", "3H");
	}

	public String getTP_AvailableLinks() {
		return prop.getProperty("tp.available-links", "6H");
	}

	public String getTP_NotAvailableLinks() {
		return prop.getProperty("tp.not-available-links", "8H");
	}

	private int getOrDefault(String key, int defauld) {
		String val = prop.getProperty(key, "" + defauld);
		return Integer.parseInt(val.trim());
	}

}
