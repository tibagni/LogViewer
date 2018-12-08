package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.log.LogTimestamp;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.util.CommonUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LogViewerPresenterTests {
  private static final String TEMP_FILE_NAME = "tempFilter";
  private static final String TEMP_FILE_EXT = ".tmp";

  private static final String TEST_SERIALIZED_FILTER = "Test,VGVzdA==,2,255:0:0";
  private static final String TEST_SERIALIZED_FILTER2 = "Test2,VGVzdA==,2,255:0:0";
  private static final String TEST_SERIALIZED_FILTER3 = "Test3,VGVzdA==,2,255:0:0";

  @Mock
  private LogViewerPreferences mockPrefs;
  @Mock
  private LogViewerView view;

  private LogViewerPresenter presenter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    presenter = new LogViewerPresenter(view, mockPrefs);
  }

  private File createTempFiltersFile() {
    try {
      File tempFile = File.createTempFile(TEMP_FILE_NAME, TEMP_FILE_EXT);

      BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
      writer.write(TEST_SERIALIZED_FILTER);
      writer.close();

      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException("Not possible to create temp file");
    }
  }

  @Test
  public void testInitNotLoadingLastFilter() {
    when(mockPrefs.shouldOpenLastFilter()).thenReturn(false);
    presenter.init();

    verify(mockPrefs, never()).getLastFilterPaths();
    verify(view, never()).configureFiltersList(any());
  }

  @Test
  public void testInitLoadingLastFilterNoFilterAvailable() {
    when(mockPrefs.shouldOpenLastFilter()).thenReturn(true);
    when(mockPrefs.getLastFilterPaths()).thenReturn(null);
    presenter.init();

    verify(view, never()).configureFiltersList(any());
  }

  @Test
  public void testInitLoadingLastFilter() {
    File filtersTempFile = createTempFiltersFile();
    when(mockPrefs.shouldOpenLastFilter()).thenReturn(true);
    when(mockPrefs.getLastFilterPaths()).thenReturn(new File[]{filtersTempFile});
    presenter.init();

    // Check that correct filter was loaded
    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view).configureFiltersList(argument.capture());

    List<Filter> loadedFilters = argument.getValue().get(filtersTempFile.getName());
    assertNotNull(loadedFilters);
    assertEquals(1, loadedFilters.size());
    assertEquals("Test", loadedFilters.get(0).getName());
    assertEquals(new Color(255, 0, 0), loadedFilters.get(0).getColor());

    filtersTempFile.delete();
  }

  @Test
  public void testAddFilter() throws FilterException {
    final String testGroup = "testGroup";

    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    presenter.addFilterForTests(testGroup, toAdd);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view).configureFiltersList(argument.capture());
    verify(view).showUnsavedFilterIndication(testGroup);

    Map<String, List<Filter>> filtersMap = argument.getValue();
    assertEquals(1, filtersMap.size());
    assertEquals(1, filtersMap.get(testGroup).size());
  }

  @Test
  public void testRemoveOneFilter() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);

    // First we add 2 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(2)).configureFiltersList(argument.capture());
    verify(view, atLeastOnce()).showUnsavedFilterIndication(testGroup);

    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(1, resultFilters.size());
    assertEquals(2, resultFilters.get(testGroup).size());

    // Now we remove the first filter
    presenter.removeFilters(testGroup, new int[]{0});
    // times refers to all times the method was called (2 for add + 1 for remove now)
    verify(view, times(3)).configureFiltersList(argument.capture());

    // And check the it was, in fact, removed
    resultFilters = argument.getValue();
    assertEquals(1, resultFilters.get(testGroup).size());

    // Verify that the other filter remains
    assertEquals("Test2", resultFilters.get(testGroup).get(0).getName());
  }

  @Test
  public void testRemoveTwoFilters() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);
    presenter.addFilterForTests(testGroup, toAdd3);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(3)).configureFiltersList(argument.capture());
    verify(view, atLeastOnce()).showUnsavedFilterIndication(testGroup);

    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());

    // Now we remove the first filter
    presenter.removeFilters(testGroup, new int[]{0, 1});
    // times refers to all times the method was called (3 for add + 1 for remove now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And check that it was, in fact, removed
    resultFilters = argument.getValue();
    assertEquals(1, resultFilters.get(testGroup).size());

    // Verify that the other filter remains
    assertEquals("Test3", resultFilters.get(testGroup).get(0).getName());
  }

  @Test
  public void testReorderFilters() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);
    presenter.addFilterForTests(testGroup, toAdd3);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(2).getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(testGroup, 2, 1);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(2).getName());
  }

  @Test
  public void testReorderFilters2() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);
    presenter.addFilterForTests(testGroup, toAdd3);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(2).getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(testGroup, 2, 0);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test3", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(2).getName());
  }

  @Test
  public void testReorderFilters3() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);
    presenter.addFilterForTests(testGroup, toAdd3);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(2).getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(testGroup, 0, 2);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test2", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(2).getName());
  }

  @Test
  public void testReorderFilters4() throws FilterException {
    final String testGroup = "testGroup";
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilterForTests(testGroup, toAdd);
    presenter.addFilterForTests(testGroup, toAdd2);
    presenter.addFilterForTests(testGroup, toAdd3);

    ArgumentCaptor<Map<String, List<Filter>>> argument = ArgumentCaptor.forClass(Map.class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Map<String, List<Filter>> resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test2", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(2).getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(testGroup, 0, 3);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.get(testGroup).size());
    assertEquals("Test2", resultFilters.get(testGroup).get(0).getName());
    assertEquals("Test3", resultFilters.get(testGroup).get(1).getName());
    assertEquals("Test", resultFilters.get(testGroup).get(2).getName());
  }

  @Test
  public void testFinishingSaveChanges() throws FilterException {
    final String testGroup = "testGroup";
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilterForTests(testGroup, filter);

    when(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CONFIRMED);
    presenter.finishing();

    verify(view).showSaveFilters(testGroup);
    verify(view).finish();
  }

  @Test
  public void testFinishingDontSaveChanges() throws FilterException {
    final String testGroup = "testGroup";
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilterForTests(testGroup, filter);

    when(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.REJECTED);
    presenter.finishing();

    verify(view, never()).showSaveFilters(testGroup);
    verify(view).finish();
  }

  @Test
  public void testFinishingCancelChanges() throws FilterException {
    final String testGroup = "testGroup";
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilterForTests(testGroup, filter);

    when(view.showAskToSaveFilterDialog(testGroup)).thenReturn(LogViewer.UserSelection.CANCELLED);
    presenter.finishing();

    verify(view, never()).showSaveFilters(testGroup);
    verify(view, never()).finish();
  }

  @Test
  public void testFinishingNoChanges() {
    presenter.finishing();

    verify(view, never()).showAskToSaveFilterDialog(any());
    verify(view, never()).showSaveFilters(any());
    verify(view).finish();
  }

  @Test
  public void testNavigateNextSingleFilter() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getNextFilteredLogForFilter(filter, -1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 0);
    assertEquals(1, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 2);
    assertEquals(0, actual);
    verify(view, times(1)).showNavigationNextOver();
  }

  @Test
  public void testNavigateNextMultipleFilters() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getNextFilteredLogForFilter(filter, -1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 0);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 2);
    assertEquals(3, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 3);
    assertEquals(5, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 5);
    assertEquals(0, actual);
    verify(view, times(1)).showNavigationNextOver();
  }

  @Test
  public void testNavigateNextMultipleFilters2() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getNextFilteredLogForFilter(filter, -1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 2);
    assertEquals(3, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 3);
    assertEquals(5, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(filter, 5);
    assertEquals(2, actual);
    verify(view, times(1)).showNavigationNextOver();
  }

  @Test
  public void testNavigatePrevSingleFilter() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getPrevFilteredLogForFilter(filter, -1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 2);
    assertEquals(1, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 0);
    assertEquals(2, actual);
    verify(view, times(1)).showNavigationPrevOver();
  }

  @Test
  public void testNavigatePrevMultipleFilters() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getPrevFilteredLogForFilter(filter, -1);
    assertEquals(5, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 5);
    assertEquals(3, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 3);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 2);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 0);
    assertEquals(5, actual);
    verify(view, times(1)).showNavigationPrevOver();
  }

  @Test
  public void testNavigatePrevMultipleFilters2() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Set 'unknown' stream allowed as we just want to test navigation (and default stream is unknown)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.UNKNOWN, true);

    int actual = presenter.getPrevFilteredLogForFilter(filter, -1);
    assertEquals(3, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 3);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 2);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(filter, 0);
    assertEquals(3, actual);
    verify(view, times(1)).showNavigationPrevOver();
  }

  @Test
  public void testAllowedStreamsSetNotAllowed() {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")
    });
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN), true);
    presenter.setStreamAllowed(LogStream.MAIN, false);
    presenter.setStreamAllowed(LogStream.EVENTS, false);
    presenter.setStreamAllowed(LogStream.RADIO, false);
    presenter.setStreamAllowed(LogStream.UNKNOWN, false);

    // 'showFilteredLogs' should be called 4 times (one for each time we called 'setStreamAllowed')
    // So we need to validate the output of all executions
    ArgumentCaptor<LogEntry[]> argument = ArgumentCaptor.forClass(LogEntry[].class);
    verify(view, times(4)).showFilteredLogs(argument.capture());

    List<LogEntry[]> allValues = argument.getAllValues();
    LogEntry[] filteredLogs = allValues.get(0);
    assertEquals(4, filteredLogs.length);

    filteredLogs = allValues.get(1);
    assertEquals(3, filteredLogs.length);

    filteredLogs = allValues.get(2);
    assertEquals(2, filteredLogs.length);

    filteredLogs = allValues.get(3);
    assertEquals(1, filteredLogs.length);
  }

  @Test
  public void testAllowedStreamsSetAllowed() {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp, "main")
    });
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.SYSTEM, true);
    presenter.setStreamAllowed(LogStream.MAIN, true);

    // 'showFilteredLogs' should be called 2 times (one for each time we called 'setStreamAllowed')
    // So we need to validate the output of all executions
    ArgumentCaptor<LogEntry[]> argument = ArgumentCaptor.forClass(LogEntry[].class);
    verify(view, times(2)).showFilteredLogs(argument.capture());

    List<LogEntry[]> allValues = argument.getAllValues();
    LogEntry[] filteredLogs = allValues.get(0);
    assertEquals(1, filteredLogs.length);

    filteredLogs = allValues.get(1);
    assertEquals(3, filteredLogs.length);
  }

  @Test
  public void testNavigateNextMultipleFiltersWithAllowedStreams() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
    });

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.MAIN, true);
    presenter.setStreamAllowed(LogStream.SYSTEM, true);

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

    // So we need to test the indices based on the above array, not the original one

    int actual = presenter.getNextFilteredLogForFilter(filter, -1);
    assertEquals(0, actual);

    actual = presenter.getNextFilteredLogForFilter(filter, 0);
    assertEquals(1, actual);

    actual = presenter.getNextFilteredLogForFilter(filter, 1);
    assertEquals(2, actual);
  }

  @Test
  public void testNavigateNextMultipleFiltersWithAllowedStreams2() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
    });

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.MAIN, true);
    presenter.setStreamAllowed(LogStream.SYSTEM, true);

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

    // So we need to test the indices based on the above array, not the original one

    int actual = presenter.getNextFilteredLogForFilter(filter, -1);
    assertEquals(1, actual);

    actual = presenter.getNextFilteredLogForFilter(filter, 1);
    assertEquals(2, actual);
  }

  @Test
  public void testNavigatePrevMultipleFiltersWithAllowedStreams() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
    });

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.MAIN, true);
    presenter.setStreamAllowed(LogStream.SYSTEM, true);

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

    // So we need to test the indices based on the above array, not the original one

    int actual = presenter.getPrevFilteredLogForFilter(filter, -1);
    assertEquals(2, actual);

    actual = presenter.getPrevFilteredLogForFilter(filter, 2);
    assertEquals(1, actual);

    actual = presenter.getPrevFilteredLogForFilter(filter, 1);
    assertEquals(0, actual);
  }

  @Test
  public void testNavigatePrevMultipleFiltersWithAllowedStreams2() throws FilterException {
    LogTimestamp timestamp = new LogTimestamp(10,
        12,
        22,
        32,
        50,
        264);

    presenter.setFilteredLogsForTesting(new LogEntry[]{
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "radio"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "events"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "bla"),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")
    });

    // This will make the filtered logs shown in UI to have only 3 entries
    // (2 for main and 1 for system)
    presenter.setAvailableStreamsForTesting(CommonUtils.setOf(LogStream.MAIN,
        LogStream.EVENTS, LogStream.RADIO, LogStream.SYSTEM, LogStream.UNKNOWN));
    presenter.setStreamAllowed(LogStream.MAIN, true);
    presenter.setStreamAllowed(LogStream.SYSTEM, true);

    List<Filter> filters = new ArrayList<>();
    Filter filter = new Filter("name", "ABCDeF", Color.black);
    filters.add(filter);
    presenter.setFiltersForTesting(filters);

    // Because of the allowed streams we set earlier, the filters shown on UI should be something like below:
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABeF log Test Log", LogLevel.INFO, timestamp, "main"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "system"),
    //LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp, "main")

    // So we need to test the indices based on the above array, not the original one

    int actual = presenter.getPrevFilteredLogForFilter(filter, -1);
    assertEquals(2, actual);

    actual = presenter.getPrevFilteredLogForFilter(filter, 2);
    assertEquals(1, actual);

    actual = presenter.getPrevFilteredLogForFilter(filter, 1);
    assertEquals(2, actual);
  }
}
