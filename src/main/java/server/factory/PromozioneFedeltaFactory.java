package factory;

import model.Promozione;
import model.PromozioneFedelta;

import java.time.LocalDate;

public class PromozioneFedeltaFactory implements PromozioneFactory {
    @Override
    public Promozione creaPromozione(String nome, String descrizione, double sconto,
                                     LocalDate dataInizio, LocalDate dataFine) {
        return new PromozioneFedelta(nome, descrizione, sconto, dataInizio, dataFine);
    }
}