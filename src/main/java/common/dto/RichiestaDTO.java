package dto;

import enums.ClasseServizio;
import enums.TipoPrezzo;

/**
 * Rappresenta una richiesta generica inviata dal client al server.
 */
public class RichiestaDTO {

    private final String tipo;
    private final String idCliente; // ✅ identificatore univoco stringa (es. UUID come stringa)
    private final TrattaDTO tratta;
    private final ClasseServizio classeServizio;
    private final TipoPrezzo tipoPrezzo;
    private final BigliettoDTO bigliettoOriginale;
    private final String messaggioExtra;
    private final Object payload;

    private RichiestaDTO(Builder builder) {
        this.tipo = builder.tipo;
        this.idCliente = builder.idCliente;
        this.tratta = builder.tratta;
        this.classeServizio = builder.classeServizio;
        this.tipoPrezzo = builder.tipoPrezzo;
        this.bigliettoOriginale = builder.bigliettoOriginale;
        this.messaggioExtra = builder.messaggioExtra;
        this.payload = builder.payload;
    }

    public String getTipo() {
        return tipo;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public TrattaDTO getTratta() {
        return tratta;
    }

    public ClasseServizio getClasseServizio() {
        return classeServizio;
    }

    public TipoPrezzo getTipoPrezzo() {
        return tipoPrezzo;
    }

    public BigliettoDTO getBigliettoOriginale() {
        return bigliettoOriginale;
    }

    public String getMessaggioExtra() {
        return messaggioExtra;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "RichiestaDTO{" +
                "tipo='" + tipo + '\'' +
                ", idCliente='" + idCliente + '\'' +
                ", tratta=" + tratta +
                ", classeServizio=" + classeServizio +
                ", tipoPrezzo=" + tipoPrezzo +
                ", bigliettoOriginale=" + bigliettoOriginale +
                ", messaggioExtra='" + messaggioExtra + '\'' +
                ", payload=" + payload +
                '}';
    }

    public static class Builder {
        private String tipo;
        private String idCliente;
        private TrattaDTO tratta;
        private ClasseServizio classeServizio;
        private TipoPrezzo tipoPrezzo;
        private BigliettoDTO bigliettoOriginale;
        private String messaggioExtra;
        private Object payload;

        public Builder tipo(String tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder idCliente(String idCliente) {
            this.idCliente = idCliente;
            return this;
        }

        public Builder tratta(TrattaDTO tratta) {
            this.tratta = tratta;
            return this;
        }

        public Builder classeServizio(ClasseServizio classeServizio) {
            this.classeServizio = classeServizio;
            return this;
        }

        public Builder tipoPrezzo(TipoPrezzo tipoPrezzo) {
            this.tipoPrezzo = tipoPrezzo;
            return this;
        }

        public Builder bigliettoOriginale(BigliettoDTO bigliettoOriginale) {
            this.bigliettoOriginale = bigliettoOriginale;
            return this;
        }

        public Builder messaggioExtra(String messaggioExtra) {
            this.messaggioExtra = messaggioExtra;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public RichiestaDTO build() {
            return new RichiestaDTO(this);
        }
    }
}