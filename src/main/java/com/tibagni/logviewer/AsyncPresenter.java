package com.tibagni.logviewer;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncPresenter {
  private AsyncView asyncView;
  private Executor executor = Executors.newSingleThreadExecutor();

  public AsyncPresenter(AsyncView asyncView) {
    this.asyncView = asyncView;
  }

  protected void doAsync(Runnable runnable) {
    asyncView.showStartLoading();
    executor.execute(runnable);
  }

  protected void updateAsyncProgress(int progress, String note) {
    SwingUtilities.invokeLater(() -> asyncView.showLoadingProgress(progress, note));
  }

  protected void doOnUiThread(Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  public interface AsyncView {
    void showStartLoading();
    void showLoadingProgress(int progress, String note);
  }
}
