package util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class InputUtils {
    public static LocalDate leggiData(Scanner sc, String prompt) {
        System.out.print(prompt);
        return LocalDate.parse(sc.nextLine());
    }

    public static LocalTime leggiOra(Scanner sc, String prompt) {
        System.out.print(prompt);
        return LocalTime.parse(sc.nextLine());
    }

    public static double leggiDouble(Scanner sc, String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(sc.nextLine());
    }

    public static int leggiInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(sc.nextLine());
    }
}