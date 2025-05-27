package model;

import dto.PromozioneDTO;
import eventi.Evento;
import eventi.EventoPromozione;
import observer.Observer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class WalletPromozioni implements Observer {

    private final List<PromozioneDTO> promozioniAttive = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void aggiorna(Evento evento) {
        if (evento instanceof EventoPromozione ep) {
            PromozioneDTO promozione = ep.getPromozione();
            promozioniAttive.add(promozione);
            System.out.println("📢 Aggiunta nuova promozione: " + promozione);

            long secondiAllaFine = Duration.between(LocalDateTime.now(), promozione.getDataFine()).getSeconds();

            if (secondiAllaFine > 0) {
                scheduler.schedule(() -> rimuoviPromozioneScaduta(promozione), secondiAllaFine, TimeUnit.SECONDS);
            } else {
                rimuoviPromozioneScaduta(promozione); // già scaduta
            }
        }
    }

    private void rimuoviPromozioneScaduta(PromozioneDTO promozione) {
        promozioniAttive.removeIf(p -> p.equals(promozione));
        System.out.println("⏳ Promozione scaduta e rimossa: " + promozione.getNome());
    }

    public List<PromozioneDTO> getPromozioniAttive() {
        return Collections.unmodifiableList(promozioniAttive);
    }
}