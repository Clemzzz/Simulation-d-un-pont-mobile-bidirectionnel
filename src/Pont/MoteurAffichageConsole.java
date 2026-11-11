package Pont;

public class MoteurAffichageConsole extends Thread {
    private Pont pont;

    public MoteurAffichageConsole(Pont pont) {
        this.pont = pont;
    }

    @Override
    public void run() {
        try {
            while (true) {
                rafraichirEcran();
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void rafraichirEcran() {
        // Effacer complètement le terminal
        System.out.print("\033[2J");

        // Replacer le curseur en haut à gauche
        System.out.print("\033[H");

        // Affichage de l'état général du pont aux lignes 1 et 2
        System.out.print("\033[1;1HEtat du pont : " + pont.getEtat());
        System.out.print("\033[2;1HVehicules sur le pont : " + pont.getNbVehiculesEngages());

        // Dessin des segments du pont à la ligne 4
        System.out.print("\033[4;1H");
        for (int i = 0; i < pont.getNbSegments(); i++) {
            Segment s = pont.getSegment(i);
            if (s.estOccupe()) {
                System.out.print("[ V" + s.getIdOccupant() + " ] ");
            } else {
                System.out.print("[ Libre ] ");
            }
        }
        System.out.println();

        // Forcer l'affichage pour éviter les scintillements
        System.out.flush();
    }
}
