package Pont;

/**
 * Énumération représentant les quatre états possibles du pont mobile.
 * Transitions autorisées : FERME -> OUVERTURE -> OUVERT -> FERMETURE -> FERME.
 */
public enum EtatPont {
    FERME,
    OUVERTURE,
    OUVERT,
    FERMETURE
}
