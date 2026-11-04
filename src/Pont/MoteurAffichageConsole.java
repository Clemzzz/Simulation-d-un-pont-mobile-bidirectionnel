package Pont;

/**
 * Thread dédié à l'affichage et à l'animation du pont dans la console (séquences ANSI).
 */
public class MoteurAffichageConsole extends Thread {
    private final Pont pont;
    private final int delaiRafraichissementMs;

    /**
     * Constructeur avec fréquence de rafraîchissement par défaut (200 ms).
     * @param pont Référence vers le pont à observer
     */
    public MoteurAffichageConsole(Pont pont) {
        this(pont, 200);
    }

    /**
     * Constructeur permettant de personnaliser le délai de rafraîchissement.
     * @param pont Référence vers le pont à observer
     * @param delaiRafraichissementMs Délai en millisecondes entre deux images
     */
    public MoteurAffichageConsole(Pont pont, int delaiRafraichissementMs) {
        this.pont = pont;
        this.delaiRafraichissementMs = delaiRafraichissementMs;
        // Thread démon pour qu'il s'arrête automatiquement à la fin de la simulation
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                rafraichirEcran();
                Thread.sleep(this.delaiRafraichissementMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Nettoie l'écran, repositionne le curseur, interroge l'état du pont et de ses segments,
     * puis force l'affichage dans le terminal.
     */
    private void rafraichirEcran() {
        // 1. Nettoyage complet de l'écran (ANSI)
        System.out.print("\033[2J");

        // 2. Repositionnement du curseur en haut à gauche (ANSI)
        System.out.print("\033[H");

        int nbSegments = pont.getNbSegments();
        EtatPont etat = pont.getEtat();
        int nbEngages = pont.getNbVehiculesEngages();

        // Affichage des informations générales (lignes 1 à 3)
        System.out.print("\033[1;1H=== SIMULATION DU PONT MOBILE ===");
        System.out.print("\033[2;1HEtat du pont       : " + etat);
        System.out.print("\033[3;1HVehicules engages  : " + nbEngages);

        // Dessin de la bordure supérieure du pont (ligne 5)
        System.out.print("\033[5;1H");
        for (int i = 0; i < nbSegments; i++) {
            System.out.print("+-------");
        }
        System.out.print("+");

        // 3. Interrogation de l'état des segments et dessin des véhicules (ligne 6)
        System.out.print("\033[6;1H");
        if (etat == EtatPont.OUVERT) {
            for (int i = 0; i < nbSegments; i++) {
                System.out.print("|  ~~~  ");
            }
            System.out.print("| (Pont ouvert aux bateaux)");
        } else {
            for (int i = 0; i < nbSegments; i++) {
                Segment segment = pont.getSegment(i);
                Integer idOccupant = segment.getIdOccupant();
                if (idOccupant != null) {
                    System.out.printf("| V%-4d ", idOccupant);
                } else {
                    System.out.print("|   .   ");
                }
            }
            System.out.print("|");
        }

        // Dessin de la bordure inférieure du pont (ligne 7)
        System.out.print("\033[7;1H");
        for (int i = 0; i < nbSegments; i++) {
            System.out.print("+-------");
        }
        System.out.print("+");

        // Numérotation des segments S1 à SN (ligne 8)
        System.out.print("\033[8;1H");
        for (int i = 0; i < nbSegments; i++) {
            System.out.printf("   S%-3d ", (i + 1));
        }
        System.out.println();

        // 4. Forcer l'affichage d'un seul coup pour éviter les scintillements
        System.out.flush();
    }
}
