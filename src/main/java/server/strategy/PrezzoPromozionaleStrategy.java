package strategy;

import model.Tratta;
import model.Promozione;
import model.PromozioneGenerale;
import model.PromozioneFedelta;
import model.PromozioneTratta;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🎉 STRATEGIA PREZZO PROMOZIONALE
 * Applica la migliore promozione disponibile
 */
public class PrezzoPromozionaleStrategy implements PrezzoStrategy {

    @Override
    public PrezzoCalcolato calcolaPrezzoFinale(Tratta tratta, ClasseServizio classeServizio,
                                               TipoPrezzo tipoPrezzo, boolean isClienteFedele,
                                               UUID idCliente, List<Promozione> promozioniAttive) {

        double prezzoBase = tratta.getPrezzi().get(classeServizio).getPrezzo(TipoPrezzo.INTERO);
        double migliorPrezzo = prezzoBase;
        double migliorSconto = 0.0;
        String descrizioneSconto = "Nessuna promozione applicabile";
        List<String> promozioniApplicate = new ArrayList<>();
        TipoPrezzo tipoPrezzoFinale = tipoPrezzo;

        LocalDate oggi = LocalDate.now();

        // 🔍 CERCA LA MIGLIORE PROMOZIONE
        for (Promozione promo : promozioniAttive) {
            if (!promo.isAttiva(oggi)) continue;

            // ✅ VERIFICA APPLICABILITÀ
            boolean applicabile = false;
            String motivoEsclusione = "";

            if (promo instanceof PromozioneGenerale) {
                applicabile = true;
            } else if (promo instanceof PromozioneFedelta promoFedelta) {
                applicabile = isClienteFedele;
                motivoEsclusione = isClienteFedele ? "" : "Richiede carta fedeltà";
            } else if (promo instanceof PromozioneTratta promoTratta) {
                applicabile = promoTratta.siApplicaAllaTratta(tratta.getId());
                motivoEsclusione = applicabile ? "" : "Non valida per questa tratta";
            }

            if (!applicabile) {
                System.out.println("⚠️ Promozione " + promo.getDescrizione() + " non applicabile: " + motivoEsclusione);
                continue;
            }

            // 💰 CALCOLA PREZZO CON QUESTA PROMOZIONE
            double prezzoConPromo = prezzoBase * (1 - promo.getSconto());
            double scontoAttuale = prezzoBase - prezzoConPromo;

            // ✅ VERIFICA SE È LA MIGLIORE
            if (prezzoConPromo < migliorPrezzo) {
                migliorPrezzo = prezzoConPromo;
                migliorSconto = scontoAttuale;
                descrizioneSconto = promo.getDescrizione() + " (-" + (int)(promo.getSconto() * 100) + "%)";
                promozioniApplicate.clear();
                promozioniApplicate.add(promo.getDescrizione());
                tipoPrezzoFinale = TipoPrezzo.PROMOZIONE;

                System.out.println("✅ Nuova migliore promozione: " + descrizioneSconto +
                        " (prezzo: €" + String.format("%.2f", prezzoConPromo) + ")");
            }
        }

        // 🏆 CONFRONTA CON PREZZO FEDELTÀ SE DISPONIBILE
        if (isClienteFedele && tipoPrezzo == TipoPrezzo.FEDELTA) {
            double prezzoFedelta = tratta.getPrezzi().get(classeServizio).getPrezzo(TipoPrezzo.FEDELTA);
            if (prezzoFedelta < migliorPrezzo) {
                migliorPrezzo = prezzoFedelta;
                migliorSconto = prezzoBase - prezzoFedelta;
                descrizioneSconto = "Sconto carta fedeltà";
                promozioniApplicate.clear();
                promozioniApplicate.add("Carta Fedeltà");
                tipoPrezzoFinale = TipoPrezzo.FEDELTA;

                System.out.println("🏆 Prezzo fedeltà è migliore: €" + String.format("%.2f", prezzoFedelta));
            }
        }

        return new PrezzoCalcolato(
                prezzoBase,
                migliorPrezzo,
                migliorSconto,
                descrizioneSconto,
                promozioniApplicate,
                tipoPrezzoFinale
        );
    }

    @Override
    public boolean isApplicabile(Tratta tratta, ClasseServizio classeServizio, boolean isClienteFedele) {
        return true; // Sempre applicabile
    }

    @Override
    public int getPriorita() {
        return 10; // Priorità alta
    }

    @Override
    public String getNome() {
        return "Promozionale";
    }
}
