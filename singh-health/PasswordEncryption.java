import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PasswordEncryption {
    
    // Define a constant secret key (must be 16, 24, or 32 bytes for AES)
    private static final String SECRET_KEY = "A9v!$b@2z#D4x&E5"; // 16-byte key
    
    // Encrypt a password
    public static String encrypt(String password) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a password as a command-line argument.");
            return;
        }
        
        try {
            // Encrypt a password from command line input
            String password = args[0];
            String encryptedPassword = encrypt(password);
            System.out.println("Encrypted Password: " + encryptedPassword);    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
