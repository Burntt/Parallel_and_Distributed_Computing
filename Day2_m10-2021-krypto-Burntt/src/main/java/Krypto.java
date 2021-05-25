import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Krypto {

    public static void main(final String[] args) throws InterruptedException {

        char[] alphabet = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};


        List<Concurrency> temps = new ArrayList<>();
        for (char c : alphabet) {
            String inputString = Character.toString(c);
            Concurrency temp = new Concurrency(inputString);
            temp.start();
            temps.add(temp);
        }
        temps.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e){

            }
        });
    }
}

class Concurrency extends Thread {

    private final String inputChar;

    Concurrency(String inputChar) {
        this.inputChar = inputChar;
    }

    @Override
    public void run() {
        try {
            if (true) {
                byte[] chs = new byte[10];
                chs[0] = (byte) inputChar.charAt(0);
                for (int maxLength = 1; maxLength < 10; ++maxLength) {
                    String firstSubtleHexSHA256 = findSubtle(chs, 1, maxLength);
                    if (firstSubtleHexSHA256 != null) {
                        System.out.println(firstSubtleHexSHA256);
                        return;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String findSubtle(byte[] chrs, int length, int maxLength) throws NoSuchAlgorithmException {
        if (length == maxLength) {
            if (hashDigest(chrs, length)) {
                return new String(chrs, 0, length);
            } else {
                return null;
            }
        }
        for (int i = 97; i < 97 + 26; i++) {
            chrs[length] = (byte) i;
            String outcome = findSubtle(chrs, length + 1, maxLength);
            if (outcome != null) {
                return outcome;
            }
        }
        return null;
    }

    public boolean hashDigest(byte[] chrs, int length) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] input = Arrays.copyOf(chrs, length);
        byte[] digest = messageDigest.digest(input);
        return (digest[0] == 0x20 && digest[1] == 0x20 && digest[2] == 0x05);
    }
}