package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.CarrierConfigSection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CarrierConfigSectionParserTests {
  private lateinit var carrierConfigSectionParser: CarrierConfigSectionParser

  companion object {
    const val CARRIER_CONFIG = "\nDUMP OF SERVICE carrier_config:\n" +
        "CarrierConfigLoader: com.android.phone.CarrierConfigLoader@eeebaef\n" +
        "Phone Id = 0\n" +
        "    Default Values from CarrierConfigManager : \n" +
        "            config1 = value1\n" +
        "            config2 = value2\n" +
        "            config3 = value3\n" +
        "\n" +
        "    mConfigFromDefaultApp : \n" +
        "            config2 = value2.2\n" +
        "            config4 = value4\n" +
        "            config5 = value5\n" +
        "            config6 = value6\n" +
        "\n" +
        "    mConfigFromOemApp : null \n" +
        "\n" +
        "    mConfigFromCarrierApp : null \n" +
        "\n" +
        "    mPersistentOverrideConfigs : null \n" +
        "\n" +
        "    mOverrideConfigs : null \n" +
        "\n" +
        "CarrierConfigLoadingLog=\n" +
        "2020-12-23T18:27:28.634 - Update config for phoneId: 0 simState: UNKNOWN\n" +
        "2020-12-23T18:27:28.662 - mHandler: 0 phoneId: 0\n" +
        "2020-12-23T18:27:42.878 - Update config for phoneId: 0 simState: LOADED\n" +
        "2020-12-23T18:27:42.998 - mHandler: 7 phoneId: 0\n" +
        "2020-12-23T18:27:43.096 - mHandler: 5 phoneId: 0\n" +
        "--------- 0.038s was the duration of dumpsys carrier_config, ending at: 2020-12-23 18:35:30\n" +
        "-------------------------------------------------------------------------------"
  }

  @Before
  fun setUp() {
    carrierConfigSectionParser = CarrierConfigSectionParser()
  }

  @Test
  fun testCarrierConfigParser() {
    val section = carrierConfigSectionParser.parse("", CARRIER_CONFIG) as CarrierConfigSection

    assertEquals(2, section.configs.size)
    assertEquals(3, section.configs["Default Values from CarrierConfigManager_0"]?.size)
    assertEquals(4, section.configs["mConfigFromDefaultApp_0"]?.size)

    assertTrue(section.configs["Default Values from CarrierConfigManager_0"]?.keys?.contains("config1") ?: false)
    assertTrue(section.configs["Default Values from CarrierConfigManager_0"]?.keys?.contains("config2") ?: false)
    assertTrue(section.configs["Default Values from CarrierConfigManager_0"]?.keys?.contains("config3") ?: false)

    assertTrue(section.configs["mConfigFromDefaultApp_0"]?.keys?.contains("config2") ?: false)
    assertTrue(section.configs["mConfigFromDefaultApp_0"]?.keys?.contains("config4") ?: false)
    assertTrue(section.configs["mConfigFromDefaultApp_0"]?.keys?.contains("config5") ?: false)
    assertTrue(section.configs["mConfigFromDefaultApp_0"]?.keys?.contains("config6") ?: false)
  }
}