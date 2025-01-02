/**
 * The {@code BluePackage} class facilitates the transmission of large data as smaller parcels
 * over a Bluetooth communication channel. It handles parcel splitting, parcel tracking, and provides
 * utility methods to manage and request specific parcels.
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * BlueRequestData requestData = new BlueRequestData("HelloWorldExampleData");
 *
 * // Send parcels
 * while (!requestData.isComplete()) {
 *     String parcel = requestData.getNextParcel();
 *     sendParcel(parcel);  // Hypothetical method to send a parcel
 * }
 *
 * // Retrieve a specific parcel if needed
 * String specificParcel = requestData.getSpecificParcel(2);
 * System.out.println("Parcel 2: " + specificParcel);
 *
 * // Check the status of the data transfer
 * if (!requestData.isTransferring()) {
 *     System.out.println("Data transfer complete.");
 * }
 *
 * // Receiving side example:
 * BluePackage receiver = new BlueRequestData("AA:003"); // Header parcel received
 * receiver.receiveParcel("AA001:DataPart1");
 * receiver.receiveParcel("AA003:DataPart3");
 * receiver.receiveParcel("AA002:DataPart2");
 * System.out.println("Complete data: " + receiver.getData());
 * }</pre>
 *
 * This class is essential for managing data transfers where the size of a single transmission is limited.
 */
package offgrid.geogram.bluetooth.comms;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Arrays;

public class BluePackage {

    // Random two bytes generated as ID
    private final String id;

    // The length of text per parcel
    private static final int TEXT_LENGTH_PER_PARCEL = 10;

    // Total number of parcels in the message
    private final int messageParcelsTotal;

    // Current parcel index
    private int messageParcelCurrent;

    // The complete data message
    private final String data;

    // The individual data parcels
    private String[] dataParcels;

    // Timestamp when data transmission started
    private final long transmissionStartTime;

    private final String checksum;

    // Indicates whether data is still being transferred
    private boolean isTransferring;

    public static BluePackage createSender(String data) {
        return new BluePackage(data, true);
    }

    /**
     * Creates a new BluePackage instance for receiving data.
     * @param header is the initial message received from the other device
     * @return a new BluePackage instance for receiving data
     */
    public static BluePackage createReceiver(String header) {
        return new BluePackage(header, false);
    }

