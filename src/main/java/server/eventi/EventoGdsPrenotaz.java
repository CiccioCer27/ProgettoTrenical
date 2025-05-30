package eventi;

import model.Biglietto;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoGdsPrenotaz implements EventoS {

    private final Biglietto biglietto;

    public EventoGdsPrenotaz(Biglietto biglietto) {
        this.biglietto = biglietto;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.GDS_PRENOTAT;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.singleton(biglietto.getIdCliente());
    }

    public Biglietto getBiglietto() {
        return biglietto;
    }
}