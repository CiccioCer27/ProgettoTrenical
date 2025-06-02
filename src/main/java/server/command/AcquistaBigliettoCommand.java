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
import persistence.MemoriaOsservatori;  // ✅ AGGIUNTO
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 🔒 ACQUISTA BIGLIETTO COMMAND - CON AUTO-ISCRIZIONE NOTIFICHE
 *
 * NUOVO: Auto-iscrizione automatica alle notifiche della tratta acquistata
 */
public class AcquistaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaFedeli;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaOsservatori memoriaOsservatori;  // ✅ AGGIUNTO
    private final BancaServiceClient banca;

    public AcquistaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti mb,
            MemoriaClientiFedeli mf,
            MemoriaTratte mt,
            MemoriaOsservatori mo,  // ✅ NUOVO PARAMETRO
            BancaServiceClient b
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = mb;
        this.memoriaFedeli = mf;
        this.memoriaTratte = mt;
        this.memoriaOsservatori = mo;  // ✅ INJECTION
        this.banca = b;
    }

    @Override
    public RispostaDTO esegui() {
        System.out.println("🔍 DEBUG ACQUISTO con AUTO-ISCRIZIONE: Iniziando acquisto");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica tipo prezzo
        boolean isFedele = memoriaFedeli.isClienteFedele(idCliente);
        if (richiesta.getTipoPrezzo() == enums.TipoPrezzo.FEDELTA && !isFedele) {
            return new RispostaDTO("KO", "❌ Prezzo fedeltà non disponibile", null);
        }

        double prezzo = tratta.getPrezzi()
                .get(richiesta.getClasseServizio())
                .getPrezzo(richiesta.getTipoPrezzo());

        System.out.println("💰 DEBUG: Prezzo calcolato: €" + prezzo);

        // Crea biglietto
        Biglietto biglietto = new Biglietto.Builder()
                .idCliente(idCliente)
                .idTratta(tratta.getId())
                .classe(richiesta.getClasseServizio())
                .prezzoPagato(prezzo)
                .dataAcquisto(LocalDate.now())
                .conCartaFedelta(isFedele)
                .tipoAcquisto("acquisto")
                .build();

        // 🔒 CONTROLLO ATOMICO CAPIENZA + PRENOTAZIONE POSTO
        int capienza = tratta.getTreno().getCapienzaTotale();
        boolean postoRiservato = memoriaBiglietti.aggiungiSeSpazioDiponibile(biglietto, capienza);

        if (!postoRiservato) {
            System.out.println("❌ DEBUG: Treno pieno - capienza rispettata atomicamente");
            return new RispostaDTO("KO", "❌ Treno pieno, nessun posto disponibile", null);
        }

        System.out.println("✅ DEBUG: Posto riservato atomicamente, procedo con pagamento");

        // 💳 PAGAMENTO
        boolean esitoPagamento = banca.paga(idCliente.toString(), prezzo, "Pagamento biglietto");

        if (!esitoPagamento) {
            // Rollback: rimuovi il biglietto se il pagamento fallisce
            memoriaBiglietti.rimuoviBiglietto(biglietto.getId());
            System.out.println("❌ DEBUG: Pagamento fallito, posto rilasciato");
            return new RispostaDTO("KO", "❌ Pagamento fallito", null);
        }

        System.out.println("✅ DEBUG: Pagamento riuscito, biglietto confermato");

        // 📡 ✅ AUTO-ISCRIZIONE alle notifiche della tratta acquistata
        try {
            memoriaOsservatori.aggiungiOsservatore(tratta.getId(), idCliente);
            System.out.println("📡 ✅ Cliente automaticamente iscritto alle notifiche tratta: " +
                    tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo());
        } catch (Exception e) {
            System.err.println("⚠️ Errore auto-iscrizione notifiche (non critico): " + e.getMessage());
        }

        // Conversione a DTO
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
                richiesta.getTipoPrezzo(),
                biglietto.getPrezzoPagato(),
                StatoBiglietto.CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Acquisto completato + notifiche attive", bigliettoDTO);
    }
}
