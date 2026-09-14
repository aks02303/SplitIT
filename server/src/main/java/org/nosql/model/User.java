package org.nosql.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// @Data comes from Lombok. It automatically generates getters, setters,
// and constructors behind the scenes so we don't have to write them!
@Data
// @Document is the equivalent of mongoose.model("User", UserSchema)
@Document(collection = "users")
public class User {

    // @Id maps to MongoDB's built-in _id field automatically
    @Id
    private String id;

    private String name;
    private String email;
    private String password;

    // In Mongoose, you had type: [], ref: "User".
    // In Spring, we store these as a List of Strings (the IDs of the friends).
    private List<String> friends = new ArrayList<>();

    // We use Object here because it was an untyped array in your Mongoose schema
    private List<Object> friendBalance = new ArrayList<>();

    // Mongoose: tokens: [{ token: { type: String } }]
    // Spring: We create a nested class to match that exact JSON structure.
    private List<AuthToken> tokens = new ArrayList<>();

    // Nested class to match the token object in your DB
    @Data
    public static class AuthToken {
        private String token;

        // Constructor to easily create a new token object
        public AuthToken(String token) {
            this.token = token;
        }
    }
}