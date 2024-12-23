package offgrid.geogram.settings;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsLoader {

    private static final String SETTINGS_FILE_NAME = "settings.json";

    public static SettingsUser loadSettings(Context context) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        File settingsFile = new File(context.getFilesDir(), SETTINGS_FILE_NAME);

        // Check if the settings file exists
        if (settingsFile.exists()) {
            try (FileReader reader = new FileReader(settingsFile)) {
                // Load settings from file
                return gson.fromJson(reader, SettingsUser.class);
            } catch (IOException e) {
                e.printStackTrace();
                // If reading fails, create default settings
                return createDefaultSettings(settingsFile, gson);
            }
        } else {
            // If the file does not exist, create default settings
            return createDefaultSettings(settingsFile, gson);
        }
    }

    private static SettingsUser createDefaultSettings(File settingsFile, Gson gson) {
        // Create default settings
        SettingsUser defaultSettings = new SettingsUser();
        defaultSettings.nickname = "User";
        defaultSettings.intro = "Welcome to Geogram!";
        defaultSettings.invisibleMode = false;
        defaultSettings.npub = "001";
        defaultSettings.nsec = "002";
        defaultSettings.beaconType = "person";
        defaultSettings.idGroup = "00001";
        defaultSettings.idDevice = "00001";

        // Save default settings to disk
        try (FileWriter writer = new FileWriter(settingsFile)) {
            gson.toJson(defaultSettings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return defaultSettings;
    }

    public static void saveSettings(Context context, SettingsUser settings) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        File settingsFile = new File(context.getFilesDir(), SETTINGS_FILE_NAME);

        try (FileWriter writer = new FileWriter(settingsFile)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
