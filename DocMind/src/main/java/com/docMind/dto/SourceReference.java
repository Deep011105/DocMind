package com.docMind.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceReference {
    private String filename;
    private Integer chunkIndex;
    private String preview;
}