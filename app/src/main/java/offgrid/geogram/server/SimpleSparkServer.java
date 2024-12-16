package offgrid.geogram.server;

import static offgrid.geogram.core.Messages.log;
import static spark.Spark.*;

public class SimpleSparkServer implements Runnable {

    private static final String TAG_ID = "offgrid-server";

    @Override
    public void run() {
        // Set the port to 5050
        port(5050);

        // Define the route for /ask/
        get("/ask/", (req, res) -> {
            // Get the 'text' parameter from the query string
            String inputText = req.queryParams("text");
            if (inputText == null) {
                inputText = "No text provided";
            }

            log(TAG_ID, "Web request for /ask/: " + inputText);

            // Set response type to JSON
            res.type("application/json");

            // Create the JSON response
            return String.format("{ \"message\": \"Hello from Spark Java!\", \"input\": \"%s\" }", inputText);
        });

        log(TAG_ID, "Server is running on http://localhost:5050/ask/");
    }
}
