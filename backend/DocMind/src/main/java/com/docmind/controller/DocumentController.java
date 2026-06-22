package com.docmind.controller;

import com.docmind.dto.response.DocumentResponse;
import com.docmind.entity.Document;
import com.docmind.entity.User;
import com.docmind.exception.ResourceNotFoundException;
import com.docmind.exception.UserEmailNotExistException;
import com.docmind.repository.DocumentRepository;
import com.docmind.repository.UserRepository;
import com.docmind.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        User user = getCurrentUser(userDetails);

        Document document = ingestionService.initiateIngestion(file, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(document));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);

        List<DocumentResponse> documents = documentRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);

        Document document = documentRepository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found"));

        return ResponseEntity.ok(Map.of(
                "id", document.getId(),
                "filename", document.getFilename(),
                "status", document.getStatus(),
                "totalChunks", document.getTotalChunks()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);

        Document document = documentRepository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Document not found"));

        documentRepository.delete(document);

        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("User is not authenticated");
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new UserEmailNotExistException("User not found"));
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileType(document.getFileType())
                .fileSizeBytes(document.getFileSizeBytes())
                .totalChunks(document.getTotalChunks())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}