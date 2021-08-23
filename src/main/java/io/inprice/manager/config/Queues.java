package io.inprice.manager.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.QueueDef;

public class Queues {

	@JsonProperty("sendingEmails")
	public QueueDef SENDING_EMAILS;

	@JsonProperty("statusChangingLinks")
	public QueueDef STATUS_CHANGING_LINKS;

	@JsonProperty("activeLinks")
	public List<QueueDef> ACTIVE_LINKS;
	
}
