package com.docMind.controller;

import com.docMind.dto.ChatRequest;
import com.docMind.dto.ChatResponse;
import com.docMind.entity.User;
import com.docMind.repository.UserRepository;
import com.docMind.service.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagQueryService ragQueryService;
    private final UserRepository userRepository;

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> query(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatResponse response = ragQueryService.query(
                request.getQuestion(),
                request.getDocumentId(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }
}