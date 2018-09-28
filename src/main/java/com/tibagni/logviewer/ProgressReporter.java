package com.tibagni.logviewer;

public interface ProgressReporter {
  void onProgress(int progress, String description);

  default void failProgress() {
    // For failed case, set progress to 100, which means it is over
    // and set the 'Failed' description
    onProgress(100, "Failed");
  }
}
