package com.tibagni.logviewer;

import com.tibagni.logviewer.filter.EditFilterDialog;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterCellRenderer;
import com.tibagni.logviewer.log.LogCellRenderer;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogListTableModel;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog;
import com.tibagni.logviewer.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.*;
import java.util.stream.IntStream;

public class LogViewerView implements LogViewer.View {
  private JTable logList;
  private JPanel mainPanel;
  private JTable filteredLogList;
  private JButton saveFilterBtn;
  private JButton addNewFilterBtn;
  private JButton openFilterBtn;
  private ReorderableList<Filter> filtersList;
  private JButton applyFiltersBtn;
  private JButton openLogsBtn;
  private JSplitPane logsPane;
  private JButton settingsBtn;

  private final LogViewer.Presenter presenter;
  private final JFileChooserExt logFileChooser;
  private final JFileChooserExt filterFileChooser;
  private ProgressMonitorExt progressMonitor;

  private LogListTableModel logListTableModel;
  private LogListTableModel filteredLogListTableModel;
  private JFrame parent;

  final private LogViewerPreferences userPrefs;

  private static final String UNSAVED_INDICATOR = " (*)";

  public LogViewerView(JFrame parent) {
    this.parent = parent;
    userPrefs = LogViewerPreferences.getInstance();
    presenter = new LogViewerPresenter(this);
    presenter.init();

    this.parent.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.parent.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        presenter.finishing();
      }
    });

    logFileChooser = new JFileChooserExt(FileSystemView.getFileSystemView().getHomeDirectory());
    filterFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    userPrefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onDefaultFiltersPathChanged() {
        filterFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
      }
    });

    addNewFilterBtn.addActionListener(e -> addFilter());

    filtersList.setCellRenderer(new FilterCellRenderer());
    filtersList.setReorderedListener(presenter::reorderFilters);
    setupFiltersContextActions();

    logList.setDefaultRenderer(LogEntry.class, new LogCellRenderer());

    filteredLogList.setDefaultRenderer(LogEntry.class, new LogCellRenderer());
    setupFilteredLogsContextActions();

    applyFiltersBtn.addActionListener(e -> applySelectedFilters());

    openLogsBtn.addActionListener(e -> openLogs());
    saveFilterBtn.addActionListener(e -> saveFilter());
    openFilterBtn.addActionListener(e -> openFilter());

    settingsBtn.addActionListener(e -> openUserPreferences());

    // Configure file drop
    new FileDrop(System.out, logsPane, files -> presenter.loadLogs(files));
