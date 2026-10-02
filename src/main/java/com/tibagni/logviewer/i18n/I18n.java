package com.tibagni.logviewer.i18n;

import com.tibagni.logviewer.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class I18n {
  private static final String BUNDLE_BASE_NAME = "properties.strings";
  private static final ResourceBundle.Control UTF8_CONTROL = new Utf8ResourceBundleControl();

  private static Locale currentLocale = Locale.getDefault();
  private static ResourceBundle bundle = loadBundle(currentLocale);

  // Common
  public static final String COMMON_OK = "common.ok";
  public static final String COMMON_CANCEL = "common.cancel";

  // Preferences Dialog
  public static final String PREF_DIALOG_TITLE = "pref.dialog.title";
  public static final String PREF_LOOK_AND_FEEL = "pref.look_and_feel";
  public static final String PREF_LOG_FONT_SIZE = "pref.log_font_size";
  public static final String PREF_LOG_FONT_BOLD = "pref.log_font_bold";
  public static final String PREF_DEFAULT_LOGS_PATH = "pref.default_logs_path";
  public static final String PREF_DEFAULT_FILTERS_PATH = "pref.default_filters_path";
  public static final String PREF_OPEN_LAST_FILTER = "pref.open_last_filter";
  public static final String PREF_APPLY_FILTERS_AFTER_EDIT = "pref.apply_filters_after_edit";
  public static final String PREF_REMEMBER_APPLIED_FILTERS = "pref.remember_applied_filters";
  public static final String PREF_COLLAPSE_ALL_GROUPS_STARTUP = "pref.collapse_all_groups_startup";
  public static final String PREF_SHOW_LINE_NUMBERS = "pref.show_line_numbers";
  public static final String PREF_SHOW_LOG_DIVIDERS = "pref.show_log_dividers";
  public static final String PREF_APPLY_FILTERS_ON_CHECK = "pref.apply_filters_on_check";
  public static final String PREF_PREFERRED_TEXT_EDITOR = "pref.preferred_text_editor";

  private I18n() {}

  public static synchronized void setLocale(Locale locale) {
    currentLocale = locale != null ? locale : Locale.getDefault();
    bundle = loadBundle(currentLocale);
  }

  public static synchronized Locale getLocale() {
    return currentLocale;
  }

  public static String get(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      Logger.error("Missing translation for key: " + key);
      return "!" + key + "!";
    }
  }

  public static String format(String key, Object... args) {
    String pattern = get(key);
    try {
      return MessageFormat.format(pattern, args);
    } catch (Exception e) {
      Logger.error("Failed to format translation for key: " + key, e);
      return pattern;
    }
  }

  private static ResourceBundle loadBundle(Locale locale) {
    try {
      return ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale, UTF8_CONTROL);
    } catch (MissingResourceException e) {
      Logger.error("Failed to load resource bundle for locale: " + locale, e);
      return ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ROOT, UTF8_CONTROL);
    }
  }

  private static class Utf8ResourceBundleControl extends ResourceBundle.Control {
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                    ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
      String bundleName = toBundleName(baseName, locale);
      String resourceName = toResourceName(bundleName, "properties");
      try (InputStream stream = loader.getResourceAsStream(resourceName)) {
        if (stream != null) {
          try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
          }
        }
      }
      return super.newBundle(baseName, locale, format, loader, reload);
    }
  }
}
