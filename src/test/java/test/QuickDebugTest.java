package test;

import persistence.MemoriaBiglietti;
import persistence.MemoriaTratte;
import observer.MemoriaBigliettiListener;
import observer.EventDispatcher;
import eventi.EventoGdsAcquisto;
import model.Biglietto;
import model.Tratta;
import model.Treno;
import model.Prezzo;
import enums.ClasseServizio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🧪 TEST DEBUG VELOCE - Verifica setup listener
 */
public class QuickDebugTest {

    public static void main(String[] args) {
        System.out.println("🧪 TEST DEBUG VELOCE");

        // Setup componenti
        MemoriaBiglietti memoria = new MemoriaBiglietti();
        MemoriaTratte memoriaTratte = new MemoriaTratte();

        // Crea tratta test
        Treno treno = new Treno.Builder()
                .numero(999)
                .tipologia("Test")
                .capienzaTotale(2) // Capienza piccolissima per test
                .build();

        Map<ClasseServizio, Prezzo> prezzi = new HashMap<>();
        prezzi.put(ClasseServizio.BASE, new Prezzo(10.0, 8.0, 6.0));

        Tratta tratta = new Tratta(
                UUID.randomUUID(),
                "TestA", "TestB",
                LocalDate.now(), LocalTime.now(),
                1, treno, prezzi
        );

        memoriaTratte.aggiungiTratta(tratta);
        System.out.println("✅ Tratta creata con capienza: " + treno.getCapienzaTotale());

        // Setup listener
        EventDispatcher dispatcher = new EventDispatcher();
        MemoriaBigliettiListener listener = new MemoriaBigliettiListener(memoria, memoriaTratte);
        dispatcher.registra(listener);

        // Test 3 biglietti (dovrebbe accettarne solo 2)
        for (int i = 1; i <= 3; i++) {
            System.out.println("\n🎫 Tentativo biglietto " + i);

            Biglietto biglietto = new Biglietto.Builder()
                    .idCliente(UUID.randomUUID())
                    .idTratta(tratta.getId())
                    .classe(ClasseServizio.BASE)
                    .prezzoPagato(10.0)
                    .dataAcquisto(LocalDate.now())
                    .tipoAcquisto("acquisto")
                    .build();

            dispatcher.dispatch(new EventoGdsAcquisto(biglietto));

            // Verifica risultato
            boolean salvato = memoria.getById(biglietto.getId()) != null;
            System.out.println("Risultato: " + (salvato ? "✅ ACCETTATO" : "❌ RIFIUTATO"));
        }

        // Statistiche finali
        long totale = memoria.contaBigliettiPerTratta(tratta.getId());
        System.out.println("\n📊 RISULTATO FINALE:");
        System.out.println("   Capienza: " + treno.getCapienzaTotale());
        System.out.println("   Biglietti salvati: " + totale);
        System.out.println("   Test: " + (totale <= treno.getCapienzaTotale() ? "✅ PASS" : "❌ FAIL"));
    }
}