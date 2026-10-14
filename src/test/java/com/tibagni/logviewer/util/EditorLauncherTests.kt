package com.tibagni.logviewer.util

import com.tibagni.logviewer.ServiceLocator
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class EditorLauncherTests {

  private var originalEditor: File? = null

  @Before
  fun setUp() {
    originalEditor = ServiceLocator.logViewerPrefs.preferredTextEditor
  }

  @After
  fun tearDown() {
    ServiceLocator.logViewerPrefs.preferredTextEditor = originalEditor
  }

  @Test
  fun testOpenInEditorWithNullPreferredEditorDoesNotThrow() {
    ServiceLocator.logViewerPrefs.preferredTextEditor = null
    val tempFile = File.createTempFile("editor_test", ".txt")
    tempFile.deleteOnExit()

    try {
      EditorLauncher.openInEditor(null, tempFile)
    } catch (e: Exception) {
      fail("openInEditor should handle unconfigured editor gracefully: ${e.message}")
    }
  }

  @Test
  fun testOpenInEditorWithNonExistentEditorDoesNotThrow() {
    ServiceLocator.logViewerPrefs.preferredTextEditor = File("/non/existent/path/to/editor")
    val tempFile = File.createTempFile("editor_test", ".txt")
    tempFile.deleteOnExit()

    try {
      EditorLauncher.openInEditor(null, tempFile)
    } catch (e: Exception) {
      fail("openInEditor should handle missing editor gracefully: ${e.message}")
    }
  }

  @Test
  fun testOpenInEditorWithValidExecutable() {
    val trueExecutable = listOf(File("/bin/true"), File("/usr/bin/true")).firstOrNull { it.exists() } ?: return
    ServiceLocator.logViewerPrefs.preferredTextEditor = trueExecutable
    val tempFile = File.createTempFile("editor_test", ".txt")
    tempFile.deleteOnExit()

    try {
      EditorLauncher.openInEditor(null, tempFile)
    } catch (e: Exception) {
      fail("openInEditor should successfully launch valid executable: ${e.message}")
    }
  }
}
