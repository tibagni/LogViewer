package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SubscriptionsSectionParserTests {
  private lateinit var subscriptionSectionParser: SubscriptionsSectionParser
  private lateinit var bugreportText: String

  @Before
  fun setUp() {
   subscriptionSectionParser = SubscriptionsSectionParser()
    bugreportText = "-------------------------------------------------------------------------------\n" +
        "DUMP OF SERVICE isub:\n" +
        "Service host process PID: 5300\n" +
        "Threads in use: 0/16\n" +
        "Client PIDs: 16507, 16467, 16341, 16304, 16143, 9597, 14642, 14980, 14643, 13537, 13156, 13002, 12016, 6145, 11239, 10993, 8257, 5677, 7781, 5334, 8291, 9279, 6720, 8930, 7090, 7697, 8252, 6724, 7628, 7172, 6527, 6708, 6619, 6460, 5207, 5181, 5166, 5563, 5096, 5002, 4646, 643\n" +
        "SubscriptionManagerService:\n" +
        "Active modem count=2\n" +
        "Logical SIM slot sub id mapping:\n" +
        "  Logical SIM slot 0: subId=2\n" +
        "  Logical SIM slot 1: subId=3\n" +
        "ICCID:\n" +
        "  slot 0: 898600451[****]\n" +
        "  slot 1: 898603152[****]\n" +
        "\n" +
        "defaultSubId=2\n" +
        "defaultVoiceSubId=2\n" +
        "defaultDataSubId=2\n" +
        "activeDataSubId=2\n" +
        "defaultSmsSubId=2\n" +
        "areAllSubscriptionsLoaded=true\n" +
        "\n" +
        "mSimState[0]=LOADED\n" +
        "mSimState[1]=LOADED\n" +
        "\n" +
        "Active subscriptions:\n" +
        "  [SubscriptionInfoInternal: id=2 iccId=898600451[****] simSlotIndex=0 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=1435 displayName=displayName carrierName=carrierName isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-16738131 number=[****] dataRoaming=0 mcc=460 mnc=02 ehplmns=46000 hplmns=46000,46000,46000 cardString=898600451[****] cardId=1 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460020592[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "  [SubscriptionInfoInternal: id=3 iccId=898603152[****] simSlotIndex=1 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=2237 displayName=displayName2 carrierName=carrierName2 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-15637249 number=[****] dataRoaming=0 mcc=460 mnc=11 ehplmns=46011,46003 hplmns= cardString=898603152[****] cardId=2 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460110268[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=850943 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "\n" +
        "All subscriptions:\n" +
        "  [SubscriptionInfoInternal: id=1 iccId=898601198[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 isRemovableEmbedded=0 carrierId=1436 displayName=displayName3 carrierName=carrierName3 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=CARRIER iconTint=-13457490 number=[****] dataRoaming=0 mcc=460 mnc=01 ehplmns=46001 hplmns=46001,46009,46001,46009 cardString=898601198[****] cardId=0 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460019551[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "  [SubscriptionInfoInternal: id=2 iccId=898600451[****] simSlotIndex=0 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=1435 displayName=displayName carrierName=carrierName isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-16738131 number=[****] dataRoaming=0 mcc=460 mnc=02 ehplmns=46000 hplmns=46000,46000,46000 cardString=898600451[****] cardId=1 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460020592[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "  [SubscriptionInfoInternal: id=3 iccId=898603152[****] simSlotIndex=1 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=2237 displayName=displayName2 carrierName=carrierName2 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-15637249 number=[****] dataRoaming=0 mcc=460 mnc=11 ehplmns=46011,46003 hplmns= cardString=898603152[****] cardId=2 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460110268[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=850943 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "\n" +
        "Embedded subscriptions: []\n" +
        "Opportunistic subscriptions: []\n" +
        "getAvailableSubscriptionInfoList: [2, 3]\n" +
        "getSelectableSubscriptionInfoList: [2, 3]\n" +
        "Euicc enabled=false\n" +
        "\n" +
        "Local log:\n" +
        "  2024-05-07T20:10:16.865182 - Created SubscriptionManagerService\n" +
        "  2024-05-07T20:10:16.915450 - Registered iSub service\n" +
        "  2024-05-07T20:10:21.457108 - markSubscriptionsInactive: slot 0\n" +
        "  2024-05-07T20:10:21.459639 - markSubscriptionsInactive: current mapping []\n" +
        "  2024-05-07T20:10:21.459678 - markSubscriptionsInactive: slot 1\n" +
        "  2024-05-07T20:10:21.460100 - markSubscriptionsInactive: current mapping []\n" +
        "  2024-05-07T20:10:32.127120 - updateSimState: slot 0 PIN_REQUIRED\n" +
        "  2024-05-07T20:10:32.176151 - updateSubscription: current mapping [slot 0: subId=2]\n" +
        "  2024-05-07T20:10:32.253272 - updateDefaultSubId: Default sub id updated from -1 to 2, phoneId=0\n" +
        "  2024-05-07T20:10:35.885069 - updateSimState: slot 1 READY\n" +
        "  2024-05-07T20:10:35.886603 - updateSubscription: current mapping [slot 0: subId=2, slot 1: subId=3]\n" +
        "  2024-05-07T20:10:51.437142 - updateSimState: slot 1 LOADED\n" +
        "  2024-05-07T20:10:51.437782 - updateSubscription: current mapping [slot 0: subId=2, slot 1: subId=3]\n" +
        "  2024-05-07T20:10:51.440247 - setDisplayNumber: subId=3, number=[****], calling package=com.android.phone\n" +
        "  2024-05-07T20:11:46.515296 - updateSimState: slot 0 READY\n" +
        "  2024-05-07T20:11:46.523830 - updateSubscription: current mapping [slot 0: subId=2, slot 1: subId=3]\n" +
        "  2024-05-07T20:11:49.071941 - updateSimState: slot 0 LOADED\n" +
        "  2024-05-07T20:11:49.072610 - updateSubscription: current mapping [slot 0: subId=2, slot 1: subId=3]\n" +
        "  2024-05-07T20:11:49.165975 - setDisplayNumber: subId=2, number=[****], calling package=com.android.phone\n" +
        "\n" +
        "SubscriptionDatabaseManager:\n" +
        "  All subscriptions:\n" +
        "    [SubscriptionInfoInternal: id=1 iccId=898601198[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 isRemovableEmbedded=0 carrierId=1436 displayName=displayName3 carrierName=carrierName3 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=CARRIER iconTint=-13457490 number=[****] dataRoaming=0 mcc=460 mnc=01 ehplmns=46001 hplmns=46001,46009,46001,46009 cardString=898601198[****] cardId=0 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460019551[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "    [SubscriptionInfoInternal: id=2 iccId=898600451[****] simSlotIndex=0 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=1435 displayName=displayName carrierName=carrierName isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-16738131 number=[****] dataRoaming=0 mcc=460 mnc=02 ehplmns=46000 hplmns=46000,46000,46000 cardString=898600451[****] cardId=1 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460020592[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "    [SubscriptionInfoInternal: id=3 iccId=898603152[****] simSlotIndex=1 portIndex=0 isEmbedded=0 isRemovableEmbedded=0 carrierId=2237 displayName=displayName2 carrierName=carrierName2 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=SIM_SPN iconTint=-15637249 number=[****] dataRoaming=0 mcc=460 mnc=11 ehplmns=46011,46003 hplmns= cardString=898603152[****] cardId=2 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460110268[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=850943 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=[****] userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]\n" +
        "  \n" +
        "  mAsyncMode=true\n" +
        "  mDatabaseInitialized=true\n" +
        "  mReadWriteLock=java.util.concurrent.locks.ReentrantReadWriteLock@2fed9d2[Write locks = 0, Read locks = 0]\n" +
        "  \n" +
        "  Local log:\n" +
        "2024-05-07T20:10:16.894355 - Loaded 3 records from the subscription database.\n" +
        "--------- 0.024s was the duration of dumpsys isub, ending at: 2024-05-07 20:11:51\n" +
        "-------------------------------------------------------------------------------"
  }

  @Test
  fun testParseIsubProperties() {
    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultSubId"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["defaultSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultDataSubId"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["defaultDataSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultVoiceSubId"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["defaultVoiceSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultSmsSubId"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["defaultSmsSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("areAllSubscriptionsLoaded"))
    assertEquals("true", subscriptionsSection.subscriptionManagerProperties["areAllSubscriptionsLoaded"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("Active modem count"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["Active modem count"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("Logical SIM slot 0: subId"))
    assertEquals("2", subscriptionsSection.subscriptionManagerProperties["Logical SIM slot 0: subId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("Logical SIM slot 1: subId"))
    assertEquals("3", subscriptionsSection.subscriptionManagerProperties["Logical SIM slot 1: subId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("mSimState[0]"))
    assertEquals("LOADED", subscriptionsSection.subscriptionManagerProperties["mSimState[0]"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("mSimState[1]"))
    assertEquals("LOADED", subscriptionsSection.subscriptionManagerProperties["mSimState[1]"])
  }

  @Test
  fun testParseActiveSubs() {
    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertEquals(2, subscriptionsSection.activeSubscriptions.size)
  }

  @Test
  fun testParseAllSubs() {
    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertEquals(3, subscriptionsSection.allSubscriptions.size)
  }

  @Test
  fun testParseSubscriptionInfo() {
    val subscriptionText = "[SubscriptionInfoInternal: id=1 iccId=898601198[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 isRemovableEmbedded=0 carrierId=1436 displayName=displayName3 carrierName=carrierName3 isOpportunistic=0 groupUuid= groupOwner= displayNameSource=CARRIER iconTint=-13457490 number=[****] dataRoaming=0 mcc=460 mnc=01 ehplmns=46001 hplmns=46001,46009,46001,46009 cardString=898601198[****] cardId=0 nativeAccessRules= carrierConfigAccessRules= countryIso=cn profileClass=-1 type=LOCAL_SIM areUiccApplicationsEnabled=1 usageSetting=DEFAULT isEnhanced4GModeEnabled=-1 isVideoTelephonyEnabled=-1 isWifiCallingEnabled=-1 isWifiCallingEnabledForRoaming=-1 wifiCallingMode=UNKNOWN wifiCallingModeForRoaming=UNKNOWN wifiCallingMDN=null enabledMobileDataPolicies= imsi=460019551[****] rcsUceEnabled=0 crossSimCallingEnabled=0 rcsConfig= allowedNetworkTypesForReasons=user=840583 deviceToDeviceStatusSharingPreference=0 isVoImsOptInEnabled=0 deviceToDeviceStatusSharingContacts= numberFromCarrier= numberFromIms=+123456789 userId=-10000 isSatelliteEnabled=-1 isGroupDisabled=false]"

    val subscriptionInfo = subscriptionSectionParser.parseSubscriptionInfo(subscriptionText)
    assertEquals(1, subscriptionInfo.id)
    assertEquals(460, subscriptionInfo.mcc)
    assertEquals(1, subscriptionInfo.mnc)
    assertEquals(1436, subscriptionInfo.carrierId)
    assertEquals("[****]", subscriptionInfo.number)
    assertEquals("+123456789", subscriptionInfo.imsNumber)
    assertEquals("", subscriptionInfo.carrierNumber)
    assertEquals("CARRIER", subscriptionInfo.nameSource)
    assertEquals("displayName3", subscriptionInfo.displayName)
    assertEquals("carrierName3", subscriptionInfo.carrierName)
  }
}