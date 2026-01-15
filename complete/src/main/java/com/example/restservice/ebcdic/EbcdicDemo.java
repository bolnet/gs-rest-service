package com.example.restservice.ebcdic;

import com.example.restservice.ebcdic.common.CustomerRecord;
import com.example.restservice.ebcdic.process1.TxtToEbcdicConverter;
import com.example.restservice.ebcdic.process2.JRecordEbcdicReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Demonstration of the complete EBCDIC file processing workflow.
 * 
 * This demo runs two separate processes:
 * 
 * PROCESS 1: TXT/JSON to EBCDIC
 *   - Reads customer data from a JSON file
 *   - Converts to EBCDIC binary format
 *   - Writes fixed-length 52-byte records
 * 
 * PROCESS 2: EBCDIC to Java Objects (using JRecord)
 *   - Loads COBOL copybook metadata
 *   - Reads the EBCDIC binary file
 *   - Parses all field types automatically
 *   - Outputs as Java objects / JSON
 */
public class EbcdicDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println();
            System.out.println("#".repeat(70));
            System.out.println("#  EBCDIC FILE PROCESSING DEMONSTRATION");
            System.out.println("#  Two-Process Architecture");
            System.out.println("#".repeat(70));
            System.out.println();
            
            // Setup paths
            Path tempDir = Files.createTempDirectory("ebcdic_demo");
            String jsonInputPath = findResource("sample_data.json");
            String copybookPath = findResource("copybooks/CUSTOMER.cbl");
            String ebcdicFilePath = tempDir.resolve("customers.ebcdic").toString();
            String jsonOutputPath = tempDir.resolve("customers_output.json").toString();
            
            System.out.println("Working directory: " + tempDir);
            System.out.println("JSON input: " + jsonInputPath);
            System.out.println("Copybook: " + copybookPath);
            System.out.println();
            
            // ============================================================
            // PROCESS 1: Convert TXT/JSON to EBCDIC
            // ============================================================
            System.out.println("\n" + "▓".repeat(70));
            System.out.println("  PROCESS 1: TXT/JSON → EBCDIC");
            System.out.println("▓".repeat(70) + "\n");
            
            TxtToEbcdicConverter converter = new TxtToEbcdicConverter();
            converter.convert(jsonInputPath, ebcdicFilePath);
            
            // Verify the EBCDIC file was created
            File ebcdicFile = new File(ebcdicFilePath);
            System.out.println("\nProcess 1 Result:");
            System.out.println("  EBCDIC file created: " + ebcdicFile.exists());
            System.out.println("  File size: " + ebcdicFile.length() + " bytes");
            System.out.println("  Expected: 156 bytes (52 bytes × 3 records)");
            
            // ============================================================
            // PROCESS 2: Read EBCDIC using JRecord and Copybook
            // ============================================================
            System.out.println("\n" + "▓".repeat(70));
            System.out.println("  PROCESS 2: EBCDIC → Java Objects (JRecord + Copybook)");
            System.out.println("▓".repeat(70) + "\n");
            
            JRecordEbcdicReader reader = new JRecordEbcdicReader(copybookPath);
            List<CustomerRecord> parsedRecords = reader.readEbcdicFile(ebcdicFilePath);
            
            // Print detailed file analysis
            reader.printFileDetails(ebcdicFilePath);
            
            // Convert to JSON
            System.out.println("\n" + "-".repeat(60));
            System.out.println("JSON Output from Process 2:");
            System.out.println("-".repeat(60));
            String jsonOutput = reader.toJson(parsedRecords);
            System.out.println(jsonOutput);
            
            // Write JSON file
            reader.writeToJson(parsedRecords, jsonOutputPath);
            
            // ============================================================
            // SUMMARY
            // ============================================================
            System.out.println("\n" + "#".repeat(70));
            System.out.println("  DEMONSTRATION COMPLETE");
            System.out.println("#".repeat(70));
            System.out.println("\nProcess Summary:");
            System.out.println("  Process 1 (TXT→EBCDIC): Pure Java EBCDIC encoding");
            System.out.println("  Process 2 (EBCDIC→Objects): JRecord with COBOL copybook");
            System.out.println("\nFiles created:");
            System.out.println("  EBCDIC: " + ebcdicFilePath);
            System.out.println("  JSON:   " + jsonOutputPath);
            System.out.println("\nRecords processed: " + parsedRecords.size());
            System.out.println("#".repeat(70));
            
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Finds a resource file in various possible locations.
     */
    private static String findResource(String resourceName) {
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
