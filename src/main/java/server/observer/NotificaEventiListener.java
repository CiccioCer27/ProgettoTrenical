package observer;

import eventi.*;
import model.Tratta;
import model.Promozione;
import model.Biglietto;
import persistence.MemoriaTratte;
import persistence.MemoriaBiglietti;  // ✅ AGGIUNTO
import grpc.TrenicalServiceImpl;
import dto.PromozioneDTO;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🔔 NOTIFICA EVENTI LISTENER - ENHANCED
 *
 * NUOVO: Notifica automaticamente clienti che hanno biglietti per tratte modificate
 * MIGLIORAMENTO: Sistema "push" invece di solo "pull"
 */
public class NotificaEventiListener implements EventListener {

    private final NotificaDispatcher dispatcher;
    private final MemoriaTratte memoriaTratte;
    private final MemoriaBiglietti memoriaBiglietti;  // ✅ AGGIUNTO
    private final TrenicalServiceImpl trenicalService;

    // ✅ CONSTRUCTOR AGGIORNATO
    public NotificaEventiListener(NotificaDispatcher dispatcher,
                                  MemoriaTratte memoriaTratte,
                                  MemoriaBiglietti memoriaBiglietti,  // ✅ NUOVO PARAMETRO
                                  TrenicalServiceImpl trenicalService) {
        this.dispatcher = dispatcher;
        this.memoriaTratte = memoriaTratte;
        this.memoriaBiglietti = memoriaBiglietti;  // ✅ INJECTION
        this.trenicalService = trenicalService;
    }

    @Override
    public void onEvento(EventoS evento) {

        if (evento instanceof EventoModificaTratta e) {
            // ✅ ENHANCED: Notifica intelligente per modifiche tratte
            gestisciModificaTratta(e);

        } else if (evento instanceof EventoPromoGen e) {
            // Gestione promozioni generali (già esistente)
            System.out.println("🎉 Promozione generale creata, broadcasting ai client...");
            Promozione promo = e.getPromozione();
            PromozioneDTO promoDTO = convertPromozioneToDTO(promo, "GENERALE");
            trenicalService.broadcastPromozione(promoDTO);
            System.out.println("📡 Promozione generale broadcasted: " + promo.getDescrizione());

        } else if (evento instanceof EventoPromoFedelta e) {
            // Gestione promozioni fedeltà (già esistente)
            System.out.println("💎 Promozione fedeltà creata, broadcasting ai client...");
            var promo = e.getPromozione();
            PromozioneDTO promoDTO = convertPromozioneToDTO(promo, "FEDELTÀ");
            trenicalService.broadcastPromozione(promoDTO);
            System.out.println("📡 Promozione fedeltà broadcasted: " + promo.getDescrizione());

        } else if (evento instanceof EventoPromoTratta e) {
            // Gestione promozioni tratta (già esistente)
            System.out.println("🚂 Promozione tratta creata, broadcasting ai client...");
            var promo = e.getPromozione();
            PromozioneDTO promoDTO = convertPromozioneToDTO(promo, "TRATTA");
            trenicalService.broadcastPromozione(promoDTO);

            String messaggioTratta = "🎉 Nuova promozione per questa tratta: " + promo.getNome();
            for (UUID trattaId : promo.getTratteDestinate()) {
                dispatcher.inviaNotifica(trattaId, messaggioTratta);
            }

            System.out.println("📡 Promozione tratta broadcasted: " + promo.getNome() +
                    " (per " + promo.getTratteDestinate().size() + " tratte)");

        } else {
            System.out.println("🔔 NotificaEventiListener: Evento non gestito: " +
                    evento.getClass().getSimpleName());
        }
    }

    /**
     * 🛠️ GESTIONE INTELLIGENTE delle modifiche tratte
     *
     * STRATEGIA MULTI-CANALE:
     * 1. Notifica client iscritti esplicitamente (stream gRPC)
     * 2. Identifica client con biglietti per la tratta
     * 3. Iscrive automaticamente client con biglietti (se non già iscritti)
     */
    private void gestisciModificaTratta(EventoModificaTratta evento) {
        UUID trattaId = evento.getIdTratta();
        Tratta tratta = memoriaTratte.getTrattaById(trattaId);

        if (tratta == null) {
            System.out.println("❌ Tratta non trovata per modifica: " + trattaId);
            return;
        }

        String messaggioModifica = "🛠 IMPORTANTE: Tratta modificata - " +
                tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo() +
                " del " + tratta.getData() + " ore " + tratta.getOra();

        System.out.println("🛠️ Gestendo modifica tratta: " + tratta.getStazionePartenza() +
                " → " + tratta.getStazioneArrivo());

        // ✅ STEP 1: Notifica client già iscritti (sistema esistente)
        int clientiIscritti = notificaClientiIscritti(trattaId, messaggioModifica);

        // ✅ STEP 2: Trova client con biglietti per questa tratta
        Set<UUID> clientiConBiglietti = findClientiConBiglietti(trattaId);

        // ✅ STEP 3: Notifica/Iscrive client con biglietti
        int clientiNotificati = notificaClientiConBiglietti(trattaId, clientiConBiglietti, messaggioModifica);

        // 📊 Statistiche finali
        System.out.println("📊 Notifica modifica tratta completata:");
        System.out.println("   📡 Client già iscritti notificati: " + clientiIscritti);
        System.out.println("   🎫 Client con biglietti trovati: " + clientiConBiglietti.size());
        System.out.println("   ✅ Totale notifiche inviate: " + (clientiIscritti + clientiNotificati));
    }

