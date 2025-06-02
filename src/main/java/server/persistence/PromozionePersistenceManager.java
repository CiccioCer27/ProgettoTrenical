package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import model.Promozione;

import java.io.IOException;
import java.util.List;

/**
 * 🎉 PROMOZIONI PERSISTENCE MANAGER - Refactored + Bug Fixed
 *
 * Gestisce la persistenza delle promozioni su file JSON.
 * Estende BasePersistenceManager per riutilizzo codice.
 *
 * BUG FIXED: Ora usa ObjectMapper con JavaTimeModule per LocalDate
 */
public class PromozionePersistenceManager extends BasePersistenceManager {

    private static final String PATH = "src/main/resources/data/promozioni.json";
    private static final TypeReference<List<Promozione>> TYPE_REF = new TypeReference<>() {};

    /**
     * Carica tutte le promozioni dal file JSON
     */
    public static List<Promozione> caricaPromozioni() throws IOException {
        return caricaLista(PATH, TYPE_REF);
    }

    /**
     * Salva tutte le promozioni sul file JSON
     */
    public static void salvaPromozioni(List<Promozione> promozioni) throws IOException {
        salvaLista(PATH, promozioni);
    }
}