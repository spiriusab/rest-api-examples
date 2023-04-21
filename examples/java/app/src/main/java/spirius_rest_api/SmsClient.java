package spirius_rest_api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;

import static java.util.Arrays.asList;

public class SmsClient {
    private static final String AUTH_VERSION = "SpiriusSmsV1";
    private static final String BASE_URL = "https://rest.spirius.com/v1";
    private final String username;
    private final SecretKeySpec sharedKey;

    public SmsClient(String sharedKey, String username) {
        this.username = username;
        this.sharedKey = new SecretKeySpec(sharedKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public void sendSMS(final String message, final String to, final String from)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mt/send";
        final var verb = "POST";

        // Using LinkedHashMap here since it implements SortedMap, which makes debugging easier
        var request_body = new LinkedHashMap<String, String>();
        request_body.put("message", message);
        request_body.put("to", to);
        request_body.put("from", from);

        perform_request(verb, path, request_body);
    }

    public void getMessageStatus(final String transactionId)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mo/status/" + transactionId;
        final var verb = "GET";

        perform_request(verb, path);
    }

    public void getMessageList(final String transactionId)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mo" + transactionId;
        final var verb = "GET";

        perform_request(verb, path);
    }

    public void getMoMessage(final String transactionId)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mo" + transactionId;
        final var verb = "GET";

        perform_request(verb, path);
    }

    public void popMoMessage(final String transactionId)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mo" + transactionId;
        final var verb = "DELETE";

        perform_request(verb, path);
    }

    public void popNextMessage(final String transactionId)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final var path = "/sms/mo/next" + transactionId;
        final var verb = "DELETE";

        perform_request(verb, path);
    }

    private void perform_request(String http_verb, String path)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, InterruptedException {
        perform_request(http_verb, path, null);
    }
    private void perform_request(String http_verb, String path, LinkedHashMap<String, String> body)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, InterruptedException {
        var requestBody = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
                .writeValueAsString(body);

        final var unixTime = System.currentTimeMillis() / 1_000L;

        var signature = this.createSignature(path, unixTime, http_verb, requestBody);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/sms/mt/send"))
                .header("X-SMS-Timestamp", Long.toString(unixTime))
                .header("Authorization", AUTH_VERSION + " " + this.username + ":" + signature)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        var response = HttpClient
                .newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    private String createSignature(String path, long unixTime, String http_verb, String requestBody) throws NoSuchAlgorithmException, InvalidKeyException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(requestBody.getBytes(StandardCharsets.UTF_8));

        var messageToSign = String.join("\n", asList(
                AUTH_VERSION,
                Long.toString(unixTime),
                http_verb,
                path,
                bytesToHex(crypt.digest())
        ));
        var bytesToSign = messageToSign.getBytes(StandardCharsets.UTF_8);

        var mac = Mac.getInstance("HmacSHA256");
        mac.init(this.sharedKey);
        byte[] macData = mac.doFinal(bytesToSign);
        return Base64.getEncoder().encodeToString(macData);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}

