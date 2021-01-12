package com.tibagni.logviewer;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncPresenter {
  private final AsyncPresenterView asyncView;
  private ExecutorService bgExecutorService = Executors.newSingleThreadExecutor();
  private Executor uiExecutor = SwingUtilities::invokeLater;

  protected AsyncPresenter(AsyncPresenterView asyncView) {
    this.asyncView = asyncView;
  }

  protected void doAsync(Runnable runnable) {
    uiExecutor.execute(asyncView::showStartLoading);
    bgExecutorService.execute(runnable);
  }

  protected void updateAsyncProgress(int progress, String note) {
    uiExecutor.execute(() -> {
      if (progress >= 100) {
        asyncView.finishLoading();
      } else {
        asyncView.showLoadingProgress(progress, note);
      }
    });
  }

  protected void doOnUiThread(Runnable runnable) {
    uiExecutor.execute(runnable);
  }

  protected void release() {
    bgExecutorService.shutdownNow();
  }

  public interface AsyncPresenterView {
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
