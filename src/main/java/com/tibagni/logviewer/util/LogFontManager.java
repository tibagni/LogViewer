package com.tibagni.logviewer.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

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

    private LogFontManager() {
        // Prevent instantiation
    }

    /**
     * Initializes and registers all custom embedded fonts.
     * Must be called during application startup.
     */
    public static void init() {
        // Register fonts (Regular and Bold styles)
        registerFont("/fonts/JetBrainsMono-Regular.ttf", "JetBrains Mono");
        registerFont("/fonts/JetBrainsMono-Bold.ttf", "JetBrains Mono");
        registerFont("/fonts/FiraCode-Regular.ttf", "Fira Code");
        registerFont("/fonts/FiraCode-Bold.ttf", "Fira Code");
        registerFont("/fonts/CascadiaCode-Regular.ttf", "Cascadia Code");
        registerFont("/fonts/CascadiaCode-Bold.ttf", "Cascadia Code");
        registerFont("/fonts/SourceCodePro-Regular.ttf", "Source Code Pro");
        registerFont("/fonts/SourceCodePro-Bold.ttf", "Source Code Pro");
        registerFont("/fonts/Hack-Regular.ttf", "Hack");
        registerFont("/fonts/Hack-Bold.ttf", "Hack");
        registerFont("/fonts/IBMPlexMono-Regular.ttf", "IBM Plex Mono");
        registerFont("/fonts/IBMPlexMono-Bold.ttf", "IBM Plex Mono");
        registerFont("/fonts/RobotoMono-Regular.ttf", "Roboto Mono");
        registerFont("/fonts/RobotoMono-Bold.ttf", "Roboto Mono");
        registerFont("/fonts/Inconsolata-Regular.ttf", "Inconsolata");
        registerFont("/fonts/Inconsolata-Bold.ttf", "Inconsolata");
        registerFont("/fonts/UbuntuMono-Regular.ttf", "Ubuntu Mono");
        registerFont("/fonts/UbuntuMono-Bold.ttf", "Ubuntu Mono");
        registerFont("/fonts/SpaceMono-Regular.ttf", "Space Mono");
        registerFont("/fonts/SpaceMono-Bold.ttf", "Space Mono");
        registerFont("/fonts/AnonymousPro-Regular.ttf", "Anonymous Pro");
        registerFont("/fonts/AnonymousPro-Bold.ttf", "Anonymous Pro");
        registerFont("/fonts/CommitMono-Regular.ttf", "CommitMono");
        registerFont("/fonts/CommitMono-Bold.ttf", "CommitMono");
        registerFont("/fonts/VictorMono-Regular.ttf", "Victor Mono");
        registerFont("/fonts/VictorMono-Bold.ttf", "Victor Mono");
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
        if (family == null || Font.MONOSPACED.equalsIgnoreCase(family) || !isFamilyAvailable(family)) {
            return new Font(Font.MONOSPACED, style, size);
        }
        return new Font(family, style, size);
    }

    /**
     * Checks if the specified font family is available and supported.
     *
     * @param family The font family name to check.
     * @return true if the font family is supported, false otherwise.
     */
    private static boolean isFamilyAvailable(String family) {
        for (String name : FONT_NAMES) {
            if (name.equals(family)) return true;
        }
        return false;
    }

    /**
     * Gets an array of all available font names supported by the app.
     * @return Array of supported font family names.
     */
    public static String[] getSupportedFonts() {
        return FONT_NAMES;
    }
}
