package persistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoriaClientiFedeli {
    private static final String PATH = "src/main/resources/data/clientiFedeli.json";
    private static final ObjectMapper mapper = new ObjectMapper();

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
            e.printStackTrace();
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
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PATH), clientiFedeli);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}