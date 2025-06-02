package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import model.Tratta;
import java.io.IOException;
import java.util.List;

/**
 * 🚂 TRATTE PERSISTENCE MANAGER - Refactored
 *
 * Gestisce la persistenza delle tratte su file JSON.
 * Estende BasePersistenceManager per riutilizzo codice.
 */
public class TrattaPersistenceManager extends BasePersistenceManager {

    private static final String PATH = "src/main/resources/data/tratte.json";
    private static final TypeReference<List<Tratta>> TYPE_REF = new TypeReference<>() {};

    /**
     * Carica tutte le tratte dal file JSON
     */
    public static List<Tratta> caricaTratte() throws IOException {
        return caricaLista(PATH, TYPE_REF);
    }

    /**
     * Salva tutte le tratte sul file JSON
     */
    public static void salvaTratte(List<Tratta> tratte) throws IOException {
        salvaLista(PATH, tratte);
    }
}