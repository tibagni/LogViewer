package com.tibagni.logviewer.updates;

public class InvalidReleaseException extends Exception {
  public InvalidReleaseException(String msg) {
    super(msg);
  }

  public InvalidReleaseException(Exception cause) {
    super(cause);
  }
}
