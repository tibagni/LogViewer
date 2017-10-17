package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.log.LogTimestamp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ParserTests {

  LogParser logParser;

  @Before
  public void setUp() {
    logParser = new LogParser(null);
  }

  @Test
  public void testParseLines() {

  }

  @Test
  public void testFindLogLevel() {
    LogLevel verbose = logParser.findLogLevel("01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai");
    LogLevel debug = logParser.findLogLevel("01-06 20:46:26.091 821-2168/? D/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai");
    LogLevel info = logParser.findLogLevel("01-06 20:46:42.501 821-2810/? I/ActivityManager: Process com.voidcorporation.carimbaai (pid 25175) (adj 0) has died.");
    LogLevel warn = logParser.findLogLevel("01-06 20:46:39.491 821-1054/? W/ActivityManager:   Force finishing activity com.voidcorporation.carimbaai/.UserProfileActivity");
    LogLevel error = logParser.findLogLevel("01-06 20:46:39.481 25175-25175/? E/AndroidRuntime: FATAL EXCEPTION: main");

    LogLevel verbose2 = logParser.findLogLevel("10-12 22:33:46.839  3172  3172 V KeyguardStatusView: refresh statusview showing:true");
    LogLevel debug2 = logParser.findLogLevel("10-12 22:53:16.205  3172  3172 D StatusBarKeyguardViewManager: requestUnlock collapse=true");
    LogLevel info2 = logParser.findLogLevel("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines");
    LogLevel warn2 = logParser.findLogLevel("10-13 03:00:11.066   442  8037 W vold    : Failed to open none: No such file or directory");
    LogLevel error2 = logParser.findLogLevel("10-13 12:27:59.318 18114 18114 E ActivityThread: Activity com.facebook.katana.activity.FbMainTabActivity has leaked ServiceConnection X.8x6@fa849cf that was originally bound here");

    Assert.assertEquals(LogLevel.VERBOSE, verbose);
    Assert.assertEquals(LogLevel.DEBUG, debug);
    Assert.assertEquals(LogLevel.INFO, info);
    Assert.assertEquals(LogLevel.WARNING, warn);
    Assert.assertEquals(LogLevel.ERROR, error);

    Assert.assertEquals(LogLevel.VERBOSE, verbose2);
    Assert.assertEquals(LogLevel.DEBUG, debug2);
    Assert.assertEquals(LogLevel.INFO, info2);
    Assert.assertEquals(LogLevel.WARNING, warn2);
    Assert.assertEquals(LogLevel.ERROR, error2);
  }

  @Test
  public void testFindTimestamp() {
    LogTimestamp expected = new LogTimestamp(10, 12, 22, 32, 50, 264);
    LogTimestamp actual = logParser.findTimestamp("10-12 22:32:50.264  2646  2664 I chatty  : uid=1000(system) batterystats-sy expire 13 lines");

    LogTimestamp expected2 = new LogTimestamp(1, 6, 20, 46, 42, 501);
    LogTimestamp actual2 = logParser.findTimestamp("01-06 20:46:42.501 821-1054/? I/WindowState: WIN DEATH: Window{431586d0 u0 com.voidcorporation.carimbaai/com.voidcorporation.carimbaai.MainActivity}");


    Assert.assertEquals(expected, actual);
    Assert.assertEquals(expected2, actual2);
  }
}
