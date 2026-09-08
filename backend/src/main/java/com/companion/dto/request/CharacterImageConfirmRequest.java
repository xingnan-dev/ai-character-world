package com.companion.dto.request;
import jakarta.validation.constraints.NotNull; import lombok.Data;
@Data public class CharacterImageConfirmRequest { @NotNull private Long generationId; @NotNull private Long characterId; }
