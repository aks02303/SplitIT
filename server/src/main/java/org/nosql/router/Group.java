package org.nosql.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "groups")
public class Group {
    @Id
    private String id;
    private String name;
    private String description;

    // Maps to your Mongoose: members: { type: [], ref: "User" }
    private List<String> members = new ArrayList<>();

    private Double balance;
}