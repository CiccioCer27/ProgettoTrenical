package eventi;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class EventoAcquistoCartaFed implements EventoS {
    private final UUID idCliente;

    public EventoAcquistoCartaFed(UUID idCliente) {
        this.idCliente = idCliente;
    }

    @Override
    public TipoEvento getTipo() {
        return TipoEvento.ACQUISTO_CARTA_FEDELTA;
    }

    @Override
    public Set<UUID> getDestinatari() {
        return Collections.singleton(idCliente);
    }

    public UUID getIdCliente() {
        return idCliente;
    }
}