package com.example.restservice.ebcdic.process2;

import com.example.restservice.ebcdic.common.CustomerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.JRecord.JRecordInterface1;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.def.IO.builders.ICobolIOBuilder;
import net.sf.JRecord.Common.IFileStructureConstants;
import net.sf.JRecord.External.CopybookLoader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PROCESS 2: Reads EBCDIC files using JRecord library and COBOL copybook metadata.
 * 
 * This process uses the JRecord library to:
 * 1. Load the COBOL copybook (metadata file) that defines the record structure
 * 2. Read the binary EBCDIC file
 * 3. Automatically decode all field types (alpha, numeric, packed decimal)
 * 4. Return parsed Java objects
 * 
 * The JRecord library handles:
 * - EBCDIC to ASCII character conversion
 * - Display numeric (PIC 9) parsing
 * - Packed decimal (COMP-3) unpacking
 */
public class JRecordEbcdicReader {
    
    private final String copybookPath;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new JRecord EBCDIC reader.
     * 
     * @param copybookPath Path to the COBOL copybook file (metadata)
     */
    public JRecordEbcdicReader(String copybookPath) {
        this.copybookPath = copybookPath;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Reads an EBCDIC file and returns a list of customer records.
     * 
     * @param ebcdicFilePath Path to the EBCDIC binary file
     * @return List of parsed customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> readEbcdicFile(String ebcdicFilePath) throws IOException {
        System.out.println("=".repeat(60));
        System.out.println("PROCESS 2: JRecord EBCDIC File Reader");
        System.out.println("=".repeat(60));
        System.out.println("Copybook (metadata): " + copybookPath);
        System.out.println("EBCDIC file: " + ebcdicFilePath);
        System.out.println();
        
        List<CustomerRecord> records = new ArrayList<>();
        
        try {
            // Create JRecord COBOL IO builder
            ICobolIOBuilder ioBuilder = JRecordInterface1.COBOL
                    .newIOBuilder(copybookPath)
                    .setFont("cp037")  // EBCDIC encoding (IBM US)
                    .setFileOrganization(IFileStructureConstants.IO_FIXED_LENGTH)
                    .setSplitCopybook(CopybookLoader.SPLIT_NONE);
            
            // Open the EBCDIC file for reading
            AbstractLineReader reader = ioBuilder.newReader(ebcdicFilePath);
            
            try {
                AbstractLine line;
                int recordNum = 0;
                
                while ((line = reader.read()) != null) {
                    recordNum++;
                    
                    // Extract fields using JRecord's field access
                    CustomerRecord record = extractRecord(line, recordNum);
                    records.add(record);
                }
                
                System.out.println("Successfully read " + recordNum + " records.");
                
            } finally {
                reader.close();
            }
            
        } catch (Exception e) {
            throw new IOException("Error reading EBCDIC file with JRecord: " + e.getMessage(), e);
        }
        
        System.out.println("=".repeat(60));
        return records;
    }
    
    /**
     * Extracts a CustomerRecord from a JRecord line.
     */
    private CustomerRecord extractRecord(AbstractLine line, int recordNum) throws Exception {
        CustomerRecord record = new CustomerRecord();
        
        // JRecord automatically handles EBCDIC decoding and COMP-3 unpacking
        long custId = line.getFieldValue("CUST-ID").asLong();
        String custName = line.getFieldValue("CUST-NAME").asString().trim();
        BigDecimal custBalance = line.getFieldValue("CUST-BALANCE").asBigDecimal();
        String custStatus = line.getFieldValue("CUST-STATUS").asString();
        int custDate = line.getFieldValue("CUST-DATE").asInt();
        
        record.setCustId(custId);
        record.setCustName(custName);
        record.setCustBalance(custBalance);
        record.setCustStatus(custStatus);
        record.setCustDate(custDate);
        
        // Print parsed record
        System.out.println("\nRecord #" + recordNum + ":");
        System.out.println("  CUST-ID:      " + custId);
        System.out.println("  CUST-NAME:    '" + custName + "'");
        System.out.println("  CUST-BALANCE: " + custBalance);
        System.out.println("  CUST-STATUS:  '" + custStatus + "'");
        System.out.println("  CUST-DATE:    " + custDate);
        
        return record;
    }
    
    /**
     * Reads EBCDIC file and displays detailed field information.
     * 
     * @param ebcdicFilePath Path to the EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void printFileDetails(String ebcdicFilePath) throws IOException {
        try {
            ICobolIOBuilder ioBuilder = JRecordInterface1.COBOL
                    .newIOBuilder(copybookPath)
                    .setFont("cp037")
                    .setFileOrganization(IFileStructureConstants.IO_FIXED_LENGTH)
                    .setSplitCopybook(CopybookLoader.SPLIT_NONE);
            
            AbstractLineReader reader = ioBuilder.newReader(ebcdicFilePath);
            
            try {
                AbstractLine line;
                int recordNum = 0;
                
                System.out.println("\n" + "=".repeat(60));
                System.out.println("EBCDIC File Details (via JRecord)");
                System.out.println("=".repeat(60));
                
                while ((line = reader.read()) != null) {
                    recordNum++;
                    byte[] rawData = line.getData();
                    
                    System.out.println("\nRecord #" + recordNum + " (Raw hex):");
                    printHexDump(rawData);
                    
                    System.out.println("\nDecoded fields:");
                    System.out.printf("  %-15s: %s%n", "CUST-ID", 
                            line.getFieldValue("CUST-ID").asLong());
                    System.out.printf("  %-15s: '%s'%n", "CUST-NAME", 
                            line.getFieldValue("CUST-NAME").asString());
                    System.out.printf("  %-15s: %s%n", "CUST-BALANCE", 
                            line.getFieldValue("CUST-BALANCE").asBigDecimal());
                    System.out.printf("  %-15s: '%s'%n", "CUST-STATUS", 
                            line.getFieldValue("CUST-STATUS").asString());
                    System.out.printf("  %-15s: %s%n", "CUST-DATE", 
                            line.getFieldValue("CUST-DATE").asInt());
                }
                
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Total records: " + recordNum);
                System.out.println("=".repeat(60));
                
            } finally {
                reader.close();
            }
            
        } catch (Exception e) {
            throw new IOException("Error printing EBCDIC file details: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts the parsed records to JSON format.
     * 
     * @param records List of customer records
     * @return JSON string
     * @throws IOException If an error occurs
     */
    public String toJson(List<CustomerRecord> records) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records);
    }
    
    /**
     * Writes parsed records to a JSON file.
     * 
     * @param records List of customer records
     * @param jsonOutputPath Path for the output JSON file
     * @throws IOException If an I/O error occurs
     */
    public void writeToJson(List<CustomerRecord> records, String jsonOutputPath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(jsonOutputPath), records);
        System.out.println("Written JSON output to: " + jsonOutputPath);
    }
    
    /**
     * Prints a hex dump of bytes.
     */
    private void printHexDump(byte[] data) {
        for (int i = 0; i < data.length; i += 16) {
            System.out.printf("  %04X: ", i);
            for (int j = 0; j < 16 && (i + j) < data.length; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
    
    /**
     * Main method for standalone execution of Process 2.
     */
    public static void main(String[] args) {
        try {
            String copybookPath = args.length > 0 ? args[0] : findDefaultCopybook();
            String ebcdicPath = args.length > 1 ? args[1] : "customers.ebcdic";
            String jsonOutput = args.length > 2 ? args[2] : null;
            
            JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
            List<CustomerRecord> records = reader.readEbcdicFile(ebcdicPath);
            
            // Print detailed output
            reader.printFileDetails(ebcdicPath);
            
            // Convert to JSON
            System.out.println("\nJSON Output:");
            System.out.println(reader.toJson(records));
            
            if (jsonOutput != null) {
                reader.writeToJson(records, jsonOutput);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String findDefaultCopybook() {
        String[] paths = {
            "src/main/resources/copybooks/CUSTOMER.cbl",
            "complete/src/main/resources/copybooks/CUSTOMER.cbl"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return "CUSTOMER.cbl";
    }
}
