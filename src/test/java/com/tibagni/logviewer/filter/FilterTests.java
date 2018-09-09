package com.tibagni.logviewer.filter;

import com.tibagni.logviewer.ProgressReporter;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogLevel;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;

import static org.mockito.Mockito.*;

public class FilterTests {

  @Test
  public void singleSimpleFilterTest() throws FilterException {
    Filter filter = new Filter("name", "filterText", Color.WHITE);
    LogEntry input[] = new LogEntry[]{
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

    LogEntry filtered[] = Filters.applyMultipleFilters(input, new Filter[]{filter}, mock(ProgressReporter.class));

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void singleSimpleFilterTestCaseInsensitive() throws FilterException {
    Filter filter = new Filter("name", "filterText", Color.WHITE);
    LogEntry input[] = new LogEntry[]{
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

    LogEntry filtered[] = Filters.applyMultipleFilters(input, new Filter[]{filter}, mock(ProgressReporter.class));

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void singleSimpleFilterTestCaseSensitive() throws FilterException {
    Filter filter = new Filter("name", "filterText", Color.WHITE, true);
    LogEntry input[] = new LogEntry[]{
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

    LogEntry filtered[] = Filters.applyMultipleFilters(input, new Filter[]{filter}, mock(ProgressReporter.class));

    Assert.assertEquals(1, filtered.length);
  }

  @Test
  public void singleRegexFilterTest() throws FilterException {
    Filter filter = new Filter("name", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE);
    LogEntry input[] = new LogEntry[]{
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

    LogEntry filtered[] = Filters.applyMultipleFilters(input, new Filter[]{filter}, mock(ProgressReporter.class));

    Assert.assertEquals(3, filtered.length);
  }

  @Test
  public void multipleFilterTest() throws FilterException {
    Filter[] filters = new Filter[]{
        new Filter("name", "[\\w\\d]+@[\\w\\d]+\\.\\w+", Color.WHITE),
        new Filter("name", "caseSensitiveText", Color.WHITE, true),
        new Filter("name", "CaSeInSeNsitiveTeXT", Color.WHITE)
    };

    LogEntry input[] = new LogEntry[]{
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

    LogEntry filtered[] = Filters.applyMultipleFilters(input, filters, mock(ProgressReporter.class));

    Assert.assertEquals(7, filtered.length);
  }

  @Test
  public void testSerializeSimpleFilter() throws FilterException {
    Filter filter1 = new Filter("Filter Name", "Filter Query", new Color(0, 0, 0));
    Filter filter2 = new Filter("Filter Name", "F", new Color(255, 255, 255));
    Filter filter3 = new Filter("filter", "filter", new Color(0, 255, 0), true);
    Filter filter4 = new Filter("(){}filter", "filter", new Color(255, 0, 0));
    Filter filter5 = new Filter("./\\()*&ˆˆ", "Filter Query", new Color(0, 0, 255));

    String serialized1 = filter1.serializeFilter();
    String serialized2 = filter2.serializeFilter();
    String serialized3 = filter3.serializeFilter();
    String serialized4 = filter4.serializeFilter();
    String serialized5 = filter5.serializeFilter();

    Assert.assertEquals("Filter Name,RmlsdGVyIFF1ZXJ5,2,0:0:0", serialized1);
    Assert.assertEquals("Filter Name,Rg==,2,255:255:255", serialized2);
    Assert.assertEquals("filter,ZmlsdGVy,0,0:255:0", serialized3);
    Assert.assertEquals("(){}filter,ZmlsdGVy,2,255:0:0", serialized4);
    Assert.assertEquals("./\\()*&ˆˆ,RmlsdGVyIFF1ZXJ5,2,0:0:255", serialized5);
  }

  @Test
  public void testSerializeRegexFilter() throws FilterException {
    Filter filter1 = new Filter("Filter Name", "\\w+@\\w+\\.(net|com)(\\.br){0,1}",
        new Color(0, 0, 0));
    Filter filter2 = new Filter("Filter Name", "\\+\\d-\\(\\d{3}\\)-\\d{3}-\\d{4}",
        new Color(255, 255, 255), true);

    String serialized1 = filter1.serializeFilter();
    String serialized2 = filter2.serializeFilter();

    Assert.assertEquals("Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0",
        serialized1);
    Assert.assertEquals("Filter Name,XCtcZC1cKFxkezN9XCktXGR7M30tXGR7NH0=,0,255:255:255",
        serialized2);
  }

  @Test
  public void testDeSerializeSimpleFilter() throws FilterException {
    String serialized1 = "Filter Name,RmlsdGVyIFF1ZXJ5,2,0:0:0";
    String serialized2 = "Filter Name,Rg==,2,255:255:255";
    String serialized3 = "filter,ZmlsdGVy,0,0:255:0";
    String serialized4 = "(){}filter,ZmlsdGVy,2,255:0:0";
    String serialized5 = "./\\()*&ˆˆ,RmlsdGVyIFF1ZXJ5,2,0:0:255";

    Filter filter1 = Filter.createFromString(serialized1);
    Filter filter2 = Filter.createFromString(serialized2);
    Filter filter3 = Filter.createFromString(serialized3);
    Filter filter4 = Filter.createFromString(serialized4);
    Filter filter5 = Filter.createFromString(serialized5);

    Assert.assertEquals("Filter Name", filter1.getName());
    Assert.assertEquals("Filter Query", filter1.getPatternString());
    Assert.assertFalse(filter1.isCaseSensitive());
    Assert.assertEquals(new Color(0, 0, 0), filter1.getColor());

    Assert.assertEquals("Filter Name", filter2.getName());
    Assert.assertEquals("F", filter2.getPatternString());
    Assert.assertFalse(filter2.isCaseSensitive());
    Assert.assertEquals(new Color(255, 255, 255), filter2.getColor());

    Assert.assertEquals("filter", filter3.getName());
    Assert.assertEquals("filter", filter3.getPatternString());
    Assert.assertTrue(filter3.isCaseSensitive());
    Assert.assertEquals(new Color(0, 255, 0), filter3.getColor());

    Assert.assertEquals("(){}filter", filter4.getName());
    Assert.assertEquals("filter", filter4.getPatternString());
    Assert.assertFalse(filter4.isCaseSensitive());
    Assert.assertEquals(new Color(255, 0, 0), filter4.getColor());

    Assert.assertEquals("./\\()*&ˆˆ", filter5.getName());
    Assert.assertEquals("Filter Query", filter5.getPatternString());
    Assert.assertFalse(filter5.isCaseSensitive());
    Assert.assertEquals(new Color(0, 0, 255), filter5.getColor());
  }

  @Test
  public void testDeSerializeRegexFilter() throws FilterException {
    String serialized1 = "Filter Name,XHcrQFx3K1wuKG5ldHxjb20pKFwuYnIpezAsMX0=,2,0:0:0";
    String serialized2 = "Filter Name,XCtcZC1cKFxkezN9XCktXGR7M30tXGR7NH0=,0,255:255:255";

    Filter filter1 = Filter.createFromString(serialized1);
    Filter filter2 = Filter.createFromString(serialized2);

    Assert.assertEquals("Filter Name", filter1.getName());
    Assert.assertEquals("\\w+@\\w+\\.(net|com)(\\.br){0,1}", filter1.getPatternString());
    Assert.assertFalse(filter1.isCaseSensitive());
    Assert.assertEquals(new Color(0, 0, 0), filter1.getColor());

    Assert.assertEquals("Filter Name", filter2.getName());
    Assert.assertEquals("\\+\\d-\\(\\d{3}\\)-\\d{3}-\\d{4}", filter2.getPatternString());
    Assert.assertTrue(filter2.isCaseSensitive());
    Assert.assertEquals(new Color(255, 255, 255), filter2.getColor());
  }
}
