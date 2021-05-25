import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TestKrypto {
    @Test
    public void testApp() throws InterruptedException {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(bos));
            Krypto.main(null);
        } finally {
            System.setOut(originalOut);
        }

        Set<Character> actualCharacters = new HashSet<>();
        Set<Character> expectedCharacters = new HashSet<>();

        for (String line : bos.toString().split("\\r?\\n")) {
            if (line.isEmpty()) {
                continue;
            }
            assertTrue(isSubtle(line), "string [" + line + "] is not subtle");
            char ch = line.charAt(0);
            int actualLength = line.length();
            int expectedLength = 6;
            if ("acdlmnsuy".indexOf(ch) != -1) {
                expectedLength = 7;
            }
            if ("bi".indexOf(ch) != -1) {
                expectedLength = 5;
            }
            assertEquals(expectedLength, actualLength,
                    "string length differs for character [" + Character.toString(ch) + "]");
            actualCharacters.add(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ch++) {
            expectedCharacters.add(ch);
        }
        assertEquals(expectedCharacters, actualCharacters);
    }

    private boolean isSubtle(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s.getBytes("UTF-8"));
            byte[] digest = md.digest();
            if (digest[0] == 0x20 && digest[1] == 0x20 && digest[2] == 0x05) {
                return true;
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return false;
        }
        return false;
    }
}