package offgrid.geogram.util;

import com.google.gson.Gson;

import offgrid.geogram.wifi.messages.Message;
import offgrid.geogram.wifi.messages.MessageHello_v1;

public class JsonUtils {

    public static String convertToJsonText(Object object) {
        if (object == null) {
            return null;
        }
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.toJson(object);
    }


    public static <T> T parseJson(String jsonString, Class<T> objectClass) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(jsonString, objectClass);
    }
}
