import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class KeyHash {
    public static String getSHA256(String input)
    {
         MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "error";
        }

        byte [] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

        BigInteger number = new BigInteger(1, hash);
 
        StringBuilder hexString = new StringBuilder(number.toString(16));
 
        while (hexString.length() < 64)
        {
            hexString.insert(0, '0');
        }
 
        return hexString.toString();
    }
}
