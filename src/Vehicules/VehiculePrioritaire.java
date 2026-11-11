package Vehicules;

import Pont.Pont;

// Ambulance ou pompiers
public class VehiculePrioritaire extends Vehicule {
    public static final int TAILLE = 1;
    public static final int DELAI_PAS = 200;

    private final String categorie;

    public VehiculePrioritaire(int id, String sens, Pont pont, String categorie) {
        super(id, sens, TAILLE, pont, DELAI_PAS);
        this.categorie = categorie;
    }

    public String getCategorie() {
        return categorie;
    }

    @Override
    public String getType() {
        return categorie;
    }

    // Les autres véhicules doivent se garer
    public void signalerPriorite() {
        getBande().declarerUrgence();
    }

    // Traverse pendant que les autres sont garés
    @Override
    protected void traverser() throws InterruptedException {
        ControleCirculation controle = getControle();
        BandeUrgence bande = getBande();
        controle.annoncerPrioritaire();
        try {
            pont.entrer();
            try {
                signalerPriorite();
                try {
                    bande.attendreArretVehicules();
                    bande.ajouterPrioritaireActif(this);
                    try {
                        positionActuelle = 0;
                        while (positionActuelle < pont.getNbSegments()) {
                            sleep(delaiPas);
                            positionActuelle++;
                        }
                    } finally {
                        bande.retirerPrioritaireActif(this);
                    }
                } finally {
                    bande.finUrgence();
                }
            } finally {
                pont.sortir();
            }
        } finally {
            controle.retirerPrioritaire();
        }
    }

    // Ne se gare jamais
    @Override
    protected void verifierUrgence() {
    }

    @Override
    public void cederPassage() {
    }
}
