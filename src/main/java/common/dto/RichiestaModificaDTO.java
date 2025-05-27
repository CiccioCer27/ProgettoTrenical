package dto;

import enums.ClasseServizio;
import enums.TipoPrezzo;

public class RichiestaModificaDTO {

    private final BigliettoDTO bigliettoVecchio;
    private final TrattaDTO tratta;
    private final ClasseServizio nuovaClasse;
    private final TipoPrezzo tipoPrezzo;
    private final String idCliente;

    public RichiestaModificaDTO(BigliettoDTO bigliettoVecchio, TrattaDTO tratta,
                                ClasseServizio nuovaClasse, TipoPrezzo tipoPrezzo, String idCliente) {
        this.bigliettoVecchio = bigliettoVecchio;
        this.tratta = tratta;
        this.nuovaClasse = nuovaClasse;
        this.tipoPrezzo = tipoPrezzo;
        this.idCliente = idCliente;
    }

    public BigliettoDTO getBigliettoVecchio() {
        return bigliettoVecchio;
    }

    public TrattaDTO getTratta() {
        return tratta;
    }

    public ClasseServizio getNuovaClasse() {
        return nuovaClasse;
    }

    public TipoPrezzo getTipoPrezzo() {
        return tipoPrezzo;
    }

    public String getIdCliente() {
        return idCliente;
    }
}