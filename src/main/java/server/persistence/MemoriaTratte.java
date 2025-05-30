package persistence;

import model.Tratta;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoriaTratte {
    private final List<Tratta> tratte = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemoriaTratte() {
        try {
            tratte.addAll(TrattaPersistenceManager.caricaTratte());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Tratta> getTutteTratte() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tratte);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void aggiungiTratta(Tratta tratta) {
        lock.writeLock().lock();
        try {
            tratte.add(tratta);
            salva();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void rimuoviTratteDelGiorno(LocalDate data) {
        lock.writeLock().lock();
        try {
            tratte.removeIf(t -> t.getData().equals(data));
            salva();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Tratta getTrattaById(UUID id) {
        lock.readLock().lock();
        try {
            return tratte.stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void salva() {
        try {
            TrattaPersistenceManager.salvaTratte(tratte);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}