package scheduling;

import model.Biglietto;
import persistence.MemoriaBiglietti;
import persistence.MemoriaOsservatori;  // ✅ AGGIUNTO

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 🔔 SCHEDULER PER GESTIRE SCADENZA PRENOTAZIONI - CON CLEANUP NOTIFICHE
 *
 * AGGIORNAMENTO: Ora rimuove anche dalle notifiche quando elimina prenotazioni scadute
 */
public class PrenotazioneScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaOsservatori memoriaOsservatori;  // ✅ AGGIUNTO

    public PrenotazioneScheduler(MemoriaBiglietti memoriaBiglietti, MemoriaOsservatori memoriaOsservatori) {
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaOsservatori = memoriaOsservatori;  // ✅ INJECTION
    }

    /**
     * Avvia il controllo periodico delle prenotazioni scadute
     */
    public void avvia() {
        // Controlla ogni 2 minuti
        scheduler.scheduleAtFixedRate(this::rimuoviPrenotazioniScadute, 2, 2, TimeUnit.MINUTES);
        System.out.println("⏰ PrenotazioneScheduler COMPLETO avviato (controllo ogni 2 minuti)");
    }

    /**
     * ✅ AGGIORNATO: Rimuove prenotazioni scadute + notifiche
     */
    private void rimuoviPrenotazioniScadute() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

            int rimosse = 0;
            int notificheRimosse = 0;

            for (Biglietto biglietto : tuttiBiglietti) {
                // Controlla solo le prenotazioni (non gli acquisti)
                if ("prenotazione".equals(biglietto.getTipoAcquisto())) {

                    // ⚠️ LOGIC SEMPLIFICATA: Rimuovi prenotazioni più vecchie di X ore
                    // In produzione, useresti LocalDateTime preciso della prenotazione
                    LocalDateTime dataPrenotazione = biglietto.getDataAcquisto().atStartOfDay();
                    long oreTrascorse = java.time.Duration.between(dataPrenotazione, now).toHours();

                    if (oreTrascorse > 2) { // Scadute dopo 2 ore (regolabile)
                        System.out.println("⏰ SCHEDULER: Rimuovendo prenotazione scaduta: " +
                                biglietto.getId().toString().substring(0, 8) + "...");

                        // 🗑️ STEP 1: Rimuovi biglietto
                        boolean rimossaDaMemoria = memoriaBiglietti.rimuoviBiglietto(biglietto.getId());

                        if (rimossaDaMemoria) {
                            rimosse++;

                            // 🗑️ STEP 2: ✅ NUOVO - Rimuovi dalle notifiche
                            try {
                                boolean rimossaDaNotifiche = memoriaOsservatori.rimuoviOsservatore(
                                        biglietto.getIdTratta(),
                                        biglietto.getIdCliente()
                                );

                                if (rimossaDaNotifiche) {
                                    notificheRimosse++;
                                }

                            } catch (Exception e) {
                                System.err.println("❌ Errore rimozione notifiche scheduler: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (rimosse > 0) {
                System.out.println("🧹 ✅ SCHEDULER CLEANUP: " + rimosse + " prenotazioni + " +
                        notificheRimosse + " notifiche rimosse");
            }

        } catch (Exception e) {
            System.err.println("❌ Errore durante pulizia COMPLETA prenotazioni: " + e.getMessage());
        }
    }

    /**
     * ✅ AGGIORNATO: Programma rimozione con cleanup notifiche
     */
    public void programmaRimozione(Biglietto prenotazione) {
        System.out.println("⏰ SCHEDULER: Programmando rimozione COMPLETA prenotazione " +
                prenotazione.getId().toString().substring(0, 8) + "... tra 10 minuti");

        scheduler.schedule(() -> {
            // Controlla se la prenotazione esiste ancora
            Biglietto biglietto = memoriaBiglietti.getById(prenotazione.getId());

            if (biglietto != null && "prenotazione".equals(biglietto.getTipoAcquisto())) {

                // 🗑️ Rimuovi biglietto
                boolean rimossa = memoriaBiglietti.rimuoviBiglietto(prenotazione.getId());

                if (rimossa) {
                    System.out.println("⏰ SCHEDULER SCADENZA: Prenotazione " +
                            prenotazione.getId().toString().substring(0, 8) + "... rimossa");

                    // 🗑️ ✅ NUOVO - Rimuovi dalle notifiche
                    try {
                        memoriaOsservatori.rimuoviOsservatore(
                                prenotazione.getIdTratta(),
                                prenotazione.getIdCliente()
                        );
                        System.out.println("📡 Cliente rimosso dalle notifiche (scadenza programmata)");

                    } catch (Exception e) {
                        System.err.println("❌ Errore rimozione notifiche scadenza programmata: " + e.getMessage());
                    }
                }

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
        System.out.println("🛑 PrenotazioneScheduler COMPLETO fermato");
    }
}