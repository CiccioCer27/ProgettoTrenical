package persistence;

import model.Promozione;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoriaPromozioni {
    private final List<Promozione> promozioni = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemoriaPromozioni() {
        try {
            promozioni.addAll(PromozionePersistenceManager.caricaPromozioni());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Promozione> getPromozioniAttive() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(promozioni);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void aggiungiPromozione(Promozione p) {
        lock.writeLock().lock();
        try {
            promozioni.add(p);
            salva();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void salva() {
        try {
            PromozionePersistenceManager.salvaPromozioni(promozioni);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}