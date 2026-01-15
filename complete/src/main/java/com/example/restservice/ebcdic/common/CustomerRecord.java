package com.example.restservice.ebcdic.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Customer record model matching the COBOL copybook structure.
 * 
 * Record Layout (52 bytes total):
 * - CUST-ID: 8 bytes, NUMERIC (PIC 9(8))
 * - CUST-NAME: 30 bytes, ALPHA (PIC X(30))
 * - CUST-BALANCE: 5 bytes, PACKED DECIMAL (PIC S9(7)V99 COMP-3)
 * - CUST-STATUS: 1 byte, ALPHA (PIC X(1))
 * - CUST-DATE: 8 bytes, NUMERIC (PIC 9(8))
 */
public class CustomerRecord {
    
    public static final int RECORD_LENGTH = 52;
    
    @JsonProperty("CUST-ID")
    private long custId;
    
    @JsonProperty("CUST-NAME")
    private String custName;
    
    @JsonProperty("CUST-BALANCE")
    private BigDecimal custBalance;
    
    @JsonProperty("CUST-STATUS")
    private String custStatus;
    
    @JsonProperty("CUST-DATE")
    private int custDate;
    
    public CustomerRecord() {
    }
    
    public CustomerRecord(long custId, String custName, BigDecimal custBalance, 
                          String custStatus, int custDate) {
        this.custId = custId;
        this.custName = custName;
        this.custBalance = custBalance;
        this.custStatus = custStatus;
        this.custDate = custDate;
    }
    
    // Getters and Setters
    public long getCustId() { return custId; }
    public void setCustId(long custId) { this.custId = custId; }
    
    public String getCustName() { return custName; }
    public void setCustName(String custName) { this.custName = custName; }
    
    public BigDecimal getCustBalance() { return custBalance; }
    public void setCustBalance(BigDecimal custBalance) { this.custBalance = custBalance; }
    
    public String getCustStatus() { return custStatus; }
    public void setCustStatus(String custStatus) { this.custStatus = custStatus; }
    
    public int getCustDate() { return custDate; }
    public void setCustDate(int custDate) { this.custDate = custDate; }
    
    @Override
    public String toString() {
        return String.format("CustomerRecord{custId=%d, custName='%s', custBalance=%s, custStatus='%s', custDate=%d}",
                custId, custName, custBalance, custStatus, custDate);
    }
}
