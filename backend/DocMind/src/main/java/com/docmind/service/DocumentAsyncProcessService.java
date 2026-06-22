package com.docmind.service;

import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import com.docmind.exception.NoTextExtractedException;
import com.docmind.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAsyncProcessService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    @Value("${app.rag.chunk-size:512}")
    private int chunkSize;

    @Async
    public void processAsync(byte[] fileBytes, String filename, Document document) {
        try {
            log.info("process thread = {}", Thread.currentThread().getName());
            // Step 1 — Parse bytes with Tika
            String rawText = parseWithTika(fileBytes, filename);

            if(rawText.isBlank()) {
                throw new NoTextExtractedException("No text extracted");
            }

            log.info("Parsed {} characters from {}", rawText.length(), filename);

            // Step 2 — Chunk
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(100)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();

            List<org.springframework.ai.document.Document> rawDocs =
                    List.of(org.springframework.ai.document.Document.builder()
                            .text(rawText)
                            .build());

            List<org.springframework.ai.document.Document> chunks = splitter.apply(rawDocs);

            if(chunks.isEmpty()) {
                throw new NoTextExtractedException(
                        "No embeddable chunks generated from file: " + filename);
            }

            log.info("Created {} chunks", chunks.size());

            // Step 3 — Enrich with metadata
            List<org.springframework.ai.document.Document> enriched = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = Map.of(
                        "documentId", document.getId().toString(),
                        "filename", document.getFilename(),
                        "fileType", document.getFileType(),
                        "chunkIndex", i,
                        "totalChunks", chunks.size(),
                        "userId", document.getUser().getId().toString(),
                        "uploadedAt", document.getCreatedAt().toString()
                );
                enriched.add(org.springframework.ai.document.Document.builder()
                        .text(chunks.get(i).getText())
                        .metadata(metadata)
                        .build());
            }

            if (enriched.isEmpty()) {
                throw new NoTextExtractedException(
                        "No valid chunks generated");
            }

            // Step 4 — Embed and store
            vectorStore.add(enriched);
            log.info("Stored {} chunks in pgvector", enriched.size());

            // Step 5 — Mark READY
            document.setStatus(DocumentStatus.READY);
            document.setTotalChunks(chunks.size());
            documentRepository.save(document);

        } catch (NoTextExtractedException e) {
            log.warn(
                    "Document {} failed ingestion: {}",
                    document.getId(),
                    e.getMessage()
            );

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        }
        catch (Exception e) {
            log.error("Ingestion failed for document={}", document.getId(), e);

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }

    private String parseWithTika(byte[] fileBytes, String filename) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        try (InputStream stream = new java.io.ByteArrayInputStream(fileBytes)) {
            parser.parse(stream, handler, metadata);
            return handler.toString().trim();
        }
    }

}
