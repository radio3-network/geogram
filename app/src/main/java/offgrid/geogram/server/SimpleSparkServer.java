package offgrid.geogram.server;

import static offgrid.geogram.core.Messages.log;
import static spark.Spark.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import offgrid.geogram.core.Central;
import offgrid.geogram.util.JsonUtils;
import offgrid.geogram.wifi.comm.WiFiMessage;
import offgrid.geogram.wifi.comm.WiFiReceiver;
import offgrid.geogram.wifi.messages.Message;

public class SimpleSparkServer implements Runnable {

    private static final String TAG_ID = "offgrid-server";
    private static final Gson gson = new Gson();

    @Override
    public void run() {
        ipAddress("0.0.0.0"); // Allow access from all network interfaces
        port(5050); // Set the port to 5050

        // Define the root route (/) to handle JSON POST requests
        post("/", (req, res) -> {
            res.type("application/json");

//            // Parse the incoming JSON object
//            JsonObject requestBody = gson.fromJson(req.body(), JsonObject.class);
//            if (requestBody == null) {
//                res.status(400);
//                return gson.toJson(createErrorResponse("Invalid JSON input"));
//            }

            //log(TAG_ID, "Received JSON request: " + requestBody);

            //Message message = JsonUtils.parseJson(requestBody.toString(), Message.class);
            //Gson gson = GsonUtils.createGson();

            String text = req.body();

            Message message;
            try{
                message = gson.fromJson(text, Message.class);
            } catch (Exception e){

                message = null;
            }

            if (message == null) {
                res.status(400);
                return gson.toJson(createErrorResponse("Invalid JSON input"));
            }

            Message reply = WiFiReceiver.processReceivedMessage(message);
            if(reply != null){
                return JsonUtils.convertToJsonText(reply);
            }else{
                res.status(400);
                return gson.toJson(createErrorResponse("Invalid message input"));
            }
           // return gson.toJson(response);
        });

        // Define a GET route for handling normal HTTP requests
        get("/", (req, res) -> {
            res.type("text/html");

            log(TAG_ID, "Received GET request at root route.");

            // Simple HTML response
            return "<html>" +
                    "<body>" +
                    "<h1>Welcome to SimpleSparkServer</h1>" +
                    "<p>The server is up and running.</p>" +
                    "</body>" +
                    "</html>";
        });

        log(TAG_ID, "Server is running on http://localhost:5050/");
    }

    // Helper method to create an error response JSON object
    private JsonObject createErrorResponse(String message) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("error", message);
        return errorResponse;
    }
}
