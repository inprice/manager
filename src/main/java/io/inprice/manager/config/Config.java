package io.inprice.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.BaseConfig;

public class Config extends BaseConfig {

	@JsonProperty("mail")
	public Mail MAIL;

	@JsonProperty("limits")
	public Limits LIMITS;

	@JsonProperty("queues")
	public Queues QUEUES;

	@JsonProperty("schedules")
	public Schedules SCHEDULES;
	
}
