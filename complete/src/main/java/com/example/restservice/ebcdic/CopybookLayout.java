package com.example.restservice.ebcdic;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.*;

/**
 * Represents the layout of a COBOL record based on a copybook definition.
 * Parses COBOL copybooks and creates field definitions for EBCDIC processing.
 */
public class CopybookLayout {
    
    private final List<CopybookField> fields;
    private final int recordLength;
    private final Map<String, CopybookField> fieldsByName;
    
    /**
     * Creates a layout with the given fields.
     */
    public CopybookLayout(List<CopybookField> fields, int recordLength) {
        this.fields = new ArrayList<>(fields);
        this.recordLength = recordLength;
        this.fieldsByName = new HashMap<>();
        for (CopybookField field : fields) {
            fieldsByName.put(field.getName(), field);
        }
    }
    
    /**
     * Creates the customer record layout based on the specification.
     * 
     * Layout:
     * - CUST-ID: Position 1, Length 8, NUMERIC (PIC 9(8))
     * - CUST-NAME: Position 9, Length 30, ALPHA (PIC X(30))
     * - CUST-BALANCE: Position 39, Length 5, PACKED (PIC S9(7)V99 COMP-3)
     * - CUST-STATUS: Position 44, Length 1, ALPHA (PIC X(1))
     * - CUST-DATE: Position 45, Length 8, NUMERIC (PIC 9(8))
     * 
     * Total Record Length: 52 bytes
     */
    public static CopybookLayout createCustomerLayout() {
        List<CopybookField> fields = new ArrayList<>();
        
        // CUST-ID: 8-byte display numeric at position 1
        fields.add(CopybookField.numeric("CUST-ID", 1, 8));
        
        // CUST-NAME: 30-byte alpha at position 9
        fields.add(CopybookField.alpha("CUST-NAME", 9, 30));
        
        // CUST-BALANCE: 5-byte packed decimal at position 39
        // PIC S9(7)V99 COMP-3 = 9 digits total (7+2) = (9+1)/2 = 5 bytes
        fields.add(CopybookField.packed("CUST-BALANCE", 39, 5, 2, true));
        
        // CUST-STATUS: 1-byte alpha at position 44
        fields.add(CopybookField.alpha("CUST-STATUS", 44, 1));
        
        // CUST-DATE: 8-byte display numeric at position 45
        fields.add(CopybookField.numeric("CUST-DATE", 45, 8));
        
        return new CopybookLayout(fields, 52);
    }
    
    /**
     * Parses a COBOL copybook file and creates a layout.
     * 
     * Note: This is a simplified parser that handles basic COBOL PIC clauses.
     */
    public static CopybookLayout parseFromFile(String copybookPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(copybookPath))) {
            return parseFromReader(reader);
        }
    }
    
    /**
     * Parses a COBOL copybook from a reader.
     */
    public static CopybookLayout parseFromReader(BufferedReader reader) throws IOException {
        List<CopybookField> fields = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            fullText.append(line).append(" ");
        }
        
        String text = fullText.toString();
        
        // Pattern to match field definitions: 05 FIELD-NAME PIC X(30).
        Pattern fieldPattern = Pattern.compile(
            "(\\d{2})\\s+([A-Z0-9-]+)\\s+PIC\\s+([SX9V()]+)(?:\\s+(COMP-3))?\\s*\\.",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = fieldPattern.matcher(text);
        int currentPosition = 1;
        
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2).toUpperCase();
            String picture = matcher.group(3).toUpperCase();
            String comp3 = matcher.group(4);
            
            // Skip group levels (01, 05 without PIC)
            if (level <= 5 && name.contains("RECORD")) {
                continue;
            }
            
            CopybookField field = parsePicture(name, currentPosition, picture, comp3 != null);
            fields.add(field);
            currentPosition += field.getLength();
        }
        
        return new CopybookLayout(fields, currentPosition - 1);
    }
    
    /**
     * Parses a PIC clause and creates a field definition.
     */
    private static CopybookField parsePicture(String name, int startPosition, 
                                               String picture, boolean isPacked) {
        // Remove parentheses notation and expand
        String expanded = expandPicture(picture);
        
        boolean signed = expanded.startsWith("S");
        if (signed) {
            expanded = expanded.substring(1);
        }
        
        // Check for decimal (V)
        int decimalPlaces = 0;
        int vPos = expanded.indexOf('V');
        if (vPos >= 0) {
            String afterV = expanded.substring(vPos + 1);
            decimalPlaces = afterV.replace("9", "9").length();
            expanded = expanded.replace("V", "");
        }
        
        int digitCount = expanded.replace("9", "9").length();
        int xCount = expanded.replace("X", "X").length() - expanded.replace("X", "").length();
        
        if (isPacked) {
            // Packed decimal: (digits + 1) / 2 bytes
            int totalDigits = digitCount + decimalPlaces;
            if (signed) totalDigits = digitCount; // sign is in last nibble
            int length = (digitCount + decimalPlaces + 1) / 2;
            return CopybookField.packed(name, startPosition, length, decimalPlaces, signed);
        } else if (xCount > 0) {
            // Alpha field
            return CopybookField.alpha(name, startPosition, xCount);
        } else {
            // Display numeric
            return CopybookField.numeric(name, startPosition, digitCount);
        }
    }
    
    /**
     * Expands PIC notation like X(30) to XXXXXX...
     */
    private static String expandPicture(String picture) {
        StringBuilder result = new StringBuilder();
        Pattern repeatPattern = Pattern.compile("([SXV9])\\((\\d+)\\)");
        
        String remaining = picture;
        Matcher matcher = repeatPattern.matcher(remaining);
        int lastEnd = 0;
        
        while (matcher.find()) {
            result.append(remaining, lastEnd, matcher.start());
            char ch = matcher.group(1).charAt(0);
            int count = Integer.parseInt(matcher.group(2));
            for (int i = 0; i < count; i++) {
                result.append(ch);
            }
            lastEnd = matcher.end();
        }
        result.append(remaining.substring(lastEnd));
        
        return result.toString();
    }
    
    // Getters
    public List<CopybookField> getFields() {
        return Collections.unmodifiableList(fields);
    }
    
    public int getRecordLength() {
        return recordLength;
    }
    
    public CopybookField getField(String name) {
        return fieldsByName.get(name);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CopybookLayout{\n");
        sb.append("  recordLength=").append(recordLength).append(",\n");
        sb.append("  fields=[\n");
        for (CopybookField field : fields) {
            sb.append("    ").append(field).append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
