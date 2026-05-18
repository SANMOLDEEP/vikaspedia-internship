package com.seo.keywordgenerator.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class KeywordRequestDTO {
    
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 10, max = 10000, message = "Content must be between 10 and 10000 characters")
    private String content;
    
    private int maxKeywords = 20;
    
    private boolean includeLongTail = true;
}
