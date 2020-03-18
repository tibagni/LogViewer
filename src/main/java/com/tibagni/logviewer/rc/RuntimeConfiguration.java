package com.tibagni.logviewer.rc;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class RuntimeConfiguration {
    private static final Path RC_FILE_PATH = Paths.get(System.getProperty("user.home"), ".logviewer");
    private static RuntimeConfiguration instance;

    private HashMap<String, Config> runtimeConfigs = new HashMap<>();

    public static final String UI_SCALE = "uiscale";

    public static void initialize() {
        if (instance != null) {
            throw new IllegalStateException("RuntimeConfiguration was already initialized!");
        }

        instance = new RuntimeConfiguration();
    }

    public static <T> T getConfig(String configName, Class<T> type) {
        if (instance.runtimeConfigs.containsKey(configName)) {
            return (T) instance.runtimeConfigs.get(configName);
        }

        return null;
    }

    public static Config getConfig(String configName) {
        return getConfig(configName, Config.class);
    }

    private RuntimeConfiguration() {
        try {
            Files.lines(RC_FILE_PATH)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(this::parseConfig);
        } catch (IOException e) {
            Logger.error("Could not read rc file", e);
        }
    }

    private void parseConfig(String configLine) {
        String[] configParts = configLine.trim().split("=");

        if (configParts.length != 2) {
            Logger.error("Malformed config line: " + configLine);
            return;
        }

        String configName = configParts[0].toLowerCase();
        String configValue = configParts[1].toLowerCase();
        Config config = null;
        switch (configName) {
            case UI_SCALE:
                config = new UIScaleConfig(configValue);
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
