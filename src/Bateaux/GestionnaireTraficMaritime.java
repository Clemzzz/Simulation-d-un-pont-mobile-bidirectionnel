package Bateaux;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import Pont.EtatPont;
import Pont.Pont;

// File FIFO des bateaux
public class GestionnaireTraficMaritime {
    private static final long MARGE_TIMER_PONT = 1000;
    private static final long INTERVALLE_VERIFICATION = 100;

    private final Queue<Bateau> fileAttenteFIFO = new ConcurrentLinkedQueue<>();
    private final Timer timerOuverture = new Timer("TimerOuverture", true);
    private final Pont pont;

    private boolean ouvertureLancee = false;
    private boolean fermetureDecidee = false;
    private boolean bateauEnPassage = false;
    private boolean tacheProgrammee = false;

    public GestionnaireTraficMaritime(Pont pont) {
        this.pont = pont;
        pont.setFileAttenteFIFO(fileAttenteFIFO);
    }

    // Ajoute un bateau à la file
    public synchronized void ajouterBateau(Bateau b) {
        fileAttenteFIFO.add(b);
        notifyAll();
        traiterFile();
    }

    // Ouvre le pont si des bateaux attendent
    public synchronized void traiterFile() {
        if (fileAttenteFIFO.isEmpty() || ouvertureLancee || fermetureDecidee || pont.getEtat() != EtatPont.FERME) {
            return;
        }
        long prochaine = pont.getHeureProchaineOuverture();
        long autorisee = prochaine == 0 ? 0 : prochaine + MARGE_TIMER_PONT;
        long attente = autorisee - System.currentTimeMillis();
        if (attente > 0) {
            programmerVerification(attente);
            return;
        }
        ouvertureLancee = true;
        Thread ouverture = new Thread(this::ouvrirPont, "OuverturePont");
        ouverture.setDaemon(true);
        ouverture.start();
    }

    // Réessaie plus tard
    private void programmerVerification(long attente) {
        if (tacheProgrammee) {
            return;
        }
        tacheProgrammee = true;
        timerOuverture.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (GestionnaireTraficMaritime.this) {
                    tacheProgrammee = false;
                    traiterFile();
                }
            }
        }, attente);
    }

    // Ouvre le pont
    private void ouvrirPont() {
        try {
            pont.demanderOuverture();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            notifyAll();
        }
    }

    // Attend son tour et le pont ouvert
    public synchronized void attendreTour(Bateau b) throws InterruptedException {
        while (fileAttenteFIFO.peek() != b || bateauEnPassage || fermetureDecidee
                || pont.getEtat() != EtatPont.OUVERT) {
            wait(INTERVALLE_VERIFICATION);
        }
        bateauEnPassage = true;
    }

    // Ferme le pont si la file est vide
    public void finTraversee(Bateau b) {
        boolean fermer;
        synchronized (this) {
            fileAttenteFIFO.remove(b);
            bateauEnPassage = false;
            fermer = fileAttenteFIFO.isEmpty();
            if (fermer) {
                fermetureDecidee = true;
            }
            notifyAll();
        }
        if (fermer) {
            try {
                pont.fermer();
            } finally {
                synchronized (this) {
                    fermetureDecidee = false;
                    ouvertureLancee = false;
                    notifyAll();
                    traiterFile();
                }
            }
        }
    }

    public synchronized List<Bateau> getFileAttente() {
        return new ArrayList<>(fileAttenteFIFO);
    }

    public synchronized int getNbBateauxEnAttente() {
        return fileAttenteFIFO.size();
    }

    public void arreter() {
        timerOuverture.cancel();
    }
}
