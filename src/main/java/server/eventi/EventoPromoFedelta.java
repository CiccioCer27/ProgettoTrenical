package eventi;

import model.PromozioneFedelta;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoPromoFedelta implements EventoS {
    private final PromozioneFedelta promozione;

    public EventoPromoFedelta(PromozioneFedelta promozione) {
        this.promozione = promozione;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.PROMO_FEDELTA;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.emptySet(); // oppure: clienti con carta fedeltà
    }

    public PromozioneFedelta getPromozione() {
        return promozione;
    }
}