package com.example.restservice.ebcdic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EBCDIC file processing functionality.
 */
class EbcdicProcessingTest {
    
    @TempDir
    Path tempDir;
    
    private EbcdicProcessingService service;
    
    @BeforeEach
    void setUp() {
        service = new EbcdicProcessingService();
    }
    
    @Test
    void testGenerateAndParseEbcdicFile() throws Exception {
        // Create sample records
        List<CustomerRecord> originalRecords = Arrays.asList(
            new CustomerRecord(12345678L, "JOHN SMITH", new BigDecimal("15234.56"), "A", 20260115),
            new CustomerRecord(87654321L, "JANE DOE", new BigDecimal("-5000.00"), "I", 20251220),
            new CustomerRecord(11112222L, "BOB WILSON", new BigDecimal("99999.99"), "A", 20260101)
        );
        
        // Generate EBCDIC file
        String ebcdicPath = tempDir.resolve("test_customers.ebcdic").toString();
        service.generateEbcdicFile(originalRecords, ebcdicPath);
        
        // Verify file was created
        File ebcdicFile = new File(ebcdicPath);
        assertTrue(ebcdicFile.exists(), "EBCDIC file should exist");
        
        // Verify file size (52 bytes per record x 3 records = 156 bytes)
        assertEquals(156, ebcdicFile.length(), "EBCDIC file should be 156 bytes");
        
        // Parse the file back
        List<CustomerRecord> parsedRecords = service.parseEbcdicFile(ebcdicPath);
        
        // Verify record count
        assertEquals(3, parsedRecords.size(), "Should parse 3 records");
        
        // Verify each record
        for (int i = 0; i < originalRecords.size(); i++) {
            CustomerRecord original = originalRecords.get(i);
            CustomerRecord parsed = parsedRecords.get(i);
            
            assertEquals(original.getCustId(), parsed.getCustId(), 
                    "CUST-ID should match for record " + i);
            assertEquals(original.getCustName().trim(), parsed.getCustName().trim(),
                    "CUST-NAME should match for record " + i);
            assertEquals(0, original.getCustBalance().compareTo(parsed.getCustBalance()),
                    "CUST-BALANCE should match for record " + i);
            assertEquals(original.getCustStatus(), parsed.getCustStatus(),
                    "CUST-STATUS should match for record " + i);
            assertEquals(original.getCustDate(), parsed.getCustDate(),
                    "CUST-DATE should match for record " + i);
        }
    }
    
    @Test
    void testJsonToEbcdicConversion() throws Exception {
        // Load from JSON resource
        String jsonPath = getResourcePath("sample_data.json");
        
        // Convert to EBCDIC
        String ebcdicPath = tempDir.resolve("json_converted.ebcdic").toString();
        service.jsonToEbcdic(jsonPath, ebcdicPath);
        
        // Verify file was created
        File ebcdicFile = new File(ebcdicPath);
        assertTrue(ebcdicFile.exists(), "EBCDIC file should exist");
        assertEquals(156, ebcdicFile.length(), "EBCDIC file should be 156 bytes");
        
        // Parse back and verify
        List<CustomerRecord> records = service.parseEbcdicFile(ebcdicPath);
        assertEquals(3, records.size(), "Should have 3 records");
        
        // Verify first record
        CustomerRecord first = records.get(0);
        assertEquals(12345678L, first.getCustId());
        assertEquals("JOHN SMITH", first.getCustName().trim());
        assertEquals(0, new BigDecimal("15234.56").compareTo(first.getCustBalance()));
        assertEquals("A", first.getCustStatus());
        assertEquals(20260115, first.getCustDate());
    }
    
    @Test
    void testEbcdicToJsonConversion() throws Exception {
        // Create sample records
        List<CustomerRecord> originalRecords = Arrays.asList(
            new CustomerRecord(12345678L, "JOHN SMITH", new BigDecimal("15234.56"), "A", 20260115)
        );
        
        // Generate EBCDIC file
        String ebcdicPath = tempDir.resolve("for_json.ebcdic").toString();
        service.generateEbcdicFile(originalRecords, ebcdicPath);
        
        // Convert to JSON
        String jsonPath = tempDir.resolve("output.json").toString();
        service.ebcdicToJson(ebcdicPath, jsonPath);
        
        // Verify JSON file was created
        File jsonFile = new File(jsonPath);
        assertTrue(jsonFile.exists(), "JSON file should exist");
        
        // Read and verify content
        String jsonContent = new String(Files.readAllBytes(Path.of(jsonPath)));
        assertTrue(jsonContent.contains("12345678"), "JSON should contain CUST-ID");
        assertTrue(jsonContent.contains("JOHN SMITH"), "JSON should contain CUST-NAME");
        assertTrue(jsonContent.contains("15234.56"), "JSON should contain CUST-BALANCE");
    }
    
    @Test
    void testNegativeBalance() throws Exception {
        // Test with negative balance (signed packed decimal)
        CustomerRecord record = new CustomerRecord(
            99999999L, "NEGATIVE TEST", new BigDecimal("-12345.67"), "I", 20260115
        );
        
        String ebcdicPath = tempDir.resolve("negative_test.ebcdic").toString();
        service.generateEbcdicFile(Arrays.asList(record), ebcdicPath);
        
        List<CustomerRecord> parsed = service.parseEbcdicFile(ebcdicPath);
        assertEquals(1, parsed.size());
        assertEquals(0, new BigDecimal("-12345.67").compareTo(parsed.get(0).getCustBalance()),
                "Negative balance should be preserved");
    }
    
