package io.inprice.manager.config;

import com.google.gson.annotations.SerializedName;

public class Intervals {

	@SerializedName("reminderForFreeAccounts")
	public String REMINDER_FOR_FREE_ACCOUNTS;

	@SerializedName("stoppingFreeAccounts")
	public String STOPPING_FREE_ACCOUNTS;

	@SerializedName("stoppingSubscribedAccounts")
	public String STOPPING_SUBSCRIBED_ACCOUNTS;

	@SerializedName("expiringPendingCheckouts")
	public String EXPIRING_PENDING_CHECKOUTS;
	
}
