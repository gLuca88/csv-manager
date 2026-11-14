# CSV Manager

Applicazione didattica Spring Boot per costruire, passo dopo passo, un gestionale
per file CSV. L'obiettivo e' esercitare lo sviluppo di API REST, la separazione
delle responsabilita', la validazione degli input e i test automatici.

## Stato attuale

Prima versione: avvio dell'applicazione e API di stato. Caricamento, validazione,
merge ed esportazione CSV sono funzionalita' pianificate e non ancora implementate.
Il progetto e' pensato per uso locale con dati dimostrativi.

## Tecnologie e requisiti

- Java 21.
- Spring Boot 4.1.2.
- Spring Web MVC e Jakarta Bean Validation.
- Maven Wrapper incluso: non e' necessario installare Maven separatamente.
- JUnit e Spring Boot Test per le verifiche automatiche.

La base e' stata generata con [Spring Initializr](https://start.spring.io/).
Non e' richiesto alcun database. Il primo utilizzo del wrapper richiede accesso
a Internet per scaricare Maven e le dipendenze.

## Avvio

Dalla radice del progetto, su Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Su Linux/macOS:

```bash
./mvnw spring-boot:run
```

In alternativa, avvia dall'IDE il metodo `main` di
`com.gianluca.csvmanager.CsvManagerApplication`.

Apri [http://localhost:8080/api/status](http://localhost:8080/api/status).
La risposta attesa e':

```json
{
  "application": "csv-manager",
  "status": "UP"
}
```

L'endpoint verifica che l'app risponda; non controlla servizi esterni.
La porta predefinita e' 8080 e il server ascolta solo su `127.0.0.1`.
Se la porta e' gia' occupata, prima dell'avvio puoi impostarne un'altra:

```powershell
$env:PORT = "8081"
.\mvnw.cmd spring-boot:run
```

In questo caso apri `http://localhost:8081/api/status`. Per fermare il server
usa `Ctrl+C` nel terminale di avvio.

## Struttura

```text
csv-manager/
|-- .mvn/wrapper/                 configurazione del Maven Wrapper
|-- mvnw / mvnw.cmd               avvio Maven su Linux/macOS e Windows
|-- pom.xml                      dipendenze e build
|-- README.md
`-- src/
    |-- main/
    |   |-- java/com/gianluca/csvmanager/
    |   |   |-- CsvManagerApplication.java
    |   |   |-- controller/
    |   |   |   `-- StatusController.java
    |   |   `-- dto/
    |   |       `-- StatusResponse.java
    |   `-- resources/
    |       `-- application.yml
    `-- test/java/com/gianluca/csvmanager/
        `-- integration/
            `-- StatusControllerIntegrationTest.java
```

Il controller espone l'API e il DTO definisce il formato della risposta.
I package `service`, `exception`, `repository` ed `entity` saranno introdotti
quando una funzionalita' ne richiedera' concretamente l'utilizzo.

## Configurazione e dati sensibili

`src/main/resources/application.yml` contiene soltanto impostazioni pubbliche:
nome dell'app, indirizzo locale e porta configurabile tramite `PORT`.
Le configurazioni locali, i file `.env`, le chiavi e le cartelle di upload/export
sono esclusi tramite `.gitignore`. Non inserire credenziali nel codice o nei commit.

## Test e build

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

I test di integrazione caricano il contesto Spring Boot e verificano le API con
MockMvc, senza avviare un server su una porta di rete. Controllano risposta JSON,
metodo HTTP non consentito e percorso inesistente.

Dopo `package`, puoi avviare l'artefatto con:

```powershell
java -jar target/csv-manager-0.0.1-SNAPSHOT.jar
```

## Metodo di lavoro

Ogni modifica coerente viene verificata e salvata in un commit con messaggio
in italiano. Il README viene aggiornato nello stesso commit quando cambiano
funzionalita', configurazione, struttura o istruzioni di esecuzione.

Primo commit:

```text
[inizializzazione]: creata base Spring Boot di CSV Manager
```

## Prossimi passi

1. Caricamento e validazione di un CSV con un parser dedicato.
2. Gestione uniforme degli errori e test dei casi limite.
3. Merge tramite chiave comune ed esportazione.
4. Persistenza e autenticazione, se richieste dalle funzionalita'.
