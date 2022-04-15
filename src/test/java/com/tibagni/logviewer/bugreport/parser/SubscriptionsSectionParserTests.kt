package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SubscriptionsSectionParserTests {
  private lateinit var subscriptionSectionParser: SubscriptionsSectionParser

  @Before
  fun setUp() {
   subscriptionSectionParser = SubscriptionsSectionParser()
  }

  @Test
  fun testParseSubscriptionControllerProperties() {
    val bugreportText = "\n-------------------------------------------------------------------------------\n" +
        "DUMP OF SERVICE isub:\n" +
        "SubscriptionController:\n" +
        " mLastISubServiceRegTime=1647880551764\n" +
        " defaultSubId=9\n" +
        " defaultDataSubId=9\n" +
        " defaultVoiceSubId=9\n" +
        " defaultSmsSubId=9\n" +
        " defaultDataPhoneId=0\n" +
        " defaultVoicePhoneId=0\n" +
        " defaultSmsPhoneId=0\n" +
        " sSlotIndexToSubId[0]: subIds=0=[9]\n" +
        "++++++++++++++++++++++++++++++++\n" +
        "--------- 0.045s was the duration of dumpsys isub, ending at: 2022-03-21 15:21:57\n"

    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultSubId"))
    assertEquals("9", subscriptionsSection.subscriptionManagerProperties["defaultSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultDataSubId"))
    assertEquals("9", subscriptionsSection.subscriptionManagerProperties["defaultDataSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultVoiceSubId"))
    assertEquals("9", subscriptionsSection.subscriptionManagerProperties["defaultVoiceSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultSmsSubId"))
    assertEquals("9", subscriptionsSection.subscriptionManagerProperties["defaultSmsSubId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultDataPhoneId"))
    assertEquals("0", subscriptionsSection.subscriptionManagerProperties["defaultDataPhoneId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultVoicePhoneId"))
    assertEquals("0", subscriptionsSection.subscriptionManagerProperties["defaultVoicePhoneId"])

    assertTrue(subscriptionsSection.subscriptionManagerProperties.containsKey("defaultSmsPhoneId"))
    assertEquals("0", subscriptionsSection.subscriptionManagerProperties["defaultSmsPhoneId"])
  }

  @Test
  fun testParseActiveSubs() {
    val bugreportText = "\n-------------------------------------------------------------------------------\n" +
        "DUMP OF SERVICE isub:\n" +
        "SubscriptionController:\n" +
        "++++++++++++++++++++++++++++++++\n" +
        " ActiveSubInfoList:\n" +
        "  {id=9 iccId=891480000[****] simSlotIndex=0 carrierId=1839 displayName=Verizon carrierName=Verizon nameSource=0 iconTint=-16746133 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@1093f86 mcc=311 mnc=480 countryIso=us isEmbedded=false nativeAccessRules=null cardString=891480000[****] cardId=8 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[311480, 310590, 310890] hplmns=[311480] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: FF82050BF6BED1F152AC1A12DC83CACBAD401775161882872C6665FC5E15C8F2 pkg: null access: 0, cert: AE23A03436DF07B0CD70FE881CDA2EC1D21215D7B7B0CC68E67B67F5DF89526A pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "++++++++++++++++++++++++++++++++\n" +
        "--------- 0.045s was the duration of dumpsys isub, ending at: 2022-03-21 15:21:57\n"

    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertEquals(1, subscriptionsSection.activeSubscriptions.size)
  }

  @Test
  fun testParseAllSubs() {
    val bugreportText = "\n-------------------------------------------------------------------------------\n" +
        "DUMP OF SERVICE isub:\n" +
        "SubscriptionController:\n" +
        "++++++++++++++++++++++++++++++++\n" +
        " AllSubInfoList:\n" +
        "  {id=1 iccId=890126007[****] simSlotIndex=-1 carrierId=1949 displayName=Metro by T-Mobile carrierName=Metro by T-Mobile nameSource=0 iconTint=-16746133 number=[****] dataRoaming=1 iconBitmap=android.graphics.Bitmap@cb10393 mcc=310 mnc=260 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890126007[****] cardId=0 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310260, 310260, 310260] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: 92B5F8117FBD9BD5738FF168A4FA12CBE284BE834EDE1A7BB44DD8455BA15920 pkg: null access: 0, cert: 3D1A4BEF6EE7AF7D34D120E7B1AAC0DD245585DE6237CF100F68333AFACFF562 pkg: null access: 0, cert: 6892793FC413019D2DF609DFED7AF622D0F2D8FCF96EFA7E3FB87EEA34E10B93 pkg: null access: 0, cert: 7B68FD9D4E7610C9CB35FC0C6CC06EA04C6906E3DFA9F48F9A05460AF36BFFFC pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "  {id=2 iccId=890139000[****] simSlotIndex=-1 carrierId=2498 displayName=Optimum carrierName=Optimum nameSource=4 iconTint=-13408298 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@d277fd0 mcc=313 mnc=390 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890139000[****] cardId=1 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[310260, 313390] hplmns=[313390, 310260] subscriptionType=0 groupOwner=null carrierConfigAccessRules=null areUiccApplicationsEnabled=true}\n" +
        "  {id=3 iccId=890141022[****] simSlotIndex=-1 carrierId=2025 displayName=H2O carrierName=H2O nameSource=4 iconTint=-13615201 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@da2fbc9 mcc=310 mnc=410 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890141022[****] cardId=2 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310410, 310410, 310410] subscriptionType=0 groupOwner=null carrierConfigAccessRules=null areUiccApplicationsEnabled=true}\n" +
        "  {id=4 iccId=890124019[****] simSlotIndex=-1 carrierId=1 displayName=T-Mobile carrierName=Mint nameSource=3 iconTint=-8708190 number=[****] dataRoaming=1 iconBitmap=android.graphics.Bitmap@d92a2ce mcc=310 mnc=240 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890124019[****] cardId=3 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[310260] hplmns=[310260, 310490, 310260, 310490] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: 92B5F8117FBD9BD5738FF168A4FA12CBE284BE834EDE1A7BB44DD8455BA15920 pkg: null access: 0, cert: 3D1A4BEF6EE7AF7D34D120E7B1AAC0DD245585DE6237CF100F68333AFACFF562 pkg: null access: 0, cert: 6892793FC413019D2DF609DFED7AF622D0F2D8FCF96EFA7E3FB87EEA34E10B93 pkg: null access: 0, cert: 7B68FD9D4E7610C9CB35FC0C6CC06EA04C6906E3DFA9F48F9A05460AF36BFFFC pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "  {id=5 iccId=890126094[****] simSlotIndex=-1 carrierId=2071 displayName=T-Mobile carrierName=Wireless nameSource=3 iconTint=-4056997 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@e5e45ef mcc=310 mnc=260 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890126094[****] cardId=4 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310260, 310260, 310260] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: 92B5F8117FBD9BD5738FF168A4FA12CBE284BE834EDE1A7BB44DD8455BA15920 pkg: null access: 0, cert: 3D1A4BEF6EE7AF7D34D120E7B1AAC0DD245585DE6237CF100F68333AFACFF562 pkg: null access: 0, cert: 6892793FC413019D2DF609DFED7AF622D0F2D8FCF96EFA7E3FB87EEA34E10B93 pkg: null access: 0, cert: 7B68FD9D4E7610C9CB35FC0C6CC06EA04C6906E3DFA9F48F9A05460AF36BFFFC pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "  {id=6 iccId=890126009[****] simSlotIndex=-1 carrierId=2083 displayName=T-Mobile carrierName=Ultra nameSource=3 iconTint=-3851991 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@d4cc8fc mcc=310 mnc=260 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890126009[****] cardId=5 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310260, 310490, 310260, 310490] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: 92B5F8117FBD9BD5738FF168A4FA12CBE284BE834EDE1A7BB44DD8455BA15920 pkg: null access: 0, cert: 3D1A4BEF6EE7AF7D34D120E7B1AAC0DD245585DE6237CF100F68333AFACFF562 pkg: null access: 0, cert: 6892793FC413019D2DF609DFED7AF622D0F2D8FCF96EFA7E3FB87EEA34E10B93 pkg: null access: 0, cert: 7B68FD9D4E7610C9CB35FC0C6CC06EA04C6906E3DFA9F48F9A05460AF36BFFFC pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "  {id=7 iccId=890141022[****] simSlotIndex=-1 carrierId=2025 displayName=Pure Talk carrierName=Pure Talk nameSource=4 iconTint=-16746133 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@dd68b85 mcc=310 mnc=410 countryIso=us isEmbedded=false nativeAccessRules=null cardString=890141022[****] cardId=6 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310410, 310410, 310410] subscriptionType=0 groupOwner=null carrierConfigAccessRules=null areUiccApplicationsEnabled=true}\n" +
        "  {id=8 iccId=891960100[****] simSlotIndex=-1 carrierId=2067 displayName=T-Mobile carrierName=LycaMobile nameSource=3 iconTint=-16746133 number=[****] dataRoaming=1 iconBitmap=android.graphics.Bitmap@ba1dda mcc=310 mnc=260 countryIso=us isEmbedded=false nativeAccessRules=null cardString=891960100[****] cardId=7 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[] hplmns=[310260] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: 92B5F8117FBD9BD5738FF168A4FA12CBE284BE834EDE1A7BB44DD8455BA15920 pkg: null access: 0, cert: 3D1A4BEF6EE7AF7D34D120E7B1AAC0DD245585DE6237CF100F68333AFACFF562 pkg: null access: 0, cert: 6892793FC413019D2DF609DFED7AF622D0F2D8FCF96EFA7E3FB87EEA34E10B93 pkg: null access: 0, cert: 7B68FD9D4E7610C9CB35FC0C6CC06EA04C6906E3DFA9F48F9A05460AF36BFFFC pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "  {id=9 iccId=891480000[****] simSlotIndex=0 carrierId=1839 displayName=Verizon carrierName=Verizon nameSource=0 iconTint=-16746133 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@4f3520b mcc=311 mnc=480 countryIso=us isEmbedded=false nativeAccessRules=null cardString=891480000[****] cardId=8 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[311480, 310590, 310890] hplmns=[311480] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: FF82050BF6BED1F152AC1A12DC83CACBAD401775161882872C6665FC5E15C8F2 pkg: null access: 0, cert: AE23A03436DF07B0CD70FE881CDA2EC1D21215D7B7B0CC68E67B67F5DF89526A pkg: null access: 0] areUiccApplicationsEnabled=true}\n" +
        "++++++++++++++++++++++++++++++++\n" +
        "--------- 0.045s was the duration of dumpsys isub, ending at: 2022-03-21 15:21:57\n"

    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertEquals(9, subscriptionsSection.allSubscriptions.size)
  }

  @Test
  fun testParseSubsLogs() {
    val bugreportText = "\n-------------------------------------------------------------------------------\n" +
        "DUMP OF SERVICE isub:\n" +
        "SubscriptionController:\n" +
        "++++++++++++++++++++++++++++++++\n" +
        "2022-03-21T14:29:26.933 - [clearSubInfoRecord]+ iccId: slotIndex:0\n" +
        "2022-03-21T14:30:48.377 - [clearSubInfoRecord]+ iccId: slotIndex:0\n" +
        "2022-03-21T14:30:48.385 - [addSubInfoRecord]+ iccid: 890141022[****], slotIndex: 0, subscriptionType: 0\n" +
        "2022-03-21T14:30:48.418 - Active subscription info list changed. [{id=7 iccId=890141022[****] simSlotIndex=0 carrierId=-1 displayName=null carrierName= nameSource=0 iconTint=-16746133 number=null dataRoaming=0 iconBitmap=android.graphics.Bitmap@610db63 mcc=null mnc=null countryIso= isEmbedded=false nativeAccessRules=null cardString=890141022[****] cardId=6 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=null hplmns=null subscriptionType=0 groupOwner=null carrierConfigAccessRules=null areUiccApplicationsEnabled=true}]\n" +
        "2022-03-21T14:30:48.419 - [addSubInfoRecord] New record created: content://telephony/siminfo/7\n" +
        "2022-03-21T14:30:48.423 - slotIndex, subId combo is added to the map.\n" +
        "2022-03-21T14:30:48.423 - [addSubInfoRecord] sSlotIndexToSubIds.size=1 slotIndex=0 subId=7 defaultSubId=6 simCount=1\n" +
        "2022-03-21T14:30:48.423 - setting default fallback subid to 7\n" +
        "2022-03-21T14:30:48.423 - [setDefaultFallbackSubId] subId=7, subscriptionType=0\n" +
        "2022-03-21T14:30:48.423 - [setDefaultFallbackSubId] set sDefaultFallbackSubId=7\n" +
        "2022-03-21T14:30:48.425 - [sendDefaultChangedBroadcast] broadcast default subId changed phoneId=0 subId=7\n" +
        "2022-03-21T14:30:48.426 - [addSubInfoRecord] one sim set defaults to subId=7\n" +
        "2022-03-21T14:30:48.426 - [setDefaultDataSubId] num phones=1, subId=7\n" +
        "2022-03-21T14:30:48.427 - [setDefaultDataSubId] phoneId=0 subId=7 RAF=561157\n" +
        "2022-03-21T14:30:48.430 - [broadcastDefaultDataSubIdChanged] subId=7\n" +
        "2022-03-21T14:30:48.432 - [setDefaultSmsSubId] subId=7\n" +
        "2022-03-21T14:30:48.435 - [broadcastDefaultSmsSubIdChanged] subId=7\n" +
        "2022-03-21T14:30:48.436 - [setDefaultVoiceSubId] subId=7\n" +
        "2022-03-21T14:30:48.439 - [broadcastDefaultVoiceSubIdChanged] subId=7\n" +
        "2022-03-21T14:30:48.440 - [addSubInfoRecord] hashmap(0,7)\n" +
        "2022-03-21T14:30:48.465 - Active subscription info list changed. [{id=7 iccId=890141022[****] simSlotIndex=0 carrierId=-1 displayName=SIM1 carrierName= nameSource=0 iconTint=-16746133 number=null dataRoaming=0 iconBitmap=android.graphics.Bitmap@a1c4ea mcc=null mnc=null countryIso= isEmbedded=false nativeAccessRules=null cardString=890141022[****] cardId=6 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=null hplmns=null subscriptionType=0 groupOwner=null carrierConfigAccessRules=null areUiccApplicationsEnabled=true}]\n" +
        "2022-03-21T14:30:48.466 - [addSubInfoRecord] sim name = SIM1\n" +
        "2022-03-21T14:30:48.466 - [addSubInfoRecord]- info size=1\n" +
        "++++++++++++++++++++++++++++++++\n" +
        "--------- 0.045s was the duration of dumpsys isub, ending at: 2022-03-21 15:21:57\n"

    val subscriptionsSection = subscriptionSectionParser.parse("", bugreportText) as SubscriptionsSection
    assertEquals(23, subscriptionsSection.logs.lines().size)
  }

  @Test
  fun testParseSubscriptionInfo() {
    val subscriptionText = "  {id=9 iccId=891480000[****] simSlotIndex=0 carrierId=1839 displayName=Verizon Wireless carrierName=Verizon nameSource=0 iconTint=-16746133 number=[****] dataRoaming=0 iconBitmap=android.graphics.Bitmap@1093f86 mcc=311 mnc=480 countryIso=us isEmbedded=false nativeAccessRules=null cardString=891480000[****] cardId=8 isOpportunistic=false groupUUID=null isGroupDisabled=false profileClass=-1 ehplmns=[311480, 310590, 310890] hplmns=[311480] subscriptionType=0 groupOwner=null carrierConfigAccessRules=[cert: FF82050BF6BED1F152AC1A12DC83CACBAD401775161882872C6665FC5E15C8F2 pkg: null access: 0, cert: AE23A03436DF07B0CD70FE881CDA2EC1D21215D7B7B0CC68E67B67F5DF89526A pkg: null access: 0] areUiccApplicationsEnabled=true}"

    val subscriptionInfo = subscriptionSectionParser.parseSubscriptionInfo(subscriptionText)
    assertEquals(9, subscriptionInfo.id)
    assertEquals(311, subscriptionInfo.mcc)
    assertEquals(480, subscriptionInfo.mnc)
    assertEquals(1839, subscriptionInfo.carrierId)
    assertEquals(0, subscriptionInfo.nameSource)
    assertEquals("Verizon Wireless", subscriptionInfo.displayName)
    assertEquals("Verizon", subscriptionInfo.carrierName)
  }
}