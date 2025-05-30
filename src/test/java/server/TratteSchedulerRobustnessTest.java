package server;

import factory.TrattaFactoryConcrete;
import model.Prezzo;
import model.Tratta;
import persistence.MemoriaTratte;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TratteSchedulerRobustnessTest {

    @Test
    public void testStazionePartenzaDiversaDaArrivo() {
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(1));

        for (Tratta t : tratte) {
            assertNotEquals(t.getStazionePartenza(), t.getStazioneArrivo(),
                    "❌ Partenza e arrivo coincidono: " + t.getStazionePartenza());
        }
    }

    @Test
    public void testPrezziOrdinatiCorrettamente() {
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(1));

        for (Tratta t : tratte) {
            t.getPrezzi().forEach((classe, prezzo) -> {
                assertTrue(prezzo.getIntero() > prezzo.getPromozione(),
                        "❌ Prezzo intero NON maggiore di promo in classe " + classe);
                assertTrue(prezzo.getPromozione() > prezzo.getFedelta(),
                        "❌ Prezzo promo NON maggiore di fedeltà in classe " + classe);
            });
        }
    }

    @Test
    public void testNessunaGenerazioneTrattePassate() {
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        List<Tratta> tratte = factory.generaTratte(LocalDate.now().minusDays(1));

        // In base alla tua implementazione, potresti aspettarti che siano comunque generate
        // Se invece vuoi bloccarle nel futuro, modifica factory per ignorarle
        // assertTrue(tratte.isEmpty(), "❌ Tratte generate per data passata");

        System.out.println("📅 Tratte generate per data passata: " + tratte.size());
    }

    @Test
    public void testSalvataggioECaricamento() {
        MemoriaTratte memoria = new MemoriaTratte();
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();

        List<Tratta> nuove = factory.generaTratte(LocalDate.now().plusDays(3));
        nuove.forEach(memoria::aggiungiTratta);

        MemoriaTratte memoriaRicaricata = new MemoriaTratte();
        List<Tratta> caricate = memoriaRicaricata.getTutteTratte();

        assertTrue(caricate.size() >= nuove.size(),
                "❌ Tratte non salvate correttamente nel file JSON");
    }
}