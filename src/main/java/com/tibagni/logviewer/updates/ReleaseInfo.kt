package com.tibagni.logviewer.updates

import org.json.JSONObject

class ReleaseInfo internal constructor(json: JSONObject) {
  val versionName: String = json.getString("tag_name")
  val releaseUrl: String = json.getString("html_url")
  val releaseNotes: String = json.getString("body")
  val version: Double = try {
    versionName.toDouble()
  } catch (nfe: NumberFormatException) {
    throw InvalidReleaseException("Invalid Release $versionName")
  }
}