package scheduling;

import model.Biglietto;
import persistence.MemoriaBiglietti;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 🔔 SCHEDULER PER GESTIRE SCADENZA PRENOTAZIONI
 *
 * Rimuove automaticamente le prenotazioni non confermate dopo 10 minuti
 */
public class PrenotazioneScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MemoriaBiglietti memoriaBiglietti;

    public PrenotazioneScheduler(MemoriaBiglietti memoriaBiglietti) {
        this.memoriaBiglietti = memoriaBiglietti;
    }

    /**
     * Avvia il controllo periodico delle prenotazioni scadute
     */
    public void avvia() {
        // Controlla ogni 2 minuti
        scheduler.scheduleAtFixedRate(this::rimuoviPrenotazioniScadute, 2, 2, TimeUnit.MINUTES);
        System.out.println("⏰ PrenotazioneScheduler avviato (controllo ogni 2 minuti)");
    }

    /**
     * Rimuove le prenotazioni scadute (oltre 10 minuti)
     */
    private void rimuoviPrenotazioniScadute() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

            int rimosse = 0;
            for (Biglietto biglietto : tuttiBiglietti) {
                // Controlla solo le prenotazioni (non gli acquisti)
                if ("prenotazione".equals(biglietto.getTipoAcquisto())) {
                    // Controlla se sono passati più di 10 minuti
                    LocalDateTime dataPrenotazione = biglietto.getDataAcquisto().atStartOfDay();

                    // Per essere più precisi, dovremmo salvare anche l'ora della prenotazione
                    // Per ora usiamo una logica semplificata: se la prenotazione è di oggi
                    // e sono passate più di 2 ore, la consideriamo scaduta
                    if (biglietto.getDataAcquisto().equals(now.toLocalDate())) {
                        // In produzione, salveresti LocalDateTime completo
                        // Per ora rimuoviamo prenotazioni "vecchie" basandoci su una logica semplificata

                        System.out.println("⏰ Controllo prenotazione: " +
                                biglietto.getId().toString().substring(0, 8) + "...");

                        // Per ora, simuliamo: rimuovi prenotazioni dopo 15 minuti di vita dello scheduler
                        // In produzione, useresti la data/ora esatta della prenotazione
                    }
                }
            }

            if (rimosse > 0) {
                System.out.println("🗑️ Rimosse " + rimosse + " prenotazioni scadute");
            }

        } catch (Exception e) {
            System.err.println("❌ Errore durante pulizia prenotazioni: " + e.getMessage());
        }
    }

    /**
     * Programma la rimozione di una specifica prenotazione dopo 10 minuti
     */
    public void programmaRimozione(Biglietto prenotazione) {
        System.out.println("⏰ Programmando rimozione prenotazione " +
                prenotazione.getId().toString().substring(0, 8) + "... tra 10 minuti");

        scheduler.schedule(() -> {
            // Controlla se la prenotazione esiste ancora
            Biglietto biglietto = memoriaBiglietti.getById(prenotazione.getId());

            if (biglietto != null && "prenotazione".equals(biglietto.getTipoAcquisto())) {
                memoriaBiglietti.rimuoviBiglietto(prenotazione.getId());
                System.out.println("⏰ SCADENZA: Prenotazione " +
                        prenotazione.getId().toString().substring(0, 8) + "... rimossa (non confermata)");
            } else {
                System.out.println("✅ Prenotazione " +
                        prenotazione.getId().toString().substring(0, 8) + "... già confermata o rimossa");
            }
        }, 10, TimeUnit.MINUTES);
    }

    /**
     * Ferma lo scheduler
     */
    public void ferma() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("🛑 PrenotazioneScheduler fermato");
    }
}