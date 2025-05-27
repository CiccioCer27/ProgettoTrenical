package model;

import java.util.UUID;

public class Cliente {
    private final UUID id;
    private final String nome;
    private final String cognome;
    private final String email;
    private final boolean fedelta;
    private final int eta;
    private final String residenza;
    private final int puntiFedelta;
    private final String cellulare;

    private Cliente(Builder builder) {
        this.id = builder.id;
        this.nome = builder.nome;
        this.cognome = builder.cognome;
        this.email = builder.email;
        this.fedelta = builder.fedelta;
        this.eta = builder.eta;
        this.residenza = builder.residenza;
        this.puntiFedelta = builder.puntiFedelta;
        this.cellulare = builder.cellulare;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
    public String getEmail() { return email; }
    public boolean isFedelta() { return fedelta; }
    public int getEta() { return eta; }
    public String getResidenza() { return residenza; }
    public int getPuntiFedelta() { return puntiFedelta; }
    public String getCellulare() { return cellulare; }

    public static class Builder {
        private UUID id = UUID.randomUUID(); // Default: genera uno nuovo
        private final String nome;
        private final String cognome;
        private final String email;

        private boolean fedelta = false;
        private int eta = 0;
        private String residenza = "";
        private int puntiFedelta = 0;
        private String cellulare = "";

        public Builder(String nome, String cognome, String email) {
            this.nome = nome;
            this.cognome = cognome;
            this.email = email;
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder fedelta(boolean fedelta) {
            this.fedelta = fedelta;
            return this;
        }

        public Builder eta(int eta) {
            this.eta = eta;
            return this;
        }

        public Builder residenza(String residenza) {
            this.residenza = residenza;
            return this;
        }

        public Builder puntiFedelta(int puntiFedelta) {
            this.puntiFedelta = puntiFedelta;
            return this;
        }

        public Builder cellulare(String cellulare) {
            this.cellulare = cellulare;
            return this;
        }

        public Cliente build() {
            return new Cliente(this);
        }
    }
}