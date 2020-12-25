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
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl;
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog;
import com.tibagni.logviewer.util.*;
import com.tibagni.logviewer.util.layout.GBConstraintsBuilder;
import com.tibagni.logviewer.util.scaling.UIScaleUtils;
import com.tibagni.logviewer.view.*;

import javax.swing.*;
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
  private final LogViewerApplication application;

  private JTable logList;
  private JPanel mainPanel;
  private JTable filteredLogList;
  private JButton addNewFilterGroupBtn;
  private JSplitPane logsPane;
  private JLabel currentLogsLbl;
  private FiltersList filtersPane;

  private JMenuItem saveFilteredLogs;

  private final LogCellRenderer logRenderer;

  private final LogViewer.Presenter presenter;
  private final JFileChooserExt logSaveFileChooser;
  private final JFileChooserExt logOpenFileChooser;
  private final JFileChooserExt filterSaveFileChooser;
  private final JFileChooserExt filterOpenFileChooser;
  private ProgressDialog progressDialog;

  private LogListTableModel logListTableModel;
  private LogListTableModel filteredLogListTableModel;
  private final JFrame parent;

  private Set<LogStream> logStreams;

  final private LogViewerPreferences userPrefs;

  public LogViewerView(JFrame parent, LogViewerApplication application, Set<File> initialLogFiles) {
    buildUi();
    configureMenuBar(parent, false);

    this.application = application;
    this.parent = parent;
    userPrefs = LogViewerPreferencesImpl.INSTANCE;
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

    logSaveFileChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    logOpenFileChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    filterSaveFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    filterOpenFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    userPrefs.addPreferenceListener(new LogViewerPreferencesImpl.Adapter() {
      @Override
      public void onDefaultFiltersPathChanged() {
        filterSaveFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
        filterOpenFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
      }

      @Override
      public void onDefaultLogsPathChanged() {
        logOpenFileChooser.setCurrentDirectory(userPrefs.getDefaultLogsPath());
      }
    });

    addNewFilterGroupBtn.addActionListener(e -> addGroup());
    setupFiltersContextActions();

    logList.setDefaultRenderer(LogEntry.class, logRenderer);
    filteredLogList.setDefaultRenderer(LogEntry.class, logRenderer);
    setupLogsContextActions();
    setupFilteredLogsContextActions();

    // Configure file drop
    new FileDrop(Logger.getDebugStream(), logsPane, presenter::loadLogs);

    // Load initial log files if any when component is shown
    if (initialLogFiles != null && !initialLogFiles.isEmpty()) {
      parent.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
          Logger.debug("Will load initial log files");
          parent.removeComponentListener(this);
          presenter.loadLogs(initialLogFiles.toArray(new File[0]));
        }
      });
    }
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
    logsMenu.addSeparator();
    saveFilteredLogs = new JMenuItem("Save Filtered Logs");
    saveFilteredLogs.addActionListener(e -> saveFilteredLogs());
    saveFilteredLogs.setEnabled(filteredLogListTableModel.getRowCount() > 0);
    logsMenu.add(saveFilteredLogs);
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

    menuBar.revalidate();
    menuBar.repaint();
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
  public void showLogs(List<LogEntry> logEntries) {
    logListTableModel.setLogs(logEntries);
  }

  @Override
  public void showCurrentLogsLocation(String logsPath) {
    Logger.debug("showCurrentLogsLocation: " + logsPath);
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
  public void showFilteredLogs(List<LogEntry> logEntries) {
    filteredLogListTableModel.setLogs(logEntries);
    logList.updateUI();
    filtersPane.updateUI();

    // Update the save menu option availability
    saveFilteredLogs.setEnabled(!logEntries.isEmpty());
  }

  @Override
  public void showAvailableLogStreams(Set<LogStream> logStreams) {
    this.logStreams = logStreams;

    // We don't need to show the streams menu if there is only one stream
    boolean showStreams = logStreams != null && logStreams.size() > 1;

    // Reconfigure menu bar to show the streams if necessary
    logRenderer.showStreams(showStreams);
    configureMenuBar(parent, showStreams);
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
            JOptionPane.DEFAULT_OPTION,
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
    logOpenFileChooser.resetChoosableFileFilters();
    logOpenFileChooser.setMultiSelectionEnabled(true);
    logOpenFileChooser.setDialogTitle("Open Logs...");
    int selectedOption = logOpenFileChooser.showOpenDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.loadLogs(logOpenFileChooser.getSelectedFiles());
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
      boolean keepCurrentFilters = false;
      if (!filtersPane.isEmpty()) {
        // Ask the user if we should keep the the existing filters
        int choice = JOptionPane.showOptionDialog(parent,
                "There are currently opened filters. Do you want to keep them?",
                "There are currently opened filters",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, new String[] {
                        "Keep existing filters and add the new one(s)",
                        "Open just the new filter(s)"},
                null);

        if (choice == -1) {
          // Dialog was canceled. Abort...
          return;
        }

        keepCurrentFilters = (choice == 0);
      }

      presenter.loadFilters(filterOpenFileChooser.getSelectedFiles(), keepCurrentFilters);
    }
  }

  private void saveFilteredLogs() {
    logSaveFileChooser.resetChoosableFileFilters();
    logSaveFileChooser.setMultiSelectionEnabled(false);
    logSaveFileChooser.setDialogTitle("Save Filtered Logs...");
    int selectedOption = logSaveFileChooser.showSaveDialog(mainPanel);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      presenter.saveFilteredLogs(logSaveFileChooser.getSelectedFile());
    }
  }

  @Override
  public void showStartLoading() {
    if (progressDialog == null) {
      progressDialog = ProgressDialog.showProgressDialog(parent);
    }
  }

  @Override
  public void showLoadingProgress(int progress, String note) {
    progressDialog.publishProgress(progress);
    progressDialog.updateProgressText(note);
  }

  @Override
  public void finishLoading() {
    progressDialog.finishProgress();
    progressDialog = null;
  }

  private void openNewWindow() {
    application.newLogViewerWindow(null);
  }

  private void openUserPreferences() {
    LogViewerPreferencesDialog.showPreferencesDialog(parent);
  }

  private void buildUi() {
    mainPanel = new JPanel();
    mainPanel.setLayout(new GridBagLayout());
    mainPanel.setPreferredSize(new Dimension(UIScaleUtils.dip(1000), UIScaleUtils.dip(500)));

    currentLogsLbl = new JLabel();
    currentLogsLbl.setAutoscrolls(true);
    currentLogsLbl.setText("");
    currentLogsLbl.setBorder(BorderFactory.createEmptyBorder(UIScaleUtils.dip(5),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(5),
            UIScaleUtils.dip(5)));
    mainPanel.add(currentLogsLbl,
        new GBConstraintsBuilder()
            .withGridx(1)
            .withGridy(0)
            .withAnchor(GridBagConstraints.WEST)
            .build());

    final JSplitPane mainSplitPane = new JSplitPane();
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(0.2);
    mainPanel.add(mainSplitPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withGridWidth(6)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());

    logsPane = new JSplitPane();
    logsPane.setDividerSize(UIScaleUtils.dip(10));
    logsPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    logsPane.setResizeWeight(0.6);

    logListTableModel = new LogListTableModel("All Logs");
    logList = new JTable(logListTableModel);
    logsPane.setLeftComponent(new JScrollPane(logList)); // Left or above (above in this case)

    filteredLogListTableModel = new LogListTableModel("Filtered Logs");
    filteredLogList = new JTable(filteredLogListTableModel);
    logsPane.setRightComponent(new JScrollPane(filteredLogList)); // Right or below (below in this case)

    mainSplitPane.setRightComponent(logsPane);

    final JPanel filtersMainPane = new JPanel();
    filtersMainPane.setLayout(new GridBagLayout());
    filtersMainPane.setBorder(BorderFactory.createTitledBorder("Filters"));
    final JPanel emptyPane = new JPanel();
    emptyPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    filtersMainPane.add(emptyPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(2)
            .withWeightx(1.0)
            .withAnchor(GridBagConstraints.SOUTH)
            .build());

    filtersPane = new FiltersList();
    filtersMainPane.add(new JScrollPane(filtersPane),
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(1)
            .withWeightx(1.0)
            .withWeighty(1.0)
            .withFill(GridBagConstraints.BOTH)
            .build());


    final JPanel filterButtonsPane = new JPanel();
    filterButtonsPane.setLayout(new FlowLayout(FlowLayout.CENTER));
    addNewFilterGroupBtn = new FlatButton();
    addNewFilterGroupBtn.setActionCommand("Add");
    addNewFilterGroupBtn.setText("New Group");
    filterButtonsPane.add(addNewFilterGroupBtn);

    filtersMainPane.add(filterButtonsPane,
        new GBConstraintsBuilder()
            .withGridx(0)
            .withGridy(0)
            .withWeightx(1.0)
            .withFill(GridBagConstraints.VERTICAL)
            .build());

    mainSplitPane.setLeftComponent(filtersMainPane);
  }
}
