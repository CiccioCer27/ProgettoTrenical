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
        if (evento instanceof EventoAcquisto) {
            confermati.add(evento.getBigliettoNuovo());
        } else if (evento instanceof EventoPrenota) {
            nonConfermati.add(evento.getBigliettoNuovo());
        } else if (evento instanceof EventoConferma) {
            BigliettoDTO b = evento.getBigliettoNuovo();
            nonConfermati.removeIf(old -> old.getId().equals(b.getId()));
            confermati.add(b);
        } else if (evento instanceof EventoModifica em) {
            confermati.removeIf(old -> old.getId().equals(em.getBigliettoOriginale().getId()));
            confermati.add(em.getBigliettoNuovo());
        }
    }

    public List<BigliettoDTO> getBigliettiConfermati() {
        return Collections.unmodifiableList(confermati);
    }

    public List<BigliettoDTO> getBigliettiNonConfermati() {
        return Collections.unmodifiableList(nonConfermati);
    }
}