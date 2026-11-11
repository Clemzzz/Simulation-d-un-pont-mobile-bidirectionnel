package Pont;

import java.util.List;
import Bateaux.Bateau;
import Vehicules.BandeUrgence;
import Vehicules.Vehicule;
import Vehicules.VehiculePrioritaire;

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

        // Lignes 1 et 2 : Etat général du pont
        System.out.print("\033[1;1HEtat du pont : " + pont.getEtat());
        System.out.print("\033[2;1HVehicules sur le pont : " + pont.getNbVehiculesEngages());

        // Ligne 4 : Sens Gauche -> Droite
        System.out.print("\033[4;1HSens GAUCHE -> DROITE : ");
        for (int i = 0; i < pont.getNbSegments(); i++) {
            Segment s = pont.getSegment("GAUCHE_DROITE", i);
            if (s.estOccupe()) {
                System.out.print("[ V" + s.getIdOccupant() + " -> ] ");
            } else {
                System.out.print("[ Libre ] ");
            }
        }

        // Ligne 5 : Sens Droite -> Gauche
        System.out.print("\033[5;1HSens DROITE -> GAUCHE : ");
        for (int i = 0; i < pont.getNbSegments(); i++) {
            Segment s = pont.getSegment("DROITE_GAUCHE", i);
            if (s.estOccupe()) {
                System.out.print("[ <- V" + s.getIdOccupant() + " ] ");
            } else {
                System.out.print("[ Libre ] ");
            }
        }

        // Ligne 7 : Bande d'urgence
        BandeUrgence bande = BandeUrgence.de(pont);
        List<Vehicule> gares = bande.getVehiculesGares();
        System.out.print("\033[7;1HBande d'urgence       : ");
        if (gares.isEmpty()) {
            System.out.print("Aucun vehicule gare");
        } else {
            for (Vehicule v : gares) {
                System.out.print(v.toString() + " ");
            }
        }

        // Ligne 8 : Véhicules prioritaires
        List<VehiculePrioritaire> prioritaires = bande.getPrioritairesActifs();
        System.out.print("\033[8;1HVehicule prioritaire  : ");
        if (prioritaires.isEmpty()) {
            System.out.print("Aucun");
        } else {
            for (VehiculePrioritaire vp : prioritaires) {
                System.out.print(vp.getCategorie() + "#" + vp.getIdVehicule()
                        + " (" + vp.getSens() + ", segment " + (vp.getPositionActuelle() + 1) + ") ");
            }
        }

        // Ligne 10 : Bateaux (en traversée et en file d'attente FIFO)
        List<?> bateaux = pont.getFileAttenteFIFO();
        System.out.print("\033[10;1HBateaux (file FIFO)   : ");
        if (bateaux.isEmpty()) {
            System.out.print("Aucun bateau");
        } else {
            for (Object obj : bateaux) {
                if (obj instanceof Bateau) {
                    Bateau b = (Bateau) obj;
                    System.out.print("Bateau#" + b.getIdBateau() + "[" + b.getDirection() + " - " + b.getEtat() + "] ");
                } else {
                    System.out.print(obj.toString() + " ");
                }
            }
        }
        System.out.println();

        // Forcer l'affichage pour éviter les scintillements
        System.out.flush();
    }
}
