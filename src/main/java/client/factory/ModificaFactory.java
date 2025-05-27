package factory;

import dto.*;
import enums.*;

import java.util.UUID;

public class ModificaFactory implements RichiestaFactory {
    private final String idCliente;
    private final BigliettoDTO bigliettoOriginale;
    private final TrattaDTO nuovaTratta;
    private final ClasseServizio nuovaClasse;
    private final TipoPrezzo tipoPrezzo;

    public ModificaFactory(String idCliente, BigliettoDTO bigliettoOriginale, TrattaDTO nuovaTratta,
                           ClasseServizio nuovaClasse, TipoPrezzo tipoPrezzo) {
        this.idCliente = idCliente;
        this.bigliettoOriginale = bigliettoOriginale;
        this.nuovaTratta = nuovaTratta;
        this.nuovaClasse = nuovaClasse;
        this.tipoPrezzo = tipoPrezzo;
    }

    @Override
    public RichiestaDTO creaRichiesta() {
        return new RichiestaDTO.Builder()
                .tipo("MODIFICA")
                .idCliente(idCliente)
                .bigliettoOriginale(bigliettoOriginale)
                .tratta(nuovaTratta)
                .classeServizio(nuovaClasse)
                .tipoPrezzo(tipoPrezzo)
                .build();
    }
}