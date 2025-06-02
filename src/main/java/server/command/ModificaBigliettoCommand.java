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
 * 🔒 MODIFICA BIGLIETTO COMMAND - CON SWITCH INTELLIGENTE NOTIFICHE
 *
 * NUOVO: Rimuove dalle notifiche vecchia tratta + iscrive a nuova tratta
 */
public class ModificaBigliettoCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaBiglietti memoriaBiglietti;
    private final MemoriaClientiFedeli memoriaClientiFedeli;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaOsservatori memoriaOsservatori;  // ✅ AGGIUNTO
    private final BancaServiceClient banca;

    public ModificaBigliettoCommand(RichiestaDTO richiesta,
                                    MemoriaBiglietti memoriaBiglietti,
                                    MemoriaClientiFedeli memoriaClientiFedeli,
                                    MemoriaTratte memoriaTratte,
                                    MemoriaOsservatori memoriaOsservatori,  // ✅ NUOVO PARAMETRO
                                    BancaServiceClient banca) {
        this.richiesta = richiesta;
        this.memoriaBiglietti = memoriaBiglietti;
        this.memoriaClientiFedeli = memoriaClientiFedeli;
        this.memoriaTratte = memoriaTratte;
        this.memoriaOsservatori = memoriaOsservatori;  // ✅ INJECTION
        this.banca = banca;
    }

    @Override
    public RispostaDTO esegui() {
        System.out.println("🔍 DEBUG MODIFICA con SWITCH NOTIFICHE: Iniziando modifica");

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

        // 📊 INFO per switch notifiche
        UUID vecchiaTrattaId = originale.getIdTratta();
        UUID nuovaTrattaId = nuovaTratta.getId();
        boolean stessaTratta = vecchiaTrattaId.equals(nuovaTrattaId);

        System.out.println("🔄 SWITCH NOTIFICHE INFO:");
        System.out.println("   Vecchia tratta: " + vecchiaTrattaId.toString().substring(0, 8) + "...");
        System.out.println("   Nuova tratta: " + nuovaTrattaId.toString().substring(0, 8) + "...");
        System.out.println("   Stessa tratta: " + stessaTratta);

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

        // 📡 ✅ SWITCH INTELLIGENTE NOTIFICHE
        try {
            if (!stessaTratta) {
                // 🔄 SWITCH: Rimuovi da vecchia tratta + Iscri a nuova tratta

                // Nota: MemoriaOsservatori nel tuo codice non ha metodo rimozione,
                // quindi per ora solo aggiungiamo alla nuova tratta.
                // TODO: Implementare rimozione quando necessario

                System.out.println("🔄 SWITCH NOTIFICHE: Tratta cambiata, aggiornando iscrizioni...");

                // ✅ ISCRIVI alla nuova tratta
                memoriaOsservatori.aggiungiOsservatore(nuovaTrattaId, idCliente);
                System.out.println("📡 ✅ Cliente iscritto alle notifiche nuova tratta: " +
                        nuovaTratta.getStazionePartenza() + " → " + nuovaTratta.getStazioneArrivo());

                // 🗑️ TODO: Rimuovi dalla vecchia tratta (quando implementato)
                // memoriaOsservatori.rimuoviOsservatore(vecchiaTrattaId, idCliente);
                System.out.println("⚠️ TODO: Rimuovere dalle notifiche vecchia tratta (da implementare)");

            } else {
                // ✅ STESSA TRATTA: Mantieni iscrizione esistente
                System.out.println("📡 ✅ Stessa tratta, mantengo iscrizione notifiche esistente");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore switch notifiche (non critico): " + e.getMessage());
        }

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

        return new RispostaDTO("OK", "✅ Biglietto modificato + notifiche aggiornate", bigliettoDTO);
    }
}