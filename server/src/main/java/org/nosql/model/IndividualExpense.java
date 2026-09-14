package org.nosql.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "individualExpense")
public class IndividualExpense {
    @Id
    private String id;
    private String addedBy; // Maps to User ID
    private Double amount;
    private String type = "";
    private Date date;
    private String category;
    private String description;
}