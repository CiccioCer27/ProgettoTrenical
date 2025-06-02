package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import model.Biglietto;

import java.io.IOException;
import java.util.List;

/**
 * 🎫 BIGLIETTI PERSISTENCE MANAGER - Refactored
 *
 * Gestisce la persistenza dei biglietti su file JSON.
 * Estende BasePersistenceManager per riutilizzo codice.
 */
public class BigliettiPersistenceManager extends BasePersistenceManager {

    private static final String PATH = "src/main/resources/data/biglietti.json";
    private static final TypeReference<List<Biglietto>> TYPE_REF = new TypeReference<>() {};

    /**
     * Carica tutti i biglietti dal file JSON
     */
    public static List<Biglietto> caricaBiglietti() throws IOException {
        return caricaLista(PATH, TYPE_REF);
    }

    /**
     * Salva tutti i biglietti sul file JSON
     */
    public static void salvaBiglietti(List<Biglietto> biglietti) throws IOException {
        salvaLista(PATH, biglietti);
    }
}
