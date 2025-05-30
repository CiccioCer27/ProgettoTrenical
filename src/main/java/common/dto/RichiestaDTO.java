package dto;

import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.time.LocalDate;

public class RichiestaDTO {

    private final String tipo;
    private final String idCliente;
    private final TrattaDTO tratta;
    private final ClasseServizio classeServizio;
    private final TipoPrezzo tipoPrezzo;
    private final BigliettoDTO biglietto;
    private final Double penale;

    // Nuovi campi per filtro tratte
    private final LocalDate data;
    private final String partenza;
    private final String arrivo;
    private final String tipoTreno;
    private final String fasciaOraria;

    private final String messaggioExtra;

    private RichiestaDTO(Builder builder) {
        this.tipo = builder.tipo;
        this.idCliente = builder.idCliente;
        this.tratta = builder.tratta;
        this.classeServizio = builder.classeServizio;
        this.tipoPrezzo = builder.tipoPrezzo;
        this.biglietto = builder.biglietto;
        this.penale = builder.penale;

        this.data = builder.data;
        this.partenza = builder.partenza;
        this.arrivo = builder.arrivo;
        this.tipoTreno = builder.tipoTreno;
        this.fasciaOraria = builder.fasciaOraria;

        this.messaggioExtra = builder.messaggioExtra;
    }

    public String getTipo() { return tipo; }
    public String getIdCliente() { return idCliente; }
    public TrattaDTO getTratta() { return tratta; }
    public ClasseServizio getClasseServizio() { return classeServizio; }
    public TipoPrezzo getTipoPrezzo() { return tipoPrezzo; }
    public BigliettoDTO getBiglietto() { return biglietto; }
    public Double getPenale() { return penale; }

    public LocalDate getData() { return data; }
    public String getPartenza() { return partenza; }
    public String getArrivo() { return arrivo; }
    public String getTipoTreno() { return tipoTreno; }
    public String getFasciaOraria() { return fasciaOraria; }

    public String getMessaggioExtra() { return messaggioExtra; }

    public static class Builder {
        private String tipo;
        private String idCliente;
        private TrattaDTO tratta;
        private ClasseServizio classeServizio;
        private TipoPrezzo tipoPrezzo;
        private BigliettoDTO biglietto;
        private Double penale;

        private LocalDate data;
        private String partenza;
        private String arrivo;
        private String tipoTreno;
        private String fasciaOraria;

        private String messaggioExtra;

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

        public Builder biglietto(BigliettoDTO biglietto) {
            this.biglietto = biglietto;
            return this;
        }

        public Builder penale(Double penale) {
            this.penale = penale;
            return this;
        }

        public Builder data(LocalDate data) {
            this.data = data;
            return this;
        }

        public Builder partenza(String partenza) {
            this.partenza = partenza;
            return this;
        }

        public Builder arrivo(String arrivo) {
            this.arrivo = arrivo;
            return this;
        }

        public Builder tipoTreno(String tipoTreno) {
            this.tipoTreno = tipoTreno;
            return this;
        }

        public Builder fasciaOraria(String fasciaOraria) {
            this.fasciaOraria = fasciaOraria;
            return this;
        }

        public Builder messaggioExtra(String messaggioExtra) {
            this.messaggioExtra = messaggioExtra;
            return this;
        }

        public RichiestaDTO build() {
            return new RichiestaDTO(this);
        }
    }

    @Override
    public String toString() {
        return "RichiestaDTO{" +
                "tipo='" + tipo + '\'' +
                ", idCliente='" + idCliente + '\'' +
                ", tratta=" + tratta +
                ", classeServizio=" + classeServizio +
                ", tipoPrezzo=" + tipoPrezzo +
                ", biglietto=" + biglietto +
                ", penale=" + penale +
                ", data=" + data +
                ", partenza='" + partenza + '\'' +
                ", arrivo='" + arrivo + '\'' +
                ", tipoTreno='" + tipoTreno + '\'' +
                ", fasciaOraria='" + fasciaOraria + '\'' +
                ", messaggioExtra='" + messaggioExtra + '\'' +
                '}';
    }
}