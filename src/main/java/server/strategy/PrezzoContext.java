package strategy;

import model.Tratta;
import model.Promozione;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import persistence.MemoriaPromozioni;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 🎯 CONTEXT per gestione strategie di pricing
 *
 * Coordina l'utilizzo delle diverse strategie e seleziona
 * automaticamente la migliore per ogni situazione
 */
public class PrezzoContext {

    private final List<PrezzoStrategy> strategie = new ArrayList<>();
    private final MemoriaPromozioni memoriaPromozioni;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // 📊 Statistiche per monitoring
    private volatile int calcoliTotali = 0;
    private volatile int calcoliConPromozioni = 0;
    private volatile double scontoTotaleConcesso = 0.0;

    public PrezzoContext(MemoriaPromozioni memoriaPromozioni) {
        this.memoriaPromozioni = memoriaPromozioni;
        inizializzaStrategieDefault();
    }

    /**
     * 🏗️ Inizializza le strategie di default
     */
    private void inizializzaStrategieDefault() {
        // Ordine di priorità (dal più basso al più alto)
        aggiungiStrategia(new PrezzoStandardStrategy());
        aggiungiStrategia(new PrezzoDinamicoStrategy());
        aggiungiStrategia(new PrezzoPromozionaleStrategy());

        System.out.println("✅ PrezzoContext inizializzato con " + strategie.size() + " strategie");
    }

