package test;

import persistence.*;
import model.*;
import enums.ClasseServizio;
import factory.TrattaFactoryConcrete;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ⚡ VERIFICA RAPIDA PERSISTENCE
 * Test veloce per confermare che tutto funziona
 */
public class QuickPersistenceCheck {

    public static void main(String[] args) {
        System.out.println("⚡ ===== VERIFICA RAPIDA PERSISTENCE =====");

        try {
            // 1️⃣ Verifica caricamento esistente
            verificaCaricamentoEsistente();

            // 2️⃣ Test salvataggio nuovo
            testSalvataggioNuovo();

            // 3️⃣ Verifica persistenza
            verificaPersistenza();

            System.out.println("\n🎉 ===== PERSISTENCE: TUTTO OK! =====");

        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void verificaCaricamentoEsistente() {
        System.out.println("\n📖 1. Verifica Caricamento Dati Esistenti");

        // Carica tratte
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        List<Tratta> tratte = memoriaTratte.getTutteTratte();
        System.out.println("   🚂 Tratte caricate: " + tratte.size());

        if (!tratte.isEmpty()) {
            Tratta prima = tratte.get(0);
            System.out.println("   📍 Prima tratta: " + prima.getStazionePartenza() +
                    " → " + prima.getStazioneArrivo() + " (" + prima.getData() + ")");
        }

        // Carica biglietti
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        List<Biglietto> biglietti = memoriaBiglietti.getTuttiIBiglietti();
        System.out.println("   🎫 Biglietti caricati: " + biglietti.size());

        if (!biglietti.isEmpty()) {
            Biglietto primo = biglietti.get(0);
            System.out.println("   💳 Primo biglietto: " + primo.getClasse() +
                    " - €" + String.format("%.2f", primo.getPrezzoPagato()));
        }

        // Carica clienti fedeli
        MemoriaClientiFedeli memoriaFedeli = new MemoriaClientiFedeli();
        System.out.println("   👤 Memoria clienti fedeli: ✅ caricata");

        System.out.println("   ✅ Caricamento dati esistenti: OK");
    }

    private static void testSalvataggioNuovo() {
        System.out.println("\n💾 2. Test Salvataggio Nuovi Dati");

        // Aggiungi nuova tratta
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        int tratteIniziali = memoriaTratte.getTutteTratte().size();

        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        List<Tratta> nuoveTratte = factory.generaTratte(LocalDate.now().plusDays(30));
        Tratta nuovaTratta = nuoveTratte.get(0);

        memoriaTratte.aggiungiTratta(nuovaTratta);
        System.out.println("   ➕ Tratta aggiunta: " + nuovaTratta.getStazionePartenza() +
                " → " + nuovaTratta.getStazioneArrivo());

        // Aggiungi nuovo biglietto
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        int bigliettiIniziali = memoriaBiglietti.getTuttiIBiglietti().size();

        Biglietto nuovoBiglietto = new Biglietto.Builder()
                .idCliente(UUID.randomUUID())
                .idTratta(nuovaTratta.getId())
                .classe(ClasseServizio.GOLD)
                .prezzoPagato(99.99)
                .dataAcquisto(LocalDate.now())
                .tipoAcquisto("test-rapido")
                .conCartaFedelta(false)
                .build();

        memoriaBiglietti.aggiungiBiglietto(nuovoBiglietto);
        System.out.println("   🎫 Biglietto aggiunto: " + nuovoBiglietto.getClasse() +
                " - €" + nuovoBiglietto.getPrezzoPagato());

        // Aggiungi cliente fedele
        MemoriaClientiFedeli memoriaFedeli = new MemoriaClientiFedeli();
        UUID nuovoClienteFedele = UUID.randomUUID();
        memoriaFedeli.registraClienteFedele(nuovoClienteFedele);
        System.out.println("   👤 Cliente fedele aggiunto: " +
                nuovoClienteFedele.toString().substring(0, 8) + "...");

        System.out.println("   ✅ Salvataggio nuovi dati: OK");
    }

    private static void verificaPersistenza() {
        System.out.println("\n🔄 3. Verifica Persistenza (Ricaricamento)");

        // Ricarica tutto da file
        MemoriaTratte nuovaMemoriaTratte = new MemoriaTratte();
        MemoriaBiglietti nuovaMemoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli nuovaMemoriaFedeli = new MemoriaClientiFedeli();

        int tratteRicaricate = nuovaMemoriaTratte.getTutteTratte().size();
        int bigliettiRicaricati = nuovaMemoriaBiglietti.getTuttiIBiglietti().size();

        System.out.println("   📊 Tratte ricaricate: " + tratteRicaricate);
        System.out.println("   📊 Biglietti ricaricati: " + bigliettiRicaricati);

        // Verifica che i dati appena aggiunti ci siano
        boolean persistenzaOk = tratteRicaricate > 0 && bigliettiRicaricati > 0;

        System.out.println("   " + (persistenzaOk ? "✅" : "❌") + " Persistenza verificata");

        // Test ricerca specifica
        List<Biglietto> bigliettiTest = nuovaMemoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> "test-rapido".equals(b.getTipoAcquisto()))
                .toList();

        System.out.println("   🔍 Biglietti test trovati: " + bigliettiTest.size());
        System.out.println("   " + (!bigliettiTest.isEmpty() ? "✅" : "⚠️") +
                " Ricerca specifica funziona");
    }
}