package com.tibagni.logviewer.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager responsible for loading and registering custom fonts
 * used within the application for logs and UI components.
 */
public class LogFontManager {
    private static final String[] FONT_NAMES = {
        "JetBrains Mono",
        "Fira Code",
        "Cascadia Code",
        "Source Code Pro",
        "Hack",
        "IBM Plex Mono",
        "Roboto Mono",
        "Inconsolata",
        "Ubuntu Mono",
        "Space Mono",
        "Anonymous Pro",
        "CommitMono",
        "Victor Mono",
        "Monaspace Neon",
        "Monospaced"
    };
    private static final Map<String, Font> loadedFonts = new HashMap<>();

    private LogFontManager() {
        // Prevent instantiation
    }

    /**
     * Initializes and registers all custom embedded fonts.
     * Must be called during application startup.
     */
    public static void init() {
        // Register fonts
        registerFont("/fonts/JetBrainsMono-Regular.ttf", "JetBrains Mono");
        registerFont("/fonts/FiraCode-Regular.ttf", "Fira Code");
        registerFont("/fonts/CascadiaCode-Regular.ttf", "Cascadia Code");
        registerFont("/fonts/SourceCodePro-Regular.ttf", "Source Code Pro");
        registerFont("/fonts/Hack-Regular.ttf", "Hack");
        registerFont("/fonts/IBMPlexMono-Regular.ttf", "IBM Plex Mono");
        registerFont("/fonts/RobotoMono-Regular.ttf", "Roboto Mono");
        registerFont("/fonts/Inconsolata-Regular.ttf", "Inconsolata");
        registerFont("/fonts/UbuntuMono-Regular.ttf", "Ubuntu Mono");
        registerFont("/fonts/SpaceMono-Regular.ttf", "Space Mono");
        registerFont("/fonts/AnonymousPro-Regular.ttf", "Anonymous Pro");
        registerFont("/fonts/CommitMono-Regular.ttf", "CommitMono");
        registerFont("/fonts/VictorMono-Regular.ttf", "Victor Mono");
        registerFont("/fonts/MonaspaceNeon.ttf", "Monaspace Neon");
    }

    /**
     * Registers an individual font from the classpath.
     * @param path The resource path to the TTF/OTF font file.
     * @param name The friendly name to associate with this font.
     */
    private static void registerFont(String path, String name) {
        try (InputStream is = LogFontManager.class.getResourceAsStream(path)) {
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                loadedFonts.put(name, font);
            }
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtains a Font instance with the requested family, style, and size.
     * @param family The font family name.
     * @param style The font style (e.g. Font.PLAIN, Font.BOLD).
     * @param size The font size in points.
     * @return The configured Font object.
     */
    public static Font getFont(String family, int style, int size) {
        if ("Monospaced".equals(family)) {
            return new Font(Font.MONOSPACED, style, size);
        }
        return new Font(family, style, size);
    }

    /**
     * Gets an array of all available font names supported by the app.
     * @return Array of supported font family names.
     */
    public static String[] getSupportedFonts() {
        return FONT_NAMES;
    }
}
