package com.tibagni.logviewer;

public interface ProgressReporter {
  void onProgress(int progress, String description);
}
