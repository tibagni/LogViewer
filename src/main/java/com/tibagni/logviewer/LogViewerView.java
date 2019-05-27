package com.tibagni.logviewer;

import com.tibagni.logviewer.about.AboutDialog;
import com.tibagni.logviewer.filter.EditFilterDialog;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FiltersList;
import com.tibagni.logviewer.log.LogCellRenderer;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogListTableModel;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog;
import com.tibagni.logviewer.util.*;
import com.tibagni.logviewer.view.FileDrop;
import com.tibagni.logviewer.view.JFileChooserExt;
import com.tibagni.logviewer.view.ProgressMonitorExt;
import com.tibagni.logviewer.view.Toast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class LogViewerView implements LogViewer.View {
  private LogViewerApplication application;

  private JTable logList;
  private JPanel mainPanel;
  private JTable filteredLogList;
  private JButton addNewFilterBtn;
  private JSplitPane logsPane;
  private JLabel currentLogsLbl;
  private FiltersList filtersPane;

  private final LogCellRenderer logRenderer;

  private final LogViewer.Presenter presenter;
  private final JFileChooserExt logFileChooser;
  private final JFileChooserExt filterSaveFileChooser;
  private final JFileChooserExt filterOpenFileChooser;
  private ProgressMonitorExt progressMonitor;

  private LogListTableModel logListTableModel;
  private LogListTableModel filteredLogListTableModel;
  private JFrame parent;

  private Set<LogStream> logStreams;

  final private LogViewerPreferences userPrefs;

  public LogViewerView(JFrame parent, LogViewerApplication application) {
    configureMenuBar(parent, false);

    this.application = application;
    this.parent = parent;
    userPrefs = LogViewerPreferences.getInstance();
    presenter = new LogViewerPresenter(this);
    presenter.init();

    logRenderer = new LogCellRenderer();

    this.parent.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.parent.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        presenter.finishing();
      }
    });

    logFileChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    filterSaveFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    filterOpenFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    userPrefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onDefaultFiltersPathChanged() {
        filterSaveFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
        filterOpenFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
      }

      @Override
      public void onDefaultLogsPathChanged() {
        logFileChooser.setCurrentDirectory(userPrefs.getDefaultLogsPath());
      }
    });

    addNewFilterBtn.addActionListener(e -> addGroup());
    setupFiltersContextActions();

    logList.setDefaultRenderer(LogEntry.class, logRenderer);
    filteredLogList.setDefaultRenderer(LogEntry.class, logRenderer);
    setupLogsContextActions();
    setupFilteredLogsContextActions();

    // Configure file drop
    new FileDrop(Logger.getDebugStream(), logsPane, files -> presenter.loadLogs(files));
  }

  private void configureMenuBar(JFrame frame, boolean showStreamsMenu) {
    JMenuBar menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');

    JMenuItem newWindowItem = new JMenuItem("New Window");
    newWindowItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    newWindowItem.addActionListener(e -> openNewWindow());
    fileMenu.add(newWindowItem);
    JMenuItem settingsItem = new JMenuItem("Settings");
    settingsItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    settingsItem.addActionListener(e -> openUserPreferences());
    fileMenu.add(settingsItem);
    menuBar.add(fileMenu);

    JMenu logsMenu = new JMenu("Logs");
    JMenuItem openLogsItem = new JMenuItem("Open Logs...");
    openLogsItem.addActionListener(e -> openLogs());
    logsMenu.add(openLogsItem);
    menuBar.add(logsMenu);
    JMenuItem refreshLogsItem = new JMenuItem("Refresh...");
    refreshLogsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    refreshLogsItem.addActionListener(e -> presenter.refreshLogs());
    logsMenu.add(refreshLogsItem);
    menuBar.add(logsMenu);

    JMenu filtersMenu = new JMenu("Filters");
    JMenuItem openFilterItem = new JMenuItem("Open Filters...");
    openFilterItem.addActionListener(e -> openFilters());
    filtersMenu.add(openFilterItem);
    menuBar.add(filtersMenu);

    if (showStreamsMenu) {
      configureStreamsMenu(menuBar);
    }

    JMenu helpMenu = new JMenu("Help");
    JMenuItem aboutItem = new JMenuItem("About");
    JMenuItem onlineHelpItem = new JMenuItem("User Guide");
    aboutItem.addActionListener(e -> AboutDialog.showAboutDialog(parent));
    onlineHelpItem.addActionListener(e -> openUserGuide());
    helpMenu.add(aboutItem);
    helpMenu.add(onlineHelpItem);
    menuBar.add(helpMenu);

    frame.setJMenuBar(menuBar);
  }

  private void configureStreamsMenu(JMenuBar menuBar) {
    JMenu streamsMenu = new JMenu("Streams");
    for (LogStream stream : logStreams) {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(stream.toString());
      item.setState(presenter.isStreamAllowed(stream));
      item.addItemListener(e -> presenter.setStreamAllowed(stream, item.isSelected()));
      streamsMenu.add(item);
    }

    menuBar.add(streamsMenu);
  }

  private void openUserGuide() {
    try {
      Desktop.getDesktop().browse(new URL(AppInfo.USER_GUIDE_URL).toURI());
    } catch (IOException | URISyntaxException e) {
      Logger.error("Failed to open online help", e);
    }
  }

  JPanel getContentPane() {
    return mainPanel;
  }

  @Override
  public void configureFiltersList(Map<String, List<Filter>> filters) {
    filtersPane.setFilters(new HashMap<>(filters));
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
  public void showCurrentLogsLocation(String logsPath) {
    String text = logsPath == null ?
        null :
        SwingUtils.truncateTextFor(
            currentLogsLbl,
            "Logs path:",
            logsPath,
            mainPanel.getWidth());

    currentLogsLbl.setText(text);
  }

  @Override
  public void showFilteredLogs(LogEntry[] logEntries) {
    filteredLogListTableModel.setLogs(logEntries);
    logList.updateUI();
    filtersPane.updateUI();
  }

  @Override
  public void showAvailableLogStreams(Set<LogStream> logStreams) {
    this.logStreams = logStreams;

    // We don't need to show the streams menu if there is only one stream
    boolean showStreams = logStreams != null && logStreams.size() > 1;

    // Reconfigure menu bar to show the streams if necessary
    logRenderer.showStreams(showStreams);
    configureMenuBar(parent, showStreams);
    SwingUtilities.updateComponentTreeUI(parent);
  }

  @Override
  public void showUnsavedFilterIndication(String group) {
    filtersPane.showUnsavedIndication(group, true);
  }

  @Override
  public void hideUnsavedFilterIndication(String group) {
    filtersPane.showUnsavedIndication(group, false);
  }

  @Override
  public LogViewer.UserSelection showAskToSaveFilterDialog(String group) {
    int userChoice = JOptionPane.showConfirmDialog(
        mainPanel.getParent(),
        group + " has unsaved changes. Do you want to save it?",
        "Unsaved changes",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);

    return LogViewer.convertFromSwing(userChoice);
  }

  @Override
  public File showSaveFilters(String group) {
    filterSaveFileChooser.resetChoosableFileFilters();
    filterSaveFileChooser.setMultiSelectionEnabled(false);
    filterSaveFileChooser.setDialogTitle("Save Filter...");
    filterSaveFileChooser.setSaveExtension(Filter.FILE_EXTENSION);
    int selectedOption = filterSaveFileChooser.showSaveDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      return filterSaveFileChooser.getSelectedFile();
    }
    return null;
  }

  @Override
  public void finish() {
    parent.dispose();
  }

  @Override
  public void showNavigationNextOver() {
    Toast.showToast(parent, StringUtils.LEFT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT);
  }

  @Override
  public void showNavigationPrevOver() {
    Toast.showToast(parent, StringUtils.RIGHT_ARROW_WITH_HOOK, Toast.LENGTH_SHORT);
  }

  private void setupFiltersContextActions() {
    filtersPane.setFiltersListener(new FiltersList.FiltersListener() {
      @Override
      public void onReordered(String group, int orig, int dest) {
       presenter.reorderFilters(group, orig, dest);
      }

      @Override
      public void onFiltersApplied() {
        presenter.applyFilters();
      }

      @Override
      public void onEditFilter(Filter filter) {
        // The filter is automatically updated by this dialog. We only check the result
        // to determine if the dialog was canceled or not
        Filter edited = EditFilterDialog.showEditFilterDialog(parent, filter);

        if (edited != null) {
          // Tell the presenter a filter was edited. It will not update the filters
          // as filters are updated by EditFilterDialog itself, it will only determine
          // if the filter was, in fact, updated and mark unsaved changes if necessary.
          presenter.filterEdited(filter);
        }
      }

      @Override
      public void onDeleteFilters(String group, int[] indices) {
        int userChoice = JOptionPane.showConfirmDialog(
            mainPanel.getParent(),
            "Are you sure you want to delete the selected filter(s)?",
            "Are you sure?",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (userChoice != JOptionPane.YES_OPTION) return;

        presenter.removeFilters(group, indices);
      }

      @Override
      public void onDeleteGroup(String group){
        int userChoice = JOptionPane.showConfirmDialog(
                mainPanel.getParent(),
                "Are you sure you want to delete this whole group?",
                "Are you sure?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if(userChoice != JOptionPane.YES_NO_OPTION) return;

        presenter.removeGroup(group);
      }

      @Override
      public void onNavigateNextFilteredLog(Filter filter) {
        int selectedFilteredLog = filteredLogList.getSelectedRow();
        int filteredLogIdx = presenter.getNextFilteredLogForFilter(filter, selectedFilteredLog);
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList, filteredLogIdx);
          filteredLogList.setRowSelectionInterval(filteredLogIdx, filteredLogIdx);
        }
      }

      @Override
      public void onNavigatePrevFilteredLog(Filter filter) {
        int selectedFilteredLog = filteredLogList.getSelectedRow();
        int filteredLogIdx = presenter.getPrevFilteredLogForFilter(filter, selectedFilteredLog);
        if (filteredLogIdx != -1) {
          SwingUtils.scrollToVisible(filteredLogList, filteredLogIdx);
          filteredLogList.setRowSelectionInterval(filteredLogIdx, filteredLogIdx);
        }
      }

      @Override
      public void onAddFilter(String group) {
        addFilter(group);
      }

      @Override
      public void onSaveFilters(String group) {
        saveFilter(group);
      }
    });
  }

  private void setupLogsContextActions() {
    logList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e) && logList.getSelectedRowCount() == 1) {
          JPopupMenu popup = new JPopupMenu();
          JMenuItem createFilterItem = popup.add("Create Filter from this line...");
          createFilterItem.addActionListener(l -> {
            LogEntry entry = (LogEntry) logListTableModel.getValueAt(logList.getSelectedRow(), 0);
            addFilterFromLogLine(entry.getLogText());
          });
          popup.show(logList, e.getX(), e.getY());
        }
      }
    });
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

  private void addGroup() {
    String newGroupName = JOptionPane.showInputDialog(parent,
        "What is the name of your new Filters Group?",
        "New Filters Group",
        JOptionPane.PLAIN_MESSAGE);

    if (!StringUtils.isEmpty(newGroupName)) {
      // If this name is already taken, a number will be appended to the end of the name
      String addedGroupName = presenter.addGroup(newGroupName);
      addFilter(addedGroupName);
    }
  }

  private void addFilter(String group) {
    Filter newFilter = EditFilterDialog.showEditFilterDialog(parent);
    if (newFilter != null) {
      presenter.addFilter(group, newFilter);
    }
  }

  private void addFilterFromLogLine(String logLine) {
    Filter filter = EditFilterDialog.showEditFilterDialogWithText(parent, logLine);
    if (filter != null) {
      List<String> groups = presenter.getGroups();
      String group = groups.size() == 1 ? groups.get(0) : null;

      if (StringUtils.isEmpty(group)) {
        String[] options = groups.toArray(new String[0]);
        int choice = JOptionPane.showOptionDialog(parent,
            "Which group do you want to add this filter to?",
            "Select Filter group",
            -1,
            JOptionPane.QUESTION_MESSAGE,
            null, options, null);

        if (choice >= 0) {
          group = options[choice];
        }
      }

      if (!StringUtils.isEmpty(group)) {
        presenter.addFilter(group, filter);
      }
    }
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

  private void saveFilter(String filtersGroup) {
    presenter.saveFilters(filtersGroup);
  }

  private void openFilters() {
    filterOpenFileChooser.resetChoosableFileFilters();
    filterOpenFileChooser.setFileFilter(new FileNameExtensionFilter("Filter files", Filter.FILE_EXTENSION));
    filterOpenFileChooser.setMultiSelectionEnabled(true);
    filterOpenFileChooser.setDialogTitle("Open Filters...");
    int selectedOption = filterOpenFileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadFilters(filterOpenFileChooser.getSelectedFiles());
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

  private void openNewWindow() {
    application.newLogViewerWindow();
  }

  private void openUserPreferences() {
    LogViewerPreferencesDialog.showPreferencesDialog(parent);
  }
}
