package eventi;

import observer.Observer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 🔔 LISTA EVENTI THREAD-SAFE MIGLIORATA
 */
public class ListaEventi {
    private static final ListaEventi instance = new ListaEventi();

    // ✅ THREAD-SAFE LIST per observers
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    // ✅ LOCK per operazioni atomiche complesse
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile int eventiNotificati = 0;

    private ListaEventi() {}

    public static ListaEventi getInstance() {
        return instance;
    }

    /**
     * ✅ AGGIUNTA THREAD-SAFE
     */
    public void aggiungiObserver(Observer o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
            System.out.println("✅ EVENTI: Observer aggiunto thread-safe. Totale: " + observers.size());
        }
    }

    /**
     * ✅ RIMOZIONE THREAD-SAFE
     */
    public void rimuoviObserver(Observer o) {
        if (observers.remove(o)) {
            System.out.println("🗑️ EVENTI: Observer rimosso thread-safe. Totale: " + observers.size());
        }
    }

    /**
     * ✅ NOTIFICA THREAD-SAFE con error handling
     */
    public void notifica(Evento evento) {
        lock.readLock().lock();
        try {
            eventiNotificati++;
            System.out.println("🔔 EVENTI: Notifica thread-safe #" + eventiNotificati +
                    " a " + observers.size() + " observers");
            System.out.println("   Evento: " + evento.getClass().getSimpleName());
            System.out.println("   Thread: " + Thread.currentThread().getName());

            // ✅ NOTIFICA CON ERROR HANDLING
            for (Observer o : observers) {
                try {
                    o.aggiorna(evento);
                } catch (Exception e) {
                    System.err.println("❌ EVENTI: Errore notifica observer " +
                            o.getClass().getSimpleName() + ": " + e.getMessage());
                    // Continua con altri observers invece di fallire tutto
                }
            }

            System.out.println("✅ EVENTI: Notifica thread-safe completata");
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
            return String.format("EVENTI Stats: Observers=%d, EventiNotificati=%d",
                    observers.size(), eventiNotificati);
        } finally {
            lock.readLock().unlock();
        }
    }
}