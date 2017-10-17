package com.tibagni.ogviewer.filter;

import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterException;
import com.tibagni.logviewer.filter.Filters;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;

public class FilterTests {

  @Test
  public void singleSimpleFilterTest() throws FilterException {
    Filter filter = new Filter("", "filterText", Color.WHITE);
    LogEntry input[] = new LogEntry[] {
        new LogEntry("Log line 1", LogLevel.DEBUG, null),
        new LogEntry("Log line 2", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
        new LogEntry("Log line 3", LogLevel.DEBUG, null),
        new LogEntry("Log line 4", LogLevel.DEBUG, null),
        new LogEntry("Log line 5", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
        new LogEntry("Log line 6", LogLevel.DEBUG, null),
        new LogEntry("Log line 7", LogLevel.DEBUG, null),
        new LogEntry("Log line 8", LogLevel.DEBUG, null),
        new LogEntry("Log line 9", LogLevel.DEBUG, null),
        new LogEntry("Log line 10", LogLevel.DEBUG, null)
    };

    LogEntry filtered[] = filter.apply(input);

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void singleSimpleFilterTestCaseInsensitive() throws FilterException {
    Filter filter = new Filter("", "filterText", Color.WHITE);
    LogEntry input[] = new LogEntry[] {
        new LogEntry("Log line 1", LogLevel.DEBUG, null),
        new LogEntry("Log line 2", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
        new LogEntry("Log line 3", LogLevel.DEBUG, null),
        new LogEntry("Log line 4", LogLevel.DEBUG, null),
        new LogEntry("Log line 5", LogLevel.DEBUG, null),
        new LogEntry("Log line containing FILTERtext", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterTEXT", LogLevel.DEBUG, null),
        new LogEntry("Log line 6", LogLevel.DEBUG, null),
        new LogEntry("Log line 7", LogLevel.DEBUG, null),
        new LogEntry("Log line 8", LogLevel.DEBUG, null),
        new LogEntry("Log line 9", LogLevel.DEBUG, null),
        new LogEntry("Log line 10", LogLevel.DEBUG, null)
    };

    LogEntry filtered[] = filter.apply(input);

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void singleSimpleFilterTestCaseSensitive() throws FilterException {
    Filter filter = new Filter("", "filterText", Color.WHITE, true);
    LogEntry input[] = new LogEntry[] {
        new LogEntry("Log line 1", LogLevel.DEBUG, null),
        new LogEntry("Log line 2", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterText", LogLevel.DEBUG, null),
        new LogEntry("Log line 3", LogLevel.DEBUG, null),
        new LogEntry("Log line 4", LogLevel.DEBUG, null),
        new LogEntry("Log line 5", LogLevel.DEBUG, null),
        new LogEntry("Log line containing FILTERtext", LogLevel.DEBUG, null),
        new LogEntry("Log line containing filterTEXT", LogLevel.DEBUG, null),
        new LogEntry("Log line 6", LogLevel.DEBUG, null),
        new LogEntry("Log line 7", LogLevel.DEBUG, null),
        new LogEntry("Log line 8", LogLevel.DEBUG, null),
        new LogEntry("Log line 9", LogLevel.DEBUG, null),
        new LogEntry("Log line 10", LogLevel.DEBUG, null)
    };

    LogEntry filtered[] = filter.apply(input);

    Assert.assertEquals(1, filtered.length);
  }

  @Test
  public void singleRegexFilterTest() throws FilterException {
    Filter filter = new Filter("", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE);
    LogEntry input[] = new LogEntry[] {
        new LogEntry("Log line 1", LogLevel.DEBUG, null),
        new LogEntry("Log line 2", LogLevel.DEBUG, null),
        new LogEntry("Log line containing eMail@bla.com", LogLevel.DEBUG, null),
        new LogEntry("Log line 3", LogLevel.DEBUG, null),
        new LogEntry("Log line 4", LogLevel.DEBUG, null),
        new LogEntry("Log line 5", LogLevel.DEBUG, null),
        new LogEntry("email@email.com", LogLevel.DEBUG, null),
        new LogEntry("Log otheremail@other.co Log", LogLevel.DEBUG, null),
        new LogEntry("Log line 6", LogLevel.DEBUG, null),
        new LogEntry("Log line 7", LogLevel.DEBUG, null),
        new LogEntry("Log line 8", LogLevel.DEBUG, null),
        new LogEntry("Log line 9", LogLevel.DEBUG, null),
        new LogEntry("Log line 10", LogLevel.DEBUG, null)
    };

    LogEntry filtered[] = filter.apply(input);

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void multipleFilterTest() throws FilterException {
    Filter[] filters = new Filter[] {
        new Filter("", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE),
        new Filter("", "caseSensitiveText", Color.WHITE, true),
        new Filter("", "CaSeInSeNsitiveTeXT", Color.WHITE)
    };

    LogEntry input[] = new LogEntry[] {
        new LogEntry("Log line containing caseinsensitivetext", LogLevel.DEBUG, null),
        new LogEntry("Log line containing caseInsensitiveText", LogLevel.DEBUG, null),
        new LogEntry("Log line containing CASEINSENSITIVETEXT", LogLevel.DEBUG, null),
        new LogEntry("Log line 2", LogLevel.DEBUG, null),
        new LogEntry("Log line containing eMail@bla.com", LogLevel.DEBUG, null),
        new LogEntry("Log line 3", LogLevel.DEBUG, null),
        new LogEntry("Log line 4", LogLevel.DEBUG, null),
        new LogEntry("Log line 5", LogLevel.DEBUG, null),
        new LogEntry("email@email.com", LogLevel.DEBUG, null),
        new LogEntry("Log otheremail@other.co Log", LogLevel.DEBUG, null),
        new LogEntry("Log line 6", LogLevel.DEBUG, null),
        new LogEntry("Log line 7", LogLevel.DEBUG, null),
        new LogEntry("Log line containing caseSensitiveText", LogLevel.DEBUG, null),
        new LogEntry("Log line containing casesensitivetext", LogLevel.DEBUG, null),
        new LogEntry("Log line containing CASESENSITIVETEXT", LogLevel.DEBUG, null)
    };

    LogEntry filtered[] = Filters.applyMultipleFilters(input, filters);

    Assert.assertEquals(7, filtered.length);
  }
}
