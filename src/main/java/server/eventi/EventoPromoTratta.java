package eventi;

import model.PromozioneTratta;

import java.util.Set;
import java.util.UUID;

public class EventoPromoTratta implements EventoS {
    private final PromozioneTratta promozione;

    public EventoPromoTratta(PromozioneTratta promozione) {
        this.promozione = promozione;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.PROMO_TRATTA;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return promozione.getTratteDestinate();
    }

    public PromozioneTratta getPromozione() {
        return promozione;
    }
}