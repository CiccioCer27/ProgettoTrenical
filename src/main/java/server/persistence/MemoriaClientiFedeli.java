package persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 💎 MEMORIA CLIENTI FEDELI - Updated per consistency
 *
 * PROBLEMA: Usa ObjectMapper diretto invece di PersistenceManager pattern
 * SOLUZIONE: Continua ad usare ObjectMapper (Set<UUID> è semplice)
 * MIGLIORAMENTO: Usa ObjectMapper con stessa configurazione di Base
 */
public class MemoriaClientiFedeli {
    private static final String PATH = "src/main/resources/data/clientiFedeli.json";

    // ✅ MIGLIORAMENTO: Usa ObjectMapper configurato come BasePersistenceManager
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Set<UUID> clientiFedeli = new HashSet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemoriaClientiFedeli() {
        try {
            File file = new File(PATH);
            if (file.exists()) {
                Set<?> loaded = mapper.readValue(file, Set.class);
                for (Object o : loaded) {
                    clientiFedeli.add(UUID.fromString(o.toString()));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Errore caricamento clienti fedeli: " + e.getMessage());
        }
    }

    public boolean isClienteFedele(UUID id) {
        lock.readLock().lock();
        try {
            return clientiFedeli.contains(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void registraClienteFedele(UUID id) {
        lock.writeLock().lock();
        try {
            clientiFedeli.add(id);
            salva();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void salva() {
        try {
            // ✅ MIGLIORAMENTO: Crea directory se non esiste
            File file = new File(PATH);
            file.getParentFile().mkdirs();

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, clientiFedeli);
        } catch (IOException e) {
            System.err.println("❌ Errore salvataggio clienti fedeli: " + e.getMessage());
        }
    }
}
