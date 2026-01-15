package com.example.restservice.ebcdic;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Utility class for converting between Java types and EBCDIC byte representations.
 * 
 * Supports:
 * - Character data (PIC X)
 * - Display numeric (PIC 9)
 * - Packed decimal (PIC S9(n)V99 COMP-3)
 */
public class EbcdicConverter {
    
    // EBCDIC charset (IBM037 - US EBCDIC)
    public static final Charset EBCDIC_CHARSET = Charset.forName("IBM037");
    
    // ASCII/UTF-8 charset for comparison
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    
    /**
     * Converts a string to EBCDIC bytes, padding or truncating to the specified length.
     * 
     * @param value The string value to convert
     * @param length The target length in bytes
     * @return EBCDIC byte array of the specified length
     */
    public static byte[] stringToEbcdic(String value, int length) {
        byte[] result = new byte[length];
        // Fill with EBCDIC spaces (0x40)
        Arrays.fill(result, (byte) 0x40);
        
        if (value != null && !value.isEmpty()) {
            byte[] valueBytes = value.getBytes(EBCDIC_CHARSET);
            int copyLength = Math.min(valueBytes.length, length);
            System.arraycopy(valueBytes, 0, result, 0, copyLength);
        }
        
        return result;
    }
    
    /**
     * Converts EBCDIC bytes to a string.
     * 
     * @param bytes The EBCDIC bytes to convert
     * @return The decoded string
     */
    public static String ebcdicToString(byte[] bytes) {
        return new String(bytes, EBCDIC_CHARSET);
    }
    
    /**
     * Converts a long value to display numeric EBCDIC bytes (PIC 9(n)).
     * 
     * @param value The numeric value
     * @param length The target length in bytes
     * @return EBCDIC byte array representing the number
     */
    public static byte[] longToDisplayNumeric(long value, int length) {
        String formatted = String.format("%0" + length + "d", Math.abs(value));
        if (formatted.length() > length) {
            formatted = formatted.substring(formatted.length() - length);
        }
        return formatted.getBytes(EBCDIC_CHARSET);
    }
    
    /**
     * Converts display numeric EBCDIC bytes to a long value.
     * 
     * @param bytes The EBCDIC bytes
     * @return The numeric value
     */
    public static long displayNumericToLong(byte[] bytes) {
        String str = ebcdicToString(bytes).trim();
        if (str.isEmpty()) {
            return 0;
        }
        return Long.parseLong(str);
    }
    
    /**
     * Converts a BigDecimal to packed decimal (COMP-3) format.
     * 
     * Packed decimal format:
     * - Each byte contains two decimal digits (one per nibble)
     * - The last nibble contains the sign (C=positive, D=negative, F=unsigned)
     * - Example: +12345 in 3 bytes = 0x12 0x34 0x5C
     * 
     * @param value The decimal value
     * @param length The target length in bytes
     * @param decimalPlaces Number of implied decimal places
     * @param signed Whether the field is signed
     * @return Packed decimal byte array
     */
    public static byte[] decimalToPackedDecimal(BigDecimal value, int length, 
                                                  int decimalPlaces, boolean signed) {
        byte[] result = new byte[length];
        Arrays.fill(result, (byte) 0x00);
        
        // Scale the value to remove decimal point
        BigDecimal scaled = value.movePointRight(decimalPlaces);
        long longValue = scaled.longValue();
        boolean negative = longValue < 0;
        longValue = Math.abs(longValue);
        
        // Calculate the maximum number of digits we can store
        int maxDigits = (length * 2) - 1;  // Last nibble is sign
        
        // Convert to string and pad with zeros
        String digits = String.format("%0" + maxDigits + "d", longValue);
        if (digits.length() > maxDigits) {
            digits = digits.substring(digits.length() - maxDigits);
        }
        
        // Pack the digits
        int digitIndex = 0;
        for (int byteIndex = 0; byteIndex < length - 1; byteIndex++) {
            int highNibble = digits.charAt(digitIndex++) - '0';
            int lowNibble = digits.charAt(digitIndex++) - '0';
            result[byteIndex] = (byte) ((highNibble << 4) | lowNibble);
        }
        
        // Last byte: one digit + sign nibble
        int lastDigit = digits.charAt(digitIndex) - '0';
        int signNibble;
        if (signed) {
            signNibble = negative ? 0x0D : 0x0C;  // D=negative, C=positive
        } else {
            signNibble = 0x0F;  // F=unsigned
        }
        result[length - 1] = (byte) ((lastDigit << 4) | signNibble);
        
        return result;
    }
    
    /**
     * Converts packed decimal (COMP-3) bytes to a BigDecimal.
     * 
     * @param bytes The packed decimal bytes
     * @param decimalPlaces Number of implied decimal places
     * @return The decimal value
     */
    public static BigDecimal packedDecimalToDecimal(byte[] bytes, int decimalPlaces) {
        StringBuilder digits = new StringBuilder();
        boolean negative = false;
        
        // Process all bytes except the last nibble
        for (int i = 0; i < bytes.length - 1; i++) {
            int b = bytes[i] & 0xFF;
            digits.append(b >> 4);     // High nibble
            digits.append(b & 0x0F);   // Low nibble
        }
        
        // Last byte: one digit + sign
        int lastByte = bytes[bytes.length - 1] & 0xFF;
        digits.append(lastByte >> 4);  // Last digit
        int signNibble = lastByte & 0x0F;
        
        // Check sign: D=negative, B=negative, others=positive
        if (signNibble == 0x0D || signNibble == 0x0B) {
            negative = true;
        }
        
        // Create BigDecimal with proper scale
        String digitString = digits.toString();
        if (digitString.isEmpty()) {
            digitString = "0";
        }
        
        BigDecimal result = new BigDecimal(digitString);
        result = result.movePointLeft(decimalPlaces);
        
        if (negative) {
            result = result.negate();
        }
        
        return result;
    }
    
    /**
     * Extracts a field from a record byte array.
     * 
     * @param record The full record bytes
     * @param field The field definition
     * @return The field bytes
     */
    public static byte[] extractField(byte[] record, CopybookField field) {
        int offset = field.getOffset();
        int length = field.getLength();
        
        byte[] result = new byte[length];
        System.arraycopy(record, offset, result, 0, length);
        return result;
    }
    
    /**
     * Sets a field value in a record byte array.
     * 
     * @param record The full record bytes
     * @param field The field definition
     * @param fieldBytes The field value bytes
     */
    public static void setField(byte[] record, CopybookField field, byte[] fieldBytes) {
        int offset = field.getOffset();
        int length = Math.min(field.getLength(), fieldBytes.length);
        System.arraycopy(fieldBytes, 0, record, offset, length);
    }
    
    /**
     * Prints a hex dump of bytes for debugging.
     * 
     * @param bytes The bytes to dump
     * @return Hex string representation
     */
    public static String hexDump(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
