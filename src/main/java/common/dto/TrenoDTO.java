package dto;

import java.util.UUID;

// DTO per la comunicazione tra client e server (immutabile)
public class TrenoDTO {
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

    public TrenoDTO(UUID id, int numero, String tipologia, int capienzaTotale,
                    boolean wifiDisponibile, boolean preseElettriche, boolean ariaCondizionata,
                    String serviziRistorazione, boolean accessibileDisabili, String nomeCommerciale) {
        this.id = id;
        this.numero = numero;
        this.tipologia = tipologia;
        this.capienzaTotale = capienzaTotale;
        this.wifiDisponibile = wifiDisponibile;
        this.preseElettriche = preseElettriche;
        this.ariaCondizionata = ariaCondizionata;
        this.serviziRistorazione = serviziRistorazione;
        this.accessibileDisabili = accessibileDisabili;
        this.nomeCommerciale = nomeCommerciale;
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
}