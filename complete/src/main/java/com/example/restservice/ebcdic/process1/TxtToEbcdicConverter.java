package com.example.restservice.ebcdic.process1;

import com.example.restservice.ebcdic.common.CustomerRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.List;

/**
 * PROCESS 1: Converts TXT/JSON files to EBCDIC binary format.
 * 
 * This process reads customer data from a text/JSON file and writes
 * a fixed-length EBCDIC binary file according to the COBOL copybook layout.
 * 
 * Record Layout (52 bytes):
 * - CUST-ID:      Position 1,  Length 8,  PIC 9(8)         - Display Numeric
 * - CUST-NAME:    Position 9,  Length 30, PIC X(30)        - Alpha
 * - CUST-BALANCE: Position 39, Length 5,  PIC S9(7)V99 COMP-3 - Packed Decimal
 * - CUST-STATUS:  Position 44, Length 1,  PIC X(1)         - Alpha
 * - CUST-DATE:    Position 45, Length 8,  PIC 9(8)         - Display Numeric
 */
public class TxtToEbcdicConverter {
    
    // Field positions and lengths from copybook
    private static final int CUST_ID_POS = 0;
    private static final int CUST_ID_LEN = 8;
    
    private static final int CUST_NAME_POS = 8;
    private static final int CUST_NAME_LEN = 30;
    
    private static final int CUST_BALANCE_POS = 38;
    private static final int CUST_BALANCE_LEN = 5;
    private static final int CUST_BALANCE_DECIMALS = 2;
    
    private static final int CUST_STATUS_POS = 43;
    private static final int CUST_STATUS_LEN = 1;
    
    private static final int CUST_DATE_POS = 44;
    private static final int CUST_DATE_LEN = 8;
    
    private static final int RECORD_LENGTH = 52;
    
    private final ObjectMapper objectMapper;
    
    public TxtToEbcdicConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Converts a JSON file to an EBCDIC binary file.
     * 
     * @param jsonInputPath Path to the JSON input file
     * @param ebcdicOutputPath Path for the EBCDIC output file
     * @throws IOException If an I/O error occurs
     */
    public void convert(String jsonInputPath, String ebcdicOutputPath) throws IOException {
        System.out.println("=".repeat(60));
        System.out.println("PROCESS 1: TXT/JSON to EBCDIC Conversion");
        System.out.println("=".repeat(60));
        System.out.println("Input file:  " + jsonInputPath);
        System.out.println("Output file: " + ebcdicOutputPath);
        System.out.println("Record length: " + RECORD_LENGTH + " bytes");
        System.out.println();
        
        // Read JSON input
        List<CustomerRecord> records = readJsonFile(jsonInputPath);
        System.out.println("Records read from JSON: " + records.size());
        
        // Write EBCDIC output
        writeEbcdicFile(records, ebcdicOutputPath);
        
        // Verify output
        File outputFile = new File(ebcdicOutputPath);
        System.out.println("\nOutput file size: " + outputFile.length() + " bytes");
        System.out.println("Expected size: " + (records.size() * RECORD_LENGTH) + " bytes");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Reads customer records from a JSON file.
     */
    public List<CustomerRecord> readJsonFile(String jsonPath) throws IOException {
        return objectMapper.readValue(
                new File(jsonPath),
                new TypeReference<List<CustomerRecord>>() {}
        );
    }
    
    /**
     * Writes customer records to an EBCDIC binary file.
     */
    public void writeEbcdicFile(List<CustomerRecord> records, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            int recordNum = 0;
            for (CustomerRecord record : records) {
                recordNum++;
                byte[] ebcdicRecord = encodeRecord(record);
                bos.write(ebcdicRecord);
                
                // Print debug info
                System.out.println("\nRecord #" + recordNum + ": " + record.getCustName().trim());
                printRecordHex(ebcdicRecord);
            }
            
            bos.flush();
        }
        
        System.out.println("\nSuccessfully wrote " + records.size() + " records to EBCDIC file.");
    }
    
    /**
     * Encodes a single CustomerRecord to EBCDIC bytes.
     */
    public byte[] encodeRecord(CustomerRecord record) {
        byte[] result = new byte[RECORD_LENGTH];
        
        // Initialize with EBCDIC spaces
        for (int i = 0; i < RECORD_LENGTH; i++) {
            result[i] = EbcdicFieldEncoder.EBCDIC_SPACE;
        }
        
        // CUST-ID: 8-byte display numeric
        byte[] custId = EbcdicFieldEncoder.encodeDisplayNumeric(record.getCustId(), CUST_ID_LEN);
        System.arraycopy(custId, 0, result, CUST_ID_POS, CUST_ID_LEN);
        
        // CUST-NAME: 30-byte alpha
        byte[] custName = EbcdicFieldEncoder.encodeAlpha(record.getCustName(), CUST_NAME_LEN);
        System.arraycopy(custName, 0, result, CUST_NAME_POS, CUST_NAME_LEN);
        
        // CUST-BALANCE: 5-byte packed decimal
        byte[] custBalance = EbcdicFieldEncoder.encodePackedDecimal(
                record.getCustBalance(), CUST_BALANCE_LEN, CUST_BALANCE_DECIMALS, true);
        System.arraycopy(custBalance, 0, result, CUST_BALANCE_POS, CUST_BALANCE_LEN);
        
        // CUST-STATUS: 1-byte alpha
        byte[] custStatus = EbcdicFieldEncoder.encodeAlpha(record.getCustStatus(), CUST_STATUS_LEN);
        System.arraycopy(custStatus, 0, result, CUST_STATUS_POS, CUST_STATUS_LEN);
        
        // CUST-DATE: 8-byte display numeric
        byte[] custDate = EbcdicFieldEncoder.encodeDisplayNumeric(record.getCustDate(), CUST_DATE_LEN);
        System.arraycopy(custDate, 0, result, CUST_DATE_POS, CUST_DATE_LEN);
        
        return result;
    }
    
    /**
     * Prints a hex dump of the record for debugging.
     */
    private void printRecordHex(byte[] record) {
        System.out.println("  CUST-ID:      " + EbcdicFieldEncoder.toHexString(
                extractBytes(record, CUST_ID_POS, CUST_ID_LEN)));
        System.out.println("  CUST-NAME:    " + EbcdicFieldEncoder.toHexString(
                extractBytes(record, CUST_NAME_POS, CUST_NAME_LEN)));
        System.out.println("  CUST-BALANCE: " + EbcdicFieldEncoder.toHexString(
                extractBytes(record, CUST_BALANCE_POS, CUST_BALANCE_LEN)));
        System.out.println("  CUST-STATUS:  " + EbcdicFieldEncoder.toHexString(
                extractBytes(record, CUST_STATUS_POS, CUST_STATUS_LEN)));
        System.out.println("  CUST-DATE:    " + EbcdicFieldEncoder.toHexString(
                extractBytes(record, CUST_DATE_POS, CUST_DATE_LEN)));
    }
    
    private byte[] extractBytes(byte[] source, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, offset, result, 0, length);
        return result;
    }
    
    /**
     * Main method for standalone execution of Process 1.
     */
    public static void main(String[] args) {
        try {
            String jsonInput = args.length > 0 ? args[0] : findDefaultJsonFile();
            String ebcdicOutput = args.length > 1 ? args[1] : "customers.ebcdic";
            
            TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
            converter.convert(jsonInput, ebcdicOutput);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String findDefaultJsonFile() {
        String[] paths = {
            "src/main/resources/sample_data.json",
            "complete/src/main/resources/sample_data.json"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return "sample_data.json";
    }
}
