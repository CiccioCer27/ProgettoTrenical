package eventi;

import dto.BigliettoDTO;

public class EventoPrenota implements Evento {
    private final BigliettoDTO biglietto;

    public EventoPrenota(BigliettoDTO biglietto) {
        this.biglietto = biglietto;
    }

    @Override
    public BigliettoDTO getBigliettoNuovo() {
        return biglietto;
    }
}