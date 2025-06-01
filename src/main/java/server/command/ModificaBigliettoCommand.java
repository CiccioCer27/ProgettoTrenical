package command;

import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import eventi.EventoGdsModifica;
import model.Biglietto;
import model.Tratta;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

public class ModificaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClientiFedeli;
    private final MemoriaTratte memoriaTratte;
    private final BancaServiceClient banca;
    private final EventDispatcher dispatcher;

    public ModificaBigliettoCommand(RichiestaDTO richiesta,
                                    MemoriaBiglietti memoriaBiglietti,
                                    MemoriaClientiFedeli memoriaClientiFedeli,
                                    MemoriaTratte memoriaTratte,
                                    BancaServiceClient banca,
                                    EventDispatcher dispatcher) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaClientiFedeli = memoriaClientiFedeli;
        this.memoriaTratte = memoriaTratte;
        this.banca = banca;
        this.dispatcher = dispatcher;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO ignored) {
        UUID idCliente = UUID.fromString(richiesta.getIdCliente());

        Biglietto originale = memoriaBiglietti.getById(richiesta.getBiglietto().getId());
        if (originale == null) {
            return new RispostaDTO("KO", "Biglietto originale non trovato", null);
        }

        Tratta nuovaTratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());
        if (nuovaTratta == null) {
            return new RispostaDTO("KO", "Tratta richiesta non trovata", null);
        }

        long prenotati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(nuovaTratta.getId()))
                .count();

        if (prenotati >= nuovaTratta.getTreno().getCapienzaTotale()) {
            return new RispostaDTO("KO", "❌ Treno pieno, impossibile modificare", null);
        }

        boolean isFedele = memoriaClientiFedeli.isClienteFedele(idCliente);
        if (richiesta.getTipoPrezzo().name().equalsIgnoreCase("FEDELTA") && !isFedele) {
            return new RispostaDTO("KO", "❌ Prezzo fedeltà non consentito per questo cliente", null);
        }

        double prezzoNuovo = nuovaTratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(richiesta.getTipoPrezzo());

        boolean pagatoBiglietto = banca.paga(idCliente.toString(), prezzoNuovo, "Pagamento nuovo biglietto");
        if (!pagatoBiglietto) {
            return new RispostaDTO("KO", "❌ Pagamento biglietto fallito", null);
        }

        double penale = (richiesta.getPenale() != null) ? richiesta.getPenale() : 5.0;
        boolean pagatoPenale = banca.paga(idCliente.toString(), penale, "Pagamento penale modifica");
        if (!pagatoPenale) {
            return new RispostaDTO("KO", "❌ Pagamento penale fallito", null);
        }

        // Rimuovi il biglietto originale
        memoriaBiglietti.rimuoviBiglietto(originale.getId());

        Biglietto nuovo = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(nuovaTratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzoNuovo)
                .dataAcquisto(LocalDate.now())
                .tipoAcquisto("modifica")
                .conCartaFedelta(isFedele)
                .build();

        // 🔧 FIX: SOLO l'evento salva il biglietto, NON salvare qui direttamente
        // RIMUOVI: memoriaBiglietti.aggiungiBiglietto(nuovo);

        // Invia evento che si occuperà del salvataggio tramite MemoriaBigliettiListener
        System.out.println("🔔 DEBUG MODIFICA: Inviando evento (che salverà il biglietto modificato)");
        dispatcher.dispatch(new EventoGdsModifica(originale, nuovo));

        // 🔧 CONVERSIONE A DTO
        ClienteDTO clienteDTO = new ClienteDTO(
                idCliente,
                "Cliente", "Test", "cliente@test.com",
                isFedele, 0, "", 0, ""
        );

        TrattaDTO trattaDTO = AssemblerTratta.toDTO(nuovaTratta);

        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                nuovo.getId(),
                clienteDTO,
                trattaDTO,
                nuovo.getClasse(),
                richiesta.getTipoPrezzo(),
                nuovo.getPrezzoPagato(),
                StatoBiglietto.CONFERMATO
        );

        return new RispostaDTO("OK", "Biglietto modificato con successo", bigliettoDTO);
    }
}