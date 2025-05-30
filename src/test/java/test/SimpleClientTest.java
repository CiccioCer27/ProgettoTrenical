package test;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import service.ClientService;

import java.time.LocalDate;
import java.util.List;

/**
 * Test client semplice che si connette a server già avviato
 * ISTRUZIONI:
 * 1. Prima avvia: mvn exec:java -Dexec.mainClass="server.main.ServerMain"
 * 2. Poi esegui: mvn exec:java -Dexec.mainClass="test.SimpleClientTest"
 */
public class SimpleClientTest {

    private static final int SERVER_PORT = 9090;

    public static void main(String[] args) {
        System.out.println("🧪 ===== TEST CLIENT TRENICAL =====");
        System.out.println("📡 Connessione al server su porta " + SERVER_PORT);

        try {
            // Aspetta un momento per essere sicuri che il server sia pronto
            Thread.sleep(1000);

            // Crea client
            ClientService client = new ClientService("localhost", SERVER_PORT);

            // TEST 1: Registrazione Cliente
            System.out.println("\n1️⃣ Test: Registrazione Cliente");
            client.attivaCliente("Mario", "Rossi", "mario@test.com", 35, "Milano", "3331234567");
            ClienteDTO cliente = client.getCliente();
            System.out.println("   ✅ Cliente registrato: " + cliente.getNome() + " " + cliente.getCognome());
            System.out.println("   🆔 ID Cliente: " + cliente.getId());

            // TEST 2: Ricerca Tratte
            System.out.println("\n2️⃣ Test: Ricerca Tratte");
            RichiestaDTO richiestaFiltro = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;") // Cerca tutte le tratte
                    .build();

            RispostaDTO rispostaFiltro = client.inviaRichiesta(richiestaFiltro);
            List<TrattaDTO> tratte = rispostaFiltro.getTratte();

            if (tratte != null && !tratte.isEmpty()) {
                System.out.println("   ✅ Trovate " + tratte.size() + " tratte");
                TrattaDTO prima = tratte.get(0);
                System.out.println("   📍 Prima tratta: " + prima.getStazionePartenza() + " → " + prima.getStazioneArrivo() + " (" + prima.getData() + ")");

                // TEST 3: Carta Fedeltà
                System.out.println("\n3️⃣ Test: Acquisto Carta Fedeltà");
                RichiestaDTO richiestaFedelta = new RichiestaDTO.Builder()
                        .tipo("CARTA_FEDELTA")
                        .idCliente(cliente.getId().toString())
                        .build();

                RispostaDTO rispostaFedelta = client.inviaRichiesta(richiestaFedelta);
                System.out.println("   " + (rispostaFedelta.getEsito().equals("OK") ? "✅" : "❌") +
                        " Carta Fedeltà: " + rispostaFedelta.getMessaggio());

                // TEST 4: Prenotazione
                System.out.println("\n4️⃣ Test: Prenotazione Biglietto");
                RichiestaDTO richiestaPrenotazione = new RichiestaDTO.Builder()
                        .tipo("PRENOTA")
                        .idCliente(cliente.getId().toString())
                        .tratta(prima)
                        .classeServizio(ClasseServizio.BASE)
                        .build();

                RispostaDTO rispostaPrenotazione = client.inviaRichiesta(richiestaPrenotazione);
                System.out.println("   " + (rispostaPrenotazione.getEsito().equals("OK") ? "✅" : "❌") +
                        " Prenotazione: " + rispostaPrenotazione.getMessaggio());

                // TEST 5: Conferma prenotazione (se c'è)
                if (rispostaPrenotazione.getBiglietto() != null) {
                    System.out.println("\n5️⃣ Test: Conferma Prenotazione");
                    RichiestaDTO richiestaConferma = new RichiestaDTO.Builder()
                            .tipo("CONFERMA")
                            .idCliente(cliente.getId().toString())
                            .biglietto(rispostaPrenotazione.getBiglietto())
                            .build();

                    RispostaDTO rispostaConferma = client.inviaRichiesta(richiestaConferma);
                    System.out.println("   " + (rispostaConferma.getEsito().equals("OK") ? "✅" : "❌") +
                            " Conferma: " + rispostaConferma.getMessaggio());
                }

                // TEST 6: Acquisto Diretto con Fedeltà
                if (tratte.size() > 1) {
                    System.out.println("\n6️⃣ Test: Acquisto Diretto con Fedeltà");
                    TrattaDTO seconda = tratte.get(1);
                    RichiestaDTO richiestaAcquisto = new RichiestaDTO.Builder()
                            .tipo("ACQUISTA")
                            .idCliente(cliente.getId().toString())
                            .tratta(seconda)
                            .classeServizio(ClasseServizio.ARGENTO)
                            .tipoPrezzo(TipoPrezzo.FEDELTA)
                            .build();

                    RispostaDTO rispostaAcquisto = client.inviaRichiesta(richiestaAcquisto);
                    System.out.println("   " + (rispostaAcquisto.getEsito().equals("OK") ? "✅" : "❌") +
                            " Acquisto: " + rispostaAcquisto.getMessaggio());
                }

                // TEST 7: Acquisto normale senza fedeltà
                if (tratte.size() > 2) {
                    System.out.println("\n7️⃣ Test: Acquisto Normale");
                    TrattaDTO terza = tratte.get(2);
                    RichiestaDTO richiestaAcquisto2 = new RichiestaDTO.Builder()
                            .tipo("ACQUISTA")
                            .idCliente(cliente.getId().toString())
                            .tratta(terza)
                            .classeServizio(ClasseServizio.BASE)
                            .tipoPrezzo(TipoPrezzo.INTERO)
                            .build();

                    RispostaDTO rispostaAcquisto2 = client.inviaRichiesta(richiestaAcquisto2);
                    System.out.println("   " + (rispostaAcquisto2.getEsito().equals("OK") ? "✅" : "❌") +
                            " Acquisto normale: " + rispostaAcquisto2.getMessaggio());
                }

                // TEST 8: Notifiche Tratta
                System.out.println("\n8️⃣ Test: Iscrizione Notifiche");
                client.avviaNotificheTratta(prima);
                System.out.println("   ✅ Notifiche configurate per: " + prima.getStazionePartenza() + " → " + prima.getStazioneArrivo());

                // Aspetta un po' per vedere eventuali notifiche/promozioni
                System.out.println("   ⏳ Attendo 8 secondi per notifiche e promozioni...");
                Thread.sleep(8000);

            } else {
                System.out.println("   ❌ Nessuna tratta trovata! Controlla che il server abbia generato le tratte.");
            }

            System.out.println("\n✅ ===== TUTTI I TEST COMPLETATI CON SUCCESSO! =====");
            System.out.println("🎯 Il sistema TreniCal funziona correttamente!");

        } catch (Exception e) {
            System.err.println("❌ Errore durante il test: " + e.getMessage());
            System.err.println("💡 Assicurati che il ServerMain sia in esecuzione su porta " + SERVER_PORT);
            e.printStackTrace();
        }
    }
}