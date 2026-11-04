package Pont;

import java.util.concurrent.locks.ReentrantLock;

public class Segment {
    private final int id;
    private final ReentrantLock verrou;


    //pour affichage console
    private Integer idVehiculeOccupant;


    public Segment(int id, ReentrantLock verrou, Integer idVehiculeOccupant){
        this.id = id;
        this.verrou = new ReentrantLock();
        this.idVehiculeOccupant = null;
    }

    /**
     * Réservation bloquante (Typiquement pour une Voiture)
     * Le thread s'endort ici tant que le verrou n'est pas disponible.
     */
    public void reserver(int idVehicule){
        verrou.lock();
        idVehiculeOccupant = idVehicule;
    }

    /**
     * Réservation conditionnelle (Typiquement pour un Camion)
     * @return true si le verrou a été acquis, false s'il était déjà pris.
     */

    public boolean tryReserver(int idVehicule) {
        if (verrou.tryLock()) {
            idVehiculeOccupant = idVehicule;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Libération du segment par le véhicule sortant
     */

    public void liberer(){
        this.idVehiculeOccupant = null;
        verrou.unlock();
    }

    public boolean estOccupe() {
        return this.idVehiculeOccupant != null;
    }

    public Integer getIdOccupant() {
        return this.idVehiculeOccupant;
    }
}
