package persistence;

import model.Biglietto;

import java.io.IOException;
import java.util.*;

public class MemoriaBiglietti {
    private final List<Biglietto> biglietti = Collections.synchronizedList(new ArrayList<>());

    public MemoriaBiglietti() {
        try {
            biglietti.addAll(BigliettiPersistenceManager.caricaBiglietti());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Biglietto> getTuttiIBiglietti() {
        return new ArrayList<>(biglietti);
    }

    public synchronized void aggiungiBiglietto(Biglietto b) {
        biglietti.add(b);
        salva();
    }

    public synchronized void rimuoviBiglietto(UUID idBiglietto) {
        biglietti.removeIf(b -> b.getId().equals(idBiglietto));
        salva();
    }

    public synchronized Biglietto getById(UUID idBiglietto) {
        return biglietti.stream()
                .filter(b -> b.getId().equals(idBiglietto))
                .findFirst()
                .orElse(null);
    }

    private synchronized void salva() {
        try {
            BigliettiPersistenceManager.salvaBiglietti(biglietti);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}