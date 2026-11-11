package Vehicules;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import Pont.Pont;

// Gère qui peut entrer sur le pont
public class ControleCirculation {
    private static final Map<Pont, ControleCirculation> INSTANCES = Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<String, Integer> surPont = new HashMap<>();
    private final Map<String, Integer> enAttente = new HashMap<>();
    private final Map<String, Integer> blocages = new HashMap<>();
    private final Map<String, Integer> prochainTicket = new HashMap<>();
    private final Map<String, Integer> ticketServi = new HashMap<>();
    private final Map<String, Set<Integer>> ticketsAbandonnes = new HashMap<>();
    private String sensPrioritaire = Vehicule.GAUCHE_DROITE;
    private int prioritairesEnApproche = 0;

    // Contrôle du pont
    public static ControleCirculation de(Pont pont) {
        return INSTANCES.computeIfAbsent(pont, p -> new ControleCirculation());
    }

    private int lire(Map<String, Integer> compteurs, String sens) {
        return compteurs.getOrDefault(sens, 0);
    }

    private void ajouter(Map<String, Integer> compteurs, String sens, int delta) {
        compteurs.put(sens, lire(compteurs, sens) + delta);
    }

    // Vérifie si ce sens peut entrer (les deux sens peuvent circuler en même temps sauf blocage par convoi ou prioritaire)
    private boolean peutEntrer(String sens) {
        return prioritairesEnApproche == 0 && lire(blocages, sens) == 0;
    }

    // Attend son tour pour entrer
    public synchronized void entrer(Vehicule v) throws InterruptedException {
        int ticket = lire(prochainTicket, v.sens);
        ajouter(prochainTicket, v.sens, 1);
        ajouter(enAttente, v.sens, 1);
        if (lire(surPont, Vehicule.sensOppose(v.sens)) > 0) {
            sensPrioritaire = v.sens;
        }
        try {
            while (ticket != lire(ticketServi, v.sens) || !peutEntrer(v.sens)) {
                wait();
            }
        } catch (InterruptedException e) {
            ajouter(enAttente, v.sens, -1);
            ticketsAbandonnes.computeIfAbsent(v.sens, s -> new HashSet<>()).add(ticket);
            sauterTicketsAbandonnes(v.sens);
            notifyAll();
            throw e;
        }
        ajouter(enAttente, v.sens, -1);
        ajouter(surPont, v.sens, 1);
        notifyAll();
    }

    // Laisse passer le suivant
    public synchronized void libererPortique(Vehicule v) {
        ajouter(ticketServi, v.sens, 1);
        sauterTicketsAbandonnes(v.sens);
        notifyAll();
    }

    private void sauterTicketsAbandonnes(String sens) {
        Set<Integer> abandonnes = ticketsAbandonnes.get(sens);
        while (abandonnes != null && abandonnes.remove(lire(ticketServi, sens))) {
            ajouter(ticketServi, sens, 1);
        }
    }

    // Sortie du pont
    public synchronized void sortir(Vehicule v) {
        ajouter(surPont, v.sens, -1);
        notifyAll();
    }

    // Bloque un sens
    public synchronized void bloquerEntree(String sens) {
        ajouter(blocages, sens, 1);
        notifyAll();
    }

    // Débloque un sens
    public synchronized void debloquerEntree(String sens) {
        ajouter(blocages, sens, -1);
        notifyAll();
    }

    // Attend que ce sens soit vide
    public synchronized void attendreSensVide(String sens) throws InterruptedException {
        while (lire(surPont, sens) > 0) {
            wait();
        }
    }

    // Un prioritaire arrive
    public synchronized void annoncerPrioritaire() {
        prioritairesEnApproche++;
        notifyAll();
    }

    // Le prioritaire est passé
    public synchronized void retirerPrioritaire() {
        prioritairesEnApproche--;
        notifyAll();
    }

    public synchronized int getNbSurPont(String sens) {
        return lire(surPont, sens);
    }

    public synchronized int getNbEnAttente(String sens) {
        return lire(enAttente, sens);
    }

    public synchronized boolean estBloque(String sens) {
        return lire(blocages, sens) > 0;
    }
}
