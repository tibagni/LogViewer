package com.tibagni.logviewer.mylogs;

import com.tibagni.logviewer.LogsRepository;
import com.tibagni.logviewer.MyLogsRepository;
import com.tibagni.logviewer.log.LogEntry;
import com.tibagni.logviewer.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages operations related to the 'My Logs' feature,
 * bridging the interaction between the current LogsRepository and MyLogsRepository.
 */
public class MyLogsManager {
    private final LogsRepository logsRepository;
    private final MyLogsRepository myLogsRepository;

    public MyLogsManager(LogsRepository logsRepository,
                         MyLogsRepository myLogsRepository) {
        this.logsRepository = logsRepository;
        this.myLogsRepository = myLogsRepository;
    }

    /**
     * Removes specific log entries from 'My Logs' based on their indices.
     * Invalid indices are ignored and a warning is logged.
     *
     * @param indices Array of indices corresponding to the log entries to be removed.
     */
    public void removeFromMyLog(int[] indices) {
        List<LogEntry> toRemove = new ArrayList<>();
        for (int i : indices) {
            int myLogsSize = myLogsRepository.getLogs().size();
            if (i < 0 || i >= myLogsSize) {
                Logger.warning("Trying to remove invalid index " + i + " from MyLogs. Current size: " + myLogsSize);
            } else {
                toRemove.add(myLogsRepository.getLogs().get(i));
            }
        }
        myLogsRepository.removeLogEntries(toRemove);
    }

    /**
     * Updates 'My Logs' when the main logs view changes.
     * It ensures that any log currently in 'My Logs' still maps to a valid entry
     * in the newly loaded/refreshed logs, updating indices if necessary.
     *
     * @return true if the logs under 'My Logs' were modified due to a mismatch, false otherwise.
     */
    public boolean updateMyLogs() {
        if (myLogsRepository.getLogs().isEmpty()) {
            return false;
        }

        boolean mismatch = false;
        for (LogEntry myLogEntry : myLogsRepository.getLogs()) {
            if (myLogEntry.getIndex() > logsRepository.getLastVisibleLogIndex()) {
                mismatch = true;
                break;
            }

            List<LogEntry> currentLogs = logsRepository.getCurrentlyOpenedLogs();
            if (myLogEntry.getIndex() >= 0 && myLogEntry.getIndex() < currentLogs.size() &&
                    !myLogEntry.equals(currentLogs.get(myLogEntry.getIndex()))) {
                mismatch = true;
                break;
            }
        }

        if (mismatch) {
            List<LogEntry> matchedLogs = new ArrayList<>();
            for (LogEntry myLogEntry : myLogsRepository.getLogs()) {
                LogEntry matchedLogEntry = logsRepository.getMatchingLogEntry(myLogEntry);
                if (matchedLogEntry != null) {
                    matchedLogs.add(matchedLogEntry);
                }
            }
            myLogsRepository.reset(matchedLogs);
            return true;
        }

        return false;
    }
}
