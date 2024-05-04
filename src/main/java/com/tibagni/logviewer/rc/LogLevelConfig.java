package com.tibagni.logviewer.rc;

import com.tibagni.logviewer.util.StringUtils;

public class LogLevelConfig implements Config<LogLevelConfig.Level> {
    public enum Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
    public static final Level DEFAULT_LEVEL = Level.WARNING;
    private final Level levelConfig;

    public LogLevelConfig(String value) {
        levelConfig = parseValue(value);
    }

    private Level parseValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return DEFAULT_LEVEL;
        }

        switch (value.toLowerCase()) {
            case "verbose":
            case "v":
                return Level.VERBOSE;
            case "debug":
            case "d":
                return Level.DEBUG;
            case "info":
            case "i":
                return Level.INFO;
            case "warning":
            case "w":
                return Level.WARNING;
            case "error":
            case "e":
                return Level.ERROR;
        }

        return DEFAULT_LEVEL;
    }

    @Override
    public Level getConfigValue() {
        return levelConfig;
    }
}
