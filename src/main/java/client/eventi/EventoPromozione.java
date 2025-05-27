package eventi;

import dto.BigliettoDTO;
import dto.PromozioneDTO;

public class EventoPromozione implements Evento{

    private final PromozioneDTO promozione;

    public EventoPromozione(PromozioneDTO promozione) {
        this.promozione = promozione;
    }

    public PromozioneDTO getPromozione() {
        return promozione;
    }

    @Override
    public String toString() {
        return "EventoPromozione: " + promozione;
    }

    @Override
    public BigliettoDTO getBigliettoNuovo() {
        return null;
    }
}