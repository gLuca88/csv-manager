package com.gianluca.csvmanager.integration;

import com.gianluca.csvmanager.controller.CsvValidationController;
import com.gianluca.csvmanager.service.CsvValidationService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CsvValidationController.class)
@Import(CsvValidationService.class)
public class CsvValidationControllerTest {

    private static final Logger log =
            LoggerFactory.getLogger(CsvValidationControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void accettaUnFileConContenuto() throws Exception {

        ClassPathResource resource =
                new ClassPathResource("csv/valido.csv");

        try (InputStream input = resource.getInputStream()) {

            MockMultipartFile file = new MockMultipartFile(
                    "file", "valido.csv", "text/csv", input
            );

            String contenuto = new String(
                    file.getBytes(), StandardCharsets.UTF_8);

            log.info("File caricato: nome={}, dimensione={} byte",
                    file.getOriginalFilename(), file.getSize());
            log.info("Contenuto del CSV di prova:\n{}", contenuto);

            assertEquals(
                    List.of("id,nome", "1,Mario", "2,Anna"),
                    contenuto.lines().toList(),
                    "Il CSV caricato deve contenere i dati attesi"
            );

            log.info("Dati verificati. Verifico HTTP 200 e riepilogo JSON: 2 record, 2 colonne, intestazione [id, nome]");

            mockMvc.perform(multipart("/api/csv/validate").file(file))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.fileName").value("valido.csv"))
                    .andExpect(jsonPath("$.totalRows").value(2))
                    .andExpect(jsonPath("$.totalColumns").value(2))
                    .andExpect(jsonPath("$.headers.length()").value(2))
                    .andExpect(jsonPath("$.headers[0]").value("id"))
                    .andExpect(jsonPath("$.headers[1]").value("nome"));

            log.info("Verifiche superate: file accettato e riepilogo JSON corretto");
        }
    }

    @Test
    void rifiutaUnFileVuoto() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "vuoto.csv", "text/csv", new byte[0]
        );

        log.info("File inviato: nome={}, dimensione={} byte",
                file.getOriginalFilename(), file.getSize());
        log.info("Verifico HTTP 400 e 'Il file è vuoto'");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Il file è vuoto"));

