package com.tibagni.logviewer.updates;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
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
      return -1;
    }
  }

  public void checkForUpdates(UpdateListener listener) {
    // If we fail to read the current version for some reason, do not proceed
    if (CURRENT_VERSION < 0) {
      return;
    }

    new Thread(() -> {
      try {
        ReleaseInfo latest = getLatestReleaseInfo();
        if (latest.getVersion() > CURRENT_VERSION) {
          SwingUtilities.invokeLater(() -> listener.onNewVersionFound(latest));
        }
      } catch (InvalidReleaseException e) {
        // Just ignore the update check if the latest version is invalid for some reason
      }
    }).start();
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
