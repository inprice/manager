package io.inprice.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mail {

	@JsonProperty("username")
	public String USERNAME;

	@JsonProperty("password")
	public String PASSWORD;

	@JsonProperty("host")
	public String HOST;

	@JsonProperty("port")
	public Integer PORT;

	@JsonProperty("auth")
	public Boolean AUTH = Boolean.FALSE;

	@JsonProperty("tlsEnabled")
	public Boolean TLS_ENABLED = Boolean.FALSE;

	@JsonProperty("defaultSender")
	public String DEFAULT_SENDER;

}
