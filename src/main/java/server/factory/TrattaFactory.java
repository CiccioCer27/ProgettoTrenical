package factory;

import model.Tratta;

import java.time.LocalDate;
import java.util.List;

public interface TrattaFactory {
    List<Tratta> generaTratte(LocalDate data);
}