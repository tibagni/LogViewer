package com.tibagni.logviewer.updates

import org.json.JSONObject

/**
 * Model representation of a remote release published on GitHub.
 *
 * Encapsulates the tag version name, release notes body, and a comparative [Version] representation.
 *
 * Pre-conditions:
 * - Expects a non-null [JSONObject] containing release metadata returned from the GitHub Releases API.
 */
class ReleaseInfo internal constructor(json: JSONObject) {
  /**
   * Raw tag name representing the release version (e.g., "2.7" or "v2.7.1").
   */
  val versionName: String = json.getString("tag_name")

  /**
   * Browser-accessible URL of the GitHub release page.
   */
  val releaseUrl: String = json.getString("html_url")

  /**
   * Release notes description text (markdown formatted).
   */
  val releaseNotes: String = json.getString("body")

  /**
   * Comparable [Version] representation parsed from [versionName] for update check eligibility.
   *
   * @throws InvalidReleaseException If the [versionName] has an invalid structure.
   */
  val version: Version = try {
    Version(versionName)
  } catch (e: IllegalArgumentException) {
    throw InvalidReleaseException("Invalid Release $versionName")
  }
}