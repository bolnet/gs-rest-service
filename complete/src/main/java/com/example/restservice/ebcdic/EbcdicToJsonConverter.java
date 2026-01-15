package com.example.restservice.ebcdic;

import net.sf.JRecord.JRecordInterface1;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.Details.LayoutDetail;
import net.sf.JRecord.Details.RecordDetail;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.def.IO.builders.ICobolIOBuilder;
import net.sf.JRecord.Common.IFileStructureConstants;
import net.sf.JRecord.External.CopybookLoader;

import java.io.File;

/**
 * Standalone EBCDIC to JSON Converter using JRecord only.
 * 
 * This program reads an EBCDIC file using a COBOL copybook (metadata file)
 * and converts the data directly to JSON without any intermediate Java classes.
 * 
 * Usage:
 *   java EbcdicToJsonConverter <copybook-file> <ebcdic-file>
 * 
 * Example:
 *   java EbcdicToJsonConverter CUSTOMER.cbl customers.ebcdic
 */
public class EbcdicToJsonConverter {
    
    public static void main(String[] args) {
        try {
            // Get file paths from arguments or use defaults
            String copybookPath = args.length > 0 ? args[0] : findFile("copybooks/CUSTOMER.cbl");
            String ebcdicPath = args.length > 1 ? args[1] : findFile("customers.ebcdic");
            
            System.err.println("Copybook: " + copybookPath);
            System.err.println("EBCDIC:   " + ebcdicPath);
            System.err.println();
            
            // Convert EBCDIC to JSON using JRecord
            String json = convertEbcdicToJson(copybookPath, ebcdicPath);
            
            // Output JSON to stdout
            System.out.println(json);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Converts an EBCDIC file to JSON using JRecord and the copybook metadata.
     * No intermediate Java classes - purely dynamic field access.
     */
    public static String convertEbcdicToJson(String copybookPath, String ebcdicPath) throws Exception {
        
        // Build JRecord IO configuration from copybook
        ICobolIOBuilder ioBuilder = JRecordInterface1.COBOL
                .newIOBuilder(copybookPath)
                .setFont("cp037")                                    // EBCDIC US encoding
                .setFileOrganization(IFileStructureConstants.IO_FIXED_LENGTH)
                .setSplitCopybook(CopybookLoader.SPLIT_NONE);
        
        // Get the layout to access field definitions dynamically
        LayoutDetail layout = ioBuilder.getLayout();
        RecordDetail record = layout.getRecord(0);
        int fieldCount = record.getFieldCount();
        
        // Build JSON array
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        // Open EBCDIC file for reading
        AbstractLineReader reader = ioBuilder.newReader(ebcdicPath);
        
        try {
            AbstractLine line;
            boolean firstRecord = true;
            
            while ((line = reader.read()) != null) {
                if (!firstRecord) {
                    json.append(",\n");
                }
                firstRecord = false;
                
                // Build JSON object for this record
                json.append("  {\n");
                
                for (int i = 0; i < fieldCount; i++) {
                    String fieldName = record.getField(i).getName();
                    int fieldType = record.getField(i).getType();
                    
                    // Add comma between fields
                    if (i > 0) {
                        json.append(",\n");
                    }
                    
                    // Format field as JSON
                    json.append("    \"").append(fieldName).append("\": ");
                    
                    // Determine if field is numeric based on type
                    // Types 0-25 are typically numeric in JRecord
                    boolean isNumeric = isNumericType(fieldType);
                    
                    if (isNumeric) {
                        // Try to get as BigDecimal for numeric fields
                        try {
                            java.math.BigDecimal bd = line.getFieldValue(fieldName).asBigDecimal();
                            // Output as plain number without scientific notation
                            json.append(bd.toPlainString());
                        } catch (Exception e) {
                            // Fallback to string
                            json.append("\"").append(escapeJson(line.getFieldValue(fieldName).asString().trim())).append("\"");
                        }
                    } else {
                        // String field - escape and quote, trim trailing spaces
                        String strValue = line.getFieldValue(fieldName).asString().trim();
                        json.append("\"").append(escapeJson(strValue)).append("\"");
                    }
                }
                
                json.append("\n  }");
            }
            
        } finally {
            reader.close();
        }
        
        json.append("\n]");
        
        return json.toString();
    }
    
    /**
     * Determines if a JRecord field type is numeric.
     * Type 0 = Character, Type 25 = Display Numeric, Type 140 = Packed Decimal
     */
    private static boolean isNumericType(int fieldType) {
        // Type 0 is character (alpha), everything else is typically numeric
        // Common types:
        //   0  = Character
        //   25 = Mainframe Zoned Numeric (Display)
        //   31 = Binary Integer
        //   35 = Positive Binary Integer  
        //   39 = Packed Decimal (mainframe)
        //  140 = Packed Decimal COMP-3
        return fieldType != 0;  // Everything except char type 0 is numeric
    }
    
    /**
     * Escapes special characters for JSON string values.
     */
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Finds a file in common resource locations.
     */
    private static String findFile(String fileName) {
        String[] paths = {
            "src/main/resources/" + fileName,
            "complete/src/main/resources/" + fileName,
            fileName
        };
        
        for (String path : paths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        
        return fileName;
    }
}
