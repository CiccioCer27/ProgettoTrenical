package strategy;

import enums.TipoPrezzo;

import java.util.List;

/**
 * 📊 RISULTATO del calcolo prezzo con dettagli
 */
public class PrezzoCalcolato {
    private final double prezzoBase;
    private final double prezzoFinale;
    private final double scontoApplicato;
    private final String descrizioneSconto;
    private final List<String> promozioniApplicate;
    private final TipoPrezzo tipoPrezzoUsato;

    public PrezzoCalcolato(double prezzoBase, double prezzoFinale, double scontoApplicato,
                           String descrizioneSconto, List<String> promozioniApplicate,
                           TipoPrezzo tipoPrezzoUsato) {
        this.prezzoBase = prezzoBase;
        this.prezzoFinale = prezzoFinale;
        this.scontoApplicato = scontoApplicato;
        this.descrizioneSconto = descrizioneSconto;
        this.promozioniApplicate = promozioniApplicate != null ? List.copyOf(promozioniApplicate) : List.of();
        this.tipoPrezzoUsato = tipoPrezzoUsato;
    }

    // Getters
    public double getPrezzoBase() {
        return prezzoBase;
    }

    public double getPrezzoFinale() {
        return prezzoFinale;
    }

    public double getScontoApplicato() {
        return scontoApplicato;
    }

    public String getDescrizioneSconto() {
        return descrizioneSconto;
    }

    public List<String> getPromozioniApplicate() {
        return promozioniApplicate;
    }

    public TipoPrezzo getTipoPrezzoUsato() {
        return tipoPrezzoUsato;
    }

    public double getPercentualeSconto() {
        return prezzoBase > 0 ? (scontoApplicato / prezzoBase) * 100 : 0;
    }

    @Override
    public String toString() {
        return String.format("PrezzoCalcolato{base=%.2f, finale=%.2f, sconto=%.2f%%, desc='%s'}",
                prezzoBase, prezzoFinale, getPercentualeSconto(), descrizioneSconto);
    }
}
