package persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import model.Biglietto;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BigliettiPersistenceManager {
    private static final String PATH = "src/main/resources/data/biglietti.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Biglietto> caricaBiglietti() throws IOException {
        File file = new File(PATH);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    public static void salvaBiglietti(List<Biglietto> biglietti) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PATH), biglietti);
    }
}