package com.gianluca.csvmanager.dto;

import java.util.List;

public record CsvValidationResponse(
        String fileName,
        long totalRows,
        int totalColumns,
        List<String> headers
) {
}
