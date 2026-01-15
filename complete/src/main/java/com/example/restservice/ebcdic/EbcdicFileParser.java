package com.example.restservice.ebcdic;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses EBCDIC files into customer records.
 * Uses the COBOL copybook layout (metadata) to interpret the file structure.
 * 
 * This implementation uses pure Java EBCDIC decoding (IBM037)
 * and handles packed decimal (COMP-3) fields.
 */
public class EbcdicFileParser {
    
    private final CopybookLayout layout;
    
    /**
     * Creates a new EBCDIC file parser using the customer layout.
     */
    public EbcdicFileParser() {
        this.layout = CopybookLayout.createCustomerLayout();
    }
    
    /**
     * Creates a new EBCDIC file parser with a custom copybook path.
     * 
     * @param copybookPath Path to the COBOL copybook file (for future use)
     */
    public EbcdicFileParser(String copybookPath) {
        // For now, use the predefined customer layout
        // In a full implementation, this would parse the copybook file
        this.layout = CopybookLayout.createCustomerLayout();
    }
    
    /**
     * Parses an EBCDIC file and returns a list of customer records.
     * 
     * @param inputPath Path to the EBCDIC file to parse
     * @return List of parsed customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> parseEbcdicFile(String inputPath) throws IOException {
        List<CustomerRecord> records = new ArrayList<>();
        int recordLength = layout.getRecordLength();
        
        try (FileInputStream fis = new FileInputStream(inputPath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] recordBytes = new byte[recordLength];
            int bytesRead;
            
            while ((bytesRead = bis.read(recordBytes)) == recordLength) {
                CustomerRecord record = convertBytesToRecord(recordBytes);
                records.add(record);
            }
            
            if (bytesRead > 0 && bytesRead < recordLength) {
                System.err.println("Warning: Incomplete record at end of file (" + 
                        bytesRead + " bytes)");
            }
        }
        
        System.out.println("Successfully parsed EBCDIC file: " + inputPath);
        System.out.println("Total records read: " + records.size());
        
        return records;
    }
    
    /**
     * Converts EBCDIC bytes to a CustomerRecord.
     * 
     * @param recordBytes The EBCDIC record bytes
     * @return The parsed customer record
     */
    public CustomerRecord convertBytesToRecord(byte[] recordBytes) {
        CustomerRecord record = new CustomerRecord();
        
        // CUST-ID: 8-byte display numeric
        CopybookField custIdField = layout.getField("CUST-ID");
        byte[] custIdBytes = EbcdicConverter.extractField(recordBytes, custIdField);
        record.setCustId(EbcdicConverter.displayNumericToLong(custIdBytes));
        
        // CUST-NAME: 30-byte alpha
        CopybookField custNameField = layout.getField("CUST-NAME");
        byte[] custNameBytes = EbcdicConverter.extractField(recordBytes, custNameField);
        record.setCustName(EbcdicConverter.ebcdicToString(custNameBytes).trim());
        
        // CUST-BALANCE: 5-byte packed decimal
        CopybookField custBalanceField = layout.getField("CUST-BALANCE");
        byte[] custBalanceBytes = EbcdicConverter.extractField(recordBytes, custBalanceField);
        BigDecimal balance = EbcdicConverter.packedDecimalToDecimal(
                custBalanceBytes, custBalanceField.getDecimalPlaces());
        record.setCustBalance(balance);
        
        // CUST-STATUS: 1-byte alpha
        CopybookField custStatusField = layout.getField("CUST-STATUS");
        byte[] custStatusBytes = EbcdicConverter.extractField(recordBytes, custStatusField);
        record.setCustStatus(EbcdicConverter.ebcdicToString(custStatusBytes));
        
        // CUST-DATE: 8-byte display numeric
        CopybookField custDateField = layout.getField("CUST-DATE");
        byte[] custDateBytes = EbcdicConverter.extractField(recordBytes, custDateField);
        record.setCustDate((int) EbcdicConverter.displayNumericToLong(custDateBytes));
        
        return record;
    }
    
    /**
     * Prints detailed information about each field in the EBCDIC file.
     * Useful for debugging and understanding the file structure.
     * 
     * @param inputPath Path to the EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void printFileDetails(String inputPath) throws IOException {
        int recordLength = layout.getRecordLength();
        
        try (FileInputStream fis = new FileInputStream(inputPath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] recordBytes = new byte[recordLength];
            int recordNum = 0;
            int bytesRead;
            
            System.out.println("=".repeat(80));
            System.out.println("EBCDIC File Details: " + inputPath);
            System.out.println("Record Length: " + recordLength + " bytes");
            System.out.println("=".repeat(80));
            
            while ((bytesRead = bis.read(recordBytes)) == recordLength) {
                recordNum++;
                System.out.println("\nRecord #" + recordNum + ":");
                System.out.println("-".repeat(40));
                
                for (CopybookField field : layout.getFields()) {
                    byte[] fieldBytes = EbcdicConverter.extractField(recordBytes, field);
                    String hexValue = EbcdicConverter.hexDump(fieldBytes);
                    
                    Object value;
                    switch (field.getType()) {
                        case ALPHA:
                            value = "'" + EbcdicConverter.ebcdicToString(fieldBytes) + "'";
                            break;
                        case NUMERIC:
                            value = EbcdicConverter.displayNumericToLong(fieldBytes);
                            break;
                        case PACKED:
                            value = EbcdicConverter.packedDecimalToDecimal(
                                    fieldBytes, field.getDecimalPlaces());
                            break;
                        default:
                            value = hexValue;
                    }
                    
                    System.out.printf("  %-15s: %-30s (hex: %s)%n",
                            field.getName(), value, hexValue);
                }
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Total records: " + recordNum);
            System.out.println("=".repeat(80));
        }
    }
    
    /**
     * Gets the layout being used by this parser.
     * 
     * @return The copybook layout
     */
    public CopybookLayout getLayout() {
        return layout;
    }
}
