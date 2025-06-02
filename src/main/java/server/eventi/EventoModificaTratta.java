package eventi;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoModificaTratta implements EventoS {
    private final UUID idTratta;

    public EventoModificaTratta(UUID idTratta) {
        this.idTratta = idTratta;
    }

    @Override
    public TipoEvento getTipo() { return TipoEvento.MODIFICA_TRATTA; }

    @Override
    public Set<UUID> getDestinatari() { return Collections.singleton(idTratta); }

    public UUID getIdTratta() { return idTratta; }
}