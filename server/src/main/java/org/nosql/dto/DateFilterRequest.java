package org.nosql.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class DateFilterRequest {
    private String frequency; // e.g., "7", "30", or "custom"
    private List<Date> selectedDate; // Array of [startDate, endDate]
    private String type; // e.g., "all", "income", "expense"
    private String userid; // The user ID requesting the data
}