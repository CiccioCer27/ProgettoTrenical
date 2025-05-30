package persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Biglietto;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BigliettiPersistenceManager {
    private static final String PATH = "src/main/resources/data/biglietti.json";

    // ✅ ObjectMapper configurato con JavaTimeModule
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static List<Biglietto> caricaBiglietti() throws IOException {
        File file = new File(PATH);
        if (!file.exists() || file.length() == 0) {
            // Crea file vuoto se non esiste
            file.getParentFile().mkdirs();
            return new ArrayList<>();
        }
        return mapper.readValue(file, new TypeReference<>() {});
    }

    public static void salvaBiglietti(List<Biglietto> biglietti) throws IOException {
        File file = new File(PATH);
        file.getParentFile().mkdirs(); // Assicura che la directory esista
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, biglietti);
    }
}