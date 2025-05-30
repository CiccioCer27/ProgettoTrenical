package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import eventi.EventoGdsAcquisto;
import model.Biglietto;
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
        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica disponibilità posti
        long postiOccupati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(tratta.getId()))
                .count();

        int capienza = tratta.getTreno().getCapienzaTotale();
        if (postiOccupati >= capienza) {
            return new RispostaDTO("KO", "❌ Treno pieno, nessun posto disponibile", null);
        }

        // Verifica se il cliente può usare il tipoPrezzo selezionato
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        switch (richiesta.getTipoPrezzo()) {
            case FEDELTA:
                if (!isFedele) {
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

        // Comunicazione con la banca
        boolean esitoPagamento = banca.paga(idCliente.toString(), prezzo, "Pagamento biglietto");
        if (!esitoPagamento) {
            return new RispostaDTO("KO", "❌ Pagamento fallito", null);
        }

        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("acquisto")
                .build();

        memoriaBiglietti.aggiungiBiglietto(biglietto);
        dispatcher.dispatch(new EventoGdsAcquisto(biglietto));

        return new RispostaDTO("OK", "✅ Acquisto completato", biglietto);
    }
}