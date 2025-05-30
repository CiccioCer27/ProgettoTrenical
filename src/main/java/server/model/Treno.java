package model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.UUID;

@JsonDeserialize(builder = Treno.Builder.class)
public class Treno {

    private final UUID id;
    private final int numero;
    private final String tipologia;
    private final int capienzaTotale;
    private final boolean wifiDisponibile;
    private final boolean preseElettriche;
    private final boolean ariaCondizionata;
    private final String serviziRistorazione;
    private final boolean accessibileDisabili;
    private final String nomeCommerciale;

    private Treno(Builder builder) {
        this.id = builder.id;
        this.numero = builder.numero;
        this.tipologia = builder.tipologia;
        this.capienzaTotale = builder.capienzaTotale;
        this.wifiDisponibile = builder.wifiDisponibile;
        this.preseElettriche = builder.preseElettriche;
        this.ariaCondizionata = builder.ariaCondizionata;
        this.serviziRistorazione = builder.serviziRistorazione;
        this.accessibileDisabili = builder.accessibileDisabili;
        this.nomeCommerciale = builder.nomeCommerciale;
    }

    public UUID getId() { return id; }
    public int getNumero() { return numero; }
    public String getTipologia() { return tipologia; }
    public int getCapienzaTotale() { return capienzaTotale; }
    public boolean isWifiDisponibile() { return wifiDisponibile; }
    public boolean isPreseElettriche() { return preseElettriche; }
    public boolean isAriaCondizionata() { return ariaCondizionata; }
    public String getServiziRistorazione() { return serviziRistorazione; }
    public boolean isAccessibileDisabili() { return accessibileDisabili; }
    public String getNomeCommerciale() { return nomeCommerciale; }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private int numero;
        private String tipologia;
        private int capienzaTotale;
        private boolean wifiDisponibile;
        private boolean preseElettriche;
        private boolean ariaCondizionata;
        private String serviziRistorazione;
        private boolean accessibileDisabili;
        private String nomeCommerciale;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder numero(int numero) { this.numero = numero; return this; }
        public Builder tipologia(String tipologia) { this.tipologia = tipologia; return this; }
        public Builder capienzaTotale(int capienzaTotale) { this.capienzaTotale = capienzaTotale; return this; }
        public Builder wifiDisponibile(boolean wifiDisponibile) { this.wifiDisponibile = wifiDisponibile; return this; }
        public Builder preseElettriche(boolean preseElettriche) { this.preseElettriche = preseElettriche; return this; }
        public Builder ariaCondizionata(boolean ariaCondizionata) { this.ariaCondizionata = ariaCondizionata; return this; }
        public Builder serviziRistorazione(String serviziRistorazione) { this.serviziRistorazione = serviziRistorazione; return this; }
        public Builder accessibileDisabili(boolean accessibileDisabili) { this.accessibileDisabili = accessibileDisabili; return this; }
        public Builder nomeCommerciale(String nomeCommerciale) { this.nomeCommerciale = nomeCommerciale; return this; }

        public Treno build() { return new Treno(this); }
    }
}