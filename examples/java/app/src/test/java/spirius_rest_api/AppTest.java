package spirius_rest_api;

import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AppTest {
    @Test
    public void testSendingSms() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        var client = new SmsClient(
                "e28f051f3e23de3b056c9e0f8653b610f76f79d5837415d88e4bb8f79d7e0b8d",
                "SomeUser"
        );

        client.sendSMS("Hello world!", "+46123456789", "SPIRIUS");
    }
}