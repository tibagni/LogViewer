package com.tibagni.logviewer.updates

import java.lang.Exception

class InvalidReleaseException : Exception {
  constructor(msg: String?) : super(msg)
  constructor(cause: Exception?) : super(cause)
}