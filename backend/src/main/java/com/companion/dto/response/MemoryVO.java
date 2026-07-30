package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryVO {

    private Long id;

    private Integer category;

    private String memoryKey;

    private String value;

    private Float importance;

    private String lastAccessTime;

    private String createTime;
}