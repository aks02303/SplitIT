package org.nosql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SplitItBackendApplication {

    public static void main(String[] args) {
        // This single line replaces your app.listen(), connects to MongoDB,
        // and starts the embedded web server on port 4000.
        SpringApplication.run(SplitItBackendApplication.class, args);
        System.out.println("Server Started at PORT 4000");
    }
}