package Pont;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Pont {
    private EtatPont etat;
    private int nbVehiculesEngages;
    private final Segment[] segments;

    private final ReentrantLock verrouGlobal;
    private final Condition autoriserEntree;
    private final Condition pontVide;

    public Pont(int nbSegments) {
        this.etat = EtatPont.FERME;
        this.nbVehiculesEngages = 0;
        this.segments = new Segment[nbSegments];
        for (int i = 0; i < nbSegments; i++) {
            this.segments[i] = new Segment(i);
        }

        this.verrouGlobal = new ReentrantLock();
        this.autoriserEntree = this.verrouGlobal.newCondition();
        this.pontVide = this.verrouGlobal.newCondition();
    }

    /**
     * 1. Barrière d'entrée des véhicules
     */
    public void entrer() throws InterruptedException {
        verrouGlobal.lock();
        try {
            while (this.etat != EtatPont.FERME) {
                autoriserEntree.await();
            }
            this.nbVehiculesEngages++;
        } finally {
            verrouGlobal.unlock();
        }
    }

    /**
     * 2. Détecteur de pont vide
     */
    public void sortir() {
        verrouGlobal.lock();
        try {
            this.nbVehiculesEngages--;
            if (this.nbVehiculesEngages == 0) {
                pontVide.signal();
            }
        } finally {
            verrouGlobal.unlock();
        }
    }

    /**
     * 3. Transition vers les bateaux
     */
    public void demanderOuverture() throws InterruptedException {
        verrouGlobal.lock();
        try {
            this.etat = EtatPont.OUVERTURE;
            while (this.nbVehiculesEngages > 0) {
                pontVide.await();
            }
            this.etat = EtatPont.OUVERT;
        } finally {
            verrouGlobal.unlock();
        }
    }

    /**
     * 4. Relance du trafic terrestre
     */
    public void fermer() {
        verrouGlobal.lock();
        try {
            this.etat = EtatPont.FERMETURE;
            this.etat = EtatPont.FERME;
            autoriserEntree.signalAll();
        } finally {
            verrouGlobal.unlock();
        }
    }

    public EtatPont getEtat() {
        verrouGlobal.lock();
        try {
            return this.etat;
        } finally {
            verrouGlobal.unlock();
        }
    }

    public int getNbVehiculesEngages() {
        verrouGlobal.lock();
        try {
            return this.nbVehiculesEngages;
        } finally {
            verrouGlobal.unlock();
        }
    }

    public Segment getSegment(int index) {
        return this.segments[index];
    }

    public int getNbSegments() {
        return this.segments.length;
    }
}
