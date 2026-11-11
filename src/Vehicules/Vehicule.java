package Vehicules;

import Pont.Pont;
import Pont.Segment;

// Véhicule qui traverse le pont
public abstract class Vehicule extends Thread {
    public static final String GAUCHE_DROITE = "GAUCHE_DROITE";
    public static final String DROITE_GAUCHE = "DROITE_GAUCHE";

    protected static final int DELAI_NOUVEL_ESSAI = 100;

    public int id;
    public String sens;
    public int taille;
    public int positionActuelle;
    public Pont pont;
    protected int delaiPas;

    public Vehicule(int id, String sens, int taille, Pont pont, int delaiPas) {
        super("Vehicule-" + id);
        this.id = id;
        this.sens = sens;
        this.taille = taille;
        this.pont = pont;
        this.delaiPas = delaiPas;
        this.positionActuelle = -1;
    }

    // Renvoie l'autre sens
    public static String sensOppose(String sens) {
        return GAUCHE_DROITE.equals(sens) ? DROITE_GAUCHE : GAUCHE_DROITE;
    }

    public int getIdVehicule() {
        return id;
    }

    public String getSens() {
        return sens;
    }

    public int getTaille() {
        return taille;
    }

    public int getPositionActuelle() {
        return positionActuelle;
    }

    public int getPositionArriere() {
        return positionActuelle - taille + 1;
    }

    public boolean estSurPont() {
        return positionActuelle >= 0 && getPositionArriere() < pont.getNbSegments();
    }

    public boolean estGare() {
        return getBande().estGare(this);
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    protected BandeUrgence getBande() {
        return BandeUrgence.de(pont);
    }

    protected ControleCirculation getControle() {
        return ControleCirculation.de(pont);
    }

    // Segment du pont à cette position
    protected Segment segment(int position) {
        int index = GAUCHE_DROITE.equals(sens) ? position : pont.getNbSegments() - 1 - position;
        return pont.getSegment(index);
    }

    protected boolean reserver(int position) {
        return segment(position).tryReserver(id);
    }

    protected void liberer(int position) {
        segment(position).liberer();
    }

    @Override
    public void run() {
        try {
            traverser();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Traversée du pont
    protected void traverser() throws InterruptedException {
        ControleCirculation controle = getControle();
        BandeUrgence bande = getBande();
        controle.entrer(this);
        try {
            try {
                pont.entrer();
            } catch (InterruptedException e) {
                controle.libererPortique(this);
                throw e;
            }
            try {
                try {
                    reserverSegmentsEntree();
                } finally {
                    controle.libererPortique(this);
                }
                positionActuelle = taille - 1;
                bande.signalerEntree(this);
                try {
                    while (getPositionArriere() < pont.getNbSegments()) {
                        verifierUrgence();
                        sleep(delaiPas);
                        verifierUrgence();
                        avancer();
                    }
                } finally {
                    bande.signalerSortie(this);
                }
            } finally {
                pont.sortir();
            }
        } finally {
            controle.sortir(this);
        }
    }

    // Réserve les premiers segments en une fois
    protected void reserverSegmentsEntree() throws InterruptedException {
        int nb = Math.min(taille, pont.getNbSegments());
        while (true) {
            int obtenus = 0;
            while (obtenus < nb && reserver(obtenus)) {
                obtenus++;
            }
            if (obtenus == nb) {
                return;
            }
            for (int i = 0; i < obtenus; i++) {
                liberer(i);
            }
            sleep(DELAI_NOUVEL_ESSAI);
        }
    }

    // Se gare si un prioritaire passe
    protected void verifierUrgence() throws InterruptedException {
        if (getBande().urgenceEnCours()) {
            cederPassage();
        }
    }

    // Avance d'un segment
    public void avancer() throws InterruptedException {
        int suivant = positionActuelle + 1;
        if (suivant < pont.getNbSegments()) {
            while (!reserver(suivant)) {
                sleep(DELAI_NOUVEL_ESSAI);
                verifierUrgence();
            }
        }
        int arriere = getPositionArriere();
        if (arriere >= 0 && arriere < pont.getNbSegments()) {
            liberer(arriere);
        }
        positionActuelle = suivant;
    }

    // Se gare puis repart
    public void cederPassage() throws InterruptedException {
        BandeUrgence bande = getBande();
        bande.garer(this);
        bande.repartir(this);
    }

    @Override
    public String toString() {
        return getType() + "#" + id + "[" + sens + "]";
    }
}
