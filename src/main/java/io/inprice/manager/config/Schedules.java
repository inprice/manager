package io.inprice.manager.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.ScheduleDef;

public class Schedules {

	@JsonProperty("deletedMemberRemover")
	public ScheduleDef DELETED_MEMBER_REMOVER;

	@JsonProperty("expiredFreeAccountStopper")
	public ScheduleDef EXPIRED_FREE_ACCOUNT_STOPPER;

	@JsonProperty("expiredSubscriptionStopper")
	public ScheduleDef EXPIRED_SUBSCRIPTION_STOPPER;

	@JsonProperty("pendingCheckoutCloser")
	public ScheduleDef PENDING_CHECKOUT_CLOSER;

	@JsonProperty("alarmNotifier")
	public ScheduleDef ALARM_NOTIFIER;
	
	@JsonProperty("freeAccountExpirationReminder")
	public ScheduleDef FREE_ACCOUNT_EXPIRATION_REMINDER;

	@JsonProperty("newlyAddedLinkPublisher")
	public ScheduleDef NEWLY_ADDED_LINK_PUBLISHER;

	@JsonProperty("tobeClassifiedLinkPublishers")
	public List<ScheduleDef> TOBE_CLASSIFIED_LINK_PUBLISHERS;

	@JsonProperty("activeLinkPublishers")
	public List<ScheduleDef> ACTIVE_LINK_PUBLISHERS;

	@JsonProperty("failedLinkPublishers")
	public List<ScheduleDef> FAILED_LINK_PUBLISHERS;
	
}
