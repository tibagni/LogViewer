package com.tibagni.logviewer.log

import com.tibagni.logviewer.util.StringUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Unit tests to verify {@link FileLogReader} behavior.
 */
class FileLogReaderTests {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Verifies that the reader correctly reads files completely and retains file contents
     * without corruption or missing entries.
     */
    @Test
    fun `test readLogs reads files completely`() {
        val file1 = tempFolder.newFile("test1.log")
        val file2 = tempFolder.newFile("test2.log")

        val content1 = "Line 1${StringUtils.LINE_SEPARATOR}Line 2${StringUtils.LINE_SEPARATOR}"
        val content2 = "Line 3${StringUtils.LINE_SEPARATOR}Line 4${StringUtils.LINE_SEPARATOR}"

        file1.writeText(content1)
        file2.writeText(content2)

        val reader = FileLogReader(arrayOf(file1, file2))
        reader.readLogs(StandardCharsets.UTF_8)

        assertEquals(2, reader.size().toLong())
        assertTrue(reader.availableLogPaths.contains(file1.path))
        assertTrue(reader.availableLogPaths.contains(file2.path))

        assertEquals(content1, reader.get(file1.path))
        assertEquals(content2, reader.get(file2.path))
    }
}
