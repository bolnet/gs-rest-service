package com.example.restservice.ebcdic;

import com.example.restservice.ebcdic.common.CustomerRecord;
import com.example.restservice.ebcdic.process1.EbcdicFieldEncoder;
import com.example.restservice.ebcdic.process1.TxtToEbcdicConverter;
import com.example.restservice.ebcdic.process2.JRecordEbcdicReader;

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
 * Integration tests for the two-process EBCDIC file processing.
 * 
 * Process 1: TXT/JSON → EBCDIC (pure Java)
 * Process 2: EBCDIC → Java Objects (JRecord + copybook)
 */
class EbcdicProcessingTest {
    
    @TempDir
    Path tempDir;
    
    private String copybookPath;
    private String jsonInputPath;
    
    @BeforeEach
    void setUp() {
        copybookPath = findResource("copybooks/CUSTOMER.cbl");
        jsonInputPath = findResource("sample_data.json");
    }
    
    /**
     * Test Process 1: Verify EBCDIC file is generated correctly.
     */
    @Test
    void testProcess1_TxtToEbcdic() throws Exception {
        String ebcdicPath = tempDir.resolve("test.ebcdic").toString();
        
        // Run Process 1
        TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
        converter.convert(jsonInputPath, ebcdicPath);
        
        // Verify file was created
        File ebcdicFile = new File(ebcdicPath);
        assertTrue(ebcdicFile.exists(), "EBCDIC file should exist");
        
        // Verify file size (52 bytes × 3 records = 156 bytes)
        assertEquals(156, ebcdicFile.length(), "EBCDIC file should be 156 bytes");
    }
    
    /**
     * Test Process 2: Verify EBCDIC file is parsed correctly using JRecord.
     */
    @Test
    void testProcess2_JRecordEbcdicReader() throws Exception {
        // First run Process 1 to create EBCDIC file
        String ebcdicPath = tempDir.resolve("test.ebcdic").toString();
        TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
        converter.convert(jsonInputPath, ebcdicPath);
        
        // Run Process 2
        JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
        List<CustomerRecord> records = reader.readEbcdicFile(ebcdicPath);
        
        // Verify record count
        assertEquals(3, records.size(), "Should parse 3 records");
        
        // Verify first record
        CustomerRecord first = records.get(0);
        assertEquals(12345678L, first.getCustId());
        assertEquals("JOHN SMITH", first.getCustName());
        assertEquals(0, new BigDecimal("15234.56").compareTo(first.getCustBalance()));
        assertEquals("A", first.getCustStatus());
        assertEquals(20260115, first.getCustDate());
    }
    
    /**
     * Test full round-trip: JSON → EBCDIC → JSON
     */
    @Test
    void testRoundTrip_JsonToEbcdicToJson() throws Exception {
        String ebcdicPath = tempDir.resolve("roundtrip.ebcdic").toString();
        
        // Process 1: JSON → EBCDIC
        TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
        List<CustomerRecord> originalRecords = converter.readJsonFile(jsonInputPath);
        converter.convert(jsonInputPath, ebcdicPath);
        
        // Process 2: EBCDIC → Objects
        JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
        List<CustomerRecord> parsedRecords = reader.readEbcdicFile(ebcdicPath);
        
        // Compare original and parsed records
        assertEquals(originalRecords.size(), parsedRecords.size());
        
        for (int i = 0; i < originalRecords.size(); i++) {
            CustomerRecord original = originalRecords.get(i);
            CustomerRecord parsed = parsedRecords.get(i);
            
            assertEquals(original.getCustId(), parsed.getCustId(), 
                    "CUST-ID mismatch at record " + i);
            assertEquals(original.getCustName().trim(), parsed.getCustName().trim(),
                    "CUST-NAME mismatch at record " + i);
            assertEquals(0, original.getCustBalance().compareTo(parsed.getCustBalance()),
                    "CUST-BALANCE mismatch at record " + i);
            assertEquals(original.getCustStatus(), parsed.getCustStatus(),
                    "CUST-STATUS mismatch at record " + i);
            assertEquals(original.getCustDate(), parsed.getCustDate(),
                    "CUST-DATE mismatch at record " + i);
        }
    }
    
