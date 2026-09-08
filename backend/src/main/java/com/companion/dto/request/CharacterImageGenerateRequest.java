package com.companion.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class CharacterImageGenerateRequest { @NotBlank @Size(max=100) private String requestId; @NotBlank @Size(max=2000) private String prompt; private Long characterId; }
