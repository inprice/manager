package io.inprice.manager.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Limits {

	@JsonProperty("dbFetch")
	public int LINK_LIMIT_FETCHING_FROM_DB;

	@JsonProperty("waitBeforeNextFetch")
	public int WAIT_LIMIT_BEFORE_NEXT_FETCH;
	
}
