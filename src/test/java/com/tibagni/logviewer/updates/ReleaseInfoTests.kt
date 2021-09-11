package com.tibagni.logviewer.updates

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReleaseInfoTests {

  companion object {
    const val LATEST_VERSION = "{\n" +
        "  \"url\": \"https://api.github.com/repos/tibagni/LogViewer/releases/38140168\",\n" +
        "  \"assets_url\": \"https://api.github.com/repos/tibagni/LogViewer/releases/38140168/assets\",\n" +
        "  \"upload_url\": \"https://uploads.github.com/repos/tibagni/LogViewer/releases/38140168/assets{?name,label}\",\n" +
        "  \"html_url\": \"https://github.com/tibagni/LogViewer/releases/tag/2.3\",\n" +
        "  \"id\": 38140168,\n" +
        "  \"author\": {},\n" +
        "  \"node_id\": \"MDc6UmVsZWFzZTM4MTQwMTY4\",\n" +
        "  \"tag_name\": \"2.3\",\n" +
        "  \"target_commitish\": \"master\",\n" +
        "  \"name\": \"2.3\",\n" +
        "  \"draft\": false,\n" +
        "  \"prerelease\": false,\n" +
        "  \"created_at\": \"2021-02-17T00:57:25Z\",\n" +
        "  \"published_at\": \"2021-02-17T00:57:33Z\",\n" +
        "  \"assets\": [],\n" +
        "  \"tarball_url\": \"https://api.github.com/repos/tibagni/LogViewer/tarball/2.3\",\n" +
        "  \"zipball_url\": \"https://api.github.com/repos/tibagni/LogViewer/zipball/2.3\",\n" +
        "  \"body\": \"release notes\"\n" +
        "}\n"
    const val LATEST_VERSION_INVALID = "{\n" +
        "  \"html_url\": \"https://github.com/tibagni/LogViewer/releases/tag/2.3\",\n" +
        "  \"tag_name\": \"2.3s\",\n" +
        "  \"body\": \"release notes\"\n" +
        "}\n"
  }

  @Test
  fun testReleaseInfo() {
    val releaseInfo = ReleaseInfo(JSONObject(LATEST_VERSION))

    assertEquals("https://github.com/tibagni/LogViewer/releases/tag/2.3", releaseInfo.releaseUrl)
    assertEquals(2.3, releaseInfo.version, 0.01)
    assertEquals("2.3", releaseInfo.versionName)
    assertEquals("release notes", releaseInfo.releaseNotes)
  }

  @Test(expected = InvalidReleaseException::class)
  fun testReleaseInfoInvalidVersion() {
    ReleaseInfo(JSONObject(LATEST_VERSION_INVALID))
  }
}