//    new FileDrop(System.out, filtersList, files -> {
//      if (files.length > 1) {
//        showErrorMessage("You can only open one filter file at a time!");
//      } else {
//        presenter.loadFilters(files[0]);
//      }
//    });
  }

  JPanel getContentPane() {
    return mainPanel;
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
    logListTableModel.setLogs(logEntries);
  }

  @Override
  public void showFilteredLogs(LogEntry[] logEntries) {
    filteredLogListTableModel.setLogs(logEntries);
    logList.updateUI();
    filtersList.updateUI();
  }

  @Override
  public void showUnsavedTitle() {
    String currentTitle = parent.getTitle();
    parent.setTitle(currentTitle + UNSAVED_INDICATOR);
  }

  @Override
  public void hideUnsavedTitle() {
    String currentTitle = parent.getTitle();
    parent.setTitle(currentTitle.replace(UNSAVED_INDICATOR, ""));
  }

  @Override
  public int showAskToSaveFilterDialog() {
    int userChoice = JOptionPane.showConfirmDialog(
        mainPanel.getParent(),
        "There are unsaved changes to your filters, do you want to save it?",
        "Unsaved changes",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (userChoice == JOptionPane.YES_OPTION) {
      saveFilter();
    }

    return userChoice;
  }

  @Override
  public void showSaveFilter() {
    saveFilter();
  }

  @Override
  public void finish() {
    parent.dispose();
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
      public void keyPressed(KeyEvent e) {
        if (!filtersList.isSelectionEmpty() && filtersList.getModel().getSize() > 0 &&
            (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
          deleteSelectedFilters();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && filtersList.getSelectedIndices().length == 1) {
          editSelectedFilter();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          filtersList.clearSelection();
        } else if (e.getKeyChar() == ',' || e.getKeyChar() == '.') {
          int filteredLogIdx;
          int selectedFilter = filtersList.getSelectedIndex();
          int selectedFilteredLog = filteredLogList.getSelectedRow();

          if (e.getKeyChar() == ',') {
            filteredLogIdx = presenter.getPrevFilteredLogForFilter(selectedFilter, selectedFilteredLog);
          } else {
            filteredLogIdx = presenter.getNextFilteredLogForFilter(selectedFilter, selectedFilteredLog);
          }

          if (filteredLogIdx != -1) {
            SwingUtils.scrollToVisible(filteredLogList, filteredLogIdx);
            filteredLogList.setRowSelectionInterval(filteredLogIdx, filteredLogIdx);
          }
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
          int selectedIndex = filteredLogList.getSelectedRow();
          LogEntry clickedEntry = (LogEntry) filteredLogListTableModel.getValueAt(selectedIndex, 0);

          int logIndex = clickedEntry.getIndex();
          SwingUtils.scrollToVisible(logList, logIndex);
          logList.setRowSelectionInterval(logIndex, logIndex);
        }
      }
    });
  }

  private void editSelectedFilter() {
    if (filtersList.getSelectedIndices().length == 1) {
      Filter selectedFilter = filtersList.getModel().getElementAt(
          filtersList.getSelectedIndex());

      // We don't care about the result here. The filter is automatically
      // updated by this dialog
      EditFilterDialog.showEditFilterDialog(parent, addNewFilterBtn, selectedFilter);

      // Tell the presenter a filter was edited. It will not update the filters
      // as filters are updated by EditFilterDialog itself, it will only determine
      // if the filter was, in fact, updated and mark unsaved changes if necessary.
      presenter.filterEdited();
    }
  }

  private void addFilter() {
    Filter newFilter = EditFilterDialog.showEditFilterDialog(parent, addNewFilterBtn);
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

    if (userChoice != JOptionPane.YES_OPTION) return;

    presenter.removeFilters(filtersList.getSelectedIndices());
  }

  private void openLogs() {
    logFileChooser.resetChoosableFileFilters();
    logFileChooser.setMultiSelectionEnabled(true);
    logFileChooser.setDialogTitle("Open Logs...");
    int selectedOption = logFileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadLogs(logFileChooser.getSelectedFiles());
    }
  }

  private void saveFilter() {
    filterFileChooser.resetChoosableFileFilters();
    filterFileChooser.setMultiSelectionEnabled(false);
    filterFileChooser.setDialogTitle("Save Filter...");
    filterFileChooser.setSaveExtension(Filter.FILE_EXTENSION);
    int selectedOption = filterFileChooser.showSaveDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.saveFilters(filterFileChooser.getSelectedFile());
    }
  }

  private void openFilter() {
    filterFileChooser.resetChoosableFileFilters();
    filterFileChooser.setFileFilter(new FileNameExtensionFilter("Filter files", Filter.FILE_EXTENSION));
    filterFileChooser.setMultiSelectionEnabled(false);
    filterFileChooser.setDialogTitle("Open Filter...");
    int selectedOption = filterFileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadFilters(filterFileChooser.getSelectedFile());
    }
  }

  @Override
  public void showStartLoading() {
    if (progressMonitor == null) {
      progressMonitor = new ProgressMonitorExt(mainPanel, "Loading...", "", 0, 100);
      progressMonitor.setMillisToDecideToPopup(100);
      progressMonitor.setCancelable(false);
      progressMonitor.setPreferredWidth(550);
    }

    progressMonitor.setProgress(0);
  }

  @Override
  public void showLoadingProgress(int progress, String note) {
    progressMonitor.setProgress(progress);
    progressMonitor.setNote(note);
  }

  private void createUIComponents() {
    logListTableModel = new LogListTableModel("All Logs");
    logList = new JTable(logListTableModel);

    filteredLogListTableModel = new LogListTableModel("Filtered Logs");
    filteredLogList = new JTable(filteredLogListTableModel);
  }

  private void openUserPreferences() {
    LogViewerPreferencesDialog.showPreferencesDialog(settingsBtn);
  }
}
