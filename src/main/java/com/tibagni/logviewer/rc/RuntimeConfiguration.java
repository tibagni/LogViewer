package com.tibagni.logviewer.rc;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

public class RuntimeConfiguration {
    private static final Path RC_FILE_PATH = Paths.get(System.getProperty("user.home"), ".logviewer");
    private static RuntimeConfiguration instance;

    private final HashMap<String, Config<?>> runtimeConfigs = new HashMap<>();

    public static final String UI_SCALE = "uiscale";
    public static final String LOG_LEVEL = "loglevel";
    public static final String CRASH_REPORT = "crashreport";

    @NotNull
    static RuntimeConfiguration initializeForTest() {
        instance = new RuntimeConfiguration();
        return instance;
    }

    public static void initialize() {
        if (instance != null) {
            throw new IllegalStateException("RuntimeConfiguration was already initialized!");
        }

        instance = new RuntimeConfiguration(RC_FILE_PATH);
    }

    public static <T> T getConfig(String configName, Class<T> type) {
        if (instance.runtimeConfigs.containsKey(configName)) {
            Object config = instance.runtimeConfigs.get(configName);
            if (type.isInstance(config)) {
                //noinspection unchecked
                return (T) config;
            }
        }

        return null;
    }

    // For test only
    private RuntimeConfiguration() { }

    private RuntimeConfiguration(Path rcFilePath) {
        if (!Files.isRegularFile(rcFilePath)) {
            Logger.debug("Config file not found");
            return;
        }

        try (Stream<String> lines = Files.lines(rcFilePath)) {
            lines.filter(StringUtils::isNotEmpty).forEach(this::parseConfig);
        } catch (IOException e) {
            Logger.error("Could not read rc file", e);
        }
    }

    // Visible for testing
    void parseConfig(@NotNull String configLine) {
        String[] configParts = configLine.trim().split("=");

        if (configParts.length != 2) {
            Logger.error("Malformed config line: " + configLine);
            return;
        }

        String configName = configParts[0].toLowerCase();
        String configValue = configParts[1].toLowerCase();
        Config<?> config = null;
        switch (configName) {
            case UI_SCALE:
                config = new UIScaleConfig(configValue);
                break;
            case LOG_LEVEL:
                config = new LogLevelConfig(configValue);
                break;
            case CRASH_REPORT:
                config = new CrashReportConfig(configValue);
                break;
            default:
                Logger.error("Invalid config: " + configName);
                break;
        }

        if (config != null) {
            runtimeConfigs.put(configName, config);
        }
    }
}
