package factory;

import dto.*;
import enums.*;

import java.util.UUID;

public class AcquistoFactory implements RichiestaFactory {
    private final String idCliente;
    private final TrattaDTO tratta;
    private final ClasseServizio classeServizio;
    private final TipoPrezzo tipoPrezzo;

    public AcquistoFactory(String idCliente, TrattaDTO tratta, ClasseServizio classeServizio, TipoPrezzo tipoPrezzo) {
        this.idCliente = idCliente;
        this.tratta = tratta;
        this.classeServizio = classeServizio;
        this.tipoPrezzo = tipoPrezzo;
    }

    @Override
    public RichiestaDTO creaRichiesta() {
        return new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(idCliente)
                .tratta(tratta)
                .classeServizio(classeServizio)
                .tipoPrezzo(tipoPrezzo)
                .build();
    }
}