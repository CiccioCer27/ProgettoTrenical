package command;

import Assembler.AssemblerTratta;
import dto.BigliettoDTO;
import dto.ClienteDTO;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import model.Biglietto;
import model.Tratta;
import persistence.MemoriaBiglietti;
import persistence.MemoriaClientiFedeli;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 🔒 PRENOTAZIONE THREAD-SAFE
 *
 * Applica la stessa logica atomica del comando acquisto
 */
public class PrenotaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaClientiFedeli memoriaFedeli;

    public PrenotaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti memoriaBiglietti,
            MemoriaTratte memoriaTratte,
            MemoriaClientiFedeli memoriaFedeli
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaTratte = memoriaTratte;
        this.memoriaFedeli = memoriaFedeli;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO r) {
        System.out.println("🔍 DEBUG PRENOTA THREAD-SAFE: Iniziando prenotazione");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica tipo prezzo se specificato
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        TipoPrezzo tipoPrezzoEffettivo = richiesta.getTipoPrezzo() != null ?
                richiesta.getTipoPrezzo() : TipoPrezzo.INTERO;

        if (tipoPrezzoEffettivo == TipoPrezzo.FEDELTA && !isFedele) {
            return new RispostaDTO("KO", "❌ Prezzo fedeltà non disponibile", null);
        }

        double prezzo = tratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(tipoPrezzoEffettivo);

        // Crea biglietto prenotato
        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("prenotazione")
                .build();

        // 🔒 CONTROLLO ATOMICO CAPIENZA + PRENOTAZIONE
        int capienza = tratta.getTreno().getCapienzaTotale();
        boolean postoRiservato = memoriaBiglietti.aggiungiSeSpazioDiponibile(biglietto, capienza);

        if (!postoRiservato) {
            System.out.println("❌ DEBUG PRENOTA: Treno pieno");
            return new RispostaDTO("KO", "❌ Treno pieno, nessun posto disponibile", null);
        }

        System.out.println("✅ DEBUG PRENOTA: Posto riservato per prenotazione");

        // Avvia timer scadenza (10 minuti)
        avviaTimerScadenza(biglietto);

        // 🔧 CONVERSIONE A DTO
        ClienteDTO clienteDTO = new ClienteDTO(
                idCliente, "Cliente", "Test", "cliente@test.com",
                isFedele, 0, "", 0, ""
        );

        TrattaDTO trattaDTO = AssemblerTratta.toDTO(tratta);

        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                biglietto.getId(),
                clienteDTO,
                trattaDTO,
                biglietto.getClasse(),
                tipoPrezzoEffettivo,
                biglietto.getPrezzoPagato(),
                StatoBiglietto.NON_CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Prenotazione effettuata", bigliettoDTO);
    }

    /**
     * ⏰ Avvia timer per rimuovere prenotazione dopo 10 minuti
     */
    private void avviaTimerScadenza(Biglietto prenotazione) {
        System.out.println("⏰ DEBUG: Timer scadenza avviato per " +
                prenotazione.getId().toString().substring(0, 8));

        new Thread(() -> {
            try {
                Thread.sleep(600000); // 10 minuti

                // Controlla se è ancora una prenotazione
                Biglietto biglietto = memoriaBiglietti.getById(prenotazione.getId());
                if (biglietto != null && "prenotazione".equals(biglietto.getTipoAcquisto())) {
                    boolean rimosso = memoriaBiglietti.rimuoviBiglietto(prenotazione.getId());
                    if (rimosso) {
                        System.out.println("⏰ SCADENZA: Prenotazione rimossa " +
                                prenotazione.getId().toString().substring(0, 8));
                    }
                } else {
                    System.out.println("✅ Prenotazione già confermata o rimossa");
                }

            } catch (InterruptedException e) {
                System.out.println("⚠️ Timer scadenza interrotto");
            }
        }).start();
    }
}