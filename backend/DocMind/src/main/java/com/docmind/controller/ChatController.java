package com.docmind.controller;

import com.docmind.dto.request.ChatRequest;
import com.docmind.dto.response.ChatResponse;
import com.docmind.entity.User;
import com.docmind.repository.UserRepository;
import com.docmind.service.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        ChatResponse response = ragQueryService.query(
                request.getQuestion(),
                request.getDocumentId(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }
}