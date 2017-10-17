package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.EditFilterDialog;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterCellRenderer;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.util.JFileChooserExt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.*;
import java.util.stream.IntStream;

public class LogViewerView implements LogViewer.View {
  private JList logList;
  private JPanel mainPanel;
  private JList filteredLogList;
  private JButton saveFilterBtn;
  private JButton addNewFilterBtn;
  private JButton openFilterBtn;
  private JList filtersList;
  private JButton applyFiltersBtn;
  private JButton openLogsBtn;

  private LogViewer.Presenter presenter;
  private JFileChooserExt fileChooser;
  private ProgressMonitor progressMonitor;

  public LogViewerView() {
    presenter = new LogViewerPresenter(this);
    fileChooser = new JFileChooserExt(FileSystemView.getFileSystemView().getHomeDirectory());

    logList.addFocusListener(new MyFocusListener(logList));
    filteredLogList.addFocusListener(new MyFocusListener(filteredLogList));
    addNewFilterBtn.addActionListener(e -> addFilter());

    filtersList.setCellRenderer(new FilterCellRenderer());
    setupFiltersContextActions();

    logList.setCellRenderer(new LogCellRenderer());
    logList.addComponentListener(new MyComponentListener(logList));

    filteredLogList.setCellRenderer(new LogCellRenderer());
    filteredLogList.addComponentListener(new MyComponentListener(filteredLogList));
    setupFilteredLogsContextActions();

    applyFiltersBtn.addActionListener(e -> applySelectedFilters());

    openLogsBtn.addActionListener(e -> openLogs());
    saveFilterBtn.addActionListener(e -> saveFilter());
    openFilterBtn.addActionListener(e -> openFilter());
  }

  @Override
  public void configureFiltersList(Filter[] filters) {
    filtersList.setListData(filters);
  }

  @Override
  public void showErrorMessage(String message) {
    JOptionPane.showMessageDialog(mainPanel, message, "Error...", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showLogs(LogEntry[] logEntries) {
    logList.setListData(logEntries);
  }

  @Override
  public void showFilteredLogs(LogEntry[] logEntries) {
    filteredLogList.setListData(logEntries);
    logList.updateUI();
  }

  private void setupFiltersContextActions() {
    JPopupMenu popup = new JPopupMenu();
    final JLabel menuTitle = new JLabel();
    menuTitle.setBorder(new EmptyBorder(0, 10, 0, 0));
    popup.add(menuTitle);
    popup.add(new JPopupMenu.Separator());
    JMenuItem deleteMenuItem = popup.add("Delete");
    JMenuItem editMenuItem = popup.add("Edit");

    deleteMenuItem.addActionListener(e -> deleteSelectedFilters());
    editMenuItem.addActionListener(e -> editSelectedFilter());

    filtersList.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        int indexClicked = filtersList.locationToIndex(me.getPoint());

        if (SwingUtilities.isRightMouseButton(me) && !filtersList.isSelectionEmpty()) {
          int[] selectedIndices = filtersList.getSelectedIndices();
          if (IntStream.of(selectedIndices).anyMatch(i -> i == indexClicked)) {
            int selectedFilters = selectedIndices.length;
            menuTitle.setText(selectedFilters + " item(s) selected");
            editMenuItem.setVisible(selectedFilters == 1);

            popup.show(filtersList, me.getX(), me.getY());
          }
        } else if (me.getClickCount() == 2) {
          // Edit filter on double click
          editSelectedFilter();
        }
      }
    });

