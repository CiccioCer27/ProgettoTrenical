package factory;

import model.Promozione;
import model.PromozioneTratta;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class PromozioneTrattaFactory {
    public Promozione creaPromozione(String nome, String descrizione, double sconto, LocalDate dataInizio, LocalDate dataFine, Set<UUID> tratteDestinate) {
        return new PromozioneTratta(nome, descrizione, sconto, dataInizio, dataFine, tratteDestinate);
    }
}