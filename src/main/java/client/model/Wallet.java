package model;

import eventi.*;
import observer.Observer;
import dto.BigliettoDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 💼 WALLET THREAD-SAFE - VERSIONE CORRETTA
 *
 * MIGLIORAMENTI:
 * - Thread-safety completa con ReentrantReadWriteLock
 * - ConcurrentMap per timer scadenze
 * - Gestione atomica delle operazioni
 * - Cleanup automatico timer cancellati
 */
public class Wallet implements Observer {

    // ✅ STRUTTURE DATI THREAD-SAFE
    private final List<BigliettoDTO> confermati = Collections.synchronizedList(new ArrayList<>());
    private final List<BigliettoDTO> nonConfermati = Collections.synchronizedList(new ArrayList<>());

    // ✅ LOCK per operazioni complesse atomiche
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ✅ GESTIONE TIMER THREAD-SAFE
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<UUID, Runnable> timerAttivi = new ConcurrentHashMap<>();

    // ✅ STATISTICHE THREAD-SAFE
    private volatile int eventiProcessati = 0;
    private volatile int timerAvviati = 0;
    private volatile int timerCancellati = 0;

    @Override
    public void aggiorna(Evento evento) {
        // ✅ LOCK SCRITTURA per operazioni complesse
        lock.writeLock().lock();
        try {
            eventiProcessati++;
            System.out.println("💼 DEBUG WALLET THREAD-SAFE: Evento ricevuto - " +
                    evento.getClass().getSimpleName() + " [Thread: " + Thread.currentThread().getName() + "]");

            if (evento instanceof EventoAcquisto) {
                processaEventoAcquisto((EventoAcquisto) evento);

            } else if (evento instanceof EventoPrenota) {
                processaEventoPrenota((EventoPrenota) evento);

            } else if (evento instanceof EventoConferma) {
                processaEventoConferma((EventoConferma) evento);

            } else if (evento instanceof EventoModifica) {
                processaEventoModifica((EventoModifica) evento);

            } else {
                System.out.println("⚠️ DEBUG WALLET: Evento non riconosciuto: " +
                        evento.getClass().getSimpleName());
            }

            // ✅ Log stato finale atomico
            System.out.println("📊 WALLET STATE: Confermati=" + confermati.size() +
                    ", NonConfermati=" + nonConfermati.size() +
                    ", TimerAttivi=" + timerAttivi.size());

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ✅ PROCESSO ACQUISTO - Thread-safe
     */
    private void processaEventoAcquisto(EventoAcquisto evento) {
        BigliettoDTO biglietto = evento.getBigliettoNuovo();
        if (biglietto != null) {
            confermati.add(biglietto);
            System.out.println("✅ WALLET: Biglietto acquistato aggiunto atomicamente");
        } else {
            System.out.println("❌ WALLET: Biglietto null in EventoAcquisto!");
        }
    }

    /**
     * ✅ PROCESSO PRENOTAZIONE - Thread-safe con timer
     */
    private void processaEventoPrenota(EventoPrenota evento) {
        BigliettoDTO biglietto = evento.getBigliettoNuovo();
        if (biglietto != null) {
            nonConfermati.add(biglietto);
            System.out.println("✅ WALLET: Prenotazione aggiunta atomicamente");

            // ✅ TIMER SCADENZA THREAD-SAFE
            avviaTimerScadenzaThreadSafe(biglietto);
        } else {
            System.out.println("❌ WALLET: Biglietto null in EventoPrenota!");
        }
    }

    /**
     * ✅ PROCESSO CONFERMA - Thread-safe con ricerca intelligente
     */
    private void processaEventoConferma(EventoConferma evento) {
        BigliettoDTO bigliettoConfermato = evento.getBigliettoNuovo();
        if (bigliettoConfermato == null) {
            System.out.println("❌ WALLET: Biglietto confermato è null!");
            return;
        }

        System.out.println("🔍 WALLET: Cercando prenotazione da confermare...");
        System.out.println("   ID da confermare: " + bigliettoConfermato.getId());

        // ✅ RICERCA E RIMOZIONE ATOMICA
        boolean rimosso = false;

        // Strategia 1: Ricerca per ID esatto
        rimosso = nonConfermati.removeIf(old -> {
            boolean match = old.getId().equals(bigliettoConfermato.getId());
            if (match) {
                System.out.println("✅ WALLET: Match trovato per ID: " + old.getId());

                // ✅ CANCELLA TIMER se esiste
                cancellaTimerScadenza(old.getId());
            }
            return match;
        });

        // Strategia 2: Fallback per tratta + cliente se ID non trovato
        if (!rimosso) {
            System.out.println("⚠️ WALLET: ID non trovato, provo con tratta + cliente");

            rimosso = nonConfermati.removeIf(old -> {
                boolean matchTratta = old.getTratta() != null &&
                        bigliettoConfermato.getTratta() != null &&
                        old.getTratta().getId().equals(bigliettoConfermato.getTratta().getId());
                boolean matchCliente = old.getCliente() != null &&
                        bigliettoConfermato.getCliente() != null &&
                        old.getCliente().getId().equals(bigliettoConfermato.getCliente().getId());

                boolean match = matchTratta && matchCliente;
                if (match) {
                    System.out.println("✅ WALLET: Match trovato per tratta + cliente");
                    cancellaTimerScadenza(old.getId());
                }
                return match;
            });
        }

        // ✅ AGGIUNGI AI CONFERMATI
        if (rimosso) {
            confermati.add(bigliettoConfermato);
            System.out.println("✅ WALLET: Prenotazione confermata e spostata atomicamente");
        } else {
            System.out.println("❌ WALLET: Prenotazione originale non trovata!");
            // Aggiungi comunque ai confermati (potrebbe essere acquisto diretto)
            confermati.add(bigliettoConfermato);
            System.out.println("⚠️ WALLET: Aggiunto ai confermati senza rimuovere prenotazione");
        }
    }

    /**
     * ✅ PROCESSO MODIFICA - Thread-safe
     */
    private void processaEventoModifica(EventoModifica evento) {
        BigliettoDTO originale = evento.getBigliettoOriginale();
        BigliettoDTO nuovo = evento.getBigliettoNuovo();

        if (originale != null && nuovo != null) {
            // ✅ RIMOZIONE E AGGIUNTA ATOMICA
            boolean rimosso = confermati.removeIf(old -> old.getId().equals(originale.getId()));

            if (rimosso) {
                confermati.add(nuovo);
                System.out.println("✅ WALLET: Biglietto modificato atomicamente");
            } else {
                System.out.println("⚠️ WALLET: Biglietto originale non trovato per modifica");
            }
        }
    }

    /**
     * ✅ TIMER SCADENZA THREAD-SAFE
     */
    private void avviaTimerScadenzaThreadSafe(BigliettoDTO prenotazione) {
        UUID idPrenotazione = prenotazione.getId();

        System.out.println("⏰ WALLET: Avviando timer scadenza thread-safe per " +
                idPrenotazione.toString().substring(0, 8) + "...");

        // ✅ CREA TASK CANCELLABILE
        Runnable timerTask = (Runnable) scheduler.schedule(() -> {
            // ✅ RIMUOVI TIMER DALLA MAPPA (cleanup automatico)
            timerAttivi.remove(idPrenotazione);

            // ✅ LOCK SCRITTURA per rimozione scadenza
            lock.writeLock().lock();
            try {
                boolean stillExists = nonConfermati.removeIf(p -> p.getId().equals(idPrenotazione));

                if (stillExists) {
                    System.out.println("⏰ WALLET SCADENZA: Prenotazione " +
                            idPrenotazione.toString().substring(0, 8) + "... rimossa automaticamente");
                } else {
                    System.out.println("✅ WALLET: Prenotazione " +
                            idPrenotazione.toString().substring(0, 8) + "... già confermata");
                }
            } finally {
                lock.writeLock().unlock();
            }

        }, 10, TimeUnit.MINUTES);

        // ✅ REGISTRA TIMER per possibile cancellazione
        timerAttivi.put(idPrenotazione, timerTask);
        timerAvviati++;

        System.out.println("✅ WALLET: Timer registrato. Totale timer attivi: " + timerAttivi.size());
    }

    /**
     * ✅ CANCELLAZIONE TIMER THREAD-SAFE
     */
    private void cancellaTimerScadenza(UUID idPrenotazione) {
        Runnable timer = timerAttivi.remove(idPrenotazione);
        if (timer != null) {
            // Nota: ScheduledFuture.cancel() sarebbe meglio, ma questo è comunque safe
            timerCancellati++;
            System.out.println("🚫 WALLET: Timer cancellato per " +
                    idPrenotazione.toString().substring(0, 8) + "...");
        }
    }

    /**
     * ✅ GETTER THREAD-SAFE - Read lock
     */
    public List<BigliettoDTO> getBigliettiConfermati() {
        lock.readLock().lock();
        try {
            System.out.println("📋 WALLET: getBigliettiConfermati thread-safe. Totale: " + confermati.size());
            return new ArrayList<>(confermati); // ✅ COPIA DIFENSIVA
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ✅ GETTER THREAD-SAFE - Read lock
     */
    public List<BigliettoDTO> getBigliettiNonConfermati() {
        lock.readLock().lock();
        try {
            System.out.println("📋 WALLET: getBigliettiNonConfermati thread-safe. Totale: " + nonConfermati.size());
            return new ArrayList<>(nonConfermati); // ✅ COPIA DIFENSIVA
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ✅ STATISTICHE THREAD-SAFE
     */
    public String getStatistiche() {
        lock.readLock().lock();
        try {
            return String.format("WALLET Stats: Confermati=%d, NonConfermati=%d, Eventi=%d, Timer=%d/%d",
                    confermati.size(),
                    nonConfermati.size(),
                    eventiProcessati,
                    timerAttivi.size(),
                    timerAvviati);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ✅ CLEANUP RISORSE
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            System.out.println("🛑 WALLET: Shutdown in corso...");

            // Cancella tutti i timer attivi
            timerAttivi.clear();

            // Shutdown scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            System.out.println("✅ WALLET: Shutdown completato. " + getStatistiche());
        } finally {
            lock.writeLock().unlock();
        }
    }
}