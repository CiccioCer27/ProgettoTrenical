package eventi;

import java.util.Set;
import java.util.UUID;

public interface EventoS {
    TipoEvento getTipo();
    Set<UUID> getDestinatari();  // Database o altri interessati
}