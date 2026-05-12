package com.docMind.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class ChatRequest {
    private String question;
    private UUID documentId;
}