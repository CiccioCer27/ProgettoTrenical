package persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Promozione;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PromozionePersistenceManager {
    private static final String PATH = "src/main/resources/data/promozioni.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Promozione> caricaPromozioni() throws IOException {
        File file = new File(PATH);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        return mapper.readValue(file, new TypeReference<>() {});
    }

    public static void salvaPromozioni(List<Promozione> promozioni) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(PATH), promozioni);
    }
}