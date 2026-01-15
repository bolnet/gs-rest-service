package com.example.restservice.ebcdic;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstration class for EBCDIC file processing.
 * Shows how to:
 * 1. Create EBCDIC files from JSON/text data
 * 2. Parse EBCDIC files using the metadata layout
 * 3. Convert between EBCDIC and JSON formats
 * 
 * Record Layout (from COBOL copybook):
 * - CUST-ID: 8 bytes, NUMERIC (PIC 9(8))
 * - CUST-NAME: 30 bytes, ALPHA (PIC X(30))
 * - CUST-BALANCE: 5 bytes, PACKED DECIMAL (PIC S9(7)V99 COMP-3)
 * - CUST-STATUS: 1 byte, ALPHA (PIC X(1))
 * - CUST-DATE: 8 bytes, NUMERIC (PIC 9(8))
 * Total Record Length: 52 bytes
 */
public class EbcdicProcessingDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(80));
            System.out.println("EBCDIC File Processing Demo");
            System.out.println("Using pure Java EBCDIC encoding (IBM037)");
            System.out.println("=".repeat(80));
            
            // Create a temporary directory for output files
            Path tempDir = Files.createTempDirectory("ebcdic_demo");
            System.out.println("\nTemporary directory: " + tempDir);
            
            // Initialize the service
            EbcdicProcessingService service = new EbcdicProcessingService();
            
            // Print the layout being used
            System.out.println("\nRecord Layout:");
            System.out.println(service.getLayout());
            
            // Demo 1: Create EBCDIC from in-memory data
            demo1_CreateFromMemory(service, tempDir);
            
            // Demo 2: Create EBCDIC from JSON file
            demo2_CreateFromJsonFile(service, tempDir);
            
            // Demo 3: Parse EBCDIC file
            demo3_ParseEbcdicFile(service, tempDir);
            
            // Demo 4: Convert EBCDIC back to JSON
            demo4_ConvertToJson(service, tempDir);
            
            // Demo 5: Show hex dump of EBCDIC data
            demo5_ShowHexDump(service, tempDir);
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Demo completed successfully!");
            System.out.println("Output files are in: " + tempDir);
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demo 1: Create EBCDIC file from in-memory customer records.
     */
    private static void demo1_CreateFromMemory(EbcdicProcessingService service, Path tempDir) 
            throws Exception {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Demo 1: Creating EBCDIC file from in-memory data");
        System.out.println("-".repeat(60));
        
        // Create sample customer records programmatically
        List<CustomerRecord> records = Arrays.asList(
            new CustomerRecord(12345678L, "JOHN SMITH", new BigDecimal("15234.56"), "A", 20260115),
            new CustomerRecord(87654321L, "JANE DOE", new BigDecimal("-5000.00"), "I", 20251220),
            new CustomerRecord(11112222L, "BOB WILSON", new BigDecimal("99999.99"), "A", 20260101)
        );
        
        System.out.println("\nInput records:");
        for (CustomerRecord record : records) {
            System.out.println("  " + record);
        }
        
        // Generate EBCDIC file
        String outputPath = tempDir.resolve("customers_memory.ebcdic").toString();
        service.generateEbcdicFile(records, outputPath);
        
        // Show file size
        File file = new File(outputPath);
        System.out.println("\nGenerated file size: " + file.length() + " bytes");
        System.out.println("Expected size: " + (52 * 3) + " bytes (52 bytes x 3 records)");
    }
    
    /**
     * Demo 2: Create EBCDIC file from JSON file.
     */
    private static void demo2_CreateFromJsonFile(EbcdicProcessingService service, Path tempDir) 
            throws Exception {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Demo 2: Creating EBCDIC file from JSON file");
        System.out.println("-".repeat(60));
        
        // Get JSON file path from resources
        String jsonPath = getResourcePath("sample_data.json");
        System.out.println("JSON input file: " + jsonPath);
        
        // Convert JSON to EBCDIC
        String outputPath = tempDir.resolve("customers_json.ebcdic").toString();
        service.jsonToEbcdic(jsonPath, outputPath);
        
        // Show file size
        File file = new File(outputPath);
        System.out.println("Generated file size: " + file.length() + " bytes");
    }
    
    /**
     * Demo 3: Parse EBCDIC file and display contents.
     */
    private static void demo3_ParseEbcdicFile(EbcdicProcessingService service, Path tempDir) 
            throws Exception {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Demo 3: Parsing EBCDIC file");
        System.out.println("-".repeat(60));
        
        String ebcdicPath = tempDir.resolve("customers_memory.ebcdic").toString();
        
        // Print detailed file contents
        service.printEbcdicFileContents(ebcdicPath);
        
        // Parse into objects
        List<CustomerRecord> records = service.parseEbcdicFile(ebcdicPath);
        System.out.println("\nParsed records as objects:");
        for (CustomerRecord record : records) {
            System.out.println("  " + record);
        }
    }
    
    /**
     * Demo 4: Convert EBCDIC file back to JSON.
     */
    private static void demo4_ConvertToJson(EbcdicProcessingService service, Path tempDir) 
            throws Exception {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Demo 4: Converting EBCDIC back to JSON");
        System.out.println("-".repeat(60));
        
        String ebcdicPath = tempDir.resolve("customers_memory.ebcdic").toString();
        String jsonOutputPath = tempDir.resolve("customers_output.json").toString();
        
        service.ebcdicToJson(ebcdicPath, jsonOutputPath);
        
        // Display the output JSON
        String jsonContent = new String(Files.readAllBytes(Path.of(jsonOutputPath)));
        System.out.println("\nOutput JSON content:");
        System.out.println(jsonContent);
    }
    
    /**
     * Demo 5: Show hex dump of EBCDIC data.
     */
    private static void demo5_ShowHexDump(EbcdicProcessingService service, Path tempDir) 
            throws Exception {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Demo 5: Hex dump of first EBCDIC record");
        System.out.println("-".repeat(60));
        
        String ebcdicPath = tempDir.resolve("customers_memory.ebcdic").toString();
        byte[] fileBytes = Files.readAllBytes(Path.of(ebcdicPath));
        
        // Show first record (52 bytes)
        byte[] firstRecord = new byte[52];
        System.arraycopy(fileBytes, 0, firstRecord, 0, 52);
        
        System.out.println("\nFirst record hex dump (52 bytes):");
        System.out.println("Offset  : 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F");
        System.out.println("-".repeat(60));
        
        for (int i = 0; i < firstRecord.length; i += 16) {
            System.out.printf("%08X: ", i);
            for (int j = 0; j < 16 && (i + j) < firstRecord.length; j++) {
                System.out.printf("%02X ", firstRecord[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        System.out.println("\nField breakdown:");
        System.out.println("  Bytes  0- 7: CUST-ID      (Display Numeric)");
        System.out.println("  Bytes  8-37: CUST-NAME    (EBCDIC Alpha)");
        System.out.println("  Bytes 38-42: CUST-BALANCE (Packed Decimal COMP-3)");
        System.out.println("  Byte     43: CUST-STATUS  (EBCDIC Alpha)");
        System.out.println("  Bytes 44-51: CUST-DATE    (Display Numeric)");
    }
    
    /**
     * Gets the path to a resource file.
     */
    private static String getResourcePath(String resourceName) throws Exception {
        // Try file system first
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
        
        // Try classpath
        InputStream is = EbcdicProcessingDemo.class.getResourceAsStream("/" + resourceName);
        if (is != null) {
            Path tempFile = Files.createTempFile("resource_", "_" + resourceName);
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return tempFile.toString();
        }
        
        throw new RuntimeException("Could not find resource: " + resourceName);
    }
}
