package com.example.restservice.ebcdic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Service class for EBCDIC file processing operations.
 * Provides high-level methods for converting between JSON/text and EBCDIC formats.
 * 
 * This service uses the COBOL copybook layout for the customer record:
 * - CUST-ID: 8 bytes, NUMERIC (PIC 9(8))
 * - CUST-NAME: 30 bytes, ALPHA (PIC X(30))
 * - CUST-BALANCE: 5 bytes, PACKED DECIMAL (PIC S9(7)V99 COMP-3)
 * - CUST-STATUS: 1 byte, ALPHA (PIC X(1))
 * - CUST-DATE: 8 bytes, NUMERIC (PIC 9(8))
 * Total Record Length: 52 bytes
 */
public class EbcdicProcessingService {
    
    private final ObjectMapper objectMapper;
    private final EbcdicFileGenerator generator;
    private final EbcdicFileParser parser;
    
    /**
     * Creates a new EBCDIC processing service.
     */
    public EbcdicProcessingService() {
        this.objectMapper = new ObjectMapper();
        this.generator = new EbcdicFileGenerator();
        this.parser = new EbcdicFileParser();
    }
    
    /**
     * Creates a new EBCDIC processing service.
     * 
     * @param copybookPath Path to the COBOL copybook file (for future use)
     */
    public EbcdicProcessingService(String copybookPath) {
        this.objectMapper = new ObjectMapper();
        this.generator = new EbcdicFileGenerator(copybookPath);
        this.parser = new EbcdicFileParser(copybookPath);
    }
    
    /**
     * Loads customer records from a JSON file.
     * 
     * @param jsonFilePath Path to the JSON file
     * @return List of customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> loadFromJsonFile(String jsonFilePath) throws IOException {
        return objectMapper.readValue(
                new File(jsonFilePath),
                new TypeReference<List<CustomerRecord>>() {}
        );
    }
    
    /**
     * Loads customer records from a JSON input stream.
     * 
     * @param inputStream Input stream containing JSON data
     * @return List of customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> loadFromJsonStream(InputStream inputStream) throws IOException {
        return objectMapper.readValue(
                inputStream,
                new TypeReference<List<CustomerRecord>>() {}
        );
    }
    
    /**
     * Loads customer records from a JSON string.
     * 
     * @param jsonString JSON string containing customer data
     * @return List of customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> loadFromJsonString(String jsonString) throws IOException {
        return objectMapper.readValue(
                jsonString,
                new TypeReference<List<CustomerRecord>>() {}
        );
    }
    
    /**
     * Converts JSON data to an EBCDIC file.
     * 
     * @param jsonFilePath Path to the input JSON file
     * @param ebcdicOutputPath Path for the output EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void jsonToEbcdic(String jsonFilePath, String ebcdicOutputPath) throws IOException {
        List<CustomerRecord> records = loadFromJsonFile(jsonFilePath);
        generateEbcdicFile(records, ebcdicOutputPath);
    }
    
    /**
     * Generates an EBCDIC file from customer records.
     * 
     * @param records List of customer records
     * @param outputPath Path for the output EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void generateEbcdicFile(List<CustomerRecord> records, String outputPath) 
            throws IOException {
        generator.generateEbcdicFile(records, outputPath);
    }
    
    /**
     * Parses an EBCDIC file and returns customer records.
     * 
     * @param ebcdicFilePath Path to the EBCDIC file
     * @return List of customer records
     * @throws IOException If an I/O error occurs
     */
    public List<CustomerRecord> parseEbcdicFile(String ebcdicFilePath) throws IOException {
        return parser.parseEbcdicFile(ebcdicFilePath);
    }
    
    /**
     * Converts an EBCDIC file to a JSON file.
     * 
     * @param ebcdicFilePath Path to the input EBCDIC file
     * @param jsonOutputPath Path for the output JSON file
     * @throws IOException If an I/O error occurs
     */
    public void ebcdicToJson(String ebcdicFilePath, String jsonOutputPath) throws IOException {
        List<CustomerRecord> records = parseEbcdicFile(ebcdicFilePath);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(jsonOutputPath), records);
        System.out.println("Successfully converted EBCDIC to JSON: " + jsonOutputPath);
    }
    
    /**
     * Prints the contents of an EBCDIC file in a human-readable format.
     * 
     * @param ebcdicFilePath Path to the EBCDIC file
     * @throws IOException If an I/O error occurs
     */
    public void printEbcdicFileContents(String ebcdicFilePath) throws IOException {
        parser.printFileDetails(ebcdicFilePath);
    }
    
    /**
     * Validates the round-trip conversion from JSON to EBCDIC and back.
     * 
     * @param jsonFilePath Path to the original JSON file
     * @param tempDir Temporary directory for intermediate files
     * @return true if the round-trip is successful
     * @throws IOException If an I/O error occurs
     */
    public boolean validateRoundTrip(String jsonFilePath, Path tempDir) throws IOException {
        // Load original records
        List<CustomerRecord> originalRecords = loadFromJsonFile(jsonFilePath);
        
        // Generate EBCDIC file
        String ebcdicPath = tempDir.resolve("temp.ebcdic").toString();
        generateEbcdicFile(originalRecords, ebcdicPath);
        
        // Parse EBCDIC file back
        List<CustomerRecord> parsedRecords = parseEbcdicFile(ebcdicPath);
        
        // Compare records
        if (originalRecords.size() != parsedRecords.size()) {
            System.err.println("Record count mismatch!");
            return false;
        }
        
        for (int i = 0; i < originalRecords.size(); i++) {
            CustomerRecord original = originalRecords.get(i);
            CustomerRecord parsed = parsedRecords.get(i);
            
            if (original.getCustId() != parsed.getCustId()) {
                System.err.println("CUST-ID mismatch at record " + i);
                return false;
            }
            
            // Compare trimmed names (EBCDIC pads with spaces)
            if (!original.getCustName().trim().equals(parsed.getCustName().trim())) {
                System.err.println("CUST-NAME mismatch at record " + i);
                return false;
            }
            
            // Compare balance with tolerance for decimal precision
            if (original.getCustBalance().compareTo(parsed.getCustBalance()) != 0) {
                System.err.println("CUST-BALANCE mismatch at record " + i);
                return false;
            }
            
            if (!original.getCustStatus().equals(parsed.getCustStatus())) {
                System.err.println("CUST-STATUS mismatch at record " + i);
                return false;
            }
            
            if (original.getCustDate() != parsed.getCustDate()) {
                System.err.println("CUST-DATE mismatch at record " + i);
                return false;
            }
        }
        
        System.out.println("Round-trip validation successful!");
        return true;
    }
    
    /**
     * Gets the record layout being used.
     * 
     * @return The copybook layout
     */
    public CopybookLayout getLayout() {
        return generator.getLayout();
    }
}
