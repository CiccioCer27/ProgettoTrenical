package dto;

import enums.ClasseServizio;
import enums.TipoPrezzo;

public class RichiestaAcquistoDTO {

    private final TrattaDTO tratta;
    private final ClasseServizio classe;
    private final TipoPrezzo tipoPrezzoRichiesto;
    private final String idCliente;

    public RichiestaAcquistoDTO(TrattaDTO tratta, ClasseServizio classe, TipoPrezzo tipoPrezzoRichiesto, String idCliente) {
        this.tratta = tratta;
        this.classe = classe;
        this.tipoPrezzoRichiesto = tipoPrezzoRichiesto;
        this.idCliente = idCliente;
    }

    public TrattaDTO getTratta() {
        return tratta;
    }

    public ClasseServizio getClasse() {
        return classe;
    }

    public TipoPrezzo getTipoPrezzoRichiesto() {
        return tipoPrezzoRichiesto;
    }

    public String getIdCliente() {
        return idCliente;
    }
}