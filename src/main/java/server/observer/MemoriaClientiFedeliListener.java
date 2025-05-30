package observer;

import eventi.EventoAcquistoCartaFed;
import eventi.EventoS;
import persistence.MemoriaClientiFedeli;

public class MemoriaClientiFedeliListener implements EventListener {

    private final MemoriaClientiFedeli memoria;

    public MemoriaClientiFedeliListener(MemoriaClientiFedeli memoria) {
        this.memoria = memoria;
    }

    @Override
    public void onEvento(EventoS evento) {
        if (evento instanceof EventoAcquistoCartaFed e) {
            memoria.registraClienteFedele(e.getIdCliente());
        }
    }
}