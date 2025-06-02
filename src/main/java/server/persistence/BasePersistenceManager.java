package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 🏗️ BASE PERSISTENCE MANAGER - DRY Principle
 *
 * Classe base astratta che elimina la duplicazione di codice
 * tra tutti i PersistenceManager del sistema.
 *
 * BENEFICI:
 * - ObjectMapper configurato una volta sola
 * - Logica caricamento/salvataggio riutilizzabile
 * - Consistency automatica tra tutti i manager
 * - Facilità aggiunta nuovi PersistenceManager
 */
public abstract class BasePersistenceManager {

    /**
     * ✅ ObjectMapper condiviso con configurazione standard
     * - JavaTimeModule per LocalDate, LocalDateTime, LocalTime
     * - Pretty printing per leggibilità JSON
     * - Date serialization come ISO string (non timestamp)
     */
    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 📋 Carica lista generica da file JSON
     *
     * @param path Percorso del file JSON
     * @param typeRef TypeReference per deserializzazione generica
     * @return Lista degli oggetti caricati (vuota se file non esiste)
     * @throws IOException Se errore lettura file
     */
    protected static <T> List<T> caricaLista(String path, TypeReference<List<T>> typeRef) throws IOException {
        File file = new File(path);

        // Se file non esiste o è vuoto, restituisci lista vuota
        if (!file.exists() || file.length() == 0) {
            // Crea directory padre se non esiste
            file.getParentFile().mkdirs();
            return new ArrayList<>();
        }

        // Deserializza JSON in lista tipizzata
        return MAPPER.readValue(file, typeRef);
    }

    /**
     * 💾 Salva lista generica su file JSON
     *
     * @param path Percorso del file JSON
     * @param lista Lista degli oggetti da salvare
     * @throws IOException Se errore scrittura file
     */
    protected static <T> void salvaLista(String path, List<T> lista) throws IOException {
        File file = new File(path);

        // Assicura che la directory padre esista
        file.getParentFile().mkdirs();

        // Serializza lista in JSON con pretty printing
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, lista);
    }

    /**
     * 🔧 Metodo di utilità per ottenere l'ObjectMapper configurato
     * Utile per operazioni avanzate nei PersistenceManager derivati
     */
    protected static ObjectMapper getMapper() {
        return MAPPER;
    }
}