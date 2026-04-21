package com.docMind.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private String filename;
    private String fileType;
    private Long fileSizeBytes;
    private Integer totalChunks;
    private String status;
    private LocalDateTime createdAt;
}