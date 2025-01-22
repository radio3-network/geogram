package offgrid.geogram.util;

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
