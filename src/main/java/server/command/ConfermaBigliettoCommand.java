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
import persistence.MemoriaTratte;
import persistence.MemoriaOsservatori;  // ✅ AGGIUNTO
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 🔒 CONFERMA BIGLIETTO COMMAND - CON AUTO-ISCRIZIONE gRPC
 */
public class ConfermaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoria;
    private final BancaServiceClient banca;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaOsservatori memoriaOsservatori;  // ✅ AGGIUNTO

    public ConfermaBigliettoCommand(RichiestaDTO richiesta, MemoriaBiglietti memoria,
                                    BancaServiceClient banca, MemoriaTratte memoriaTratte,
                                    MemoriaOsservatori memoriaOsservatori) {  // ✅ NUOVO PARAMETRO
        this.richiesta = richiesta;
        this.memoria = memoria;
        this.banca = banca;
        this.memoriaTratte = memoriaTratte;
        this.memoriaOsservatori = memoriaOsservatori;  // ✅ INJECTION
    }

    @Override
    public RispostaDTO esegui() {
        System.out.println("🔍 DEBUG CONFERMA con AUTO-ISCRIZIONE: Iniziando conferma");

        BigliettoDTO bigliettoPrenotato = richiesta.getBiglietto();
        if (bigliettoPrenotato == null) {
            return new RispostaDTO("KO", "❌ Biglietto non specificato", null);
        }

        // Trova prenotazione
        Biglietto bigliettoModel = memoria.getById(bigliettoPrenotato.getId());
        if (bigliettoModel == null) {
            return new RispostaDTO("KO", "❌ Prenotazione non trovata", null);
        }

        if (!"prenotazione".equals(bigliettoModel.getTipoAcquisto())) {
            return new RispostaDTO("KO", "❌ Il biglietto non è una prenotazione", null);
        }

        // 💳 Pagamento
        boolean pagato = banca.paga(
                bigliettoModel.getIdCliente().toString(),
                bigliettoModel.getPrezzoPagato(),
                "Pagamento conferma biglietto"
        );

        if (!pagato) {
            return new RispostaDTO("KO", "❌ Pagamento non riuscito", null);
        }

        // 🔒 OPERAZIONE ATOMICA: Rimuovi prenotazione + Aggiungi confermato
        boolean successo = memoria.confermaPrenotazione(bigliettoModel);

        if (!successo) {
            return new RispostaDTO("KO", "❌ Errore durante la conferma", null);
        }

        System.out.println("✅ DEBUG CONFERMA: Prenotazione confermata atomicamente");

        // 📡 ✅ ENSURE AUTO-ISCRIZIONE alle notifiche (conferma che sia attiva)
        try {
            // Il cliente dovrebbe già essere iscritto dalla prenotazione,
            // ma assicuriamoci che l'iscrizione sia attiva
            memoriaOsservatori.aggiungiOsservatore(bigliettoModel.getIdTratta(), bigliettoModel.getIdCliente());
            System.out.println("📡 ✅ Confermata iscrizione notifiche per biglietto confermato");
        } catch (Exception e) {
            System.err.println("⚠️ Errore conferma iscrizione notifiche (non critico): " + e.getMessage());
        }

        // Il biglietto confermato ha lo stesso ID ma tipo diverso
        Biglietto confermato = new Biglietto(
                bigliettoModel.getId(), // Stesso ID
                bigliettoModel.getIdCliente(),
                bigliettoModel.getIdTratta(),
                bigliettoModel.getClasse(),
                bigliettoModel.isConCartaFedelta(),
                bigliettoModel.getPrezzoPagato(),
                LocalDate.now(),
                "acquisto" // Cambia tipo
        );

        // DTO response
        ClienteDTO clienteDTO = new ClienteDTO(
                confermato.getIdCliente(), "Cliente", "Test", "cliente@test.com",
                confermato.isConCartaFedelta(), 0, "", 0, ""
        );

        TrattaDTO trattaDTO = createMinimalTrattaDTO(confermato.getIdTratta());

        BigliettoDTO bigliettoDTO = new BigliettoDTO(
                confermato.getId(),
                clienteDTO,
                trattaDTO,
                confermato.getClasse(),
                confermato.isConCartaFedelta() ? TipoPrezzo.FEDELTA : TipoPrezzo.INTERO,
                confermato.getPrezzoPagato(),
                StatoBiglietto.CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Biglietto confermato + notifiche attive", bigliettoDTO);
    }

    private TrattaDTO createMinimalTrattaDTO(UUID idTratta) {
        return new TrattaDTO(
                idTratta, "Partenza", "Arrivo",
                LocalDate.now(), java.time.LocalTime.now(), 1,
                null, null
        );
    }
}
