package eventi;

import model.Biglietto;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoGdsAcquisto implements EventoS {
    private final Biglietto biglietto;

    public EventoGdsAcquisto(Biglietto biglietto) {
        this.biglietto = biglietto;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.GDS_ACQUISTO;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.singleton(biglietto.getIdTratta());
    }

    public Biglietto getBiglietto() {
        return biglietto;
    }
}