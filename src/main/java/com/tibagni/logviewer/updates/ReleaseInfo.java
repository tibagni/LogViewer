package com.tibagni.logviewer.updates;

import org.json.JSONObject;

public class ReleaseInfo {
  private String versionName;
  private double version;
  private String releaseUrl;
  private String releaseNotes;

  ReleaseInfo(JSONObject json) throws InvalidReleaseException {
    versionName = json.getString("tag_name");
    releaseUrl = json.getString("html_url");
    releaseNotes = json.getString("body");

    try {
      version = Double.parseDouble(versionName);
    } catch (NumberFormatException nfe) {
      throw new InvalidReleaseException("Invalid Release " + versionName);
    }
  }

  public String getVersionName() {
    return versionName;
  }

  public double getVersion() {
    return version;
  }

  public String getReleaseUrl() {
    return releaseUrl;
  }

  public String getReleaseNotes() {
    return releaseNotes;
  }
}
