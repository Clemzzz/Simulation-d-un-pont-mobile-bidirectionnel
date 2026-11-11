package Bateaux;

// Bateau qui passe sous le pont
public class Bateau extends Thread {
    public static final String NORD_SUD = "NORD_SUD";
    public static final String SUD_NORD = "SUD_NORD";

    public static final int ETAPES_TRAVERSEE = 5;
    public static final int DELAI_ETAPE = 500;

    public enum EtatBateau { EN_ATTENTE, EN_TRAVERSEE, PASSE }

    private final int id;
    private final String direction;
    private final GestionnaireTraficMaritime gestionnaire;
    private volatile int progression;
    private volatile EtatBateau etat;

    public Bateau(int id, String direction, GestionnaireTraficMaritime gestionnaire) {
        super("Bateau-" + id);
        this.id = id;
        this.direction = direction;
        this.gestionnaire = gestionnaire;
        this.progression = 0;
        this.etat = EtatBateau.EN_ATTENTE;
    }

    public int getIdBateau() {
        return id;
    }

    public String getDirection() {
        return direction;
    }

    public int getProgression() {
        return progression;
    }

    public EtatBateau getEtat() {
        return etat;
    }

    // Attend son tour puis traverse
    @Override
    public void run() {
        try {
            gestionnaire.ajouterBateau(this);
            gestionnaire.attendreTour(this);
            etat = EtatBateau.EN_TRAVERSEE;
            try {
                while (progression < ETAPES_TRAVERSEE) {
                    sleep(DELAI_ETAPE);
                    progression++;
                }
            } finally {
                etat = EtatBateau.PASSE;
                gestionnaire.finTraversee(this);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return "Bateau#" + id + "[" + direction + "]";
    }
}
