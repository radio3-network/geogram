package offgrid.geogram.settings;

import android.content.Context;
import android.util.Log;

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

        Log.i("SettingsLoader", "Settings file location: " + settingsFile.getAbsolutePath());

        if (settingsFile.exists()) {
            try (FileReader reader = new FileReader(settingsFile)) {
                return gson.fromJson(reader, SettingsUser.class);
            } catch (IOException e) {
                Log.e("SettingsLoader", "Failed to read settings file, creating default settings.", e);
                return createDefaultSettings(context, gson);
            }
        } else {
            Log.i("SettingsLoader", "Settings file not found, creating default settings.");
            return createDefaultSettings(context, gson);
        }
    }

    private static SettingsUser createDefaultSettings(Context context, Gson gson) {
        SettingsUser defaultSettings = new SettingsUser();
        defaultSettings.nickname = "User";
        defaultSettings.intro = "Welcome to Geogram!";
        defaultSettings.invisibleMode = false;
        defaultSettings.npub = "001";
        defaultSettings.nsec = "002";
        defaultSettings.beaconType = "person";
        defaultSettings.idGroup = "00001";
        defaultSettings.idDevice = "00001";

        saveSettings(context, defaultSettings);

        return defaultSettings;
    }

    public static void saveSettings(Context context, SettingsUser settings) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        File settingsFile = new File(context.getFilesDir(), SETTINGS_FILE_NAME);

        try (FileWriter writer = new FileWriter(settingsFile)) {
            gson.toJson(settings, writer);
            writer.flush(); // Ensure data is written to disk
            Log.i("SettingsLoader", "Settings saved successfully to: " + settingsFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SettingsLoader", "Error saving settings file", e);
        }
    }
}
