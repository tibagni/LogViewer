package com.tibagni.logviewer.log.parser;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ParserTests {

  private LogParser logParser;

  @Mock
  private LogReader reader;

  @Mock
  private ProgressReporter progressReporter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    logParser = new LogParser(reader, progressReporter);
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

  @Test
  public void testParseLogs() throws LogReaderException, LogParserException {
    final String TEST_LOG_LINE = "10-12 22:32:50.264  2646  2664 I test  : Test log Test Log";
    final Set<String> logNames = new HashSet<>();
    logNames.add("main");
    logNames.add("radio");
    logNames.add("system");
    logNames.add("events");
    final String[] expectedLogs = new String[] {
        TEST_LOG_LINE,
        TEST_LOG_LINE,
        TEST_LOG_LINE,
        TEST_LOG_LINE
    };

    when(reader.getAvailableLogsNames()).thenReturn(logNames);
    when(reader.get(any())).thenReturn(TEST_LOG_LINE);

    LogEntry[] entries = logParser.parseLogs();

    ArgumentCaptor<Integer> progressCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
    verify(progressReporter, times(7))
        .onProgress(progressCaptor.capture(), descriptionCaptor.capture());

    List<Integer> progresses = progressCaptor.getAllValues();
    List<String> descriptions = descriptionCaptor.getAllValues();

    assertTrue(progresses.contains(0));
    assertTrue(progresses.contains(15));
    assertTrue(progresses.contains(30));
    assertTrue(progresses.contains(45));
    assertTrue(progresses.contains(80));
    assertTrue(progresses.contains(95));
    assertTrue(progresses.contains(100));

    assertTrue(descriptions.contains("Reading main..."));
    assertTrue(descriptions.contains("Reading radio..."));
    assertTrue(descriptions.contains("Reading system..."));
    assertTrue(descriptions.contains("Reading events..."));
    assertTrue(descriptions.contains("Sorting..."));
    assertTrue(descriptions.contains("Setting index..."));
    assertTrue(descriptions.contains("Completed"));

    String[] actualLogs = Arrays.stream(entries)
        .map(it -> it.getLogText())
        .collect(Collectors.toList())
        .toArray(new String[0]);
    assertArrayEquals(expectedLogs, actualLogs);
  }

  @Test(expected = LogParserException.class)
  public void testParseInvalidLogs() throws LogReaderException, LogParserException {
    final String TEST_LOG_LINE = buildHugeLogPayload();
    final Set<String> logNames = new HashSet<>();
    logNames.add("bugreport");

    when(reader.getAvailableLogsNames()).thenReturn(logNames);
    when(reader.get(any())).thenReturn(TEST_LOG_LINE);

    logParser.parseLogs();
  }

  private String buildHugeLogPayload() {
    StringBuilder builder = new StringBuilder();
    builder.append("10-12 22:32:50.264  2646  2664 I test  : Test log Test Log");

    for (int i = 0; i < 1000; i++) {
      builder.append(" Test log Test Log test");
      builder.append(System.lineSeparator());
    }

    return builder.toString();
  }
}
