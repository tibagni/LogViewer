package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.SubscriptionInfo
import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubscriptionsSectionPanelTests {

  @Test
  fun testConstructWithEmptySubscriptionsSection() {
    val section = SubscriptionsSection(
      subscriptionManagerProperties = emptyMap(),
      activeSubscriptions = emptyList(),
      allSubscriptions = emptyList(),
      logs = ""
    )
    val panel = SubscriptionsSectionPanel(section)

    assertNotNull(panel)
    assertEquals(section.sectionName, panel.titleLabel.text)

    panel.addNotify()
    panel.removeNotify()
  }

  @Test
  fun testConstructWithPopulatedSubscriptionsSection() {
    val sub1 = SubscriptionInfo(
      id = 1,
      carrierId = 1435,
      displayName = "Verizon",
      carrierName = "Verizon Wireless",
      mcc = 311,
      mnc = 480,
      gid1 = "BAE0000000000000",
      isEmbedded = 0,
      profileClass = -1,
      number = "123",
      imsNumber = "456",
      carrierNumber = "789",
      nameSource = "SIM",
      fullText = "[SubscriptionInfoInternal: id=1]"
    )
    val sub2 = SubscriptionInfo(
      id = 2,
      carrierId = 2237,
      displayName = "T-Mobile",
      carrierName = "T-Mobile US",
      mcc = 310,
      mnc = 260,
      gid1 = "50",
      isEmbedded = 1,
      profileClass = 2,
      number = "987",
      imsNumber = "654",
      carrierNumber = "321",
      nameSource = "CARRIER",
      fullText = "[SubscriptionInfoInternal: id=2]"
    )

    val section = SubscriptionsSection(
      subscriptionManagerProperties = mapOf(
        "defaultSubId" to "1",
        "mSimState[0]" to "LOADED"
      ),
      activeSubscriptions = listOf(sub1),
      allSubscriptions = listOf(sub1, sub2),
      logs = "Sample subscription logs"
    )
    val panel = SubscriptionsSectionPanel(section)

    assertNotNull(panel)
    assertEquals(section.sectionName, panel.titleLabel.text)

    panel.addNotify()
    panel.removeNotify()
  }

  @Test
  fun testRepeatedNotifyLifecycle() {
    val section = SubscriptionsSection(
      subscriptionManagerProperties = mapOf("defaultSubId" to "1"),
      activeSubscriptions = emptyList(),
      allSubscriptions = emptyList(),
      logs = "logs"
    )
    val panel = SubscriptionsSectionPanel(section)

    panel.addNotify()
    panel.removeNotify()
    panel.addNotify()
    panel.removeNotify()

    assertEquals(section.sectionName, panel.titleLabel.text)
  }
}
