package io.inprice.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mail {

	@JsonProperty("sender")
	public String SENDER;

	@JsonProperty("password")
	public String PASSWORD;

}
