package com.tibagni.logviewer;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.PropertiesWrapper;
import com.tibagni.logviewer.util.StringUtils;

import java.io.IOException;

public class AppInfo {
  public static final String APPLICATION_NAME = "Log Viewer";
  public static final String LATEST_RELEASE_URL =
      "https://api.github.com/repos/tibagni/LogViewer/releases/latest";
  public static final String USER_GUIDE_URL = "https://tibagni.github.io/LogViewer/";
  public static final String GITHUB_URL = "https://github.com/tibagni/LogViewer";
  public static final String APP_PROPERTIES_FILE = "properties/app.properties";

  private static final String VERSION_KEY = "version";

  private static String cachedVersionStr;

  private AppInfo() {}

  public static String getCurrentVersion() {
    if (!StringUtils.isEmpty(cachedVersionStr)) {
      return cachedVersionStr;
    }

    String currentVersion = "unknown";
    try {
      PropertiesWrapper appProperties = new PropertiesWrapper(APP_PROPERTIES_FILE);
      currentVersion = appProperties.get(VERSION_KEY);
      cachedVersionStr = currentVersion;
    } catch (IOException e) {
      Logger.error("Failed to get current version", e);
      cachedVersionStr = null;
    }

    return currentVersion;
  }

  public static double getCurrentVersionNumber() {
    String versionName = getCurrentVersion();
    try {
      return Double.parseDouble(versionName);
    } catch (NumberFormatException nfe) {
      Logger.error("Not possible to parse current version: "
          + versionName, nfe);
      return -1;
    }
  }
}
