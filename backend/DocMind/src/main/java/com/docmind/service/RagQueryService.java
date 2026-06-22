package com.docmind.service;

import com.docmind.dto.SourceReference;
import com.docmind.dto.response.ChatResponse;
import com.docmind.entity.ChatSession;
import com.docmind.repository.ChatSessionRepository;
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
        // Search only within the current user's document
        var b = new FilterExpressionBuilder();
        var filter = b.and(
                b.eq("documentId", documentId.toString()),
                b.eq("userId", userId.toString())
        );

        log.info("DocumentId = {}", documentId);
        log.info("UserId = {}", userId);
        log.info("Filter = {}", filter.build());

        var chunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
//                        .similarityThreshold(similarityThreshold)
                        .filterExpression(filter.build())
                        .build()
        );

        log.info("Retrieved {} chunks for question", chunks == null ? 0 : chunks.size());

        // No relevant chunks found
        if (chunks == null || chunks.isEmpty()) {
            return ChatResponse.builder()
                    .answer("I could not find relevant information in this document.")
                    .sources(List.of())
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }

        // Build context
        String context = chunks.stream()
                .map(org.springframework.ai.document.Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = """
            You are a precise document assistant.

            Rules:
            1. Answer only from the provided context.
            2. Do not use outside knowledge.
            3. If the answer is not present in the context, reply:
               "I don't have enough information in this document to answer that."
            4. Never make up facts.

            Context:
            """ + context;

        String answer;

        try {
            answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                answer = "Unable to generate a response.";
            }

        } catch (Exception e) {
            log.error("LLM query failed", e);
            answer = "The AI service is currently unavailable. Please try again later.";
        }

        long responseTime = System.currentTimeMillis() - start;

        List<SourceReference> sources = chunks.stream()
                .map(chunk -> {

                    String filename = String.valueOf(
                            chunk.getMetadata()
                                    .getOrDefault("filename", "Unknown")
                    );

                    int chunkIndex = 0;

                    Object indexObj = chunk.getMetadata().get("chunkIndex");

                    if (indexObj instanceof Integer i) {
                        chunkIndex = i;
                    }

                    String text = chunk.getText();

                    String preview;

                    if (text == null || text.isBlank()) {
                        preview = "";
                    } else {
                        preview = text.length() > 150
                                ? text.substring(0, 150) + "..."
                                : text;
                    }

                    return SourceReference.builder()
                            .filename(filename)
                            .chunkIndex(chunkIndex)
                            .preview(preview)
                            .build();
                })
                .toList();

        saveChatSession(
                userId,
                documentId,
                question,
                answer,
                (int) responseTime
        );

        log.info(
                "Question processed in {} ms using {} chunks",
                responseTime,
                chunks.size()
        );

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