package net.flamgop.crypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class KeyCalculation {

    public static String calculateKeyA(String uid, int sector) throws NoSuchAlgorithmException {
        // Calculate the prefix and postfix
        BigInteger prefix = BigInteger.valueOf(3)
                .multiply(BigInteger.valueOf(5))
                .multiply(BigInteger.valueOf(23))
                .multiply(new BigInteger("38844225342798321268237511320137937"));

        BigInteger postfix = BigInteger.valueOf(3)
                .multiply(BigInteger.valueOf(7))
                .multiply(new BigInteger("9985861487287759675192201655940647"));

        // Convert prefix and postfix to their hexadecimal representations
        StringBuilder prefixHex = new StringBuilder(prefix.toString(16));
        while (prefixHex.length() < 32) {
            prefixHex.insert(0, "0");
        }

        StringBuilder postfixHex = new StringBuilder(postfix.toString(16));
        while (postfixHex.length() < 30) {
            postfixHex.insert(0, "0");
        }

        // Concatenate prefix, UID, and postfix
        String concatenated = prefixHex + uid + postfixHex;

        // Calculate SHA-1 digest
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Digest = sha1.digest(hexStringToByteArray(concatenated));

        // Extract key bytes
        byte[] keyBytes = new byte[]{sha1Digest[3], sha1Digest[2], sha1Digest[1], sha1Digest[0], sha1Digest[7], sha1Digest[6]};

        // Convert key bytes to hexadecimal representation
        StringBuilder keyHex = new StringBuilder();
        for (byte b : keyBytes) {
            keyHex.append(String.format("%02x", b));
        }

        return keyHex.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final char charGlyph_[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    public static String hexlify(byte[] bytes)
    {
        StringBuilder hexAscii = new StringBuilder(bytes.length * 2);

        for (int i=0; i < bytes.length; ++i)
        {
            byte b = bytes[i];
            hexAscii.append( charGlyph_[ (int)(b & 0xf0) >> 4] );
            hexAscii.append( charGlyph_[ (int)(b & 0x0f)] );
        }
        return hexAscii.toString();
    }
}
