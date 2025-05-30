package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enums.ClasseServizio;

import java.time.LocalDate;
import java.util.UUID;

public class Biglietto {

    private final UUID id;
    private final UUID idCliente;
    private final UUID idTratta;
    private final ClasseServizio classe;
    private final boolean conCartaFedelta;
    private final double prezzoPagato;
    private final LocalDate dataAcquisto;
    private final String tipoAcquisto;

    // ✅ Constructor per Jackson
    @JsonCreator
    public Biglietto(
            @JsonProperty("id") UUID id,
            @JsonProperty("idCliente") UUID idCliente,
            @JsonProperty("idTratta") UUID idTratta,
            @JsonProperty("classe") ClasseServizio classe,
            @JsonProperty("conCartaFedelta") boolean conCartaFedelta,
            @JsonProperty("prezzoPagato") double prezzoPagato,
            @JsonProperty("dataAcquisto") LocalDate dataAcquisto,
            @JsonProperty("tipoAcquisto") String tipoAcquisto
    ) {
        this.id = id;
        this.idCliente = idCliente;
        this.idTratta = idTratta;
        this.classe = classe;
        this.conCartaFedelta = conCartaFedelta;
        this.prezzoPagato = prezzoPagato;
        this.dataAcquisto = dataAcquisto;
        this.tipoAcquisto = tipoAcquisto;
    }

    // ✅ Constructor privato per Builder
    private Biglietto(Builder builder) {
        this.id = builder.id;
        this.idCliente = builder.idCliente;
        this.idTratta = builder.idTratta;
        this.classe = builder.classe;
        this.conCartaFedelta = builder.conCartaFedelta;
        this.prezzoPagato = builder.prezzoPagato;
        this.dataAcquisto = builder.dataAcquisto;
        this.tipoAcquisto = builder.tipoAcquisto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    public UUID getIdTratta() {
        return idTratta;
    }

    public ClasseServizio getClasse() {
        return classe;
    }

    public boolean isConCartaFedelta() {
        return conCartaFedelta;
    }

    public double getPrezzoPagato() {
        return prezzoPagato;
    }

    public LocalDate getDataAcquisto() {
        return dataAcquisto;
    }

    public String getTipoAcquisto() {
        return tipoAcquisto;
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID idCliente;
        private UUID idTratta;
        private ClasseServizio classe;
        private boolean conCartaFedelta;
        private double prezzoPagato;
        private LocalDate dataAcquisto;
        private String tipoAcquisto;

        public Builder idCliente(UUID idCliente) {
            this.idCliente = idCliente;
            return this;
        }

        public Builder idTratta(UUID idTratta) {
            this.idTratta = idTratta;
            return this;
        }

        public Builder classe(ClasseServizio classe) {
            this.classe = classe;
            return this;
        }

        public Builder conCartaFedelta(boolean conCartaFedelta) {
            this.conCartaFedelta = conCartaFedelta;
            return this;
        }

        public Builder prezzoPagato(double prezzoPagato) {
            this.prezzoPagato = prezzoPagato;
            return this;
        }

        public Builder dataAcquisto(LocalDate dataAcquisto) {
            this.dataAcquisto = dataAcquisto;
            return this;
        }

        public Builder tipoAcquisto(String tipoAcquisto) {
            this.tipoAcquisto = tipoAcquisto;
            return this;
        }

        public Biglietto build() {
            return new Biglietto(this);
        }
    }

    @Override
    public String toString() {
        return "Biglietto{" +
                "id=" + id +
                ", idCliente=" + idCliente +
                ", idTratta=" + idTratta +
                ", classe=" + classe +
                ", conCartaFedelta=" + conCartaFedelta +
                ", prezzoPagato=" + prezzoPagato +
                ", dataAcquisto=" + dataAcquisto +
                ", tipoAcquisto='" + tipoAcquisto + '\'' +
                '}';
    }
}