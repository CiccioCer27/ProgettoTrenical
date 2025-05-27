package dto;

import enums.ClasseServizio;

public class RichiestaPrenotazioneDTO {

    private final TrattaDTO tratta;
    private final ClasseServizio classe;
    private final String idCliente;

    public RichiestaPrenotazioneDTO(TrattaDTO tratta, ClasseServizio classe, String idCliente) {
        this.tratta = tratta;
        this.classe = classe;
        this.idCliente = idCliente;
    }

    public TrattaDTO getTratta() {
        return tratta;
    }

    public ClasseServizio getClasse() {
        return classe;
    }

    public String getIdCliente() {
        return idCliente;
    }
}