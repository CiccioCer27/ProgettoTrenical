package factory;

import dto.RichiestaDTO;

import java.util.UUID;

public class FedeltaFactory implements RichiestaFactory {
    private final String idCliente;

    public FedeltaFactory(String idCliente) {
        this.idCliente = idCliente;
    }

    @Override
    public RichiestaDTO creaRichiesta() {
        return new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(idCliente)
                .build();
    }
}