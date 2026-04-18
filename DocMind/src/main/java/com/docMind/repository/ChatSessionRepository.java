package com.docMind.repository;

import com.docMind.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdAndDocumentIdOrderByCreatedAtDesc(
            UUID userId, UUID documentId
    );
}