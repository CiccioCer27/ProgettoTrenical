package factory;

import model.Promozione;
import model.PromozioneGenerale;

import java.time.LocalDate;

public class PromozioneGeneraleFactory implements PromozioneFactory {
    @Override
    public Promozione creaPromozione(String nome, String descrizione, double sconto,
                                     LocalDate dataInizio, LocalDate dataFine) {
        return new PromozioneGenerale(nome, descrizione, sconto, dataInizio, dataFine);
    }
}
