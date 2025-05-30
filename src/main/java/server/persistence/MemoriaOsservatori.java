package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoriaOsservatori {
    private static final String PATH = "src/main/resources/data/osservatoriTratte.json";
    private static final ObjectMapper mapper = new ObjectMapper();

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
            e.printStackTrace();
        }
    }

    public Set<UUID> getOsservatori(UUID idTratta) {
        lock.readLock().lock();
        try {
            return new HashSet<>(osservatori.getOrDefault(idTratta, Set.of()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void aggiungiOsservatore(UUID idTratta, UUID idCliente) {
        lock.writeLock().lock();
        try {
            osservatori.computeIfAbsent(idTratta, k -> new HashSet<>()).add(idCliente);
            salva();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void salva() {
        try {
            // converto per serializzare come Map<String, List<String>>
            Map<String, List<String>> serializableMap = new HashMap<>();
            for (Map.Entry<UUID, Set<UUID>> entry : osservatori.entrySet()) {
                List<String> lista = new ArrayList<>();
                for (UUID u : entry.getValue()) {
                    lista.add(u.toString());
                }
                serializableMap.put(entry.getKey().toString(), lista);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PATH), serializableMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}