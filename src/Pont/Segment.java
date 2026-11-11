package Pont;

import java.util.concurrent.locks.ReentrantLock;

public class Segment {
    private int id;
    private ReentrantLock verrou;
    private Integer idVehiculeOccupant;

    public Segment(int id) {
        this.id = id;
        this.verrou = new ReentrantLock();
        this.idVehiculeOccupant = null;
    }

    // Réservation bloquante (pour une voiture)
    public void reserver(int id) {
        verrou.lock();
        this.idVehiculeOccupant = id;
    }

    // Réservation non bloquante (pour un camion afin d'éviter les interblocages)
    public boolean tryReserver(int id) {
        if (verrou.tryLock()) {
            this.idVehiculeOccupant = id;
            return true;
        }
        return false;
    }

    // Libération du segment quand le véhicule le quitte
    public void liberer() {
        this.idVehiculeOccupant = null;
        verrou.unlock();
    }

    public boolean estOccupe() {
        return this.idVehiculeOccupant != null;
    }

    public Integer getIdOccupant() {
        return this.idVehiculeOccupant;
    }

    public int getId() {
        return this.id;
    }
}
