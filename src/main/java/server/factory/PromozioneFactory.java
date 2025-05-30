package factory;

import model.Promozione;
import java.time.LocalDate;

public interface PromozioneFactory {
    Promozione creaPromozione(String nome, String descrizione, double sconto, LocalDate dataInizio, LocalDate dataFine);
}