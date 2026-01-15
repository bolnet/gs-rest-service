package com.example.restservice.ebcdic;

import java.io.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Generates EBCDIC files from customer records.
 * Uses the COBOL copybook layout to format records correctly.
 * 
 * This implementation uses pure Java EBCDIC encoding (IBM037)
 * and handles packed decimal (COMP-3) fields.
 */
public class EbcdicFileGenerator {
    
    private final CopybookLayout layout;
    
    /**
     * Creates a new EBCDIC file generator using the customer layout.
     */
    public EbcdicFileGenerator() {
        this.layout = CopybookLayout.createCustomerLayout();
    }
    
    /**
     * Creates a new EBCDIC file generator with a custom copybook path.
     * 
     * @param copybookPath Path to the COBOL copybook file (for future use)
     */
    public EbcdicFileGenerator(String copybookPath) {
        // For now, use the predefined customer layout
        // In a full implementation, this would parse the copybook file
        this.layout = CopybookLayout.createCustomerLayout();
    }
    
    /**
     * Generates an EBCDIC file from a list of customer records.
     * 
     * @param records List of customer records to write
     * @param outputPath Path for the output EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void generateEbcdicFile(List<CustomerRecord> records, String outputPath) 
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            for (CustomerRecord record : records) {
                byte[] recordBytes = convertRecordToBytes(record);
                bos.write(recordBytes);
            }
            
            bos.flush();
        }
        
        System.out.println("Successfully generated EBCDIC file: " + outputPath);
        System.out.println("Total records written: " + records.size());
        System.out.println("Record length: " + layout.getRecordLength() + " bytes");
    }
    
    /**
     * Converts a CustomerRecord to EBCDIC bytes.
     * 
     * @param record The customer record
     * @return EBCDIC byte array for the record
     */
    public byte[] convertRecordToBytes(CustomerRecord record) {
        byte[] recordBytes = new byte[layout.getRecordLength()];
        
        // Initialize with EBCDIC spaces
        for (int i = 0; i < recordBytes.length; i++) {
            recordBytes[i] = 0x40;  // EBCDIC space
        }
        
        // CUST-ID: 8-byte display numeric
        CopybookField custIdField = layout.getField("CUST-ID");
        byte[] custIdBytes = EbcdicConverter.longToDisplayNumeric(
                record.getCustId(), custIdField.getLength());
        EbcdicConverter.setField(recordBytes, custIdField, custIdBytes);
        
        // CUST-NAME: 30-byte alpha
        CopybookField custNameField = layout.getField("CUST-NAME");
        byte[] custNameBytes = EbcdicConverter.stringToEbcdic(
                record.getCustName(), custNameField.getLength());
        EbcdicConverter.setField(recordBytes, custNameField, custNameBytes);
        
        // CUST-BALANCE: 5-byte packed decimal
        CopybookField custBalanceField = layout.getField("CUST-BALANCE");
        byte[] custBalanceBytes = EbcdicConverter.decimalToPackedDecimal(
                record.getCustBalance(),
                custBalanceField.getLength(),
                custBalanceField.getDecimalPlaces(),
                custBalanceField.isSigned());
        EbcdicConverter.setField(recordBytes, custBalanceField, custBalanceBytes);
        
        // CUST-STATUS: 1-byte alpha
        CopybookField custStatusField = layout.getField("CUST-STATUS");
        byte[] custStatusBytes = EbcdicConverter.stringToEbcdic(
                record.getCustStatus(), custStatusField.getLength());
        EbcdicConverter.setField(recordBytes, custStatusField, custStatusBytes);
        
        // CUST-DATE: 8-byte display numeric
        CopybookField custDateField = layout.getField("CUST-DATE");
        byte[] custDateBytes = EbcdicConverter.longToDisplayNumeric(
                record.getCustDate(), custDateField.getLength());
        EbcdicConverter.setField(recordBytes, custDateField, custDateBytes);
        
        return recordBytes;
    }
    
    /**
     * Gets the layout being used by this generator.
     * 
     * @return The copybook layout
     */
    public CopybookLayout getLayout() {
        return layout;
    }
}
