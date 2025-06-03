package strategy;

import model.Tratta;
import model.Promozione;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.util.List;
import java.util.UUID;

/**
 * 💰 STRATEGIA PREZZO STANDARD
 * Applica solo i prezzi base senza promozioni speciali
 */
public class PrezzoStandardStrategy implements PrezzoStrategy {

    @Override
    public PrezzoCalcolato calcolaPrezzoFinale(Tratta tratta, ClasseServizio classeServizio,
                                               TipoPrezzo tipoPrezzo, boolean isClienteFedele,
                                               UUID idCliente, List<Promozione> promozioniAttive) {

        double prezzoBase = tratta.getPrezzi().get(classeServizio).getPrezzo(tipoPrezzo);

        return new PrezzoCalcolato(
                prezzoBase,
                prezzoBase,
                0.0,
                "Prezzo standard",
                List.of(),
                tipoPrezzo
        );
    }

    @Override
    public boolean isApplicabile(Tratta tratta, ClasseServizio classeServizio, boolean isClienteFedele) {
        return true; // Sempre applicabile come fallback
    }

    @Override
    public int getPriorita() {
        return 1; // Priorità più bassa
    }

    @Override
    public String getNome() {
        return "Standard";
    }
}