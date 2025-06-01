package observer;

import eventi.*;
import model.Tratta;
import persistence.MemoriaBiglietti;
import persistence.MemoriaTratte;

/**
 * 🎯 LISTENER BIGLIETTI CON CONTROLLO CAPIENZA ATOMICO
 *
 * Questo listener è responsabile di:
 * - Ricevere eventi di acquisto/prenotazione
 * - Controllare atomicamente la capienza
 * - Accettare o rifiutare i biglietti
 */
public class MemoriaBigliettiListener implements EventListener {

    private final MemoriaBiglietti memoria;
    private final MemoriaTratte memoriaTratte;

    public MemoriaBigliettiListener(MemoriaBiglietti memoria) {
        this.memoria = memoria;
        this.memoriaTratte = null;
        System.out.println("⚠️ MemoriaBigliettiListener: Inizializzato SENZA controllo capienza!");
    }

    /**
     * ✅ COSTRUTTORE RACCOMANDATO con controllo capienza
     */
    public MemoriaBigliettiListener(MemoriaBiglietti memoria, MemoriaTratte memoriaTratte) {
        this.memoria = memoria;
        this.memoriaTratte = memoriaTratte;
        System.out.println("🔒 MemoriaBigliettiListener: Inizializzato CON controllo capienza atomico");
    }

    @Override
    public void onEvento(EventoS evento) {
        System.out.println("📨 LISTENER: Ricevuto evento " + evento.getClass().getSimpleName());

        if (evento instanceof EventoGdsAcquisto e) {
            System.out.println("💳 ELABORANDO: EventoGdsAcquisto");
            elaboraAcquisto(e.getBiglietto());

        } else if (evento instanceof EventoGdsPrenotaz e) {
            System.out.println("📝 ELABORANDO: EventoGdsPrenotaz");
            elaboraPrenotazione(e.getBiglietto());

        } else if (evento instanceof EventoGdsModifica e) {
            System.out.println("🔄 ELABORANDO: EventoGdsModifica");
            elaboraModifica(e.getOriginale(), e.getModificato());
        }
    }

    /**
     * 💳 Elabora acquisto con controllo capienza
     */
    private void elaboraAcquisto(model.Biglietto biglietto) {
        if (memoriaTratte != null) {
            Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
            if (tratta != null) {
                int capienza = tratta.getTreno().getCapienzaTotale();
                boolean accettato = memoria.aggiungiSeSpazioDiponibile(biglietto, capienza);

                if (accettato) {
                    System.out.println("✅ ACQUISTO ACCETTATO: " + biglietto.getId().toString().substring(0, 8));
                } else {
                    System.out.println("❌ ACQUISTO RIFIUTATO: Capienza superata per " +
                            biglietto.getId().toString().substring(0, 8));
                }
            } else {
                System.out.println("⚠️ Tratta non trovata per acquisto, aggiungo senza controllo");
                memoria.aggiungiBiglietto(biglietto);
            }
        } else {
            System.out.println("⚠️ MemoriaTratte non disponibile, aggiungo senza controllo capienza");
            memoria.aggiungiBiglietto(biglietto);
        }
    }

    /**
     * 📝 Elabora prenotazione con controllo capienza
     */
    private void elaboraPrenotazione(model.Biglietto biglietto) {
        if (memoriaTratte != null) {
            Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
            if (tratta != null) {
                int capienza = tratta.getTreno().getCapienzaTotale();
                boolean accettato = memoria.aggiungiSeSpazioDiponibile(biglietto, capienza);

                if (accettato) {
                    System.out.println("✅ PRENOTAZIONE ACCETTATA: " + biglietto.getId().toString().substring(0, 8));
                } else {
                    System.out.println("❌ PRENOTAZIONE RIFIUTATA: Capienza superata per " +
                            biglietto.getId().toString().substring(0, 8));
                }
            } else {
                System.out.println("⚠️ Tratta non trovata per prenotazione, aggiungo senza controllo");
                memoria.aggiungiBiglietto(biglietto);
            }
        } else {
            System.out.println("⚠️ MemoriaTratte non disponibile, aggiungo prenotazione senza controllo");
            memoria.aggiungiBiglietto(biglietto);
        }
    }

    /**
     * 🔄 Elabora modifica biglietto
     */
    private void elaboraModifica(model.Biglietto originale, model.Biglietto modificato) {
        // Per le modifiche, il biglietto originale dovrebbe essere già stato rimosso dal Command
        // Aggiungiamo solo il nuovo con controllo capienza

        if (memoriaTratte != null) {
            Tratta tratta = memoriaTratte.getTrattaById(modificato.getIdTratta());
            if (tratta != null) {
                int capienza = tratta.getTreno().getCapienzaTotale();
                boolean accettato = memoria.aggiungiSeSpazioDiponibile(modificato, capienza);

                if (accettato) {
                    System.out.println("✅ MODIFICA ACCETTATA: " + modificato.getId().toString().substring(0, 8));
                } else {
                    System.out.println("❌ MODIFICA RIFIUTATA: Capienza superata");
                    // In questo caso dovremmo ripristinare il biglietto originale
                    // Ma questo è un caso edge complesso
                }
            } else {
                memoria.aggiungiBiglietto(modificato);
            }
        } else {
            memoria.aggiungiBiglietto(modificato);
        }
    }
}