        log.info("Verifiche superate: file vuoto rifiutato");
    }

    @Test
    void rifiutaUnFileConEstensioneNonCsv() throws Exception {

        String contenuto = "id,nome\n1,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.txt",
                "text/plain",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("File inviato: nome={}, tipo={}, dimensione={} byte",
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize());
        log.info("Verifico HTTP 400 per estensione .txt");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Il file deve avere estensione .csv"));

        log.info("Verifiche superate: file .txt rifiutato");
    }

    @Test
    void rifiutaUnCsvConColonneDuplicate() throws Exception {

        String contenuto = "id,nome,nome\n1,Mario,Rossi";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colonne-duplicate.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 400 per il nome duplicato 'nome'");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "L'intestazione contiene nomi vuoti o duplicati"));

        log.info("Verifiche superate: intestazione duplicata rifiutata");
    }

    @Test
    void rifiutaUnCsvConNomeColonnaVuoto() throws Exception {

        String contenuto = "id,,nome\n1,Rossi,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colonna-vuota.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 400 per la seconda colonna senza nome");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "L'intestazione contiene nomi vuoti o duplicati"));

        log.info("Verifiche superate: nome di colonna vuoto rifiutato");
    }

    @Test
    void rifiutaUnNomeColonnaCompostoSoloDaSpazi() throws Exception {

        String contenuto = "id,   ,nome\n1,Rossi,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colonna-con-spazi.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Intestazione inviata: [id], [   ], [nome]");
        log.info("Verifico HTTP 400 per il nome composto solo da spazi");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "L'intestazione contiene nomi vuoti o duplicati"));

        log.info("Verifiche superate: nome composto solo da spazi rifiutato");
    }

    @Test
    void rifiutaColonneDuplicateDopoLaRimozioneDegliSpazi()
            throws Exception {

        String contenuto = "id,nome, nome \n1,Mario,Rossi";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicati-con-spazi.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Intestazione inviata: [id], [nome], [ nome ]");
        log.info("Verifico HTTP 400 per i nomi duplicati dopo il trim");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "L'intestazione contiene nomi vuoti o duplicati"));

        log.info("Verifiche superate: duplicati con spazi rifiutati");
    }

    @Test
    void accettaNomiColonnaConAccentiESpazi() throws Exception {

        String contenuto = "id,quantità,nome cliente\n1,10,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "intestazione-valida.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 200 e riepilogo JSON: 1 record, 3 colonne, intestazione [id, quantità, nome cliente]");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fileName").value("intestazione-valida.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.totalColumns").value(3))
                .andExpect(jsonPath("$.headers.length()").value(3))
                .andExpect(jsonPath("$.headers[0]").value("id"))
                .andExpect(jsonPath("$.headers[1]").value("quantità"))
                .andExpect(jsonPath("$.headers[2]").value("nome cliente"));

        log.info("Verifiche superate: accenti e spazi interni accettati e riepilogo JSON corretto");
    }

    @Test
    void accettaUnaVirgolaNelNomeColonnaTraVirgolette() throws Exception {

        String contenuto = "id,\"cognome,nome\"\n1,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virgola-tra-virgolette.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 200 e riepilogo JSON: 1 record, 2 colonne; il secondo nome è 'cognome,nome'");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fileName").value("virgola-tra-virgolette.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.totalColumns").value(2))
                .andExpect(jsonPath("$.headers.length()").value(2))
                .andExpect(jsonPath("$.headers[0]").value("id"))
                .andExpect(jsonPath("$.headers[1]").value("cognome,nome"));

        log.info("Verifiche superate: virgola nel nome della colonna interpretata correttamente e riepilogo JSON corretto");
    }

    @Test
    void rifiutaUnIntestazioneConVirgoletteNonChiuse()
            throws Exception {

        String contenuto = "id,\"nome\n1,Mario";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virgolette-non-chiuse.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 400 per le virgolette non chiuse");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Impossibile leggere il file CSV"));

        log.info("Verifiche superate: virgolette non chiuse rifiutate");
    }

    @Test
    void rifiutaUnRecordConMenoColonne() throws Exception {

        String contenuto = "id,nome\n1";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colonna-mancante.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico il record 2: attese 2 colonne, presente 1");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Record 2: attese 2 colonne, trovate 1"));

        log.info("Verifiche superate: colonna mancante rilevata");
    }

    @Test
    void rifiutaUnRecordConPiuColonne() throws Exception {

        String contenuto = "id,nome\n1,Mario,Rossi";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "colonna-aggiuntiva.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico il record 2: attese 2 colonne, presenti 3");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Record 2: attese 2 colonne, trovate 3"));

        log.info("Verifiche superate: colonna aggiuntiva rilevata");
    }

    @Test
    void rifiutaUnRecordConCampoVuoto() throws Exception {

        String contenuto = "id,nome\n1,";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campo-vuoto.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 400: il campo 'nome' del record 2 è vuoto");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Record 2: campo vuoto nella colonna 'nome'"));

        log.info("Verifiche superate: campo vuoto rilevato");
    }

    @Test
    void rifiutaUnCampoCompostoSoloDaSpazi() throws Exception {

        String contenuto = "id,nome\n1,\"   \"";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campo-con-spazi.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 400 per il campo 'nome' composto solo da spazi");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Record 2: campo vuoto nella colonna 'nome'"));

        log.info("Verifiche superate: campo composto solo da spazi rifiutato");
    }

    @Test
    void accettaUnaVirgolaNelValoreTraVirgolette() throws Exception {

        String contenuto = "id,nome\n1,\"Rossi,Mario\"";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valore-con-virgola.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 200 e riepilogo JSON: 1 record, 2 colonne, intestazione [id, nome]");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fileName").value("valore-con-virgola.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.totalColumns").value(2))
                .andExpect(jsonPath("$.headers.length()").value(2))
                .andExpect(jsonPath("$.headers[0]").value("id"))
                .andExpect(jsonPath("$.headers[1]").value("nome"));

        log.info("Verifiche superate: virgola nel valore accettata e riepilogo JSON corretto");
    }

    @Test
    void accettaUnCsvConSolaIntestazione() throws Exception {

        String contenuto = "id,nome";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sola-intestazione.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 200 e riepilogo JSON: "
                + "0 record di dati, 2 colonne, intestazione [id, nome]");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fileName").value("sola-intestazione.csv"))
                .andExpect(jsonPath("$.totalRows").value(0))
                .andExpect(jsonPath("$.totalColumns").value(2))
                .andExpect(jsonPath("$.headers.length()").value(2))
                .andExpect(jsonPath("$.headers[0]").value("id"))
                .andExpect(jsonPath("$.headers[1]").value("nome"));

        log.info("Verifiche superate: CSV con sola intestazione accettato "
                + "e conteggio dei record pari a zero");
    }

    @Test
    void contaUnCampoMultilineaComeUnSoloRecord() throws Exception {

        String contenuto =
                "id,descrizione\n1,\"Prima riga\nSeconda riga\"";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campo-multilinea.csv",
                "text/csv",
                contenuto.getBytes(StandardCharsets.UTF_8)
        );

        log.info("CSV di prova:\n{}", contenuto);
        log.info("Verifico HTTP 200 e riepilogo JSON: "
                + "1 record di dati, 2 colonne, intestazione [id, descrizione]");

        mockMvc.perform(multipart("/api/csv/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.fileName").value("campo-multilinea.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.totalColumns").value(2))
                .andExpect(jsonPath("$.headers.length()").value(2))
                .andExpect(jsonPath("$.headers[0]").value("id"))
                .andExpect(jsonPath("$.headers[1]").value("descrizione"));

        log.info("Verifiche superate: campo multilinea accettato "
                + "e conteggiato come un solo record");
    }

}
