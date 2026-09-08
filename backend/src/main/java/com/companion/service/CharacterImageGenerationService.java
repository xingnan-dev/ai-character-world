package com.companion.service;
import com.companion.dto.request.*; import com.companion.dto.response.CharacterImageGenerationResponse;
public interface CharacterImageGenerationService { CharacterImageGenerationResponse generate(Long userId, CharacterImageGenerateRequest request); void confirm(Long userId, CharacterImageConfirmRequest request); }
