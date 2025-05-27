package factory;

import dto.*;
import enums.*;

import java.util.UUID;

public class PrenotazioneFactory implements RichiestaFactory {
    private final String idCliente;
    private final TrattaDTO tratta;
    private final ClasseServizio classeServizio;

    public PrenotazioneFactory(String idCliente, TrattaDTO tratta, ClasseServizio classeServizio) {
        this.idCliente = idCliente;
        this.tratta = tratta;
        this.classeServizio = classeServizio;
    }

    @Override
    public RichiestaDTO creaRichiesta() {
        return new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(idCliente)
                .tratta(tratta)
                .classeServizio(classeServizio)
                .build();
    }
}