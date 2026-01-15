package com.example.restservice.ebcdic.process1;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * PROCESS 1: Low-level encoder for converting Java types to EBCDIC byte representations.
 * 
 * Supports:
 * - Alpha fields (PIC X) - Character data in EBCDIC encoding
 * - Display Numeric (PIC 9) - Numeric characters in EBCDIC
 * - Packed Decimal (PIC S9(n)V99 COMP-3) - Compressed numeric with sign
 */
public class EbcdicFieldEncoder {
    
    /** EBCDIC charset (IBM037 - US EBCDIC) */
    public static final Charset EBCDIC = Charset.forName("IBM037");
    
    /** EBCDIC space character */
    public static final byte EBCDIC_SPACE = 0x40;
    
    /**
     * Encodes a string to EBCDIC bytes (PIC X).
     * Pads with EBCDIC spaces if shorter, truncates if longer.
     * 
     * @param value The string to encode
     * @param length Target length in bytes
     * @return EBCDIC encoded bytes
     */
    public static byte[] encodeAlpha(String value, int length) {
        byte[] result = new byte[length];
        Arrays.fill(result, EBCDIC_SPACE);
        
        if (value != null && !value.isEmpty()) {
            byte[] encoded = value.getBytes(EBCDIC);
            int copyLen = Math.min(encoded.length, length);
            System.arraycopy(encoded, 0, result, 0, copyLen);
        }
        
        return result;
    }
    
    /**
     * Encodes a long to display numeric EBCDIC bytes (PIC 9).
     * Uses EBCDIC digits F0-F9.
     * 
     * @param value The numeric value
     * @param length Target length in bytes
     * @return EBCDIC encoded numeric bytes
     */
    public static byte[] encodeDisplayNumeric(long value, int length) {
        String formatted = String.format("%0" + length + "d", Math.abs(value));
        if (formatted.length() > length) {
            formatted = formatted.substring(formatted.length() - length);
        }
        return formatted.getBytes(EBCDIC);
    }
    
    /**
     * Encodes a BigDecimal to packed decimal (COMP-3) format.
     * 
     * Packed decimal format:
     * - Each byte holds 2 digits (one per nibble)
     * - Last nibble is the sign: C=positive, D=negative, F=unsigned
     * 
     * Example: +12345.67 with 2 decimal places → 01 23 45 67 C (5 bytes for S9(7)V99)
     * 
     * @param value The decimal value
     * @param length Target length in bytes
     * @param decimalPlaces Implied decimal places
     * @param signed Whether the field is signed
     * @return Packed decimal bytes
     */
    public static byte[] encodePackedDecimal(BigDecimal value, int length, 
                                              int decimalPlaces, boolean signed) {
        byte[] result = new byte[length];
        Arrays.fill(result, (byte) 0x00);
        
        // Scale value to remove decimal point
        BigDecimal scaled = value.movePointRight(decimalPlaces);
        long longValue = scaled.longValue();
        boolean negative = longValue < 0;
        longValue = Math.abs(longValue);
        
        // Calculate max digits (each byte = 2 nibbles, last nibble = sign)
        int maxDigits = (length * 2) - 1;
        
        // Format with leading zeros
        String digits = String.format("%0" + maxDigits + "d", longValue);
        if (digits.length() > maxDigits) {
            digits = digits.substring(digits.length() - maxDigits);
        }
        
        // Pack digits into bytes
        int digitIndex = 0;
        for (int byteIndex = 0; byteIndex < length - 1; byteIndex++) {
            int highNibble = digits.charAt(digitIndex++) - '0';
            int lowNibble = digits.charAt(digitIndex++) - '0';
            result[byteIndex] = (byte) ((highNibble << 4) | lowNibble);
        }
        
        // Last byte: final digit + sign nibble
        int lastDigit = digits.charAt(digitIndex) - '0';
        int signNibble = signed ? (negative ? 0x0D : 0x0C) : 0x0F;
        result[length - 1] = (byte) ((lastDigit << 4) | signNibble);
        
        return result;
    }
    
    /**
     * Returns a hex dump of bytes for debugging.
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
