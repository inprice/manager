package io.inprice.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.AppBase;

public class App extends AppBase {

	@JsonProperty("linkReviewPeriod")
	public int LINK_REVIEW_PERIOD = 30; //in minutes

}
