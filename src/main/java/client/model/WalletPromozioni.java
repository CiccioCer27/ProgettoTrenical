package model;

import dto.PromozioneDTO;
import eventi.Evento;
import eventi.EventoPromozione;
import observer.Observer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 🎉 WALLET PROMOZIONI THREAD-SAFE
 */
public class WalletPromozioni implements Observer {

    // ✅ THREAD-SAFE COLLECTIONS
    private final List<PromozioneDTO> promozioniAttive = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentMap<String, Runnable> timerPromozioni = new ConcurrentHashMap<>();

    // ✅ LOCK per operazioni atomiche
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void aggiorna(Evento evento) {
        if (evento instanceof EventoPromozione ep) {
            lock.writeLock().lock();
            try {
                PromozioneDTO promozione = ep.getPromozione();

                // ✅ AGGIUNGI THREAD-SAFE
                promozioniAttive.add(promozione);
                System.out.println("📢 PROMOZIONI: Aggiunta thread-safe: " + promozione);

                // ✅ TIMER SCADENZA THREAD-SAFE
                long secondiAllaFine = Duration.between(LocalDateTime.now(), promozione.getDataFine()).getSeconds();

                if (secondiAllaFine > 0) {
                    String keyPromo = promozione.getNome() + "_" + promozione.getDataInizio();

                    Runnable taskScadenza = (Runnable) scheduler.schedule(() -> {
                        rimuoviPromozioneScaduta(promozione);
                        timerPromozioni.remove(keyPromo);
                    }, secondiAllaFine, TimeUnit.SECONDS);

                    timerPromozioni.put(keyPromo, taskScadenza);
                } else {
                    rimuoviPromozioneScaduta(promozione); // già scaduta
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * ✅ RIMOZIONE THREAD-SAFE
     */
    private void rimuoviPromozioneScaduta(PromozioneDTO promozione) {
        lock.writeLock().lock();
        try {
            promozioniAttive.removeIf(p -> p.equals(promozione));
            System.out.println("⏳ PROMOZIONI: Rimossa scaduta thread-safe: " + promozione.getNome());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ✅ GETTER THREAD-SAFE
     */
    public List<PromozioneDTO> getPromozioniAttive() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(promozioniAttive); // ✅ COPIA DIFENSIVA
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ✅ CLEANUP THREAD-SAFE
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            timerPromozioni.clear();
            scheduler.shutdown();

            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
