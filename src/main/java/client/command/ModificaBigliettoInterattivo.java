package command;

import adapter.ClientEventAdapter;
import command.Command;
import dto.BigliettoDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import model.Wallet;
import service.ClientService;

import java.util.List;
import java.util.Scanner;

public class ModificaBigliettoInterattivo implements Command {
    private final ClientService clientService;
    private final Wallet wallet;

    public ModificaBigliettoInterattivo(ClientService clientService, Wallet wallet) {
        this.clientService = clientService;
        this.wallet = wallet;
    }

    @Override
    public void esegui() {
        System.out.println("\n🔄 MODIFICA BIGLIETTO");
        System.out.println("-".repeat(20));

        try {
            List<BigliettoDTO> biglietti = wallet.getBigliettiConfermati();
            if (biglietti.isEmpty()) {
                System.out.println("ℹ️ Non hai biglietti da modificare");
                return;
            }

            System.out.println("📋 TUOI BIGLIETTI:");
            for (int i = 0; i < biglietti.size(); i++) {
                System.out.println((i + 1) + ") " + formatBigliettoDettagliato(biglietti.get(i)));
            }

            Scanner scanner = new Scanner(System.in);
            System.out.print("Scegli biglietto (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());
            if (scelta < 1 || scelta > biglietti.size()) return;

            BigliettoDTO bigliettoOriginale = biglietti.get(scelta - 1);

            // Selezione nuova tratta
            TrattaDTO nuovaTratta = selezionaTratta(scanner);
            if (nuovaTratta == null) return;

            ClasseServizio nuovaClasse = selezionaClasse(scanner);
            if (nuovaClasse == null) return;

            TipoPrezzo tipoPrezzo = selezionaTipoPrezzo(scanner);
            if (tipoPrezzo == null) return;

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("MODIFICA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .biglietto(bigliettoOriginale)
                    .tratta(nuovaTratta)
                    .classeServizio(nuovaClasse)
                    .tipoPrezzo(tipoPrezzo)
                    .penale(5.0)
                    .build();

            System.out.println("⏳ Invio modifica...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Biglietto modificato!");

                if (risposta.getBiglietto() != null) {
                    ClientEventAdapter.processaModifica(bigliettoOriginale, risposta.getBiglietto());
                }
            } else {
                throw new RuntimeException("Modifica fallita: " + risposta.getMessaggio());
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Inserisci un numero valido!");
        } catch (Exception e) {
            throw new RuntimeException("Errore modifica: " + e.getMessage());
        }
    }

    private String formatBigliettoDettagliato(BigliettoDTO biglietto) {
        try {
            String partenza = biglietto.getTratta() != null ? biglietto.getTratta().getStazionePartenza() : "N/A";
            String arrivo = biglietto.getTratta() != null ? biglietto.getTratta().getStazioneArrivo() : "N/A";
            String data = biglietto.getTratta() != null && biglietto.getTratta().getData() != null ?
                    biglietto.getTratta().getData().toString() : "N/A";

            return String.format("ID:%s | %s → %s | %s | %s | €%.2f",
                    biglietto.getId().toString().substring(0, 8),
                    partenza, arrivo, data,
                    biglietto.getClasseServizio(),
                    biglietto.getPrezzoEffettivo());
        } catch (Exception e) {
            return "Biglietto non valido: " + e.getMessage();
        }
    }

    private TrattaDTO selezionaTratta(Scanner scanner) {
        try {
            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;")
                    .build();

            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
                System.out.println("❌ Nessuna tratta disponibile");
                return null;
            }

            System.out.println("\n📋 TRATTE DISPONIBILI:");
            int limite = Math.min(risposta.getTratte().size(), 10);
            for (int i = 0; i < limite; i++) {
                System.out.println((i + 1) + ") " + formatTratta(risposta.getTratte().get(i)));
            }

            System.out.print("Scegli nuova tratta (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > limite) {
                return null;
            }

            return risposta.getTratte().get(scelta - 1);
        } catch (Exception e) {
            System.out.println("❌ Errore selezione tratta: " + e.getMessage());
            return null;
        }
    }

    private ClasseServizio selezionaClasse(Scanner scanner) {
        try {
            System.out.println("\n🎭 CLASSI:");
            ClasseServizio[] classi = ClasseServizio.values();
            for (int i = 0; i < classi.length; i++) {
                System.out.println((i + 1) + ") " + classi[i]);
            }

            System.out.print("Scegli nuova classe (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > classi.length) {
                return null;
            }

            return classi[scelta - 1];
        } catch (Exception e) {
            System.out.println("❌ Errore selezione classe: " + e.getMessage());
            return null;
        }
    }

    private TipoPrezzo selezionaTipoPrezzo(Scanner scanner) {
        try {
            System.out.println("\n💰 TIPI PREZZO:");
            TipoPrezzo[] tipi = TipoPrezzo.values();
            for (int i = 0; i < tipi.length; i++) {
                System.out.println((i + 1) + ") " + tipi[i]);
            }

            System.out.print("Scegli tipo prezzo (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > tipi.length) {
                return null;
            }

            return tipi[scelta - 1];
        } catch (Exception e) {
            System.out.println("❌ Errore selezione tipo prezzo: " + e.getMessage());
            return null;
        }
    }

    private String formatTratta(TrattaDTO tratta) {
        try {
            return String.format("%s → %s | %s %s | Bin.%d",
                    tratta.getStazionePartenza(),
                    tratta.getStazioneArrivo(),
                    tratta.getData(),
                    tratta.getOra(),
                    tratta.getBinario());
        } catch (Exception e) {
            return "Tratta non valida: " + e.getMessage();
        }
    }
}