package server;

import factory.TrattaFactoryConcrete;
import model.Tratta;
import model.Treno;
import model.Prezzo;
import persistence.MemoriaTratte;
import scheduling.TratteScheduler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TratteSchedulerFullTest {

    @Test
    public void testGenerazioneTratteConDettagliPerUnMinuto() throws InterruptedException {
        MemoriaTratte memoria = new MemoriaTratte();
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        TratteScheduler scheduler = new TratteScheduler(memoria, factory);

        // Esegue la generazione manuale (senza aspettare mezzanotte)
        scheduler.testAggiornaTratte(); // ← metodo @VisibleForTesting
        System.out.println("⏳ Attendo 60 secondi per osservare le tratte generate...");
        Thread.sleep(60_000);

        List<Tratta> tratte = memoria.getTutteTratte();

        assertNotNull(tratte, "❌ La lista delle tratte è nulla");
        assertTrue(tratte.size() >= 15, "❌ Numero tratte troppo basso: " + tratte.size());

        System.out.println("✅ Numero tratte generate: " + tratte.size());
        System.out.println("--------------------------------------------------");

        for (Tratta tratta : tratte) {
            System.out.println("🛤 Tratta: " + tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo());
            System.out.println("📅 Data: " + tratta.getData() + " | 🕒 Ora: " + tratta.getOra() + " | Binario: " + tratta.getBinario());

            Treno treno = tratta.getTreno();
            System.out.println("🚆 Treno #" + treno.getNumero() + " - " + treno.getNomeCommerciale());
            System.out.println("   ▸ Tipologia: " + treno.getTipologia());
            System.out.println("   ▸ Capienza: " + treno.getCapienzaTotale());
            System.out.println("   ▸ Servizi: WiFi=" + treno.isWifiDisponibile() +
                    ", Prese=" + treno.isPreseElettriche() +
                    ", A/C=" + treno.isAriaCondizionata() +
                    ", Ristoro=" + treno.getServiziRistorazione());

            System.out.println("💸 Prezzi per classe:");
            tratta.getPrezzi().forEach((classe, prezzo) -> {
                System.out.println("   ▸ " + classe.name() + ": Intero = €" + String.format("%.2f", prezzo.getIntero()) +
                        ", Promo = €" + String.format("%.2f", prezzo.getPromozione()) +
                        ", Fedeltà = €" + String.format("%.2f", prezzo.getFedelta()));
            });

            System.out.println("--------------------------------------------------");
        }
    }
}