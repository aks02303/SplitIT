package org.nosql.controller;

import org.nosql.model.PdfDetails;
import org.nosql.repository.PdfDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FileController {

    @Autowired
    private PdfDetailsRepository pdfDetailsRepository;

    // Define the upload directory (relative to your project root)
    private final String UPLOAD_DIR = "./files/";

    // --- 1. UPLOAD FILE ---
    // Replaces: app.post("/upload-files", upload.single("file"), ...)
    @PostMapping("/upload-files")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("group") String groupId) {
        try {
            // 1. Create the "files" directory if it doesn't exist
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. Generate filename (Using timestamp is safer than Math.random() to prevent overwriting!)
            String uniqueSuffix = String.valueOf(System.currentTimeMillis());
            String fileName = "Receipt" + uniqueSuffix + ".pdf";

            // 3. Save the file physically to the disk (Replaces Multer's diskStorage)
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());

            // 4. Save metadata to MongoDB
            PdfDetails pdfDetails = new PdfDetails();
            pdfDetails.setGroup(groupId);
            pdfDetails.setPdf(fileName);
            pdfDetailsRepository.save(pdfDetails);

            return ResponseEntity.ok(Map.of("status", "ok"));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // --- 2. GET FILES FOR GROUP ---
    // Replaces: app.get("/get-files/:groupId", ...)
    @GetMapping("/get-files/{groupId}")
    public ResponseEntity<?> getFiles(@PathVariable String groupId) {
        try {
            List<PdfDetails> files = pdfDetailsRepository.findByGroup(groupId);
            return ResponseEntity.ok(Map.of("status", "ok", "data", files));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Error fetching files");
        }
    }
}