    // Add the shortcuts for the context menu
    filtersList.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (!filtersList.isSelectionEmpty() && filtersList.getModel().getSize() > 0 &&
            (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
          deleteSelectedFilters();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && filtersList.getSelectedIndices().length == 1) {
          editSelectedFilter();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          filtersList.clearSelection();
        }
      }
    });

    filtersList.addListSelectionListener(e -> applyFiltersBtn.setEnabled(!filtersList.isSelectionEmpty()));
  }

  private void setupFilteredLogsContextActions() {
    filteredLogList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int selectedIndex = filteredLogList.getSelectedIndex();
          LogEntry clickedEntry = (LogEntry) filteredLogList.getModel().getElementAt(selectedIndex);

          logList.ensureIndexIsVisible(clickedEntry.getIndex());
          logList.setSelectedIndex(clickedEntry.getIndex());
        }
      }
    });
  }

  private void editSelectedFilter() {
    if (filtersList.getSelectedIndices().length == 1) {
      // We don't care about the return value here
      EditFilterDialog.showEditFilterDialog(addNewFilterBtn,
          (Filter) filtersList.getModel().getElementAt(filtersList.getSelectedIndex()));
    }
  }

  private void addFilter() {
    Filter newFilter = EditFilterDialog.showEditFilterDialog(addNewFilterBtn);
    if (newFilter != null) {
      presenter.addFilter(newFilter);
    }
  }

  private void applySelectedFilters() {
    presenter.applyFilters(filtersList.getSelectedIndices());
  }

  private void deleteSelectedFilters() {
    int userChoice = JOptionPane.showConfirmDialog(
        mainPanel.getParent(),
        "Are you sure you want to delete the selected filter(s)?",
        "Are you sure?",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (userChoice == JOptionPane.NO_OPTION) return;

    presenter.removeFilters(filtersList.getSelectedIndices());
  }

  private void openLogs() {
    fileChooser.resetChoosableFileFilters();
    fileChooser.setMultiSelectionEnabled(true);
    fileChooser.setDialogTitle("Open Logs...");
    int selectedOption = fileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadLogs(fileChooser.getSelectedFiles());
    }
  }

  private void saveFilter() {
    fileChooser.resetChoosableFileFilters();
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.setDialogTitle("Save Filter...");
    fileChooser.setSaveExtension(Filter.FILE_EXTENSION);
    int selectedOption = fileChooser.showSaveDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.saveFilters(fileChooser.getSelectedFile());
    }
  }

  private void openFilter() {
    fileChooser.resetChoosableFileFilters();
    fileChooser.setFileFilter(new FileNameExtensionFilter("Filter files", Filter.FILE_EXTENSION));
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.setDialogTitle("Open Filter...");
    int selectedOption = fileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadFilters(fileChooser.getSelectedFile());
    }
  }

  @Override
  public void showStartLoading() {
    if (progressMonitor == null) {
      progressMonitor = new ProgressMonitor(mainPanel, "Loading...", "", 0, 100);
      progressMonitor.setMillisToDecideToPopup(100);
    }

    progressMonitor.setProgress(0);
  }

  @Override
  public void showLoadingProgress(int progress, String note) {
    progressMonitor.setProgress(progress);
    progressMonitor.setNote(note);
  }

  private class MyComponentListener extends ComponentAdapter {
    private JList target;
    private ActionListener resizeCells = e -> {
      // This will ensure the Cells have proper heights based on the content
      target.setFixedCellHeight(1);
      target.setFixedCellHeight(-1);
    };

    private Timer recalculateTimer;

    public MyComponentListener(JList target) {
      this.target = target;
      this.recalculateTimer = new Timer(1000, resizeCells);
      this.recalculateTimer.setRepeats(false);
    }

    @Override
    public void componentResized(ComponentEvent e) {
      // Use a Timer instead of resizing the cells every time the size changes.
      // Otherwise performance will be horrible
      if (recalculateTimer.isRunning()) {
        recalculateTimer.restart();
      } else {
        recalculateTimer.start();
      }
    }
  }

  private class MyFocusListener extends FocusAdapter {
    private JList target;

    public MyFocusListener(JList target) {
      this.target = target;
    }

    @Override
    public void focusLost(FocusEvent e) {
      target.clearSelection();
    }
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("LogViewer");
    LogViewerView logViewer = new LogViewerView();

    frame.setContentPane(logViewer.mainPanel);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }
}
