package com.docMind.controller;

import com.docMind.dto.DocumentResponse;
import com.docMind.entity.Document;
import com.docMind.entity.User;
import com.docMind.repository.DocumentRepository;
import com.docMind.repository.UserRepository;
import com.docMind.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document doc = ingestionService.initiateIngestion(file, user);
        return ResponseEntity.ok(toResponse(doc));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<DocumentResponse> docs = documentRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(docs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        documentRepository.deleteByIdAndUserId(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .fileType(doc.getFileType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .totalChunks(doc.getTotalChunks())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Document doc = documentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return ResponseEntity.ok(Map.of(
                "id", doc.getId(),
                "status", doc.getStatus(),
                "totalChunks", doc.getTotalChunks(),
                "filename", doc.getFilename()
        ));
    }
}