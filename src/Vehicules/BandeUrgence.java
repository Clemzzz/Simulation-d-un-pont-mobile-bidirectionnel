package Vehicules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import Pont.Pont;

// Bande où se garent les véhicules
public class BandeUrgence {
    private static final Map<Pont, BandeUrgence> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());

    private final List<Vehicule> vehiculesGares = new ArrayList<>();
    private int urgences = 0;
    private int enCirculation = 0;

    // Bande d'urgence du pont
    public static BandeUrgence de(Pont pont) {
        return INSTANCES.computeIfAbsent(pont, p -> new BandeUrgence());
    }

    public synchronized boolean urgenceEnCours() {
        return urgences > 0;
    }

    // Début du passage d'un prioritaire
    public synchronized void declarerUrgence() {
        urgences++;
        notifyAll();
    }

    // Fin du passage d'un prioritaire
    public synchronized void finUrgence() {
        urgences--;
        notifyAll();
    }

    public synchronized void signalerEntree(Vehicule v) {
        enCirculation++;
        notifyAll();
    }

    public synchronized void signalerSortie(Vehicule v) {
        enCirculation--;
        notifyAll();
    }

    // Attend que tout le monde soit garé
    public synchronized void attendreArretVehicules() throws InterruptedException {
        while (enCirculation > vehiculesGares.size()) {
            wait();
        }
    }

    // Gare le véhicule jusqu'à la fin de l'urgence
    public synchronized void garer(Vehicule v) throws InterruptedException {
        if (!vehiculesGares.contains(v)) {
            vehiculesGares.add(v);
            notifyAll();
        }
        while (urgenceEnCours()) {
            wait();
        }
    }

    // Le véhicule repart
    public synchronized void repartir(Vehicule v) {
        vehiculesGares.remove(v);
        notifyAll();
    }

    public synchronized boolean estGare(Vehicule v) {
        return vehiculesGares.contains(v);
    }

    public synchronized List<Vehicule> getVehiculesGares() {
        return new ArrayList<>(vehiculesGares);
    }
}
