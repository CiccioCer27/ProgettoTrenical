package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import eventi.EventoGdsPrenotaz;
import model.Biglietto;
import model.Tratta;
import observer.EventDispatcher;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.UUID;

public class PrenotaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaClientiFedeli memoriaFedeli;
    private final EventDispatcher dispatcher;

    public PrenotaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti memoriaBiglietti,
            MemoriaTratte memoriaTratte,
            MemoriaClientiFedeli memoriaFedeli,
            EventDispatcher dispatcher
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaTratte = memoriaTratte;
        this.memoriaFedeli = memoriaFedeli;
        this.dispatcher = dispatcher;
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
            return new RispostaDTO("KO", "❌ Nessun posto disponibile su questo treno", null);
        }

        // Verifica se può usare quel tipo di prezzo
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        switch (richiesta.getTipoPrezzo()) {
            case FEDELTA:
                if (!isFedele) {
                    return new RispostaDTO("KO", "❌ Prezzo fedeltà non disponibile per questo cliente", null);
                }
                break;

        }

        double prezzo = tratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(richiesta.getTipoPrezzo());

        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("prenotazione")
                .build();

        memoriaBiglietti.aggiungiBiglietto(biglietto);
        dispatcher.dispatch(new EventoGdsPrenotaz(biglietto));

        return new RispostaDTO("OK", "✅ Prenotazione effettuata", biglietto);
    }
}