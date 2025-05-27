package eventi;

import dto.BigliettoDTO;

public class EventoModifica implements Evento {
    private final BigliettoDTO bigliettoOriginale;
    private final BigliettoDTO bigliettoModificato;

    public EventoModifica(BigliettoDTO originale, BigliettoDTO modificato) {
        this.bigliettoOriginale = originale;
        this.bigliettoModificato = modificato;
    }

    public BigliettoDTO getBigliettoOriginale() {
        return bigliettoOriginale;
    }

    @Override
    public BigliettoDTO getBigliettoNuovo() {
        return bigliettoModificato;
    }
}