package io.inprice.manager.config;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import io.inprice.common.config.ScheduleDef;

public class Schedules {

	@SerializedName("deletedMemberRemover")
	public ScheduleDef DELETED_MEMBER_REMOVER;

	@SerializedName("expiredFreeAccountStopper")
	public ScheduleDef EXPIRED_FREE_ACCOUNT_STOPPER;

	@SerializedName("expiredSubscriptionStopper")
	public ScheduleDef EXPIRED_SUBSCRIPTION_STOPPER;

	@SerializedName("pendingCheckoutCloser")
	public ScheduleDef PENDING_CHECKOUT_CLOSER;

	@SerializedName("alarmNotifier")
	public ScheduleDef ALARM_NOTIFIER;
	
	@SerializedName("freeAccountExpirationReminder")
	public ScheduleDef FREE_ACCOUNT_EXPIRATION_REMINDER;

	@SerializedName("newlyAddedLinkPublisher")
	public ScheduleDef NEWLY_ADDED_LINK_PUBLISHER;

	@SerializedName("activeLinkPublishers")
	public List<ScheduleDef> ACTIVE_LINK_PUBLISHERS;

	@SerializedName("failedLinkPublishers")
	public List<ScheduleDef> FAILED_LINK_PUBLISHERS;
	
}
