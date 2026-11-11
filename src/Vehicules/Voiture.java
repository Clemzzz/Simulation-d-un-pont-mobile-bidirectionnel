package Vehicules;

import Pont.Pont;

// Voiture : 1 segment
public class Voiture extends Vehicule {
    public static final int TAILLE = 1;
    public static final int DELAI_PAS = 400;

    public Voiture(int id, String sens, Pont pont) {
        super(id, sens, TAILLE, pont, DELAI_PAS);
    }
}
