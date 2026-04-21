package com.docMind.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ChatRequest {
    private String question;
    private UUID documentId;
}