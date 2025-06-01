package persistence;

import model.Biglietto;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 🔒 MEMORIA BIGLIETTI THREAD-SAFE
 *
 * Versione corretta che previene l'overselling attraverso:
 * - Controlli atomici di capienza
 * - Sincronizzazione completa
 * - Lock per operazioni critiche
 */
public class MemoriaBiglietti {
    private final List<Biglietto> biglietti = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemoriaBiglietti() {
        try {
            biglietti.addAll(BigliettiPersistenceManager.caricaBiglietti());
            System.out.println("💾 MemoriaBiglietti: Caricati " + biglietti.size() + " biglietti da file");
        } catch (IOException e) {
            System.err.println("❌ Errore caricamento biglietti: " + e.getMessage());
        }
    }

    /**
     * 📋 Ottieni tutti i biglietti (thread-safe)
     */
    public List<Biglietto> getTuttiIBiglietti() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(biglietti);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 🔒 METODO CRITICO: Aggiungi biglietto CON controllo capienza atomico
     *
     * Questo è il cuore della soluzione al problema di overselling.
     * Il controllo capienza + aggiunta avvengono atomicamente.
     */
    public boolean aggiungiSeSpazioDiponibile(Biglietto biglietto, int capienzaMassima) {
        lock.writeLock().lock();
        try {
            // 1️⃣ Conta biglietti esistenti per questa tratta
            long bigliettiEsistenti = biglietti.stream()
                    .filter(b -> b.getIdTratta().equals(biglietto.getIdTratta()))
                    .count();

            String trattaId = biglietto.getIdTratta().toString().substring(0, 8);
            System.out.println("🔍 CONTROLLO ATOMICO: Tratta " + trattaId +
                    " - Esistenti: " + bigliettiEsistenti + "/" + capienzaMassima);

            // 2️⃣ Se c'è spazio, aggiungi IMMEDIATAMENTE
            if (bigliettiEsistenti < capienzaMassima) {
                biglietti.add(biglietto);
                salvaInterno(); // Salva senza prendere lock aggiuntivi

                System.out.println("✅ BIGLIETTO ACCETTATO: Posto " + (bigliettiEsistenti + 1) + "/" + capienzaMassima +
                        " | ID: " + biglietto.getId().toString().substring(0, 8));
                return true;
            } else {
                System.out.println("❌ BIGLIETTO RIFIUTATO: Treno pieno (" + bigliettiEsistenti + "/" + capienzaMassima + ")" +
                        " | ID tentativo: " + biglietto.getId().toString().substring(0, 8));
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ➕ Aggiungi biglietto senza controllo capienza (da usare con cautela)
     */
    public void aggiungiBiglietto(Biglietto b) {
        lock.writeLock().lock();
        try {
            biglietti.add(b);
            salvaInterno();
            System.out.println("⚠️ BIGLIETTO AGGIUNTO SENZA CONTROLLO CAPIENZA: " +
                    b.getId().toString().substring(0, 8));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🗑️ Rimuovi biglietto
     */
    public void rimuoviBiglietto(UUID idBiglietto) {
        lock.writeLock().lock();
        try {
            boolean rimosso = biglietti.removeIf(b -> b.getId().equals(idBiglietto));
            if (rimosso) {
                salvaInterno();
                System.out.println("🗑️ BIGLIETTO RIMOSSO: " + idBiglietto.toString().substring(0, 8));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🔍 Trova biglietto per ID
     */
    public Biglietto getById(UUID idBiglietto) {
        lock.readLock().lock();
        try {
            return biglietti.stream()
                    .filter(b -> b.getId().equals(idBiglietto))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 Conta biglietti per tratta (thread-safe)
     */
    public long contaBigliettiPerTratta(UUID idTratta) {
        lock.readLock().lock();
        try {
            return biglietti.stream()
                    .filter(b -> b.getIdTratta().equals(idTratta))
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 🔍 DEBUG: Ottieni statistiche dettagliate per tratta
     */
    public void stampaStatisticheTratta(UUID idTratta) {
        lock.readLock().lock();
        try {
            List<Biglietto> bigliettiTratta = biglietti.stream()
                    .filter(b -> b.getIdTratta().equals(idTratta))
                    .toList();

            System.out.println("📊 STATISTICHE TRATTA " + idTratta.toString().substring(0, 8) + ":");
            System.out.println("   Biglietti totali: " + bigliettiTratta.size());

            Map<String, Long> perTipo = bigliettiTratta.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            b -> b.getTipoAcquisto(),
                            java.util.stream.Collectors.counting()));

            perTipo.forEach((tipo, count) ->
                    System.out.println("   " + tipo + ": " + count));

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 💾 Salvataggio interno (da chiamare già dentro un lock)
     */
    private void salvaInterno() {
        try {
            BigliettiPersistenceManager.salvaBiglietti(biglietti);
        } catch (IOException e) {
            System.err.println("❌ Errore salvataggio biglietti: " + e.getMessage());
        }
    }

    /**
     * 💾 Salvataggio pubblico (con lock)
     */
    public void salva() {
        lock.readLock().lock();
        try {
            salvaInterno();
        } finally {
            lock.readLock().unlock();
        }
    }
}