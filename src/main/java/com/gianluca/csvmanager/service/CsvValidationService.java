package com.gianluca.csvmanager.service;


import com.gianluca.csvmanager.dto.CsvValidationResponse;
import com.gianluca.csvmanager.exception.InvalidCsvException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class CsvValidationService {

    public CsvValidationResponse validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("Il file è vuoto");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {

            throw new InvalidCsvException(
                    "Il file deve avere estensione .csv");
        }

        return validateContent(file);
    }


    private CsvValidationResponse validateContent(MultipartFile file) {

        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setAllowMissingColumnNames(false)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .get();

        try (
                Reader reader = new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8
                );
                CSVParser parser = format.parse(reader)
        ) {
            validateHeader(parser);

            long totalRows = validateRecords(parser);
            List<String> headers = List.copyOf(parser.getHeaderNames());

            return new CsvValidationResponse(
                    file.getOriginalFilename(),
                    totalRows,
                    headers.size(),
                    headers
            );

        } catch (IllegalArgumentException exception) {
            throw new InvalidCsvException(
                    "L'intestazione contiene nomi vuoti o duplicati");

        } catch (IOException | UncheckedIOException exception) {
            throw new InvalidCsvException(
                    "Impossibile leggere il file CSV");
        }
    }

    private void validateHeader(CSVParser parser) {

        if (parser.getHeaderNames().isEmpty()) {
            throw new InvalidCsvException(
                    "L'intestazione del CSV è assente");
        }
    }

    private long validateRecords(CSVParser parser) {

        int expectedColumns = parser.getHeaderNames().size();
        long totalRows = 0;

        for (CSVRecord record : parser) {

            long recordNumber = record.getRecordNumber() + 1;
            int actualColumns = record.size();

            if (actualColumns != expectedColumns) {
                throw new InvalidCsvException(
                        "Record " + recordNumber
                                + ": attese " + expectedColumns
                                + " colonne, trovate " + actualColumns
                );
            }

            for (int columnIndex = 0;
                 columnIndex < expectedColumns;
                 columnIndex++) {

                String value = record.get(columnIndex);

                if (value.isBlank()) {

                    String columnName =
                            parser.getHeaderNames().get(columnIndex);

                    throw new InvalidCsvException(
                            "Record " + recordNumber
                                    + ": campo vuoto nella colonna '"
                                    + columnName + "'"
                    );
                }
            }

            totalRows++;
        }

        return totalRows;
    }

}
