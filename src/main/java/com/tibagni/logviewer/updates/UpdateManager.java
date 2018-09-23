package com.tibagni.logviewer.updates;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.SwingUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

public class UpdateManager {
  private final String LATEST_RELEASE_URL;
  private final double CURRENT_VERSION;

  public UpdateManager(String latestReleaseUrl, String currentVersion) {
    LATEST_RELEASE_URL = latestReleaseUrl;
    CURRENT_VERSION = parseCurrentVersion(currentVersion);
  }

  private double parseCurrentVersion(String strVal) {
    try {
      return Double.parseDouble(strVal);
    } catch (NumberFormatException nfe) {
      Logger.error("Not possible to parse current version: " + strVal, nfe);
      return -1;
    }
  }

  public void checkForUpdates(UpdateListener listener) {
    // If we fail to read the current version for some reason, do not proceed
    if (CURRENT_VERSION < 0) {
      Logger.error("Not a valid current version. Do not check for updates");
      return;
    }

    Logger.debug("Start checking for updates...");
    SwingUtils.doAsync(
        () ->  getLatestReleaseInfo(),
        latest -> notifyIfUpdateAvailable(latest, listener),
        tr -> Logger.error("Not possible to get latest release info", tr)
    );
  }

  private void notifyIfUpdateAvailable(ReleaseInfo latest, UpdateListener listener) {
    if (latest.getVersion() > CURRENT_VERSION) {
      Logger.debug("New version available: " + latest.getVersionName());
      listener.onNewVersionFound(latest);
    } else {
      Logger.debug("LogViewer is up to date on version " + CURRENT_VERSION);
    }
  }

  private ReleaseInfo getLatestReleaseInfo() throws InvalidReleaseException {
    try {
      URL url = new URL(LATEST_RELEASE_URL);
      JSONObject jsonResult = new JSONObject(IOUtils.toString(url, Charset.forName("UTF-8")));
      return new ReleaseInfo(jsonResult);
    } catch (IOException | JSONException e) {
      throw new InvalidReleaseException(e);
    }
  }

  public interface UpdateListener {
    void onNewVersionFound(ReleaseInfo newRelease);
  }
}
