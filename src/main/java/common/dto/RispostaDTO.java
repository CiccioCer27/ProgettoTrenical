 package dto;

public class RispostaDTO {

    private final String esito;           // "OK" o "ERRORE"
    private final String messaggio;       // eventuale messaggio di errore o conferma
    private final Object dati;            // può essere BigliettoDTO, List<TrattaDTO>, ecc.

    public RispostaDTO(String esito, String messaggio, Object dati) {
        this.esito = esito;
        this.messaggio = messaggio;
        this.dati = dati;
    }

    public String getEsito() {
        return esito;
    }

    public String getMessaggio() {
        return messaggio;
    }

    public Object getDati() {
        return dati;
    }

    public dto.BigliettoDTO getBiglietto() {
        if (dati instanceof dto.BigliettoDTO) {
            return (dto.BigliettoDTO) dati;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<dto.TrattaDTO> getTratte() {
        if (dati instanceof java.util.List<?> list && !list.isEmpty() && list.get(0) instanceof dto.TrattaDTO) {
            return (java.util.List<dto.TrattaDTO>) list;
        }
        return null;
    }
} 