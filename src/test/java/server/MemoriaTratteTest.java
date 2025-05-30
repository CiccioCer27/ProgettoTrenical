package server;

import factory.TrattaFactoryConcrete;
import model.Tratta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.MemoriaTratte;
import persistence.TrattaPersistenceManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MemoriaTratteTest {

    private static final String PATH = "src/main/resources/data/tratte.json";

    @BeforeEach
    void clean() {
        File file = new File(PATH);
        if (file.exists()) {
            file.delete();
        } else {
            try {
                file.getParentFile().mkdirs(); // crea le cartelle se non ci sono
                file.createNewFile();          // crea il file vuoto
            } catch (IOException e) {
                e.printStackTrace();
                fail("Errore nella creazione del file JSON di test: " + e.getMessage());
            }
        }
    }

    @Test
    void salvaECaricaFunzionaCorrettamente() {
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        MemoriaTratte memoria = new MemoriaTratte();

        LocalDate data = LocalDate.now().plusDays(1);
        Tratta tratta = factory.generaTratte(data).get(0);

        memoria.aggiungiTratta(tratta);

        // Carichiamo nuovamente dal file
        try {
            List<Tratta> caricate = TrattaPersistenceManager.caricaTratte();
            assertEquals(1, caricate.size(), "La lista caricata deve contenere una tratta");
            assertEquals(tratta.getId(), caricate.get(0).getId(), "Le tratte devono avere lo stesso ID");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Errore durante salvataggio/caricamento: " + e.getMessage());
        }
    }
}