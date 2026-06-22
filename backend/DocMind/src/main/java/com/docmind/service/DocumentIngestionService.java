package com.docmind.service;

import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import com.docmind.entity.User;
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

    private final DocumentRepository documentRepository;
    private final DocumentAsyncProcessService documentAsyncProcessService;

    @Value("${app.rag.chunk-size:512}")
    private int chunkSize;

    public Document initiateIngestion(MultipartFile file, User user) throws Exception {
        log.info("initiate thread = {}", Thread.currentThread().getName());
        // Copy bytes BEFORE the async call — MultipartFile closes after HTTP request ends
        byte[] fileBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        Document doc = Document.builder()
                .user(user)
                .filename(originalFilename)
                .fileType(extractExtension(originalFilename))
                .fileSizeBytes(fileSize)
                .status(DocumentStatus.PROCESSING)
                .build();

        Document saved = documentRepository.save(doc);
        log.info("Document saved id={}, starting ingestion", saved.getId());

        // Pass bytes instead of MultipartFile
        documentAsyncProcessService.processAsync(fileBytes, originalFilename, saved);
        return saved;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}