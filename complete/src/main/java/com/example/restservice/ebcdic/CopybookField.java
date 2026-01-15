package com.example.restservice.ebcdic;

/**
 * Represents a field definition from a COBOL copybook.
 * 
 * Supports the following field types:
 * - ALPHA (PIC X): Character fields
 * - NUMERIC (PIC 9): Display numeric fields
 * - PACKED (COMP-3): Packed decimal fields
 */
public class CopybookField {
    
    public enum FieldType {
        ALPHA,      // PIC X(n) - Character data
        NUMERIC,    // PIC 9(n) - Display numeric
        PACKED      // PIC S9(n)V99 COMP-3 - Packed decimal
    }
    
    private final String name;
    private final int startPosition;  // 1-based start position
    private final int length;         // Length in bytes
    private final FieldType type;
    private final String picture;     // Original PIC clause
    private final int decimalPlaces;  // For packed decimal fields
    private final boolean signed;     // For packed decimal fields
    
    public CopybookField(String name, int startPosition, int length, 
                         FieldType type, String picture, int decimalPlaces, boolean signed) {
        this.name = name;
        this.startPosition = startPosition;
        this.length = length;
        this.type = type;
        this.picture = picture;
        this.decimalPlaces = decimalPlaces;
        this.signed = signed;
    }
    
    // Convenience constructor for alpha fields
    public static CopybookField alpha(String name, int startPosition, int length) {
        return new CopybookField(name, startPosition, length, 
                FieldType.ALPHA, "X(" + length + ")", 0, false);
    }
    
    // Convenience constructor for numeric fields
    public static CopybookField numeric(String name, int startPosition, int length) {
        return new CopybookField(name, startPosition, length, 
                FieldType.NUMERIC, "9(" + length + ")", 0, false);
    }
    
    // Convenience constructor for packed decimal fields
    public static CopybookField packed(String name, int startPosition, int length, 
                                       int decimalPlaces, boolean signed) {
        return new CopybookField(name, startPosition, length, 
                FieldType.PACKED, "S9(n)V99 COMP-3", decimalPlaces, signed);
    }
    
    // Getters
    public String getName() { return name; }
    public int getStartPosition() { return startPosition; }
    public int getLength() { return length; }
    public FieldType getType() { return type; }
    public String getPicture() { return picture; }
    public int getDecimalPlaces() { return decimalPlaces; }
    public boolean isSigned() { return signed; }
    
    /**
     * Gets the 0-based byte offset in the record.
     */
    public int getOffset() {
        return startPosition - 1;
    }
    
    @Override
    public String toString() {
        return String.format("CopybookField{name='%s', start=%d, length=%d, type=%s, pic='%s'}",
                name, startPosition, length, type, picture);
    }
}
