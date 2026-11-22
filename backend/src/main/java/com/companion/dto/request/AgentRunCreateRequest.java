package com.companion.dto.request;
import jakarta.validation.constraints.*;import lombok.Data;
@Data public class AgentRunCreateRequest {@NotBlank @Size(max=64) private String requestId;@NotBlank @Size(max=4000) private String goal;@Min(1) @Max(10) private Integer maxSteps;}
