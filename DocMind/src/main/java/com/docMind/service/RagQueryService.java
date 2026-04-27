package com.docMind.service;

import com.docMind.dto.ChatResponse;
import com.docMind.dto.SourceReference;
import com.docMind.entity.ChatSession;
import com.docMind.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ChatSessionRepository chatSessionRepository;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    public ChatResponse query(String question, UUID documentId, UUID userId) {
        long start = System.currentTimeMillis();

        // Step 1 — Build filter so we only search this user's document
        var b = new FilterExpressionBuilder();
        var filter = b.and(
                b.eq("documentId", documentId.toString()),
                b.eq("userId", userId.toString())
        );

        var chunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(filter.build())
                        .build()
        );

        log.info("Retrieved {} chunks for question", chunks.size());

        // Step 3 — If nothing found, skip LLM call
        if (chunks.isEmpty()) {
            return ChatResponse.builder()
                    .answer("I could not find relevant information in this document.")
                    .sources(List.of())
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        // Step 4 — Build context from retrieved chunks
        String context = chunks.stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 5 — Call Mistral with grounded system prompt
        String systemPrompt = """
            You are a precise document assistant.
            Answer using ONLY the context provided below.
            If the answer is not in the context, say:
            "I don't have enough information in this document to answer that."
            Never make up facts.

            Context:
            """ + context;

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - start;

        // Step 6 — Build source citations
        List<SourceReference> sources = chunks.stream()
                .map(chunk -> SourceReference.builder()
                        .filename((String) chunk.getMetadata().get("filename"))
                        .chunkIndex((Integer) chunk.getMetadata()
                                .getOrDefault("chunkIndex", 0))
                        .preview(chunk.getText().substring(
                                0, Math.min(150, chunk.getText().length())) + "...")
                        .build())
                .collect(Collectors.toList());

        // Step 7 — Save to history
        saveChatSession(userId, documentId, question, answer, (int) responseTime);

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .responseTimeMs(responseTime)
                .build();
    }

    private void saveChatSession(UUID userId, UUID documentId,
                                 String query, String answer, int ms) {
        try {
            chatSessionRepository.save(ChatSession.builder()
                    .userId(userId)
                    .documentId(documentId)
                    .userQuery(query)
                    .llmResponse(answer)
                    .responseTimeMs(ms)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save chat session", e);
        }
    }
}