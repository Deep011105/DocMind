package com.docMind.service;

import com.docMind.entity.Document;
import com.docMind.entity.User;
import com.docMind.repository.DocumentRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    @Value("${app.rag.chunk-size:512}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:50}")
    private int chunkOverlap;

    public Document initiateIngestion(MultipartFile file, User user) throws Exception {
        // Copy bytes BEFORE the async call — MultipartFile closes after HTTP request ends
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        Document doc = Document.builder()
                .user(user)
                .filename(originalFilename)
                .fileType(extractExtension(originalFilename))
                .fileSizeBytes(fileSize)
                .status("PROCESSING")
                .build();

        Document saved = documentRepository.save(doc);
        log.info("Document saved id={}, starting ingestion", saved.getId());

        // Pass bytes instead of MultipartFile
        processAsync(fileBytes, originalFilename, saved);
        return saved;
    }

    @Async
    public void processAsync(byte[] fileBytes, String filename, Document document) {
        try {
            // Step 1 — Parse bytes with Tika
            String rawText = parseWithTika(fileBytes, filename);
            log.info("Parsed {} characters from {}", rawText.length(), filename);

            // Step 2 — Chunk
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(chunkOverlap)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();

            List<org.springframework.ai.document.Document> rawDocs =
                    List.of(org.springframework.ai.document.Document.builder()
                            .text(rawText)
                            .build());

            List<org.springframework.ai.document.Document> chunks = splitter.apply(rawDocs);
            log.info("Created {} chunks", chunks.size());

            // Step 3 — Enrich with metadata
            List<org.springframework.ai.document.Document> enriched = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = Map.of(
                        "documentId",  document.getId().toString(),
                        "filename",    document.getFilename(),
                        "chunkIndex",  i,
                        "totalChunks", chunks.size(),
                        "userId",      document.getUser().getId().toString()
                );
                enriched.add(org.springframework.ai.document.Document.builder()
                        .text(chunks.get(i).getText())
                        .metadata(metadata)
                        .build());
            }

            // Step 4 — Embed and store
            vectorStore.add(enriched);
            log.info("Stored {} chunks in pgvector", enriched.size());

            // Step 5 — Mark READY
            document.setStatus("READY");
            document.setTotalChunks(chunks.size());
            documentRepository.save(document);

        } catch (Exception e) {
            log.error("Ingestion failed for document={}", document.getId(), e);
            document.setStatus("FAILED");
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

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}