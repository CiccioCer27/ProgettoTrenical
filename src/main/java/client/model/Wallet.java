package model;

import eventi.*;
import observer.Observer;
import dto.BigliettoDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Wallet implements Observer {
    private final List<BigliettoDTO> confermati = new ArrayList<>();
    private final List<BigliettoDTO> nonConfermati = new ArrayList<>();

    @Override
    public void aggiorna(Evento evento) {
        System.out.println("💼 DEBUG WALLET: Evento ricevuto - " + evento.getClass().getSimpleName());

        if (evento instanceof EventoAcquisto) {
            System.out.println("💳 DEBUG WALLET: Processando EventoAcquisto");
            BigliettoDTO biglietto = evento.getBigliettoNuovo();
            if (biglietto != null) {
                confermati.add(biglietto);
                System.out.println("✅ DEBUG WALLET: Biglietto aggiunto ai confermati. Totale: " + confermati.size());
            } else {
                System.out.println("❌ DEBUG WALLET: Biglietto null in EventoAcquisto!");
            }

        } else if (evento instanceof EventoPrenota) {
            System.out.println("📝 DEBUG WALLET: Processando EventoPrenota");
            BigliettoDTO biglietto = evento.getBigliettoNuovo();
            if (biglietto != null) {
                nonConfermati.add(biglietto);
                System.out.println("✅ DEBUG WALLET: Prenotazione aggiunta. Totale: " + nonConfermati.size());

                // 🔔 Avvia timer di scadenza per la prenotazione (10 minuti)
                avviaTimerScadenza(biglietto);
            } else {
                System.out.println("❌ DEBUG WALLET: Biglietto null in EventoPrenota!");
            }

        } else if (evento instanceof EventoConferma) {
            System.out.println("✅ DEBUG WALLET: Processando EventoConferma");
            BigliettoDTO bigliettoConfermato = evento.getBigliettoNuovo();

            if (bigliettoConfermato != null) {
                System.out.println("🔍 DEBUG WALLET: Cercando prenotazione da confermare...");
                System.out.println("   ID da confermare: " + bigliettoConfermato.getId());

                // 🔧 PROBLEMA ERA QUI: Cerchiamo per ID della prenotazione originale
                // Il server potrebbe restituire un nuovo biglietto con ID diverso

                // Strategia 1: Prova prima con l'ID esatto
                boolean rimosso = nonConfermati.removeIf(old -> {
                    boolean match = old.getId().equals(bigliettoConfermato.getId());
                    if (match) {
                        System.out.println("✅ DEBUG WALLET: Match trovato per ID: " + old.getId());
                    }
                    return match;
                });

                // Strategia 2: Se non trova per ID, cerca per tratta + cliente (fallback)
                if (!rimosso) {
                    System.out.println("⚠️ DEBUG WALLET: ID non trovato, provo con tratta + cliente");

                    rimosso = nonConfermati.removeIf(old -> {
                        boolean matchTratta = old.getTratta() != null &&
                                bigliettoConfermato.getTratta() != null &&
                                old.getTratta().getId().equals(bigliettoConfermato.getTratta().getId());
                        boolean matchCliente = old.getCliente() != null &&
                                bigliettoConfermato.getCliente() != null &&
                                old.getCliente().getId().equals(bigliettoConfermato.getCliente().getId());

                        boolean match = matchTratta && matchCliente;
                        if (match) {
                            System.out.println("✅ DEBUG WALLET: Match trovato per tratta + cliente");
                        }
                        return match;
                    });
                }

                if (rimosso) {
                    confermati.add(bigliettoConfermato);
                    System.out.println("✅ DEBUG WALLET: Prenotazione confermata e spostata. Confermati: " +
                            confermati.size() + ", Non confermati: " + nonConfermati.size());
                } else {
                    System.out.println("❌ DEBUG WALLET: Prenotazione originale non trovata!");
                    // Aggiungi comunque ai confermati
                    confermati.add(bigliettoConfermato);
                    System.out.println("⚠️ DEBUG WALLET: Aggiunto ai confermati senza rimuovere prenotazione");
                }

            } else {
                System.out.println("❌ DEBUG WALLET: Biglietto confermato è null!");
            }

        } else if (evento instanceof EventoModifica em) {
            System.out.println("🔄 DEBUG WALLET: Processando EventoModifica");
            BigliettoDTO originale = em.getBigliettoOriginale();
            BigliettoDTO nuovo = em.getBigliettoNuovo();

            if (originale != null && nuovo != null) {
                confermati.removeIf(old -> old.getId().equals(originale.getId()));
                confermati.add(nuovo);
                System.out.println("✅ DEBUG WALLET: Biglietto modificato. Totale confermati: " + confermati.size());
            }
        } else {
            System.out.println("⚠️ DEBUG WALLET: Evento non riconosciuto: " + evento.getClass().getSimpleName());
        }
    }

    /**
     * 🔔 Avvia timer per rimuovere automaticamente la prenotazione dopo 10 minuti
     */
    private void avviaTimerScadenza(BigliettoDTO prenotazione) {
        System.out.println("⏰ DEBUG WALLET: Avviando timer scadenza per prenotazione " +
                prenotazione.getId().toString().substring(0, 8) + "...");

        // Timer di 10 minuti (600000 ms)
        new Thread(() -> {
            try {
                Thread.sleep(600000); // 10 minuti

                // Controlla se la prenotazione è ancora presente (non confermata)
                boolean stillExists = nonConfermati.stream()
                        .anyMatch(p -> p.getId().equals(prenotazione.getId()));

                if (stillExists) {
                    System.out.println("⏰ SCADENZA: Rimuovendo prenotazione scaduta " +
                            prenotazione.getId().toString().substring(0, 8) + "...");

                    nonConfermati.removeIf(p -> p.getId().equals(prenotazione.getId()));
                    System.out.println("❌ Prenotazione scaduta rimossa dal wallet");
                } else {
                    System.out.println("✅ Prenotazione " + prenotazione.getId().toString().substring(0, 8) +
                            "... già confermata, timer cancellato");
                }

            } catch (InterruptedException e) {
                System.out.println("⚠️ Timer scadenza interrotto");
            }
        }).start();
    }

    public List<BigliettoDTO> getBigliettiConfermati() {
        System.out.println("📋 DEBUG WALLET: getBigliettiConfermati chiamato. Totale: " + confermati.size());
        return Collections.unmodifiableList(confermati);
    }

    public List<BigliettoDTO> getBigliettiNonConfermati() {
        System.out.println("📋 DEBUG WALLET: getBigliettiNonConfermati chiamato. Totale: " + nonConfermati.size());
        return Collections.unmodifiableList(nonConfermati);
    }
}