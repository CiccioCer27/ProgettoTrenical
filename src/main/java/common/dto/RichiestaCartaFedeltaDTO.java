package dto;

public class RichiestaCartaFedeltaDTO {

    private final String idCliente;

    public RichiestaCartaFedeltaDTO(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getIdCliente() {
        return idCliente;
    }
}