    private BluePackage(String input, boolean isSender) {
        if (isSender) {
            if (input == null) {
                throw new IllegalArgumentException("Data cannot be null");
            }
            this.id = generateRandomId();
            this.data = input;
            this.messageParcelCurrent = -1;
            this.transmissionStartTime = System.currentTimeMillis();
            this.isTransferring = true;
            this.messageParcelsTotal = (int) Math.ceil((double) this.data.length() / TEXT_LENGTH_PER_PARCEL);
            this.checksum = calculateChecksum(input);
            splitDataIntoParcels();
        } else {
            String[] parts = input.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid header format");
            }
            this.id = parts[0];
            this.messageParcelsTotal = Integer.parseInt(parts[1]);
            this.dataParcels = new String[messageParcelsTotal];
            this.data = null;
            this.messageParcelCurrent = -1;
            this.transmissionStartTime = System.currentTimeMillis();
            this.isTransferring = true;
            this.checksum = parts[2]; // don't calculate the checksum initially
        }
    }



    /**
     * Splits the data into smaller parcels based on TEXT_LENGTH_PER_PARCEL.
     * Each parcel will contain at most {@code TEXT_LENGTH_PER_PARCEL} characters.
     */
    private void splitDataIntoParcels() {
        int dataLength = data.length();
        dataParcels = new String[messageParcelsTotal];

        for (int i = 0; i < messageParcelsTotal; i++) {
            int start = i * TEXT_LENGTH_PER_PARCEL;
            int end = Math.min(start + TEXT_LENGTH_PER_PARCEL, dataLength);
            dataParcels[i] = data.substring(start, end);
        }
    }

    /**
     * Receives a parcel and stores it in the appropriate slot based on its ID.
     *
     * @param parcel The parcel received (format: "AA###:parcelText").
     * @throws IllegalArgumentException If the parcel format is invalid or the ID does not match.
     */
    public void receiveParcel(String parcel) {
        String[] parts = parcel.split(":", 2);
        if (parts.length != 2 || !parts[0].startsWith(id)) {
            throw new IllegalArgumentException("Invalid parcel format or ID mismatch");
        }

        int parcelIndex = Integer.parseInt(parts[0].substring(id.length()));
        if (parcelIndex >= 0 && parcelIndex < messageParcelsTotal) {
            dataParcels[parcelIndex] = parts[1];
        }
    }

    /**
     * Generates a unique random ID using two bytes for each data transmission.
     *
     * @return A unique 2-byte random ID as a hexadecimal string.
     */
    public String generateRandomId() {
        Random random = new Random();
        char firstChar = (char) ('A' + random.nextInt(26)); // Random letter A-Z
        char secondChar = (char) ('A' + random.nextInt(26)); // Random letter A-Z
        return "" + firstChar + secondChar;
    }

    /**
     * Retrieves the next data parcel to send. Automatically increments the current parcel index.
     *
     * @return The next parcel as a string, or {@code null} if all parcels have been sent.
     */
    public String getNextParcel() {

        if (messageParcelCurrent == -1) {
            messageParcelCurrent++;
            // First parcel is the header with ID and total parcel count
            return String.format(Locale.US, "%s:%03d:%s", id, messageParcelsTotal, checksum);
        } else if (messageParcelCurrent < messageParcelsTotal) {
            // Subsequent parcels contain the data
            String parcelId = String.format(Locale.US, "%s%03d", id, messageParcelCurrent);
            String parcelData = dataParcels[messageParcelCurrent];
            messageParcelCurrent++;
            return parcelId + ":" + parcelData  ;
        }
        return null;
    }


    /**
     * Requests a specific parcel by its index (1-based).
     * This is useful for retransmitting lost parcels during communication.
     *
     * @param index The index of the parcel to retrieve (1-based).
     * @return The requested parcel as a string, or {@code null} if the index is invalid.
     */
    public String getSpecificParcel(int index) {
        if (index > 0 && index <= messageParcelsTotal) {
            return dataParcels[index - 1];
        }
        return null;
    }

    /**
     * Checks whether all parcels have been sent in sequence.
     *
     * @return {@code true} if all parcels are sent, {@code false} otherwise.
     */
    public boolean hasSentAllParcelsOnce() {
        return messageParcelCurrent >= messageParcelsTotal;
    }

    /**
     * Checks whether all parcels have been received.
     *
     * @return {@code true} if all parcels are received, {@code false} otherwise.
     */
    public boolean allParcelsReceivedAndValid() {
        if (dataParcels == null) {
            return false;
        }

        // it is false when there are still null sections on the array
        if(!Arrays.stream(dataParcels).allMatch(Objects::nonNull)){
            return false;
        }

        // fields should be complete now, but do they match the checksum?
        String data = String.join("", dataParcels);;
        String checksumReceived = calculateChecksum(data);
        return checksumReceived.equals(this.checksum);
    }

    /**
     * Marks the transfer as complete or still ongoing.
     *
     * @param transferring {@code true} if data is still being transferred, {@code false} otherwise.
     */
    public void setTransferring(boolean transferring) {
        this.isTransferring = transferring;
    }

    /**
     * Checks if data is still being transferred.
     *
     * @return {@code true} if data is being transferred, {@code false} otherwise.
     */
    public boolean isTransferring() {
        return isTransferring;
    }

    /**
     * Retrieves the unique ID for this data transmission.
     *
     * @return The unique ID as a string.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the length of each parcel.
     *
     * @return The parcel length as an integer.
     */
    public int getTextLengthPerParcel() {
        return TEXT_LENGTH_PER_PARCEL;
    }

    /**
     * Retrieves the total number of parcels for the complete message.
     *
     * @return The total number of parcels as an integer.
     */
    public int getMessageParcelsTotal() {
        return messageParcelsTotal;
    }

    /**
     * Retrieves the current parcel index that is being processed.
     *
     * @return The current parcel index as an integer.
     */
    public int getMessageParcelCurrent() {
        return messageParcelCurrent;
    }

    /**
     * Retrieves the complete data message by combining all received parcels.
     *
     * @return The full data message as a string, or null if parcels are missing.
     */
    public String getData() {
        if (!allParcelsReceivedAndValid()) {
            return null;
        }
        return String.join("", dataParcels);
    }

    /**
     * Retrieves all data parcels as an array.
     *
     * @return An array of strings representing the data parcels.
     */
    public String[] getDataParcels() {
        return dataParcels;
    }

    /**
     * Retrieves the timestamp when data transmission started.
     *
     * @return The timestamp as a long value.
     */
    public long getTransmissionStartTime() {
        return transmissionStartTime;
    }

    /**
     * Calculates a 4-letter checksum for the given data.
     *
     * @param data The input data for which to calculate the checksum.
     * @return A 4-letter checksum.
     */
    public String calculateChecksum(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        int sum = 0;
        for (char c : data.toCharArray()) {
            sum += c; // Add ASCII value of each character
        }

        // Reduce the sum to 4 letters
        char[] checksum = new char[4];
        for (int i = 0; i < 4; i++) {
            checksum[i] = (char) ('A' + (sum % 26));
            sum /= 26; // Shift to the next letter
        }
        return new String(checksum);
    }
}
