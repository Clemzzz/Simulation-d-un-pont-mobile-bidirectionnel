import java.util.ArrayList;
import java.util.List;

import Bateaux.Bateau;
import Bateaux.GestionnaireTraficMaritime;
import Pont.MoteurAffichageConsole;
import Pont.Pont;
import Vehicules.Camion;
import Vehicules.ConvoiExceptionnel;
import Vehicules.Vehicule;
import Vehicules.VehiculePrioritaire;
import Vehicules.Voiture;

// Lance la simulation
public class Main {
    private static final int NB_SEGMENTS = 8;

    private static final String GD = Vehicule.GAUCHE_DROITE;
    private static final String DG = Vehicule.DROITE_GAUCHE;

    private static final List<Thread> threads = new ArrayList<>();
    private static int prochainId = 1;

    private static Pont pont;
    private static GestionnaireTraficMaritime gestionnaire;

    // Scénario de démonstration
    public static void main(String[] args) throws InterruptedException {
        pont = new Pont(NB_SEGMENTS);
        gestionnaire = new GestionnaireTraficMaritime(pont);

        MoteurAffichageConsole affichage = new MoteurAffichageConsole(pont);
        affichage.setDaemon(true);
        affichage.start();

        voiture(GD);
        pause(500);
        voiture(DG);
        pause(500);
        camion(GD);
        pause(700);
        voiture(GD);
        pause(700);
        voiture(DG);
        pause(700);
        camion(DG);
        pause(1500);

        convoi(GD);
        pause(300);
        voiture(DG);
        voiture(GD);
        pause(2000);

        prioritaire(DG, "Ambulance");
        pause(3000);

        bateau(Bateau.NORD_SUD);
        pause(300);
        bateau(Bateau.SUD_NORD);
        pause(1000);
        voiture(GD);
        camion(DG);
        pause(12000);

        voiture(GD);
        pause(600);
        camion(GD);
        pause(600);
        voiture(DG);
        pause(1500);
        prioritaire(GD, "Pompiers");
        pause(1000);
        voiture(DG);
        pause(3000);

        bateau(Bateau.SUD_NORD);
        pause(2000);
        convoi(DG);
        pause(1000);
        voiture(GD);
        pause(8000);
        voiture(DG);
        camion(GD);
        pause(10000);
        voiture(GD);

        for (Thread t : threads) {
            t.join();
        }
        pause(1000);

        affichage.marquerTerminee();
        System.out.println("Simulation terminee.");
        gestionnaire.arreter();
    }

    private static void lancer(Thread t) {
        threads.add(t);
        t.start();
    }

    private static void voiture(String sens) {
        lancer(new Voiture(prochainId++, sens, pont));
    }

    private static void camion(String sens) {
        lancer(new Camion(prochainId++, sens, pont));
    }

    private static void convoi(String sens) {
        lancer(new ConvoiExceptionnel(prochainId++, sens, pont));
    }

    private static void prioritaire(String sens, String categorie) {
        lancer(new VehiculePrioritaire(prochainId++, sens, pont, categorie));
    }

    private static void bateau(String direction) {
        lancer(new Bateau(prochainId++, direction, gestionnaire));
    }

    private static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
