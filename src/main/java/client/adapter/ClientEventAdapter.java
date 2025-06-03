package adapter;

import dto.BigliettoDTO;
import dto.RispostaDTO;
import eventi.*;
import service.ClientService;

/**
 * 🔄 ADAPTER PER EVENTI CLIENT-SERVER (CON DEBUG)
 *
 * Converte le risposte del server in eventi client-side
 * per sincronizzare il wallet con le operazioni
 */
public class ClientEventAdapter {

    /**
     * Processa una risposta del server e genera eventi appropriati
     */
    public static synchronized void processaRisposta(RispostaDTO risposta, String tipoOperazione) {
        System.out.println("🔍 DEBUG ADAPTER (THREAD-SAFE): processaRisposta chiamato");
        System.out.println("   Thread: " + Thread.currentThread().getName());
        System.out.println("   Tipo operazione: " + tipoOperazione);

        if (risposta == null) {
            System.out.println("❌ DEBUG: Risposta è null!");
            return;
        }

        if (!risposta.getEsito().equals("OK")) {
            System.out.println("❌ DEBUG: Esito non OK: " + risposta.getEsito());
            return;
        }

        BigliettoDTO biglietto = risposta.getBiglietto();
        System.out.println("🎫 DEBUG: Biglietto dalla risposta: " + (biglietto != null ? "PRESENTE" : "NULL"));

        if (biglietto == null) {
            System.out.println("❌ DEBUG: Biglietto è null nella risposta!");
            // Proviamo a vedere se ci sono altri dati
            System.out.println("   Messaggio: " + risposta.getMessaggio());
            System.out.println("   Dati: " + risposta.getDati());
            return;
        }

        System.out.println("📋 DEBUG: Dettagli biglietto:");
        System.out.println("   ID: " + biglietto.getId());
        System.out.println("   Cliente: " + (biglietto.getCliente() != null ? biglietto.getCliente().getNome() : "NULL"));
        System.out.println("   Tratta: " + (biglietto.getTratta() != null ? biglietto.getTratta().getStazionePartenza() + "→" + biglietto.getTratta().getStazioneArrivo() : "NULL"));

        // Genera evento appropriato basato sul tipo di operazione
        Evento evento = switch (tipoOperazione.toUpperCase()) {
            case "ACQUISTA" -> {
                System.out.println("🔄 DEBUG: Creando EventoAcquisto");
                yield new EventoAcquisto(biglietto);
            }
            case "PRENOTA" -> {
                System.out.println("🔄 DEBUG: Creando EventoPrenota");
                yield new EventoPrenota(biglietto);
            }
            case "CONFERMA" -> {
                System.out.println("🔄 DEBUG: Creando EventoConferma");
                yield new EventoConferma(biglietto);
            }
            case "MODIFICA" -> {
                System.out.println("🔄 DEBUG: Creando EventoAcquisto per modifica");
                yield new EventoAcquisto(biglietto);
            }
            default -> {
                System.out.println("❌ DEBUG: Tipo operazione non riconosciuto: " + tipoOperazione);
                yield null;
            }
        };

        if (evento != null) {
            System.out.println("✅ DEBUG ADAPTER: Notifica thread-safe in corso...");

            // ✅ SINCRONIZZAZIONE EXTRA per eventi critici
            synchronized (ListaEventi.getInstance()) {
                ListaEventi.getInstance().notifica(evento);
            }

            System.out.println("✅ DEBUG ADAPTER: Notifica thread-safe completata!");
        } else {
            System.out.println("❌ DEBUG: Evento è null, non posso notificare");
        }
    }

    /**
     * Processa modifica biglietto (richiede biglietto originale)
     */
    public static void processaModifica(BigliettoDTO bigliettoOriginale, BigliettoDTO bigliettoNuovo) {
        System.out.println("🔄 DEBUG: processaModifica chiamato");

        if (bigliettoOriginale != null && bigliettoNuovo != null) {
            EventoModifica evento = new EventoModifica(bigliettoOriginale, bigliettoNuovo);
            System.out.println("✅ DEBUG: Evento modifica generato");
            ListaEventi.getInstance().notifica(evento);
            System.out.println("✅ DEBUG: Evento modifica notificato");
        } else {
            System.out.println("❌ DEBUG: Biglietti per modifica sono null");
        }
    }

    /**
     * Metodo helper per debug - conta gli observers (usa reflection)
     */
    private static int getObserverCount(ListaEventi listaEventi) {
        try {
            java.lang.reflect.Field field = ListaEventi.class.getDeclaredField("observers");
            field.setAccessible(true);
            java.util.List<?> observers = (java.util.List<?>) field.get(listaEventi);
            return observers.size();
        } catch (Exception e) {
            return -1; // Non riuscito a contare
        }
    }
}