    /**
     * ➕ Aggiunge una nuova strategia
     */
    public void aggiungiStrategia(PrezzoStrategy strategia) {
        lock.writeLock().lock();
        try {
            strategie.add(strategia);
            // Ordina per priorità (priorità più alta prima)
            strategie.sort((s1, s2) -> Integer.compare(s2.getPriorita(), s1.getPriorita()));

            System.out.println("➕ Strategia aggiunta: " + strategia.getNome() +
                    " (priorità: " + strategia.getPriorita() + ")");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🗑️ Rimuove una strategia
     */
    public void rimuoviStrategia(String nomeStrategia) {
        lock.writeLock().lock();
        try {
            boolean rimossa = strategie.removeIf(s -> s.getNome().equals(nomeStrategia));
            if (rimossa) {
                System.out.println("🗑️ Strategia rimossa: " + nomeStrategia);
            } else {
                System.out.println("⚠️ Strategia non trovata: " + nomeStrategia);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 💰 METODO PRINCIPALE - Calcola il prezzo ottimale
     */
    public PrezzoCalcolato calcolaPrezzoOttimale(
            Tratta tratta,
            ClasseServizio classeServizio,
            TipoPrezzo tipoPrezzo,
            boolean isClienteFedele,
            UUID idCliente
    ) {
        lock.readLock().lock();
        try {
            calcoliTotali++;

            System.out.println("💰 Calcolo prezzo per tratta: " +
                    tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo() +
                    " | Cliente fedele: " + isClienteFedele +
                    " | Tipo: " + tipoPrezzo);

            // 📋 Ottieni promozioni attive
            List<Promozione> promozioniAttive = memoriaPromozioni.getPromozioniAttive();
            System.out.println("🎉 Promozioni attive: " + promozioniAttive.size());

            PrezzoCalcolato migliorRisultato = null;
            PrezzoStrategy strategiaUsata = null;

            // 🔍 PROVA TUTTE LE STRATEGIE APPLICABILI
            for (PrezzoStrategy strategia : strategie) {
                if (!strategia.isApplicabile(tratta, classeServizio, isClienteFedele)) {
                    System.out.println("⚠️ Strategia " + strategia.getNome() + " non applicabile");
                    continue;
                }

                try {
                    System.out.println("🔄 Testando strategia: " + strategia.getNome());

                    PrezzoCalcolato risultato = strategia.calcolaPrezzoFinale(
                            tratta, classeServizio, tipoPrezzo, isClienteFedele, idCliente, promozioniAttive
                    );

                    System.out.println("📊 Risultato " + strategia.getNome() + ": " + risultato);

                    // ✅ SELEZIONA IL PREZZO MIGLIORE (più basso)
                    if (migliorRisultato == null || risultato.getPrezzoFinale() < migliorRisultato.getPrezzoFinale()) {
                        migliorRisultato = risultato;
                        strategiaUsata = strategia;
                        System.out.println("🏆 Nuovo miglior prezzo: €" +
                                String.format("%.2f", risultato.getPrezzoFinale()) +
                                " (strategia: " + strategia.getNome() + ")");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Errore strategia " + strategia.getNome() + ": " + e.getMessage());
                    // Continua con la prossima strategia
                }
            }

            // ✅ FALLBACK se nessuna strategia funziona
            if (migliorRisultato == null) {
                System.out.println("⚠️ Nessuna strategia applicabile, uso prezzo base");
                double prezzoBase = tratta.getPrezzi().get(classeServizio).getPrezzo(tipoPrezzo);
                migliorRisultato = new PrezzoCalcolato(
                        prezzoBase, prezzoBase, 0.0, "Prezzo base (fallback)", List.of(), tipoPrezzo
                );
                strategiaUsata = new PrezzoStandardStrategy();
            }

            // 📊 AGGIORNA STATISTICHE
            if (migliorRisultato.getScontoApplicato() > 0) {
                calcoliConPromozioni++;
                scontoTotaleConcesso += migliorRisultato.getScontoApplicato();
            }

            System.out.println("🎯 PREZZO FINALE: €" + String.format("%.2f", migliorRisultato.getPrezzoFinale()) +
                    " (strategia: " + (strategiaUsata != null ? strategiaUsata.getNome() : "fallback") + ")");

            return migliorRisultato;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 🔍 Anteprima prezzi per tutte le strategie (per debugging)
     */
    public List<PrezzoCalcolato> anteprimaPrezzi(
            Tratta tratta,
            ClasseServizio classeServizio,
            TipoPrezzo tipoPrezzo,
            boolean isClienteFedele,
            UUID idCliente
    ) {
        lock.readLock().lock();
        try {
            List<PrezzoCalcolato> risultati = new ArrayList<>();
            List<Promozione> promozioniAttive = memoriaPromozioni.getPromozioniAttive();

            for (PrezzoStrategy strategia : strategie) {
                if (strategia.isApplicabile(tratta, classeServizio, isClienteFedele)) {
                    try {
                        PrezzoCalcolato risultato = strategia.calcolaPrezzoFinale(
                                tratta, classeServizio, tipoPrezzo, isClienteFedele, idCliente, promozioniAttive
                        );
                        risultati.add(risultato);
                    } catch (Exception e) {
                        System.err.println("❌ Errore anteprima strategia " + strategia.getNome() + ": " + e.getMessage());
                    }
                }
            }

            return risultati;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 Statistiche del context
     */
    public String getStatistiche() {
        lock.readLock().lock();
        try {
            double percentualeConPromozioni = calcoliTotali > 0 ?
                    (calcoliConPromozioni * 100.0) / calcoliTotali : 0;

            return String.format(
                    "PrezzoContext: %d calcoli (%d con promozioni = %.1f%%) | Sconto totale: €%.2f",
                    calcoliTotali, calcoliConPromozioni, percentualeConPromozioni, scontoTotaleConcesso
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📋 Lista delle strategie configurate
     */
    public List<String> getStrategieAttive() {
        lock.readLock().lock();
        try {
            return strategie.stream()
                    .map(s -> s.getNome() + " (priorità: " + s.getPriorita() + ")")
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 🔧 Reset statistiche
     */
    public void resetStatistiche() {
        lock.writeLock().lock();
        try {
            calcoliTotali = 0;
            calcoliConPromozioni = 0;
            scontoTotaleConcesso = 0.0;
            System.out.println("🔧 Statistiche PrezzoContext resettate");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 📊 Statistiche dettagliate
     */
    public void stampaStatisticheDettagliate() {
        lock.readLock().lock();
        try {
            System.out.println("\n💰 STATISTICHE PREZZO CONTEXT:");
            System.out.println("   🔢 Calcoli totali: " + calcoliTotali);
            System.out.println("   🎉 Calcoli con promozioni: " + calcoliConPromozioni);
            System.out.println("   💸 Sconto totale concesso: €" + String.format("%.2f", scontoTotaleConcesso));

            if (calcoliTotali > 0) {
                double percentualePromozioni = (calcoliConPromozioni * 100.0) / calcoliTotali;
                double scontoMedio = scontoTotaleConcesso / calcoliConPromozioni;

                System.out.println("   📈 Percentuale con promozioni: " + String.format("%.1f%%", percentualePromozioni));
                if (calcoliConPromozioni > 0) {
                    System.out.println("   💰 Sconto medio: €" + String.format("%.2f", scontoMedio));
                }
            }

            System.out.println("\n🎯 STRATEGIE ATTIVE:");
            for (int i = 0; i < strategie.size(); i++) {
                PrezzoStrategy strategia = strategie.get(i);
                System.out.println("   " + (i + 1) + ") " + strategia.getNome() +
                        " (priorità: " + strategia.getPriorita() + ")");
            }

            System.out.println("\n🎉 PROMOZIONI ATTIVE: " + memoriaPromozioni.getPromozioniAttive().size());

        } finally {
            lock.readLock().unlock();
        }
    }
}