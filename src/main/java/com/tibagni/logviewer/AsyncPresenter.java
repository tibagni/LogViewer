package com.tibagni.logviewer;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AsyncPresenter {
  private final AsyncView asyncView;
  private ExecutorService bgExecutorService = Executors.newSingleThreadExecutor();
  private Executor uiExecutor = SwingUtilities::invokeLater;

  AsyncPresenter(AsyncView asyncView) {
    this.asyncView = asyncView;
  }

  void doAsync(Runnable runnable) {
    uiExecutor.execute(asyncView::showStartLoading);
    bgExecutorService.execute(runnable);
  }

  void updateAsyncProgress(int progress, String note) {
    uiExecutor.execute(() -> {
      if (progress >= 100) {
        asyncView.finishLoading();
      } else {
        asyncView.showLoadingProgress(progress, note);
      }
    });
  }

  void doOnUiThread(Runnable runnable) {
    uiExecutor.execute(runnable);
  }

  void release() {
    bgExecutorService.shutdownNow();
  }

  public interface AsyncView {
    void showStartLoading();
    void showLoadingProgress(int progress, String note);
    void finishLoading();
  }


  // Test helpers
  public void setBgExecutorService(ExecutorService bgExecutorService) {
    this.bgExecutorService = bgExecutorService;
  }
  public void setUiExecutor(Executor uiExecutor) {
    this.uiExecutor = uiExecutor;
  }
}
