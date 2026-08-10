package com.companion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUpdateRequest {

    @NotNull
    private Long id;

    @Pattern(regexp = "(?s).*\\S.*", message = "value must not be blank")
    @Size(max = 2000, message = "value must not exceed 2000 characters")
    private String value;

    @DecimalMin(value = "0.0", message = "importance must be at least 0.0")
    @DecimalMax(value = "1.0", message = "importance must not exceed 1.0")
    private Float importance;
}
