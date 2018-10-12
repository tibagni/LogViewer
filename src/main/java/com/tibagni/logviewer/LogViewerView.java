package com.tibagni.logviewer;

import com.tibagni.logviewer.about.AboutDialog;
import com.tibagni.logviewer.filter.EditFilterDialog;
import com.tibagni.logviewer.filter.Filter;
import com.tibagni.logviewer.filter.FilterCellRenderer;
import com.tibagni.logviewer.log.LogCellRenderer;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.log.LogListTableModel;
import com.tibagni.logviewer.log.LogStream;
import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.preferences.LogViewerPreferences;
import com.tibagni.logviewer.preferences.LogViewerPreferencesDialog;
import com.tibagni.logviewer.util.*;
import com.tibagni.logviewer.view.Toast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

public class LogViewerView implements LogViewer.View {
  private LogViewerApplication application;

  private JTable logList;
  private JPanel mainPanel;
  private JTable filteredLogList;
  private JButton addNewFilterBtn;
  private ReorderableList<Filter> filtersList;
  private JButton applyFiltersBtn;
  private JSplitPane logsPane;
  private JLabel currentLogsLbl;

  private final LogViewer.Presenter presenter;
  private final JFileChooserExt logFileChooser;
  private final JFileChooserExt filterFileChooser;
  private ProgressMonitorExt progressMonitor;

  private LogListTableModel logListTableModel;
  private LogListTableModel filteredLogListTableModel;
  private JFrame parent;

  private Set<LogStream> logStreams;

  final private LogViewerPreferences userPrefs;

  private static final String UNSAVED_INDICATOR = " (*)";

  public LogViewerView(JFrame parent, LogViewerApplication application) {
    configureMenuBar(parent);
    this.application = application;
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

    logFileChooser = new JFileChooserExt(userPrefs.getDefaultLogsPath());
    filterFileChooser = new JFileChooserExt(userPrefs.getDefaultFiltersPath());
    userPrefs.addPreferenceListener(new LogViewerPreferences.Adapter() {
      @Override
      public void onDefaultFiltersPathChanged() {
        filterFileChooser.setCurrentDirectory(userPrefs.getDefaultFiltersPath());
      }

      @Override
      public void onDefaultLogsPathChanged() {
        logFileChooser.setCurrentDirectory(userPrefs.getDefaultLogsPath());
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

    // Configure file drop
    new FileDrop(Logger.getDebugStream(), logsPane, files -> presenter.loadLogs(files));
  }

  private void configureMenuBar(JFrame frame) {
    ImageIcon newWindowIcon = SwingUtils
        .getIconFromResource(this, "Icons/new_window.png");
    ImageIcon settingsIcon = SwingUtils
        .getIconFromResource(this, "Icons/settings.png");
    ImageIcon saveIcon = SwingUtils
        .getIconFromResource(this, "Icons/save_file.png");
    ImageIcon openIcon = SwingUtils
        .getIconFromResource(this, "Icons/open_file.png");

    JMenuBar menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');

    JMenuItem newWindowItem = new JMenuItem("New Window", newWindowIcon);
    newWindowItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    newWindowItem.addActionListener(e -> openNewWindow());
    fileMenu.add(newWindowItem);
    JMenuItem settingsItem = new JMenuItem("Settings", settingsIcon);
    settingsItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    settingsItem.addActionListener(e -> openUserPreferences());
    fileMenu.add(settingsItem);
    menuBar.add(fileMenu);

    JMenu logsMenu = new JMenu("Logs");
    JMenuItem openLogsItem = new JMenuItem("Open Logs...", openIcon);
    openLogsItem.addActionListener(e -> openLogs());
    logsMenu.add(openLogsItem);
    menuBar.add(logsMenu);

    JMenu filtersMenu = new JMenu("Filter");
    JMenuItem openFilterItem = new JMenuItem("Open Filter...", openIcon);
    openFilterItem.addActionListener(e -> openFilter());
    JMenuItem saveFilterItem = new JMenuItem("Save Filter...", saveIcon);
    saveFilterItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    saveFilterItem.addActionListener(e -> saveFilter());
    filtersMenu.add(openFilterItem);
    filtersMenu.add(saveFilterItem);
    menuBar.add(filtersMenu);

    configureStreamsMenu(menuBar);

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
    if (logStreams == null || logStreams.size() <= 1) {
      // We don't need to show the streams menu if there is only one stream
      return;
    }

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
  public void showCurrentLogsLocation(String logsPath) {
    String text = SwingUtils.truncateTextFor(
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
    filtersList.updateUI();
  }

  @Override
  public void showAvailableLogStreams(Set<LogStream> logStreams) {
    this.logStreams = logStreams;

    // Reconfigure menu bar to show the streams if necessary
    configureMenuBar(parent);
    SwingUtilities.updateComponentTreeUI(parent);
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
  public LogViewer.UserSelection showAskToSaveFilterDialog() {
    int userChoice = JOptionPane.showConfirmDialog(
        mainPanel.getParent(),
        "There are unsaved changes to your filters, do you want to save it?",
        "Unsaved changes",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);

    return LogViewer.convertFromSwing(userChoice);
  }

  @Override
  public void showSaveFilter() {
    saveFilter();
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

  private void openNewWindow() {
    application.newLogViewerWindow();
  }

  private void openUserPreferences() {
    LogViewerPreferencesDialog.showPreferencesDialog(parent);
  }
}
