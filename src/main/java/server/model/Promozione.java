package model;

import java.time.LocalDate;
import java.util.UUID;

public interface Promozione{
    boolean isAttiva(LocalDate data);
    boolean siApplicaAllaTratta(UUID idTratta);
    boolean applicabileSoloCartaFedelta();
    double getSconto();
    String getDescrizione();
    LocalDate getDataInizio();
    LocalDate getDataFine();
}