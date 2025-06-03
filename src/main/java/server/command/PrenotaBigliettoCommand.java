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
import persistence.MemoriaOsservatori;  // ✅ AGGIUNTO

import java.time.LocalDate;
import java.util.UUID;

/**
 * 🔒 PRENOTAZIONE BIGLIETTO COMMAND - CON AUTO-ISCRIZIONE NOTIFICHE
 */
public class PrenotaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaClientiFedeli memoriaFedeli;
    private final MemoriaOsservatori memoriaOsservatori;  // ✅ AGGIUNTO

    public PrenotaBigliettoCommand(
            RichiestaDTO richiesta,
            MemoriaBiglietti memoriaBiglietti,
            MemoriaTratte memoriaTratte,
            MemoriaClientiFedeli memoriaFedeli,
            MemoriaOsservatori memoriaOsservatori  // ✅ NUOVO PARAMETRO
    ) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaTratte = memoriaTratte;
        this.memoriaFedeli = memoriaFedeli;
        this.memoriaOsservatori = memoriaOsservatori;  // ✅ INJECTION
    }

    @Override
    public RispostaDTO esegui() {
        System.out.println("🔍 DEBUG PRENOTA con AUTO-ISCRIZIONE: Iniziando prenotazione");

        UUID idCliente = UUID.fromString(richiesta.getIdCliente());
        Tratta tratta = memoriaTratte.getTrattaById(richiesta.getTratta().getId());

        if (tratta == null) {
            return new RispostaDTO("KO", "❌ Tratta non trovata", null);
        }

        // Verifica tipo prezzo
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

        // 📡 ✅ AUTO-ISCRIZIONE alle notifiche della tratta prenotata
        try {
            memoriaOsservatori.aggiungiOsservatore(tratta.getId(), idCliente);
            System.out.println("📡 ✅ Cliente automaticamente iscritto alle notifiche tratta prenotata: " +
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
                tipoPrezzoEffettivo,
                biglietto.getPrezzoPagato(),
                StatoBiglietto.NON_CONFERMATO
        );

        return new RispostaDTO("OK", "✅ Prenotazione effettuata + notifiche attive", bigliettoDTO);
    }

    /**
     * ⏰ Avvia timer per rimuovere prenotazione dopo 10 minuti
     */
    private void avviaTimerScadenza(Biglietto prenotazione) {
        System.out.println("⏰ DEBUG: Timer scadenza COMPLETO avviato per " +
                prenotazione.getId().toString().substring(0, 8));

        new Thread(() -> {
            try {
                Thread.sleep(600000); // 10 minuti

                // ✅ VERIFICA che prenotazione esista ancora
                Biglietto biglietto = memoriaBiglietti.getById(prenotazione.getId());
                if (biglietto != null && "prenotazione".equals(biglietto.getTipoAcquisto())) {

                    System.out.println("⏰ SCADENZA: Rimuovendo prenotazione scaduta " +
                            prenotazione.getId().toString().substring(0, 8) + "...");

                    // 🗑️ STEP 1: Rimuovi biglietto dalla memoria
                    boolean rimossoDaMemoria = memoriaBiglietti.rimuoviBiglietto(prenotazione.getId());

                    if (rimossoDaMemoria) {
                        System.out.println("✅ Prenotazione rimossa dalla memoria biglietti");

                        // 🗑️ STEP 2: ✅ NUOVO - Rimuovi anche dalle notifiche
                        try {
                            boolean rimossoDaNotifiche = memoriaOsservatori.rimuoviOsservatore(
                                    prenotazione.getIdTratta(),
                                    prenotazione.getIdCliente()
                            );

                            if (rimossoDaNotifiche) {
                                System.out.println("📡 ✅ Cliente rimosso dalle notifiche tratta (prenotazione scaduta)");
                            } else {
                                System.out.println("⚠️ Cliente non era nelle notifiche (già rimosso?)");
                            }

                        } catch (Exception e) {
                            System.err.println("❌ Errore rimozione notifiche scadenza: " + e.getMessage());
                        }

                        System.out.println("🧹 ✅ CLEANUP SCADENZA COMPLETO: Prenotazione + Notifiche rimosse");

                    } else {
                        System.out.println("⚠️ Prenotazione non trovata in memoria per rimozione");
                    }

                } else {
                    System.out.println("✅ Prenotazione " + prenotazione.getId().toString().substring(0, 8) +
                            "... già confermata o rimossa, timer annullato");
                }

            } catch (InterruptedException e) {
                System.out.println("⚠️ Timer scadenza COMPLETO interrotto");
            }
        }).start();
    }}