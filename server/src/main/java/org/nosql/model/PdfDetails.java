package org.nosql.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "PdfDetails")
public class PdfDetails {
    @Id
    private String id;
    private String group; // groupId
    private String pdf;   // filename
}