package Vehicules;

import pont.Pont;

public abstract class Vehicule extends Thread {
    public int id;
    public String sens;
    public int taille;
    public int positionActuelle;
    public Pont pont;

    public Vehicule(int id, String sens, int taille, Pont pont) {
        this.id = id;
        this.sens = sens;
        this.taille = taille;
        this.pont = pont;
        this.positionActuelle = -1;
    }

    public void run() {
        try {
            pont.entrer(this);
            positionActuelle = taille - 1;
            while (positionActuelle - taille + 1 < pont.getNbSegments()) {
                sleep(500);
                if (pont.urgenceEnCours(this)) {
                    cederPassage();
                }
                avancer();
            }
            pont.sortir(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void avancer() throws InterruptedException {
        int suivant = positionActuelle + 1;
        if (suivant < pont.getNbSegments()) {
            pont.reserverSegment(this, suivant);
        }
        int arriere = positionActuelle - taille + 1;
        if (arriere >= 0) {
            pont.libererSegment(this, arriere);
        }
        positionActuelle = suivant;
    }

    public void cederPassage() throws InterruptedException {
        pont.getBande().garer(this);
    }
}