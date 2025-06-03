package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 👁️ MEMORIA OSSERVATORI - COMPLETA con rimozione
 *
 * AGGIORNAMENTO: Aggiunto supporto per rimozione osservatori
 * MIGLIORAMENTO: Metodi per gestione completa iscrizioni notifiche
 */
public class MemoriaOsservatori {
    private static final String PATH = "src/main/resources/data/osservatoriTratte.json";

    // ✅ ObjectMapper configurato come BasePersistenceManager
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<UUID, Set<UUID>> osservatori = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemoriaOsservatori() {
        try {
            File file = new File(PATH);
            if (file.exists()) {
                Map<String, List<String>> raw = mapper.readValue(file, new TypeReference<>() {});
                raw.forEach((key, valueList) -> {
                    UUID trattaId = UUID.fromString(key);
                    Set<UUID> utenti = new HashSet<>();
                    for (String s : valueList) {
                        utenti.add(UUID.fromString(s));
                    }
                    osservatori.put(trattaId, utenti);
                });
            }
        } catch (IOException e) {
            System.err.println("❌ Errore caricamento osservatori: " + e.getMessage());
        }
    }

    /**
     * 📋 Ottieni tutti gli osservatori di una tratta
     */
    public Set<UUID> getOsservatori(UUID idTratta) {
        lock.readLock().lock();
        try {
            return new HashSet<>(osservatori.getOrDefault(idTratta, Set.of()));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ➕ Aggiungi osservatore a una tratta
     */
    public void aggiungiOsservatore(UUID idTratta, UUID idCliente) {
        lock.writeLock().lock();
        try {
            osservatori.computeIfAbsent(idTratta, k -> new HashSet<>()).add(idCliente);
            salva();
            System.out.println("📡 ✅ Cliente " + idCliente.toString().substring(0, 8) +
                    "... iscritto a notifiche tratta " + idTratta.toString().substring(0, 8) + "...");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ➖ ✅ NUOVO: Rimuovi osservatore da una tratta
     */
    public boolean rimuoviOsservatore(UUID idTratta, UUID idCliente) {
        lock.writeLock().lock();
        try {
            Set<UUID> osservatoriTratta = osservatori.get(idTratta);
            if (osservatoriTratta == null) {
                System.out.println("⚠️ Nessun osservatore per tratta " + idTratta.toString().substring(0, 8) + "...");
                return false;
            }

            boolean rimosso = osservatoriTratta.remove(idCliente);

            if (rimosso) {
                // Se il set diventa vuoto, rimuovi la tratta dalla mappa
                if (osservatoriTratta.isEmpty()) {
                    osservatori.remove(idTratta);
                }
                salva();
                System.out.println("🗑️ ✅ Cliente " + idCliente.toString().substring(0, 8) +
                        "... rimosso da notifiche tratta " + idTratta.toString().substring(0, 8) + "...");
            } else {
                System.out.println("⚠️ Cliente " + idCliente.toString().substring(0, 8) +
                        "... non era iscritto a tratta " + idTratta.toString().substring(0, 8) + "...");
            }

            return rimosso;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🗑️ ✅ NUOVO: Rimuovi cliente da TUTTE le tratte (quando cliente si cancella)
     */
    public int rimuoviOsservatoreDaTutteLeTratte(UUID idCliente) {
        lock.writeLock().lock();
        try {
            int rimozioni = 0;
            Set<UUID> tratteVuote = new HashSet<>();

            for (Map.Entry<UUID, Set<UUID>> entry : osservatori.entrySet()) {
                UUID trattaId = entry.getKey();
                Set<UUID> osservatoriTratta = entry.getValue();

                if (osservatoriTratta.remove(idCliente)) {
                    rimozioni++;
                    System.out.println("🗑️ Cliente rimosso da tratta " + trattaId.toString().substring(0, 8) + "...");

                    // Segna tratta come vuota se non ha più osservatori
                    if (osservatoriTratta.isEmpty()) {
                        tratteVuote.add(trattaId);
                    }
                }
            }

            // Rimuovi tratte senza osservatori
            for (UUID trattaVuota : tratteVuote) {
                osservatori.remove(trattaVuota);
            }

            if (rimozioni > 0) {
                salva();
                System.out.println("🗑️ ✅ Cliente " + idCliente.toString().substring(0, 8) +
                        "... rimosso da " + rimozioni + " tratte");
            }

            return rimozioni;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 🔄 ✅ NUOVO: Sposta osservatore da una tratta a un'altra (per modifiche biglietto)
     */
    public void spostaOsservatore(UUID idCliente, UUID vecchiaTratta, UUID nuovaTratta) {
        lock.writeLock().lock();
        try {
            boolean rimossoDaVecchia = false;

            // Rimuovi dalla vecchia tratta se esiste
            Set<UUID> osservatoriVecchia = osservatori.get(vecchiaTratta);
            if (osservatoriVecchia != null && osservatoriVecchia.remove(idCliente)) {
                rimossoDaVecchia = true;

                // Se set diventa vuoto, rimuovi la tratta
                if (osservatoriVecchia.isEmpty()) {
                    osservatori.remove(vecchiaTratta);
                }
            }

            // Aggiungi alla nuova tratta
            osservatori.computeIfAbsent(nuovaTratta, k -> new HashSet<>()).add(idCliente);

            salva();

            System.out.println("🔄 ✅ SWITCH NOTIFICHE: Cliente " + idCliente.toString().substring(0, 8) + "...");
            System.out.println("   🗑️ Rimosso da: " + vecchiaTratta.toString().substring(0, 8) + "... (" + rimossoDaVecchia + ")");
            System.out.println("   ➕ Aggiunto a: " + nuovaTratta.toString().substring(0, 8) + "...");

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 📊 ✅ NUOVO: Verifica se cliente è iscritto a una tratta
     */
    public boolean isIscrittoATratta(UUID idTratta, UUID idCliente) {
        lock.readLock().lock();
        try {
            Set<UUID> osservatoriTratta = osservatori.get(idTratta);
            return osservatoriTratta != null && osservatoriTratta.contains(idCliente);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 ✅ NUOVO: Ottieni tutte le tratte a cui un cliente è iscritto
     */
    public Set<UUID> getTratteDelCliente(UUID idCliente) {
        lock.readLock().lock();
        try {
            Set<UUID> tratteCliente = new HashSet<>();

            for (Map.Entry<UUID, Set<UUID>> entry : osservatori.entrySet()) {
                if (entry.getValue().contains(idCliente)) {
                    tratteCliente.add(entry.getKey());
                }
            }

            return tratteCliente;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 📊 ✅ NUOVO: Statistiche per debugging
     */
    public void stampaStatistiche() {
        lock.readLock().lock();
        try {
            System.out.println("\n📊 STATISTICHE OSSERVATORI:");
            System.out.println("   🚂 Tratte monitorate: " + osservatori.size());

            int totalOsservatori = osservatori.values().stream()
                    .mapToInt(Set::size)
                    .sum();
            System.out.println("   👥 Osservatori totali: " + totalOsservatori);

            if (!osservatori.isEmpty()) {
                System.out.println("   📋 DETTAGLIO PER TRATTA:");
                osservatori.forEach((tratta, clienti) -> {
                    System.out.println("      Tratta " + tratta.toString().substring(0, 8) +
                            "...: " + clienti.size() + " osservatori");
                });
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 💾 Salvataggio interno (chiamato sotto lock)
     */
    private void salva() {
        try {
            // Crea directory se non esiste
            File file = new File(PATH);
            file.getParentFile().mkdirs();

            // Converte per serializzazione come Map<String, List<String>>
            Map<String, List<String>> serializableMap = new HashMap<>();
            for (Map.Entry<UUID, Set<UUID>> entry : osservatori.entrySet()) {
                List<String> lista = new ArrayList<>();
                for (UUID u : entry.getValue()) {
                    lista.add(u.toString());
                }
                serializableMap.put(entry.getKey().toString(), lista);
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, serializableMap);
        } catch (IOException e) {
            System.err.println("❌ Errore salvataggio osservatori: " + e.getMessage());
        }
    }
}