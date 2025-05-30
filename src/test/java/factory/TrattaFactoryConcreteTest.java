package factory;

import enums.ClasseServizio;
import model.Tratta;
import model.Prezzo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrattaFactoryConcreteTest {

    @Test
    void generaTratte_generatesFiveTratteForGivenDate() {
        TrattaFactory factory = new TrattaFactoryConcrete();
        LocalDate data = LocalDate.now().plusDays(1); // data del giorno successivo

        List<Tratta> tratte = factory.generaTratte(data);

        // Verifica che siano 5 tratte
        assertEquals(5, tratte.size(), "Devono essere generate 5 tratte");

        for (Tratta tratta : tratte) {
            // Verifica data
            assertEquals(data, tratta.getData(), "La data della tratta deve corrispondere");

            // Verifica treno presente
            assertNotNull(tratta.getTreno(), "Ogni tratta deve avere un treno associato");

            // Verifica stazioni valide
            assertNotNull(tratta.getStazionePartenza());
            assertNotNull(tratta.getStazioneArrivo());
            assertNotEquals(tratta.getStazionePartenza(), tratta.getStazioneArrivo(), "Partenza e arrivo devono essere diverse");

            // Verifica prezzi
            Map<ClasseServizio, Prezzo> prezzi = tratta.getPrezzi();
            assertEquals(3, prezzi.size(), "Devono esserci 3 fasce di prezzo (BASE, ARGENTO, GOLD)");
            for (Prezzo prezzo : prezzi.values()) {
                assertTrue(prezzo.getIntero() > 0, "Prezzo intero deve essere positivo");
                assertTrue(prezzo.getPromozione() > 0, "Prezzo promo deve essere positivo");
                assertTrue(prezzo.getFedelta() > 0, "Prezzo fedeltà deve essere positivo");
            }
        }
    }
}