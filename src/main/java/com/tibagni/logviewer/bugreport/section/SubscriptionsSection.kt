package com.tibagni.logviewer.bugreport.section

data class SubscriptionsSection(
  val subscriptionManagerProperties: Map<String, String>,
  val activeSubscriptions: List<SubscriptionInfo>,
  val allSubscriptions: List<SubscriptionInfo>,
  val logs: String
) : BugReportSection(SectionNames.SUBSCRIPTIONS)

data class SubscriptionInfo(
  val id: Int,
  val mcc: Int,
  val mnc: Int,
  val carrierId: Int,
  val number: String,
  val imsNumber: String,
  val carrierNumber: String,
  val displayName: String,
  val carrierName: String,
  val nameSource: String,
  val fullText: String
)