package eventi;

import model.Biglietto;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoGdsModifica implements EventoS {
    private final Biglietto originale;
    private final Biglietto modificato;

    public EventoGdsModifica(Biglietto originale, Biglietto modificato) {
        this.originale = originale;
        this.modificato = modificato;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.GDS_MODIFICA;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.singleton(originale.getIdTratta());
    }

    public Biglietto getOriginale() {
        return originale;
    }

    public Biglietto getModificato() {
        return modificato;
    }
}