    /**
     * Test negative balance handling (signed packed decimal).
     */
    @Test
    void testNegativeBalance() throws Exception {
        // Verify second record has negative balance
        String ebcdicPath = tempDir.resolve("negative.ebcdic").toString();
        
        TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
        converter.convert(jsonInputPath, ebcdicPath);
        
        JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
        List<CustomerRecord> records = reader.readEbcdicFile(ebcdicPath);
        
        // Second record should have negative balance
        CustomerRecord second = records.get(1);
        assertEquals(87654321L, second.getCustId());
        assertTrue(second.getCustBalance().compareTo(BigDecimal.ZERO) < 0,
                "Balance should be negative");
        assertEquals(0, new BigDecimal("-5000.00").compareTo(second.getCustBalance()));
    }
    
    /**
     * Test EBCDIC field encoder - alpha fields.
     */
    @Test
    void testEbcdicFieldEncoder_Alpha() {
        byte[] result = EbcdicFieldEncoder.encodeAlpha("HELLO", 10);
        
        assertEquals(10, result.length);
        // First 5 bytes should be EBCDIC "HELLO"
        // Remaining 5 bytes should be EBCDIC spaces (0x40)
        assertEquals(0x40, result[5] & 0xFF); // Space
        assertEquals(0x40, result[9] & 0xFF); // Space
    }
    
    /**
     * Test EBCDIC field encoder - display numeric.
     */
    @Test
    void testEbcdicFieldEncoder_DisplayNumeric() {
        byte[] result = EbcdicFieldEncoder.encodeDisplayNumeric(12345, 8);
        
        assertEquals(8, result.length);
        // Should be EBCDIC "00012345" → F0 F0 F0 F1 F2 F3 F4 F5
        assertEquals((byte) 0xF0, result[0]); // '0'
        assertEquals((byte) 0xF0, result[1]); // '0'
        assertEquals((byte) 0xF0, result[2]); // '0'
        assertEquals((byte) 0xF1, result[3]); // '1'
        assertEquals((byte) 0xF2, result[4]); // '2'
        assertEquals((byte) 0xF3, result[5]); // '3'
        assertEquals((byte) 0xF4, result[6]); // '4'
        assertEquals((byte) 0xF5, result[7]); // '5'
    }
    
    /**
     * Test EBCDIC field encoder - packed decimal positive.
     */
    @Test
    void testEbcdicFieldEncoder_PackedDecimalPositive() {
        // 12345.67 with 2 decimal places = 1234567 packed in 5 bytes
        byte[] result = EbcdicFieldEncoder.encodePackedDecimal(
                new BigDecimal("12345.67"), 5, 2, true);
        
        assertEquals(5, result.length);
        // Should be 00 12 34 56 7C (positive sign C)
        assertEquals((byte) 0x00, result[0]);
        assertEquals((byte) 0x12, result[1]);
        assertEquals((byte) 0x34, result[2]);
        assertEquals((byte) 0x56, result[3]);
        assertEquals((byte) 0x7C, result[4]); // 7 + C (positive)
    }
    
    /**
     * Test EBCDIC field encoder - packed decimal negative.
     */
    @Test
    void testEbcdicFieldEncoder_PackedDecimalNegative() {
        // -5000.00 with 2 decimal places = -500000 packed
        byte[] result = EbcdicFieldEncoder.encodePackedDecimal(
                new BigDecimal("-5000.00"), 5, 2, true);
        
        assertEquals(5, result.length);
        // Last nibble should be D (negative sign)
        assertEquals(0x0D, result[4] & 0x0F);
    }
    
    /**
     * Test JSON to object conversion in Process 2.
     */
    @Test
    void testJRecordEbcdicReader_ToJson() throws Exception {
        String ebcdicPath = tempDir.resolve("tojson.ebcdic").toString();
        
        TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
        converter.convert(jsonInputPath, ebcdicPath);
        
        JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
        List<CustomerRecord> records = reader.readEbcdicFile(ebcdicPath);
        
        String json = reader.toJson(records);
        
        assertTrue(json.contains("12345678"), "JSON should contain CUST-ID");
        assertTrue(json.contains("JOHN SMITH"), "JSON should contain CUST-NAME");
        assertTrue(json.contains("15234.56"), "JSON should contain CUST-BALANCE");
    }
    
    private String findResource(String resourceName) {
        String[] paths = {
            "src/main/resources/" + resourceName,
            "complete/src/main/resources/" + resourceName,
            "../complete/src/main/resources/" + resourceName
        };
        
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        
        throw new RuntimeException("Could not find resource: " + resourceName);
    }
}
