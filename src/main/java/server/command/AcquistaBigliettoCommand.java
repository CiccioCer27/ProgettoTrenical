package command;

import Assembler.AssemblerBiglietto;
import Assembler.AssemblerCliente;
import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import eventi.EventoGdsAcquisto;
import model.Biglietto;
import model.Cliente;
import model.Tratta;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

public class AcquistaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaFedeli;
    private final MemoriaTratte memoriaTratte;
    private final EventDispatcher dispatcher;
    private final BancaServiceClient banca;

    public AcquistaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti mb,
            MemoriaClientiFedeli mf,
            MemoriaTratte mt,
            EventDispatcher d,
            BancaServiceClient b
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = mb;
        this.memoriaFedeli = mf;
        this.memoriaTratte = mt;
        this.dispatcher = d;
        this.banca = b;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO r) {
        System.out.println("🔍 DEBUG SERVER: Iniziando elaborazione acquisto biglietto");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            System.out.println("❌ DEBUG SERVER: Tratta non trovata");
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica disponibilità posti
        long postiOccupati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(tratta.getId()))
                .count();

        int capienza = tratta.getTreno().getCapienzaTotale();
        if (postiOccupati >= capienza) {
            System.out.println("❌ DEBUG SERVER: Treno pieno");
            return new RispostaDTO("KO", "❌ Treno pieno, nessun posto disponibile", null);
        }

        // Verifica se il cliente può usare il tipoPrezzo selezionato
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        switch (richiesta.getTipoPrezzo()) {
            case FEDELTA:
                if (!isFedele) {
                    System.out.println("❌ DEBUG SERVER: Prezzo fedeltà non disponibile");
                    return new RispostaDTO("KO", "❌ Prezzo fedeltà non disponibile per questo cliente", null);
                }
                break;
            case PROMOZIONE:
                // Qui potresti aggiungere eventuali controlli su promo attive
                break;
            default:
                // INTERO va sempre bene
                break;
        }

        double prezzo = tratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(richiesta.getTipoPrezzo());

        System.out.println("💰 DEBUG SERVER: Prezzo calcolato: €" + prezzo);

        // Comunicazione con la banca
        boolean esitoPagamento = banca.paga(idCliente.toString(), prezzo, "Pagamento biglietto");
        if (!esitoPagamento) {
            System.out.println("❌ DEBUG SERVER: Pagamento fallito");
            return new RispostaDTO("KO", "❌ Pagamento fallito", null);
        }

        System.out.println("✅ DEBUG SERVER: Pagamento riuscito, creando biglietto...");

        // Crea il biglietto (model)
        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("acquisto")
                .build();

        System.out.println("🎫 DEBUG SERVER: Biglietto model creato");
        System.out.println("   ID: " + biglietto.getId());
        System.out.println("   ID Cliente: " + biglietto.getIdCliente());
        System.out.println("   ID Tratta: " + biglietto.getIdTratta());

        // Salva il biglietto
        memoriaBiglietti.aggiungiBiglietto(biglietto);
        dispatcher.dispatch(new EventoGdsAcquisto(biglietto));

        // 🔧 PROBLEMA ERA QUI: Dobbiamo convertire Biglietto -> BigliettoDTO

        // Crea ClienteDTO minimale per il DTO
        ClienteDTO clienteDTO = new ClienteDTO(
                idCliente,
                "Cliente", // Nome minimale
                "Test",    // Cognome minimale
                "cliente@test.com", // Email minimale
                isFedele,
                0, // Età
                "", // Residenza
                0, // Punti fedeltà
                "" // Cellulare
        );

        // Converte Tratta -> TrattaDTO
        TrattaDTO trattaDTO = AssemblerTratta.toDTO(tratta);

        // Crea BigliettoDTO completo
        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                biglietto.getId(),
                clienteDTO,
                trattaDTO,
                biglietto.getClasse(),
                richiesta.getTipoPrezzo(),
                biglietto.getPrezzoPagato(),
                StatoBiglietto.CONFERMATO
        );

        System.out.println("🔍 DEBUG SERVER: Preparando risposta acquisto");
        System.out.println("   BigliettoDTO creato: " + (bigliettoDTO != null ? "SÌ" : "NO"));
        if (bigliettoDTO != null) {
            System.out.println("   ID BigliettoDTO: " + bigliettoDTO.getId());
            System.out.println("   Cliente DTO: " + (bigliettoDTO.getCliente() != null ? "SÌ" : "NO"));
            System.out.println("   Tratta DTO: " + (bigliettoDTO.getTratta() != null ? "SÌ" : "NO"));
        }

        RispostaDTO risposta = new RispostaDTO("OK", "✅ Acquisto completato", bigliettoDTO);
        System.out.println("🔍 DEBUG SERVER: RispostaDTO creata");
        System.out.println("   getBiglietto(): " + (risposta.getBiglietto() != null ? "SÌ" : "NO"));
        System.out.println("   getDati(): " + (risposta.getDati() != null ? risposta.getDati().getClass().getSimpleName() : "NULL"));

        return risposta;
    }
}