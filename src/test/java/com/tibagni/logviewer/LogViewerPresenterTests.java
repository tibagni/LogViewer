package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import com.tibagni.logviewer.log.LogTimestamp;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
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
      File tempFile =  File.createTempFile(TEMP_FILE_NAME, TEMP_FILE_EXT);

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

    verify(mockPrefs, never()).getLastFilterPath();
    verify(view, never()).configureFiltersList(any());
  }

  @Test
  public void testInitLoadingLastFilterNoFilterAvailable() {
    when(mockPrefs.shouldOpenLastFilter()).thenReturn(true);
    when(mockPrefs.getLastFilterPath()).thenReturn(null);
    presenter.init();

    verify(view, never()).configureFiltersList(any());
  }

  @Test
  public void testInitLoadingLastFilter() {
    File filtersTempFile = createTempFiltersFile();
    when(mockPrefs.shouldOpenLastFilter()).thenReturn(true);
    when(mockPrefs.getLastFilterPath()).thenReturn(filtersTempFile);
    presenter.init();

    // Check that correct filter was loaded
    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view).configureFiltersList(argument.capture());

    Filter[] loadedFilters = argument.getValue();
    assertNotNull(loadedFilters);
    assertEquals(1, loadedFilters.length);
    assertEquals("Test", loadedFilters[0].getName());
    assertEquals(new Color(255, 0, 0), loadedFilters[0].getColor());

    filtersTempFile.delete();
  }

  @Test
  public void testAddFilter() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    presenter.addFilter(toAdd);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view).configureFiltersList(argument.capture());
    verify(view).showUnsavedTitle();

    Filter[] addedFilters = argument.getValue();
    assertEquals(1, addedFilters.length);
  }

  @Test
  public void testRemoveOneFilter() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);

    // First we add 2 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(2)).configureFiltersList(argument.capture());
    verify(view).showUnsavedTitle();

    Filter[] resultFilters = argument.getValue();
    assertEquals(2, resultFilters.length);

    // Now we remove the first filter
    presenter.removeFilters(new int[] {0});
    // times refers to all times the method was called (2 for add + 1 for remove now)
    verify(view, times(3)).configureFiltersList(argument.capture());

    // And check the it was, in fact, removed
    resultFilters = argument.getValue();
    assertEquals(1, resultFilters.length);

    // Verify that the other filter remains
    assertEquals("Test2", resultFilters[0].getName());
  }

  @Test
  public void testRemoveTwoFilters() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);
    presenter.addFilter(toAdd3);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(3)).configureFiltersList(argument.capture());
    verify(view).showUnsavedTitle();

    Filter[] resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);

    // Now we remove the first filter
    presenter.removeFilters(new int[] {0, 1});
    // times refers to all times the method was called (3 for add + 1 for remove now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And check that it was, in fact, removed
    resultFilters = argument.getValue();
    assertEquals(1, resultFilters.length);

    // Verify that the other filter remains
    assertEquals("Test3", resultFilters[0].getName());
  }

  @Test
  public void testReorderFilters() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);
    presenter.addFilter(toAdd3);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Filter[] resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test", resultFilters[0].getName());
    assertEquals("Test2", resultFilters[1].getName());
    assertEquals("Test3", resultFilters[2].getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(2, 1);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test", resultFilters[0].getName());
    assertEquals("Test3", resultFilters[1].getName());
    assertEquals("Test2", resultFilters[2].getName());
  }

  @Test
  public void testReorderFilters2() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);
    presenter.addFilter(toAdd3);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Filter[] resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test", resultFilters[0].getName());
    assertEquals("Test2", resultFilters[1].getName());
    assertEquals("Test3", resultFilters[2].getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(2, 0);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test3", resultFilters[0].getName());
    assertEquals("Test", resultFilters[1].getName());
    assertEquals("Test2", resultFilters[2].getName());
  }

  @Test
  public void testReorderFilters3() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);
    presenter.addFilter(toAdd3);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Filter[] resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test", resultFilters[0].getName());
    assertEquals("Test2", resultFilters[1].getName());
    assertEquals("Test3", resultFilters[2].getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(0, 2);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test2", resultFilters[0].getName());
    assertEquals("Test", resultFilters[1].getName());
    assertEquals("Test3", resultFilters[2].getName());
  }
  @Test
  public void testReorderFilters4() throws FilterException {
    Filter toAdd = Filter.createFromString(TEST_SERIALIZED_FILTER);
    Filter toAdd2 = Filter.createFromString(TEST_SERIALIZED_FILTER2);
    Filter toAdd3 = Filter.createFromString(TEST_SERIALIZED_FILTER3);

    // First we add 3 filters
    presenter.addFilter(toAdd);
    presenter.addFilter(toAdd2);
    presenter.addFilter(toAdd3);

    ArgumentCaptor<Filter[]> argument = ArgumentCaptor.forClass(Filter[].class);
    verify(view, times(3)).configureFiltersList(argument.capture());

    // Ensure the order is the added order
    Filter[] resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test", resultFilters[0].getName());
    assertEquals("Test2", resultFilters[1].getName());
    assertEquals("Test3", resultFilters[2].getName());

    // Now we exchange Test3 with Test2
    presenter.reorderFilters(0, 3);
    // times refers to all times the method was called (3 for add + 1 for reorder now)
    verify(view, times(4)).configureFiltersList(argument.capture());

    // And now check the new order
    resultFilters = argument.getValue();
    assertEquals(3, resultFilters.length);
    assertEquals("Test2", resultFilters[0].getName());
    assertEquals("Test3", resultFilters[1].getName());
    assertEquals("Test", resultFilters[2].getName());
  }

  @Test
  public void testFinishingSaveChanges() throws FilterException {
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilter(filter);

    when(view.showAskToSaveFilterDialog()).thenReturn(LogViewer.UserSelection.CONFIRMED);
    presenter.finishing();

    verify(view).showSaveFilter();
    verify(view).finish();
  }

  @Test
  public void testFinishingDontSaveChanges() throws FilterException {
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilter(filter);

    when(view.showAskToSaveFilterDialog()).thenReturn(LogViewer.UserSelection.REJECTED);
    presenter.finishing();

    verify(view, never()).showSaveFilter();
    verify(view).finish();
  }

  @Test
  public void testFinishingCancelChanges() throws FilterException {
    Filter filter = Filter.createFromString(TEST_SERIALIZED_FILTER);

    // Add a filter to simulate 'unsaved changes'
    presenter.addFilter(filter);

    when(view.showAskToSaveFilterDialog()).thenReturn(LogViewer.UserSelection.CANCELLED);
    presenter.finishing();

    verify(view, never()).showSaveFilter();
    verify(view, never()).finish();
  }

  @Test
  public void testFinishingNoChanges() {
    presenter.finishing();

    verify(view, never()).showAskToSaveFilterDialog();
    verify(view, never()).showSaveFilter();
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getNextFilteredLogForFilter(0, -1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 0);
    assertEquals(1, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 2);
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getNextFilteredLogForFilter(0, -1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 0);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 2);
    assertEquals(3, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 3);
    assertEquals(5, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 5);
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getNextFilteredLogForFilter(0, -1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 2);
    assertEquals(3, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 3);
    assertEquals(5, actual);
    verify(view, never()).showNavigationNextOver();

    actual = presenter.getNextFilteredLogForFilter(0, 5);
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getPrevFilteredLogForFilter(0, -1);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 2);
    assertEquals(1, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 1);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 0);
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getPrevFilteredLogForFilter(0, -1);
    assertEquals(5, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 5);
    assertEquals(3, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 3);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 2);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 0);
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

    presenter.setFilteredLogsForTesting(new LogEntry[] {
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : AB log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABDeF log Test Log", LogLevel.INFO, timestamp),
        new LogEntry("10-12 22:32:50.264  2646  2664 I test  : ABCD log Test Log", LogLevel.INFO, timestamp)
    });

    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("name", "ABCDeF", Color.black));
    presenter.setFiltersForTesting(filters);

    int actual = presenter.getPrevFilteredLogForFilter(0, -1);
    assertEquals(3, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 3);
    assertEquals(2, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 2);
    assertEquals(0, actual);
    verify(view, never()).showNavigationPrevOver();

    actual = presenter.getPrevFilteredLogForFilter(0, 0);
    assertEquals(3, actual);
    verify(view, times(1)).showNavigationPrevOver();
  }
}
