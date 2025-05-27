package dto;

import java.util.UUID;

public class ClienteDTO {
    private final UUID id;
    private final String nome;
    private final String cognome;
    private final String email;
    private final boolean fedelta;
    private final int eta;
    private final String residenza;
    private final int puntiFedelta;
    private final String cellulare;

    public ClienteDTO(UUID id, String nome, String cognome, String email,
                      boolean fedelta, int eta, String residenza,
                      int puntiFedelta, String cellulare) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.fedelta = fedelta;
        this.eta = eta;
        this.residenza = residenza;
        this.puntiFedelta = puntiFedelta;
        this.cellulare = cellulare;
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
}