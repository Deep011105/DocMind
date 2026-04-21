package com.docMind.service;

import com.docMind.entity.Document;
import com.docMind.entity.User;
import com.docMind.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.DocumentReader;
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

    public Document initiateIngestion(MultipartFile file, User user) {
        Document doc = Document.builder()
                .user(user)
                .filename(file.getOriginalFilename())
                .fileType(extractExtension(file.getOriginalFilename()))
                .fileSizeBytes(file.getSize())
                .status("PROCESSING")
                .build();

        Document saved = documentRepository.save(doc);
        log.info("Document saved id={}, starting ingestion", saved.getId());
        processAsync(file, saved);
        return saved;
    }

    @Async
    public void processAsync(MultipartFile file, Document document) {
        try {
            // Step 1 — Parse file to text
            String rawText = parseWithTika(file);
            log.info("Parsed {} characters", rawText.length());

            // Step 2 — Split into chunks
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(50)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();
            List<org.springframework.ai.document.Document> rawDocs =
                    List.of(org.springframework.ai.document.Document.builder()
                            .text(rawText)
                            .build());
            List<org.springframework.ai.document.Document> chunks =
                    splitter.apply(rawDocs);

            log.info("Created {} chunks", chunks.size());

            // Step 3 — Add metadata to each chunk
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

            // Step 4 — Embed and store in pgvector
            vectorStore.add(enriched);
            log.info("Stored {} chunks in pgvector", enriched.size());

            // Step 5 — Mark as READY
            document.setStatus("READY");
            document.setTotalChunks(chunks.size());
            documentRepository.save(document);

        } catch (Exception e) {
            log.error("Ingestion failed for document={}", document.getId(), e);
            document.setStatus("FAILED");
            documentRepository.save(document);
        }
    }

    private String parseWithTika(MultipartFile file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try (InputStream stream = file.getInputStream()) {
            parser.parse(stream, handler, metadata);
            return handler.toString().trim();
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}