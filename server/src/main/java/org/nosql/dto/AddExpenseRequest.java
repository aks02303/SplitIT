package org.nosql.dto;

import lombok.Data;
import java.util.Date;

@Data
public class AddExpenseRequest {
    private String groupId;
    private String paidBy;
    private String category;
    private String description;
    private Double amount;
    private Date date;
}