package command;

import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import model.Biglietto;
import model.Tratta;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 🔒 MODIFICA THREAD-SAFE (senza EventDispatcher)
 */
public class ModificaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClientiFedeli;
    private final MemoriaTratte memoriaTratte;
    private final BancaServiceClient banca;

    // ✅ Costruttore COMPATIBILE (senza EventDispatcher)
    public ModificaBigliettoCommand(RichiestaDTO richiesta,
                                    MemoriaBiglietti memoriaBiglietti,
                                    MemoriaClientiFedeli memoriaClientiFedeli,
                                    MemoriaTratte memoriaTratte,
                                    BancaServiceClient banca) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaClientiFedeli = memoriaClientiFedeli;
        this.memoriaTratte = memoriaTratte;
        this.banca = banca;
    }

    // ⚠️ Costruttore di compatibilità per vecchie chiamate con EventDispatcher
    @Deprecated
    public ModificaBigliettoCommand(RichiestaDTO richiesta,
                                    MemoriaBiglietti memoriaBiglietti,
                                    MemoriaClientiFedeli memoriaClientiFedeli,
                                    MemoriaTratte memoriaTratte,
                                    BancaServiceClient banca,
                                    observer.EventDispatcher dispatcher) {
        // Ignora l'EventDispatcher nella versione thread-safe
        this(richiesta, memoriaBiglietti, memoriaClientiFedeli, memoriaTratte, banca);
        System.out.println("⚠️ DEPRECATO: EventDispatcher ignorato in ModificaBigliettoCommand thread-safe");
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO ignored) {
        System.out.println("🔍 DEBUG MODIFICA THREAD-SAFE: Iniziando modifica");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());

        // Trova biglietto originale
        Biglietto originale = memoriaBiglietti.getById(richiesta.getBiglietto().getId());
        if (originale == null) {
            return new RispostaDTO("KO", "❌ Biglietto originale non trovato", null);
        }

        // Trova nuova tratta
        Tratta nuovaTratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());
        if (nuovaTratta == null) {
            return new RispostaDTO("KO", "❌ Tratta richiesta non trovata", null);
        }

        // Verifica tipo prezzo
        boolean isFedele = memoriaClientiFedeli.isClienteFedele(idCliente);
        if (richiesta.getTipoPrezzo() == enums.TipoPrezzo.FEDELTA && !isFedele) {
            return new RispostaDTO("KO", "❌ Prezzo fedeltà non consentito", null);
        }

        double prezzoNuovo = nuovaTratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(richiesta.getTipoPrezzo());

        double penale = (richiesta.getPenale() != null) ? richiesta.getPenale() : 5.0;

        // 💳 Pagamenti
        boolean pagatoBiglietto = banca.paga(idCliente.toString(), prezzoNuovo, "Pagamento nuovo biglietto");
        if (!pagatoBiglietto) {
            return new RispostaDTO("KO", "❌ Pagamento biglietto fallito", null);
        }

        boolean pagatoPenale = banca.paga(idCliente.toString(), penale, "Pagamento penale modifica");
        if (!pagatoPenale) {
            return new RispostaDTO("KO", "❌ Pagamento penale fallito", null);
        }

        // 🔒 MODIFICA ATOMICA: Rimuovi vecchio + Aggiungi nuovo
        Biglietto nuovo = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(nuovaTratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzoNuovo)
                .dataAcquisto(LocalDate.now())
                .tipoAcquisto("modifica")
                .conCartaFedelta(isFedele)
                .build();

        // Operazione atomica di modifica
        int capienza = nuovaTratta.getTreno().getCapienzaTotale();
        boolean modificaRiuscita = memoriaBiglietti.modificaBigliettoAtomico(originale.getId(), nuovo, capienza);

        if (!modificaRiuscita) {
            return new RispostaDTO("KO", "❌ Modifica fallita: treno pieno o errore interno", null);
        }

        System.out.println("✅ DEBUG MODIFICA: Modifica completata atomicamente");

        // DTO response
        ClienteDTO clienteDTO = new ClienteDTO(
                idCliente, "Cliente", "Test", "cliente@test.com",
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

        return new RispostaDTO("OK", "✅ Biglietto modificato con successo", bigliettoDTO);
    }
}