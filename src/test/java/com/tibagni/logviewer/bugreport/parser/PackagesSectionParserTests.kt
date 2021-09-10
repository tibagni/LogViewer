package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.PackagesSection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PackagesSectionParserTests {
  private lateinit var applicationPackagesSectionParser: ApplicationPackagesSectionParser
  private lateinit var systemHiddenPackagesSectionParser: SystemHiddenPackagesSectionParser

  companion object {
    const val APP_PACKAGES = "\nPackages:\n" +
        "  Package [com.android.internal.display.cutout.emulation.noCutout] (26b9e29):\n" +
        "    userId=10020\n" +
        "    pkg=Package{eb905ae com.android.internal.display.cutout.emulation.noCutout}\n" +
        "    codePath=/product/overlay/DisplayCutoutNoCutout\n" +
        "    resourcePath=/product/overlay/DisplayCutoutNoCutout\n" +
        "    legacyNativeLibraryDir=/product/overlay/DisplayCutoutNoCutout/lib\n" +
        "    primaryCpuAbi=null\n" +
        "    secondaryCpuAbi=null\n" +
        "    versionCode=1 minSdk=30 targetSdk=30\n" +
        "    versionName=1.0\n" +
        "    splits=[base]\n" +
        "    apkSigningVersion=3\n" +
        "    applicationInfo=ApplicationInfo{eb905ae com.android.internal.display.cutout.emulation.noCutout}\n" +
        "    flags=[ SYSTEM ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE PRODUCT PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]\n" +
        "    forceQueryable=false\n" +
        "    queriesPackages=[]\n" +
        "    dataDir=/data/user/0/com.android.internal.display.cutout.emulation.noCutout\n" +
        "    supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]\n" +
        "    timeStamp=2008-12-31 22:00:00\n" +
        "    firstInstallTime=2008-12-31 22:00:00\n" +
        "    lastUpdateTime=2008-12-31 22:00:00\n" +
        "    signatures=PackageSignatures{7169f4f version:3, signatures:[d7f1f224], past signatures:[]}\n" +
        "    installPermissionsFixed=false\n" +
        "    pkgFlags=[ SYSTEM ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    overlayTarget=android\n" +
        "    overlayCategory=com.android.internal.display_cutout_emulation\n" +
        "    User 0: ceDataInode=4022 installed=true hidden=false suspended=false distractionFlags=0 stopped=false notLaunched=false enabled=0 instant=false virtual=false\n" +
        "  Package [com.google.android.networkstack.tethering] (91a2c3a):\n" +
        "    userId=1073\n" +
        "    sharedUser=SharedUserSetting{f4616dc android.uid.networkstack/1073}\n" +
        "    pkg=Package{5f6f3e5 com.google.android.networkstack.tethering}\n" +
        "    codePath=/apex/com.android.tethering/priv-app/TetheringGoogle\n" +
        "    resourcePath=/apex/com.android.tethering/priv-app/TetheringGoogle\n" +
        "    legacyNativeLibraryDir=/apex/com.android.tethering/priv-app/TetheringGoogle/lib\n" +
        "    primaryCpuAbi=arm64-v8a\n" +
        "    secondaryCpuAbi=null\n" +
        "    versionCode=30 minSdk=30 targetSdk=29\n" +
        "    versionName=11-6970168\n" +
        "    splits=[base]\n" +
        "    apkSigningVersion=3\n" +
        "    applicationInfo=ApplicationInfo{5f6f3e5 com.google.android.networkstack.tethering}\n" +
        "    flags=[ SYSTEM HAS_CODE PERSISTENT ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE DEFAULT_TO_DEVICE_PROTECTED_STORAGE DIRECT_BOOT_AWARE PRIVILEGED PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]\n" +
        "    forceQueryable=false\n" +
        "    queriesPackages=[]\n" +
        "    dataDir=/data/user_de/0/com.google.android.networkstack.tethering\n" +
        "    supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]\n" +
        "    usesLibraries:\n" +
        "      android.test.base\n" +
        "    usesLibraryFiles:\n" +
        "      /system/framework/android.test.base.jar\n" +
        "    timeStamp=1969-12-31 21:00:00\n" +
        "    firstInstallTime=1969-12-31 21:00:00\n" +
        "    lastUpdateTime=1969-12-31 21:00:00\n" +
        "    signatures=PackageSignatures{93ceba version:3, signatures:[aab1d69d], past signatures:[]}\n" +
        "    installPermissionsFixed=true\n" +
        "    pkgFlags=[ SYSTEM HAS_CODE PERSISTENT ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    User 0: ceDataInode=4028 installed=true hidden=false suspended=false distractionFlags=0 stopped=false notLaunched=false enabled=0 instant=false virtual=false\n" +
        "    overlay paths:\n" +
        "      /product/overlay/NavigationBarModeGestural/NavigationBarModeGesturalOverlay.apk\n" +
        "      /product/overlay/TetheringOverlay.apk\n\n"
    const val APP_PACKAGES_MISSING_INFO = "\nPackages:\n" +
        "  Package [com.android.internal.display.cutout.emulation.noCutout] (26b9e29):\n" +
        "    userId=10020\n" +
        "  Package [com.google.android.networkstack.tethering] (91a2c3a):\n" +
        "    userId=1073\n\n"

    const val SYSTEM_PACKAGES = "\nHidden system packages:\n" +
        "  Package [com.android.vending] (70a7174):\n" +
        "    userId=10202\n" +
        "    pkg=Package{1bbd69d com.android.vending}\n" +
        "    codePath=/product/priv-app/Phonesky\n" +
        "    resourcePath=/product/priv-app/Phonesky\n" +
        "    legacyNativeLibraryDir=/product/priv-app/Phonesky/lib\n" +
        "    primaryCpuAbi=armeabi-v7a\n" +
        "    secondaryCpuAbi=null\n" +
        "    versionCode=82242510 minSdk=21 targetSdk=29\n" +
        "    versionName=22.4.25-21 [0] [PR] 337959405\n" +
        "    splits=[base]\n" +
        "    apkSigningVersion=0\n" +
        "    applicationInfo=ApplicationInfo{1bbd69d com.android.vending}\n" +
        "    flags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP RESTORE_ANY_VERSION ]\n" +
        "    privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE HAS_DOMAIN_URLS PARTIALLY_DIRECT_BOOT_AWARE PRIVILEGED PRODUCT PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]\n" +
        "    forceQueryable=false\n" +
        "    queriesPackages=[]\n" +
        "    queriesIntents=[Intent { act=android.intent.action.VIEW cat=[android.intent.category.BROWSABLE] dat=http://www.example.com/... }, Intent { act=android.support.customtabs.action.CustomTabsService }]\n" +
        "    dataDir=/data/user/0/com.android.vending\n" +
        "    supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]\n" +
        "    usesLibraries:\n" +
        "      android.test.base\n" +
        "    usesOptionalLibraries:\n" +
        "      org.apache.http.legacy\n" +
        "    timeStamp=2008-12-31 22:00:00\n" +
        "    firstInstallTime=2008-12-31 22:00:00\n" +
        "    lastUpdateTime=2008-12-31 22:00:00\n" +
        "    signatures=PackageSignatures{2cc6712 version:0, signatures:[], past signatures:[]}\n" +
        "    installPermissionsFixed=false\n" +
        "    pkgFlags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP RESTORE_ANY_VERSION ]\n" +
        "    declared permissions:\n" +
        "      com.android.vending.appdiscoveryservice.permission.ACCESS_APP_DISCOVERY_SERVICE: prot=normal\n" +
        "      com.android.vending.CHECK_LICENSE: prot=normal|instant\n" +
        "    install permissions:\n" +
        "      android.permission.REAL_GET_TASKS: granted=true\n" +
        "      android.permission.WRITE_SETTINGS: granted=true\n" +
        "    User 0: ceDataInode=0 installed=true hidden=false suspended=false distractionFlags=0 stopped=false notLaunched=false enabled=0 instant=false virtual=false\n" +
        "      gids=[3002, 3003, 3001, 3007]\n" +
        "  Package [com.google.android.webview] (25b8ee3):\n" +
        "    userId=10221\n" +
        "    pkg=Package{d1730e0 com.google.android.webview}\n" +
        "    codePath=/product/app/WebViewGoogle\n" +
        "    resourcePath=/product/app/WebViewGoogle\n" +
        "    legacyNativeLibraryDir=/product/app/WebViewGoogle/lib\n" +
        "    primaryCpuAbi=armeabi-v7a\n" +
        "    secondaryCpuAbi=arm64-v8a\n" +
        "    versionCode=424018533 minSdk=29 targetSdk=30\n" +
        "    versionName=86.0.4240.185\n" +
        "    splits=[base]\n" +
        "    apkSigningVersion=0\n" +
        "    applicationInfo=ApplicationInfo{d1730e0 com.google.android.webview}\n" +
        "    flags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE PRODUCT PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]\n" +
        "    forceQueryable=false\n" +
        "    queriesPackages=[]\n" +
        "    dataDir=/data/user/0/com.google.android.webview\n" +
        "    supportsScreens=[small, medium, large, xlarge, resizeable, anyDensity]\n" +
        "    usesStaticLibraries:\n" +
        "      com.google.android.trichromelibrary version:424018533\n" +
        "    timeStamp=2008-12-31 22:00:00\n" +
        "    firstInstallTime=2008-12-31 22:00:00\n" +
        "    lastUpdateTime=2008-12-31 22:00:00\n" +
        "    signatures=PackageSignatures{1043f99 version:0, signatures:[], past signatures:[]}\n" +
        "    installPermissionsFixed=false\n" +
        "    pkgFlags=[ SYSTEM HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP ]\n" +
        "    install permissions:\n" +
        "      android.permission.FOREGROUND_SERVICE: granted=true\n" +
        "      android.permission.INTERNET: granted=true\n" +
        "      android.permission.ACCESS_NETWORK_STATE: granted=true\n" +
        "    User 0: ceDataInode=0 installed=true hidden=false suspended=false distractionFlags=0 stopped=false notLaunched=false enabled=0 instant=false virtual=false\n" +
        "      gids=[3003]\n\n"
    const val SYSTEM_PACKAGES_MISSING_INFO = "\nHidden system packages:\n" +
        "  Package [com.android.vending] (70a7174):\n" +
        "    userId=10020\n" +
        "  Package [com.google.android.webview] (25b8ee3):\n" +
        "    userId=1073\n\n"
  }

  @Before
  fun setUp() {
    applicationPackagesSectionParser = ApplicationPackagesSectionParser()
    systemHiddenPackagesSectionParser = SystemHiddenPackagesSectionParser()
  }

  @Test
  fun testParseApplicationPackages() {
    val section = applicationPackagesSectionParser.parse("", APP_PACKAGES) as PackagesSection

    assertEquals(2, section.packages.size)
    assertEquals("Application Packages", section.sectionName)

    assertEquals("com.android.internal.display.cutout.emulation.noCutout", section.packages[0].packageName)
    assertEquals("/data/user/0/com.android.internal.display.cutout.emulation.noCutout", section.packages[0].dataDir)
    assertEquals("1", section.packages[0].versionCode)
    assertEquals("1.0", section.packages[0].versionName)
    assertFalse(section.packages[0].rawText.isEmpty())

    assertEquals("com.google.android.networkstack.tethering", section.packages[1].packageName)
    assertEquals("/data/user_de/0/com.google.android.networkstack.tethering", section.packages[1].dataDir)
    assertEquals("30", section.packages[1].versionCode)
    assertEquals("11-6970168", section.packages[1].versionName)
    assertFalse(section.packages[1].rawText.isEmpty())
  }

  @Test
  fun testParseApplicationPackagesWithMissingInfo() {
    val section = applicationPackagesSectionParser.parse("", APP_PACKAGES_MISSING_INFO) as PackagesSection

    assertEquals(2, section.packages.size)
    assertEquals("Application Packages", section.sectionName)

    assertEquals("com.android.internal.display.cutout.emulation.noCutout", section.packages[0].packageName)
    assertEquals("Not Found", section.packages[0].dataDir)
    assertEquals("Not Found", section.packages[0].versionCode)
    assertEquals("Not Found", section.packages[0].versionName)
    assertFalse(section.packages[0].rawText.isEmpty())

    assertEquals("com.google.android.networkstack.tethering", section.packages[1].packageName)
    assertEquals("Not Found", section.packages[1].dataDir)
    assertEquals("Not Found", section.packages[1].versionCode)
    assertEquals("Not Found", section.packages[1].versionName)
    assertFalse(section.packages[1].rawText.isEmpty())
  }

  @Test
  fun testParseApplicationPackagesInvalidBugreport() {
    val section = applicationPackagesSectionParser.parse("", "INVALID")

    assertNull(section)
  }

  @Test
  fun testParseSystemHiddenPackages() {
    val section = systemHiddenPackagesSectionParser.parse("", SYSTEM_PACKAGES) as PackagesSection

    assertEquals(2, section.packages.size)
    assertEquals("Hidden system packages", section.sectionName)

    assertEquals("com.android.vending", section.packages[0].packageName)
    assertEquals("/data/user/0/com.android.vending", section.packages[0].dataDir)
    assertEquals("82242510", section.packages[0].versionCode)
    assertEquals("22.4.25-21", section.packages[0].versionName)
    assertFalse(section.packages[0].rawText.isEmpty())

    assertEquals("com.google.android.webview", section.packages[1].packageName)
    assertEquals("/data/user/0/com.google.android.webview", section.packages[1].dataDir)
    assertEquals("424018533", section.packages[1].versionCode)
    assertEquals("86.0.4240.185", section.packages[1].versionName)
    assertFalse(section.packages[1].rawText.isEmpty())
  }

  @Test
  fun testParseSystemHiddenPackagesWithMissingInfo() {
    val section = systemHiddenPackagesSectionParser.parse("", SYSTEM_PACKAGES_MISSING_INFO) as PackagesSection

    assertEquals(2, section.packages.size)
    assertEquals("Hidden system packages", section.sectionName)

    assertEquals("com.android.vending", section.packages[0].packageName)
    assertEquals("Not Found", section.packages[0].dataDir)
    assertEquals("Not Found", section.packages[0].versionCode)
    assertEquals("Not Found", section.packages[0].versionName)
    assertFalse(section.packages[0].rawText.isEmpty())

    assertEquals("com.google.android.webview", section.packages[1].packageName)
    assertEquals("Not Found", section.packages[1].dataDir)
    assertEquals("Not Found", section.packages[1].versionCode)
    assertEquals("Not Found", section.packages[1].versionName)
    assertFalse(section.packages[1].rawText.isEmpty())
  }

  @Test
  fun testParseSystemHiddenPackagesInvalidBugreport() {
    val section = systemHiddenPackagesSectionParser.parse("", "INVALID")

    assertNull(section)
  }
}