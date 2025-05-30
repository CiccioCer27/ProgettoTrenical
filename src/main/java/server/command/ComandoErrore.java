package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;

public class ComandoErrore implements ServerCommand {

    private final String messaggio;

    public ComandoErrore(String messaggio) {
        this.messaggio = messaggio;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO richiesta) {
        return new RispostaDTO("KO", messaggio, null);
    }
}