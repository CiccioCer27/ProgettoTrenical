package factory;

import model.Promozione;
import model.PromozioneTratta;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class PromozioneTrattaFactory implements PromozioneFactory {

    private final Set<UUID> tratteDestinate;

    /**
     * Costruttore con Dependency Injection delle tratte
     *
     * @param tratteDestinate Set di UUID delle tratte a cui applicare la promozione
     */
    public PromozioneTrattaFactory(Set<UUID> tratteDestinate) {
        if (tratteDestinate == null || tratteDestinate.isEmpty()) {
            throw new IllegalArgumentException("❌ PromozioneTratta richiede almeno una tratta destinataria");
        }
        this.tratteDestinate = Set.copyOf(tratteDestinate); // Immutable copy
    }

    /**
     * ✅ FIXED: Ora implementa correttamente l'interface PromozioneFactory
     */
    @Override
    public Promozione creaPromozione(String nome, String descrizione, double sconto,
                                     LocalDate dataInizio, LocalDate dataFine) {
        return new PromozioneTratta(nome, descrizione, sconto, dataInizio, dataFine, tratteDestinate);
    }

    /**
     * Metodo di utilità per ottenere le tratte associate
     */
    public Set<UUID> getTratteDestinate() {
        return tratteDestinate;
    }
}