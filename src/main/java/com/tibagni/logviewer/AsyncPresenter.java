package com.tibagni.logviewer;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AsyncPresenter {
  private final AsyncView asyncView;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  AsyncPresenter(AsyncView asyncView) {
    this.asyncView = asyncView;
  }

  void doAsync(Runnable runnable) {
    asyncView.showStartLoading();
    executor.execute(runnable);
  }

  void updateAsyncProgress(int progress, String note) {
    SwingUtilities.invokeLater(() -> asyncView.showLoadingProgress(progress, note));
  }

  void doOnUiThread(Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  void release() {
    executor.shutdownNow();
  }

  public interface AsyncView {
    void showStartLoading();
    void showLoadingProgress(int progress, String note);
  }
}
