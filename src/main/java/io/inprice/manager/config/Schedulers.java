package io.inprice.manager.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.common.config.SchedulerDef;

public class Schedulers {

	@JsonProperty("deletedMemberRemover")
	public SchedulerDef DELETED_MEMBER_REMOVER;

	@JsonProperty("expiredFreeWorkspaceStopper")
	public SchedulerDef EXPIRED_FREE_WORKSPACE_STOPPER;

	@JsonProperty("expiredSubscriptionStopper")
	public SchedulerDef EXPIRED_SUBSCRIPTION_STOPPER;

	@JsonProperty("pendingCheckoutCloser")
	public SchedulerDef PENDING_CHECKOUT_CLOSER;

	@JsonProperty("alarmNotifier")
	public SchedulerDef ALARM_NOTIFIER;
	
	@JsonProperty("freeWorkspaceExpirationReminder")
	public SchedulerDef FREE_WORKSPACE_EXPIRATION_REMINDER;

	@JsonProperty("newlyAddedLinkPublisher")
	public SchedulerDef NEWLY_ADDED_LINK_PUBLISHER;

	@JsonProperty("tobeClassifiedLinkPublishers")
	public List<SchedulerDef> TOBE_CLASSIFIED_LINK_PUBLISHERS;

	@JsonProperty("activeLinkPublishers")
	public List<SchedulerDef> ACTIVE_LINK_PUBLISHERS;

	@JsonProperty("failedLinkPublishers")
	public List<SchedulerDef> FAILED_LINK_PUBLISHERS;
	
}