    @Test
    void testMaximumValues() throws Exception {
        // Test with maximum values for each field
        CustomerRecord record = new CustomerRecord(
            99999999L, // Max 8-digit CUST-ID
            "AAAAAAAAAABBBBBBBBBBCCCCCCCCCC", // Max 30-char CUST-NAME
            new BigDecimal("9999999.99"), // Max positive balance
            "Z",
            99991231 // Max date
        );
        
        String ebcdicPath = tempDir.resolve("max_values.ebcdic").toString();
        service.generateEbcdicFile(Arrays.asList(record), ebcdicPath);
        
        List<CustomerRecord> parsed = service.parseEbcdicFile(ebcdicPath);
        assertEquals(1, parsed.size());
        
        CustomerRecord parsedRecord = parsed.get(0);
        assertEquals(99999999L, parsedRecord.getCustId());
        assertEquals("AAAAAAAAAABBBBBBBBBBCCCCCCCCCC", parsedRecord.getCustName().trim());
        assertEquals(0, new BigDecimal("9999999.99").compareTo(parsedRecord.getCustBalance()));
        assertEquals("Z", parsedRecord.getCustStatus());
        assertEquals(99991231, parsedRecord.getCustDate());
    }
    
    @Test
    void testLoadFromJsonString() throws Exception {
        String jsonString = "[{\"CUST-ID\": 11111111, \"CUST-NAME\": \"TEST USER\", " +
                "\"CUST-BALANCE\": 100.00, \"CUST-STATUS\": \"A\", \"CUST-DATE\": 20260101}]";
        
        List<CustomerRecord> records = service.loadFromJsonString(jsonString);
        
        assertEquals(1, records.size());
        assertEquals(11111111L, records.get(0).getCustId());
        assertEquals("TEST USER", records.get(0).getCustName());
    }
    
    @Test
    void testEbcdicStringConversion() {
        String original = "HELLO WORLD";
        byte[] ebcdicBytes = EbcdicConverter.stringToEbcdic(original, 20);
        
        // Verify length
        assertEquals(20, ebcdicBytes.length);
        
        // Convert back
        String converted = EbcdicConverter.ebcdicToString(ebcdicBytes).trim();
        assertEquals(original, converted);
    }
    
    @Test
    void testPackedDecimalConversion() {
        // Test positive value
        BigDecimal positive = new BigDecimal("12345.67");
        byte[] packedPositive = EbcdicConverter.decimalToPackedDecimal(positive, 5, 2, true);
        BigDecimal convertedPositive = EbcdicConverter.packedDecimalToDecimal(packedPositive, 2);
        assertEquals(0, positive.compareTo(convertedPositive), "Positive value should match");
        
        // Test negative value
        BigDecimal negative = new BigDecimal("-9876.54");
        byte[] packedNegative = EbcdicConverter.decimalToPackedDecimal(negative, 5, 2, true);
        BigDecimal convertedNegative = EbcdicConverter.packedDecimalToDecimal(packedNegative, 2);
        assertEquals(0, negative.compareTo(convertedNegative), "Negative value should match");
        
        // Test zero
        BigDecimal zero = BigDecimal.ZERO;
        byte[] packedZero = EbcdicConverter.decimalToPackedDecimal(zero, 5, 2, true);
        BigDecimal convertedZero = EbcdicConverter.packedDecimalToDecimal(packedZero, 2);
        assertEquals(0, zero.compareTo(convertedZero), "Zero should match");
    }
    
    @Test
    void testDisplayNumericConversion() {
        long original = 12345678L;
        byte[] numericBytes = EbcdicConverter.longToDisplayNumeric(original, 8);
        
        // Verify length
        assertEquals(8, numericBytes.length);
        
        // Convert back
        long converted = EbcdicConverter.displayNumericToLong(numericBytes);
        assertEquals(original, converted);
    }
    
    @Test
    void testCopybookLayout() {
        CopybookLayout layout = CopybookLayout.createCustomerLayout();
        
        assertEquals(52, layout.getRecordLength(), "Record length should be 52 bytes");
        assertEquals(5, layout.getFields().size(), "Should have 5 fields");
        
        // Verify field positions
        CopybookField custId = layout.getField("CUST-ID");
        assertNotNull(custId);
        assertEquals(1, custId.getStartPosition());
        assertEquals(8, custId.getLength());
        assertEquals(CopybookField.FieldType.NUMERIC, custId.getType());
        
        CopybookField custBalance = layout.getField("CUST-BALANCE");
        assertNotNull(custBalance);
        assertEquals(39, custBalance.getStartPosition());
        assertEquals(5, custBalance.getLength());
        assertEquals(CopybookField.FieldType.PACKED, custBalance.getType());
        assertEquals(2, custBalance.getDecimalPlaces());
        assertTrue(custBalance.isSigned());
    }
    
    private String getResourcePath(String resourceName) throws Exception {
        String[] possiblePaths = {
            "src/main/resources/" + resourceName,
            "complete/src/main/resources/" + resourceName,
            "../complete/src/main/resources/" + resourceName
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        
        InputStream is = getClass().getResourceAsStream("/" + resourceName);
        if (is != null) {
            Path tempFile = Files.createTempFile("resource_", "_" + resourceName);
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return tempFile.toString();
        }
        
        throw new RuntimeException("Could not find resource: " + resourceName);
    }
}
