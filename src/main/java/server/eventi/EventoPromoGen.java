package eventi;
import model.*;
import model.PromozioneGenerale;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoPromoGen implements EventoS {
    private final Promozione promozione;

    public EventoPromoGen(Promozione promozione) {
        this.promozione = promozione;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.PROMO_GENERALE;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.emptySet(); // tutti i clienti
    }

    public Promozione getPromozione() {
        return promozione;
    }
}