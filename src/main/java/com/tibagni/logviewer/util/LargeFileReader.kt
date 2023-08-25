package com.tibagni.logviewer.util

import com.tibagni.logviewer.logger.Logger
import java.io.*
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class LargeFileReader @JvmOverloads constructor(
  file: File,
  private val charset: Charset? = null,
  private val bufferSize: Int = 1024 * 1024,
  private val threadPoolSize: Int = 1,
  private val handle: IFileHandle
) {

  private val executorService: ExecutorService
  private val fileLength: Long
  private var rAccessFile: RandomAccessFile
  private val startEndPairs: MutableSet<StartEndPair>
  private val counter = AtomicLong(0)
  private val startEndIndexer = AtomicInteger(0)
  private lateinit var cyclicBarrier: CyclicBarrier

  init {
    fileLength = file.length()
    rAccessFile = RandomAccessFile(file, "r")
    executorService = Executors.newWorkStealingPool(threadPoolSize)
    startEndPairs = HashSet()
  }

  fun start(): LargeFileReader {
    calculateStartEnd(0, fileLength / threadPoolSize)
    val startTime = System.currentTimeMillis()
    cyclicBarrier = CyclicBarrier(startEndPairs.size) {
      Logger.info("LargeFileReader: use time: " + (System.currentTimeMillis() - startTime))
      Logger.info("LargeFileReader: all line: " + counter.get())
      shutdown()
    }
    for (pair in startEndPairs) {
      executorService.execute(SliceReaderTask(pair))
    }
    return this
  }

  @Throws(IOException::class)
  fun startAndWaitComplete() {
    start()
    waitComplete()
  }

  @Throws(IOException::class)
  private fun calculateStartEnd(start: Long, size: Long) {
    if (start > fileLength - 1) {
      return
    }
    var endPosition = start + size - 1
    if (endPosition >= fileLength - 1) {
      startEndPairs.add(
        StartEndPair(
          startEndIndexer.getAndIncrement(),
          start, fileLength - 1
        )
      )
      return
    }
    rAccessFile.seek(endPosition)
    var tmp = rAccessFile.read().toByte()
    while (tmp != '\n'.code.toByte() && tmp != '\r'.code.toByte()) {
      endPosition++
      if (endPosition >= fileLength - 1) {
        endPosition = fileLength - 1
        break
      }
      rAccessFile.seek(endPosition)
      tmp = rAccessFile.read().toByte()
    }
    startEndPairs.add(
      StartEndPair(
        startEndIndexer.getAndIncrement(),
        start, endPosition
      )
    )
    calculateStartEnd(endPosition + 1, size)
  }

  fun shutdown() {
    rAccessFile.close()
    executorService.shutdown()
  }

  fun waitComplete() {
    executorService.awaitTermination(10, TimeUnit.MINUTES)
  }

  private fun handle(sliceIndex: Int, bytes: ByteArray) {
    val line: String = if (charset == null) {
      String(bytes)
    } else {
      String(bytes, charset)
    }
    handle.handle(sliceIndex, line)
    counter.incrementAndGet()
  }

  private data class StartEndPair(
    val index: Int,
    val start: Long,
    val end: Long
  )

  private inner class SliceReaderTask(pair: StartEndPair) : Runnable {
    private val start: Long
    private val sliceSize: Long
    private val readBuff: ByteArray
    private val sliceIndex: Int

    init {
      start = pair.start
      sliceSize = pair.end - pair.start + 1
      readBuff = ByteArray(bufferSize)
      sliceIndex = pair.index
    }

    override fun run() {
      try {
        val mapBuffer = rAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, start, sliceSize)
        ByteArrayOutputStream().use {
          var offset = 0
          while (offset < sliceSize) {
            val readLength: Int = if (offset + bufferSize <= sliceSize) {
              bufferSize
            } else {
              (sliceSize - offset).toInt()
            }
            mapBuffer[readBuff, 0, readLength]
            for (i in 0 until readLength) {
              val tmp = readBuff[i]
              if (tmp == '\n'.code.toByte() || tmp == '\r'.code.toByte()) {
                handle(sliceIndex, it.toByteArray())
                it.reset()
              } else {
                it.write(tmp.toInt())
              }
            }
            offset += bufferSize
          }
          if (it.size() > 0) {
            handle(sliceIndex, it.toByteArray())
          }
        }
        cyclicBarrier.await()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}