package com.gianluca.csvmanager.controller;


import com.gianluca.csvmanager.dto.CsvValidationResponse;
import com.gianluca.csvmanager.exception.InvalidCsvException;
import com.gianluca.csvmanager.service.CsvValidationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/csv")
public class CsvValidationController {

    private final CsvValidationService csvValidationService;

    public CsvValidationController(
            CsvValidationService csvValidationService) {
        this.csvValidationService = csvValidationService;
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validate(
            @RequestParam("file") MultipartFile file) {

        try {
            CsvValidationResponse response =
                    csvValidationService.validate(file);

            return ResponseEntity.ok(response);

        } catch (InvalidCsvException exception) {

            return ResponseEntity.badRequest()
                    .body(exception.getMessage());
        }
    }
}
