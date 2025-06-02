package persistence;

import model.Biglietto;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 🔍 MEMORIA BIGLIETTI - VERSIONE DETECTIVE
 *
 * Con logging ultra-dettagliato per trovare il bug di overselling
 */
public class MemoriaBiglietti {
    private final List<Biglietto> biglietti = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Statistiche per debugging
    private int tentativiTotali = 0;
    private int tentativiAccettati = 0;
    private int tentativiRifiutati = 0;

    // 🔍 DETECTIVE MODE: Traccia ogni inserimento
    private int inserimentiTotali = 0;
    private final Map<String, Integer> inserimentiPerMetodo = new HashMap<>();

    public MemoriaBiglietti() {
        try {
            biglietti.addAll(BigliettiPersistenceManager.caricaBiglietti());
            System.out.println("💾 MemoriaBiglietti DETECTIVE: Caricati " + biglietti.size() + " biglietti");
        } catch (IOException e) {
            System.err.println("❌ Errore caricamento biglietti: " + e.getMessage());
        }
    }

    /**
     * 🔒 METODO ATOMICO (dovrebbe essere l'UNICO punto di inserimento)
     */
    public boolean aggiungiSeSpazioDiponibile(Biglietto biglietto, int capienzaMassima) {
        lock.writeLock().lock();
        try {
            tentativiTotali++;
            String logPrefix = "[ATOMICO #" + tentativiTotali + "]";

            // Conta biglietti esistenti per questa tratta
            long bigliettiEsistenti = biglietti.stream()
                    .filter(b -> b.getIdTratta().equals(biglietto.getIdTratta()))
                    .count();

            String trattaId = biglietto.getIdTratta().toString().substring(0, 8);
            String bigliettoId = biglietto.getId().toString().substring(0, 8);

            System.out.println("🔍 " + logPrefix + " CONTROLLO ATOMICO:");
            System.out.println("   Thread: " + Thread.currentThread().getName());
            System.out.println("   Tratta: " + trattaId + " | Biglietto: " + bigliettoId);
            System.out.println("   Esistenti: " + bigliettiEsistenti + "/" + capienzaMassima);

            // Decisione atomica
            if (bigliettiEsistenti < capienzaMassima) {
                biglietti.add(biglietto);
                salvaInterno();

                // 🔍 DETECTIVE: Traccia inserimento
                inserimentiTotali++;
                inserimentiPerMetodo.merge("aggiungiSeSpazioDiponibile", 1, Integer::sum);

                tentativiAccettati++;
                System.out.println("✅ " + logPrefix + " ACCETTATO: posto " + (bigliettiEsistenti + 1) + "/" + capienzaMassima);
                System.out.println("   🔍 DETECTIVE: Inserimento #" + inserimentiTotali + " via ATOMICO");

                return true;
            } else {
                tentativiRifiutati++;
                System.out.println("❌ " + logPrefix + " RIFIUTATO: treno pieno");
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🔄 CONFERMA PRENOTAZIONE ATOMICA
     */
    public boolean confermaPrenotazione(Biglietto prenotazione) {
        lock.writeLock().lock();
        try {
            System.out.println("🔍 DETECTIVE CONFERMA: Inizio operazione");
            System.out.println("   Thread: " + Thread.currentThread().getName());
            System.out.println("   Prenotazione ID: " + prenotazione.getId().toString().substring(0, 8));

            // Trova e rimuovi la prenotazione
            int sizePreRimozione = biglietti.size();
            boolean prenotazioneTrovata = biglietti.removeIf(b ->
                    b.getId().equals(prenotazione.getId()) &&
                            "prenotazione".equals(b.getTipoAcquisto()));
            int sizePostRimozione = biglietti.size();

            System.out.println("   🔍 Rimozione: " + sizePreRimozione + " → " + sizePostRimozione +
                    " (trovata: " + prenotazioneTrovata + ")");

            if (!prenotazioneTrovata) {
                System.out.println("❌ CONFERMA: Prenotazione non trovata " +
                        prenotazione.getId().toString().substring(0, 8));
                return false;
            }

            // Crea biglietto confermato con stesso ID
            Biglietto confermato = new Biglietto(
                    prenotazione.getId(), // STESSO ID
                    prenotazione.getIdCliente(),
                    prenotazione.getIdTratta(),
                    prenotazione.getClasse(),
                    prenotazione.isConCartaFedelta(),
                    prenotazione.getPrezzoPagato(),
                    java.time.LocalDate.now(),
                    "acquisto" // Tipo cambia
            );

            int sizePreAggiunta = biglietti.size();
            biglietti.add(confermato);
            int sizePostAggiunta = biglietti.size();
            salvaInterno();

            // 🔍 DETECTIVE: Traccia inserimento
            inserimentiTotali++;
            inserimentiPerMetodo.merge("confermaPrenotazione", 1, Integer::sum);

            System.out.println("   🔍 Aggiunta: " + sizePreAggiunta + " → " + sizePostAggiunta);
            System.out.println("✅ CONFERMA ATOMICA: " + prenotazione.getId().toString().substring(0, 8));
            System.out.println("   🔍 DETECTIVE: Inserimento #" + inserimentiTotali + " via CONFERMA");

            return true;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🔄 MODIFICA BIGLIETTO ATOMICA
     */
    public boolean modificaBigliettoAtomico(UUID idVecchio, Biglietto nuovo, int capienzaMassima) {
        lock.writeLock().lock();
        try {
            System.out.println("🔍 DETECTIVE MODIFICA: Inizio operazione");
            System.out.println("   Thread: " + Thread.currentThread().getName());
            System.out.println("   ID Vecchio: " + idVecchio.toString().substring(0, 8));
            System.out.println("   ID Nuovo: " + nuovo.getId().toString().substring(0, 8));

            // Trova biglietto originale
            Biglietto originale = biglietti.stream()
                    .filter(b -> b.getId().equals(idVecchio))
                    .findFirst()
                    .orElse(null);

            if (originale == null) {
                System.out.println("❌ MODIFICA: Biglietto originale non trovato");
                return false;
            }

            // Controlla capienza per la nuova tratta
            long bigliettiNuovaTratta = biglietti.stream()
                    .filter(b -> b.getIdTratta().equals(nuovo.getIdTratta()))
                    .filter(b -> !b.getId().equals(idVecchio)) // Escludi quello che modifichiamo
                    .count();

            System.out.println("   🔍 Capienza nuova tratta: " + bigliettiNuovaTratta + "/" + capienzaMassima);

            if (bigliettiNuovaTratta >= capienzaMassima) {
                System.out.println("❌ MODIFICA: Nuova tratta piena");
                return false;
            }

            // Operazione atomica: rimuovi vecchio + aggiungi nuovo
            int sizePreRimozione = biglietti.size();
            biglietti.removeIf(b -> b.getId().equals(idVecchio));
            int sizePostRimozione = biglietti.size();

            biglietti.add(nuovo);
            int sizePostAggiunta = biglietti.size();
            salvaInterno();

            // 🔍 DETECTIVE: Traccia inserimento
            inserimentiTotali++;
            inserimentiPerMetodo.merge("modificaBigliettoAtomico", 1, Integer::sum);

            System.out.println("   🔍 Rimozione: " + sizePreRimozione + " → " + sizePostRimozione);
            System.out.println("   🔍 Aggiunta: " + sizePostRimozione + " → " + sizePostAggiunta);
            System.out.println("✅ MODIFICA ATOMICA: " + idVecchio.toString().substring(0, 8) +
                    " → " + nuovo.getId().toString().substring(0, 8));
            System.out.println("   🔍 DETECTIVE: Inserimento #" + inserimentiTotali + " via MODIFICA");

            return true;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ⚠️ METODO DEPRECATO - DEVE ESSERE TRACCIATO!
     */
    @Deprecated
    public void aggiungiBiglietto(Biglietto b) {
        lock.writeLock().lock();
        try {
            // 🚨 ALLARME: Questo metodo NON dovrebbe essere usato!
            System.out.println("🚨 DETECTIVE ALLARME: aggiungiBiglietto() DEPRECATO CHIAMATO!");
            System.out.println("   Thread: " + Thread.currentThread().getName());
            System.out.println("   Biglietto: " + b.getId().toString().substring(0, 8));
            System.out.println("   Tratta: " + b.getIdTratta().toString().substring(0, 8));

            // Stack trace per trovare il colpevole
            System.out.println("   🔍 STACK TRACE:");
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 1; i < Math.min(stack.length, 6); i++) {
                System.out.println("      " + i + ") " + stack[i]);
            }

            int sizePreAggiunta = biglietti.size();
            biglietti.add(b);
            int sizePostAggiunta = biglietti.size();
            salvaInterno();

            // 🔍 DETECTIVE: Traccia inserimento ILLEGALE
            inserimentiTotali++;
            inserimentiPerMetodo.merge("aggiungiBiglietto_DEPRECATO", 1, Integer::sum);

            System.out.println("   🔍 Aggiunta DEPRECATA: " + sizePreAggiunta + " → " + sizePostAggiunta);
            System.out.println("🚨 DETECTIVE: Inserimento ILLEGALE #" + inserimentiTotali + " via DEPRECATO");

        } finally {
            lock.writeLock().unlock();
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
     * 🗑️ Rimuovi biglietto (per rollback pagamenti)
     */
    public boolean rimuoviBiglietto(UUID idBiglietto) {
        lock.writeLock().lock();
        try {
            System.out.println("🗑️ DETECTIVE RIMOZIONE: " + idBiglietto.toString().substring(0, 8));
            System.out.println("   Thread: " + Thread.currentThread().getName());

            int sizePreRimozione = biglietti.size();
            boolean rimosso = biglietti.removeIf(b -> b.getId().equals(idBiglietto));
            int sizePostRimozione = biglietti.size();

            if (rimosso) {
                salvaInterno();
                System.out.println("   🔍 Rimozione: " + sizePreRimozione + " → " + sizePostRimozione);
                System.out.println("✅ DETECTIVE: Rimosso con successo");
            } else {
                System.out.println("❌ DETECTIVE: Biglietto non trovato per rimozione");
            }

            return rimosso;
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
     * ✅ Verifica integrità capienza per tutte le tratte
     */
    public boolean verificaIntegrita(Map<UUID, Integer> capienzaPerTratta) {
        lock.readLock().lock();
        try {
            boolean integrita = true;

            for (Map.Entry<UUID, Integer> entry : capienzaPerTratta.entrySet()) {
                UUID idTratta = entry.getKey();
                int capienzaMassima = entry.getValue();

                long bigliettiVenduti = biglietti.stream()
                        .filter(b -> b.getIdTratta().equals(idTratta))
                        .count();

                if (bigliettiVenduti > capienzaMassima) {
                    System.out.println("❌ VIOLAZIONE CAPIENZA:");
                    System.out.println("   Tratta: " + idTratta.toString().substring(0, 8));
                    System.out.println("   Venduti: " + bigliettiVenduti + "/" + capienzaMassima);
                    integrita = false;
                }
            }

            return integrita;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 Statistiche dettagliate per debugging
     */
    public void stampaStatisticheDettagliate() {
        lock.readLock().lock();
        try {
            System.out.println("\n📊 STATISTICHE DETECTIVE:");
            System.out.println("   🎫 Biglietti totali: " + biglietti.size());
            System.out.println("   📈 Tentativi atomici: " + tentativiTotali);
            System.out.println("   ✅ Accettati atomici: " + tentativiAccettati);
            System.out.println("   ❌ Rifiutati atomici: " + tentativiRifiutati);
            System.out.println("   🔍 Inserimenti totali: " + inserimentiTotali);

            System.out.println("\n🕵️ INSERIMENTI PER METODO:");
            inserimentiPerMetodo.forEach((metodo, count) ->
                    System.out.println("   " + metodo + ": " + count + " inserimenti"));

            if (tentativiTotali > 0) {
                double percentualeSuccesso = (tentativiAccettati * 100.0) / tentativiTotali;
                System.out.println("   📈 Tasso successo atomico: " + String.format("%.1f%%", percentualeSuccesso));
            }

            // Raggruppa per tratta con ID completi
            System.out.println("\n🚂 BIGLIETTI PER TRATTA (DETECTIVE):");
            Map<UUID, List<Biglietto>> bigliettiPerTratta = biglietti.stream()
                    .collect(java.util.stream.Collectors.groupingBy(b -> b.getIdTratta()));

            bigliettiPerTratta.forEach((tratta, bigliettiTratta) -> {
                System.out.println("   Tratta " + tratta.toString().substring(0, 8) + ":");
                System.out.println("      Biglietti: " + bigliettiTratta.size());
                bigliettiTratta.forEach(b ->
                        System.out.println("         - " + b.getId().toString().substring(0, 8) +
                                " (" + b.getTipoAcquisto() + ")"));
            });

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 Statistiche rapide per monitoring
     */
    public String getStatistiche() {
        lock.readLock().lock();
        try {
            return String.format("Biglietti: %d | Atomici: %d (✅%d ❌%d) | Inserimenti: %d",
                    biglietti.size(), tentativiTotali, tentativiAccettati, tentativiRifiutati, inserimentiTotali);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 💾 Salvataggio interno (chiamato sotto lock)
     */
    private void salvaInterno() {
        try {
            BigliettiPersistenceManager.salvaBiglietti(biglietti);
        } catch (IOException e) {
            System.err.println("❌ Errore salvataggio biglietti: " + e.getMessage());
        }
    }

    /**
     * 💾 Salvataggio pubblico (con lock proprio)
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