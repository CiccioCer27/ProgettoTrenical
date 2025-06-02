package factory;

import model.Promozione;
import java.time.LocalDate;

/**
 * 🏭 INTERFACE FACTORY per creazione Promozioni
 *
 * MIGLIORAMENTO: Signature unificata per tutte le implementazioni
 */
public interface PromozioneFactory {
    /**
     * Crea una promozione con parametri base comuni
     */
    Promozione creaPromozione(String nome, String descrizione, double sconto,
                              LocalDate dataInizio, LocalDate dataFine);
}
