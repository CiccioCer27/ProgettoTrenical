package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Tratta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrattaPersistenceManager {
    private static final String PATH = "src/main/resources/data/tratte.json";

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static List<Tratta> caricaTratte() throws IOException {
        File file = new File(PATH);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    public static void salvaTratte(List<Tratta> tratte) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PATH), tratte);
    }
}