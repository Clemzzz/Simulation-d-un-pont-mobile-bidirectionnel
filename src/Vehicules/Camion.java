package Vehicules;

import Pont.Pont;

// Camion : 2 segments
public class Camion extends Vehicule {
    public static final int TAILLE = 2;
    public static final int DELAI_PAS = 600;

    public Camion(int id, String sens, Pont pont) {
        super(id, sens, TAILLE, pont, DELAI_PAS);
    }
}
