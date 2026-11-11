package Pont;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Pont {
    private EtatPont etat;
    private int nbVehiculesEngages;
    private boolean demandeOuvertureEnCours;

    // Un tableau de segments par sens pour permettre la circulation simultanée dans les deux sens
    private Segment[] segmentsGaucheDroite;
    private Segment[] segmentsDroiteGauche;

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
        this.demandeOuvertureEnCours = false;

        this.segmentsGaucheDroite = new Segment[nbSegments];
        this.segmentsDroiteGauche = new Segment[nbSegments];
        for (int i = 0; i < nbSegments; i++) {
            this.segmentsGaucheDroite[i] = new Segment(i);
            this.segmentsDroiteGauche[i] = new Segment(i);
        }

        this.verrouGlobal = new ReentrantLock();
        this.autoriserEntree = this.verrouGlobal.newCondition();
        this.pontVide = this.verrouGlobal.newCondition();

        this.heureFermetureComplete = 0;
        this.heureProchaineOuverture = 0;
        // Timer en mode démon (true) pour que le programme puisse s'arrêter tout seul
        this.timerOuverture = new Timer(true);
        this.fileAttenteFIFO = null;
    }

    // 1. Barrière d'entrée des véhicules
    public void entrer() throws InterruptedException {
        verrouGlobal.lock();
        try {
            while (this.etat != EtatPont.FERME || this.demandeOuvertureEnCours) {
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
            // Bloque les nouvelles entrées et attend que le pont soit vide avant de passer à OUVERTURE
            this.demandeOuvertureEnCours = true;
            while (this.nbVehiculesEngages > 0) {
                pontVide.await();
            }
            this.etat = EtatPont.OUVERTURE;
        } finally {
            verrouGlobal.unlock();
        }

        // Petite pause pour qu'on voie l'état OUVERTURE à l'écran
        Thread.sleep(1000);

        verrouGlobal.lock();
        try {
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
        } finally {
            verrouGlobal.unlock();
        }

        // Petite pause pour qu'on voie l'état FERMETURE à l'écran
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verrouGlobal.lock();
        try {
            this.etat = EtatPont.FERME;
            this.demandeOuvertureEnCours = false;

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

    public List<?> getFileAttenteFIFO() {
        if (this.fileAttenteFIFO == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.fileAttenteFIFO);
    }

    public EtatPont getEtat() {
        return this.etat;
    }

    public int getNbVehiculesEngages() {
        return this.nbVehiculesEngages;
    }

    // Récupère un segment selon le sens de circulation
    public Segment getSegment(String sens, int index) {
        if ("DROITE_GAUCHE".equals(sens)) {
            return this.segmentsDroiteGauche[index];
        }
        return this.segmentsGaucheDroite[index];
    }

    // Par défaut (sens Gauche -> Droite)
    public Segment getSegment(int index) {
        return this.segmentsGaucheDroite[index];
    }

    public int getNbSegments() {
        return this.segmentsGaucheDroite.length;
    }

    public long getHeureFermetureComplete() {
        return this.heureFermetureComplete;
    }

    public long getHeureProchaineOuverture() {
        return this.heureProchaineOuverture;
    }
}
