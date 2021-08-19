package io.inprice.manager.config;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.BaseConfig;

public class Config extends BaseConfig {

	@SerializedName("mail")
	public Mail MAIL;

	@SerializedName("limits")
	public Limits LIMITS;

	@SerializedName("intervals")
	public Intervals INTERVALS;

	@SerializedName("queues")
	public Queues QUEUES;
	
}
