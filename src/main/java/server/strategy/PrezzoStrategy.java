// src/main/java/server/strategy/PrezzoStrategy.java
package strategy;

import model.Tratta;
import model.Promozione;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.util.List;
import java.util.UUID;

/**
 * 💰 STRATEGY INTERFACE per calcolo prezzi dinamici
 *
 * Permette di implementare diverse strategie di pricing basate su:
 * - Promozioni attive
 * - Tipo cliente (fedele/normale)
 * - Classe di servizio
 * - Periodo di viaggio
 */
public interface PrezzoStrategy {

    /**
     * Calcola il prezzo finale per una tratta specifica
     *
     * @param tratta La tratta per cui calcolare il prezzo
     * @param classeServizio Classe di servizio richiesta
     * @param tipoPrezzo Tipo prezzo base (INTERO, FEDELTA, PROMOZIONE)
     * @param isClienteFedele Se il cliente ha carta fedeltà
     * @param idCliente ID del cliente (per promozioni personalizzate)
     * @param promozioniAttive Lista delle promozioni attualmente attive
     * @return PrezzoCalcolato con dettagli del calcolo
     */
    PrezzoCalcolato calcolaPrezzoFinale(
            Tratta tratta,
            ClasseServizio classeServizio,
            TipoPrezzo tipoPrezzo,
            boolean isClienteFedele,
            UUID idCliente,
            List<Promozione> promozioniAttive
    );

    /**
     * Verifica se questa strategia può essere applicata
     */
    boolean isApplicabile(Tratta tratta, ClasseServizio classeServizio, boolean isClienteFedele);

    /**
     * Priorità della strategia (più alto = priorità maggiore)
     */
    int getPriorita();

    /**
     * Nome della strategia per logging/debug
     */
    String getNome();
}

