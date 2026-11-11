package Vehicules;

import java.util.concurrent.Semaphore;

import Pont.Pont;

// Convoi : 4 segments, sens opposé bloqué
public class ConvoiExceptionnel extends Vehicule {
    public static final int TAILLE = 4;
    public static final int DELAI_PAS = 800;

    // Un seul convoi à la fois
    private static final Semaphore PASSAGE_CONVOI = new Semaphore(1, true);

    public ConvoiExceptionnel(int id, String sens, Pont pont) {
        super(id, sens, TAILLE, pont, DELAI_PAS);
    }

    // Bloque l'autre sens pendant la traversée
    @Override
    protected void traverser() throws InterruptedException {
        PASSAGE_CONVOI.acquire();
        ControleCirculation controle = getControle();
        String oppose = sensOppose(sens);
        try {
            controle.bloquerEntree(oppose);
            try {
                controle.attendreSensVide(oppose);
                super.traverser();
            } finally {
                controle.debloquerEntree(oppose);
            }
        } finally {
            PASSAGE_CONVOI.release();
        }
    }
}
