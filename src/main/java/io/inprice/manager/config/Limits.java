package io.inprice.manager.config;

import com.google.gson.annotations.SerializedName;

public class Limits {

	@SerializedName("dbFetch")
	public int LINK_LIMIT_FETCHING_FROM_DB;

	@SerializedName("waitBeforeNextFetch")
	public int WAIT_LIMIT_BEFORE_NEXT_FETCH;
	
}
