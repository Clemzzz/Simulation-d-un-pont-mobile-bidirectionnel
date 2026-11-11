package Pont;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Pont {
    private EtatPont etat;
    private int nbVehiculesEngages;
    private Segment[] segments;

    private ReentrantLock verrouGlobal;
    private Condition autoriserEntree;
    private Condition pontVide;

    // Attributs pour la temporisation des bateaux (1 minute = 60000 ms)
    private long heureFermetureComplete;
    private long heureProchaineOuverture;
    private Timer timerOuverture;
    private Queue<?> fileAttenteFIFO;

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

        this.heureFermetureComplete = 0;
        this.heureProchaineOuverture = 0;
        this.timerOuverture = new Timer();
        this.fileAttenteFIFO = null;
    }

    // 1. Barrière d'entrée des véhicules
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

    // 2. Détecteur de pont vide à la sortie d'un véhicule
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

    // 3. Transition vers les bateaux (ouverture du pont)
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

    // 4. Relance du trafic terrestre et temporisation d'une minute
    public void fermer() {
        verrouGlobal.lock();
        try {
            this.etat = EtatPont.FERMETURE;
            this.etat = EtatPont.FERME;

            // Enregistrement de l'heure de fermeture et calcul de la prochaine ouverture (+1 min)
            this.heureFermetureComplete = System.currentTimeMillis();
            this.heureProchaineOuverture = this.heureFermetureComplete + 60000;

            // Réveil de tous les véhicules en attente à l'entrée
            autoriserEntree.signalAll();

            // TimerTask exécutée au bout d'une minute (60000 ms)
            timerOuverture.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (fileAttenteFIFO != null && !fileAttenteFIFO.isEmpty()) {
                        try {
                            demanderOuverture();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 60000);

        } finally {
            verrouGlobal.unlock();
        }
    }

    public void setFileAttenteFIFO(Queue<?> fileAttenteFIFO) {
        this.fileAttenteFIFO = fileAttenteFIFO;
    }

    public EtatPont getEtat() {
        return this.etat;
    }

    public int getNbVehiculesEngages() {
        return this.nbVehiculesEngages;
    }

    public Segment getSegment(int index) {
        return this.segments[index];
    }

    public int getNbSegments() {
        return this.segments.length;
    }

    public long getHeureFermetureComplete() {
        return this.heureFermetureComplete;
    }

    public long getHeureProchaineOuverture() {
        return this.heureProchaineOuverture;
    }
}