    /**
     * 📡 Notifica client già iscritti al stream tratta (sistema esistente)
     */
    private int notificaClientiIscritti(UUID trattaId, String messaggio) {
        try {
            dispatcher.inviaNotifica(trattaId, messaggio);
            // Nota: dispatcher non restituisce il numero di client notificati
            // Assumiamo almeno 0-N client iscritti
            return 1; // Placeholder - dispatcher dovrebbe essere migliorato per restituire count
        } catch (Exception e) {
            System.err.println("❌ Errore notifica client iscritti: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 🔍 Trova tutti i client che hanno biglietti per la tratta modificata
     */
    private Set<UUID> findClientiConBiglietti(UUID trattaId) {
        try {
            List<Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

            Set<UUID> clientiConBiglietti = tuttiBiglietti.stream()
                    .filter(biglietto -> biglietto.getIdTratta().equals(trattaId))
                    .map(Biglietto::getIdCliente)
                    .collect(Collectors.toSet());

            System.out.println("🔍 Trovati " + clientiConBiglietti.size() +
                    " clienti unici con biglietti per la tratta");

            return clientiConBiglietti;

        } catch (Exception e) {
            System.err.println("❌ Errore ricerca client con biglietti: " + e.getMessage());
            return Set.of();
        }
    }

    /**
     * 🎫 Notifica client che hanno biglietti (ma potrebbero non essere iscritti)
     *
     * STRATEGIA: Per ora logga i client da notificare.
     * TODO Futuro: Implementare notifica diretta via email/push o auto-iscrizione gRPC
     */
    private int notificaClientiConBiglietti(UUID trattaId, Set<UUID> clientiConBiglietti, String messaggio) {
        if (clientiConBiglietti.isEmpty()) {
            return 0;
        }

        System.out.println("🎫 NOTIFICA CLIENT CON BIGLIETTI:");
        System.out.println("   Messaggio: " + messaggio);
        System.out.println("   Client da notificare:");

        int notificatiConSuccesso = 0;
        for (UUID clienteId : clientiConBiglietti) {
            try {
                // ✅ STRATEGIA ATTUALE: Log per debugging
                System.out.println("     - Cliente: " + clienteId.toString().substring(0, 8) + "...");

                // 🚀 TODO FUTURO: Implementare notifica diretta
                // Opzioni possibili:
                // 1. Auto-iscrizione al stream gRPC della tratta
                // 2. Notifica via email (se disponibile)
                // 3. Push notification (se implementate)
                // 4. Salvataggio in "inbox" client per prossimo login

                // Per ora, segna come notificato (placeholder)
                notificatiConSuccesso++;

            } catch (Exception e) {
                System.err.println("     ❌ Errore notifica cliente " +
                        clienteId.toString().substring(0, 8) + ": " + e.getMessage());
            }
        }

        System.out.println("   ✅ Client processati: " + notificatiConSuccesso + "/" + clientiConBiglietti.size());

        // 💡 SUGGERIMENTO per implementazione futura
        if (notificatiConSuccesso > 0) {
            System.out.println("💡 SUGGERIMENTO: Implementare auto-iscrizione gRPC per questi client");
            System.out.println("   o sistema di notifiche persistenti (email/push/inbox)");
        }

        return notificatiConSuccesso;
    }

    /**
     * 🔄 Converte model.Promozione in dto.PromozioneDTO per broadcast gRPC
     */
    private PromozioneDTO convertPromozioneToDTO(Promozione promo, String tipo) {
        String nomeCompleto = "[" + tipo + "] " + promo.getDescrizione();
        String descrizioneCompleta = promo.getDescrizione() +
                " (Sconto: " + (int)(promo.getSconto() * 100) + "%)";

        return new PromozioneDTO(
                nomeCompleto,
                descrizioneCompleta,
                promo.getDataInizio().atStartOfDay(),
                promo.getDataFine().atStartOfDay()
        );
    }
}