package Pont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Bateaux.Bateau;
import Vehicules.BandeUrgence;
import Vehicules.ControleCirculation;
import Vehicules.Vehicule;
import Vehicules.VehiculePrioritaire;

/**
 * Affichage animé dans le terminal (ASCII + codes ANSI) montrant les mouvements
 * des véhicules, des bateaux et l'ouverture/fermeture du pont.
 */
public class MoteurAffichageConsole extends Thread {
    private static final int LARGEUR = 84;
    private static final int HAUTEUR_SCENE = 15;
    private static final int SEG_W = 7;
    private static final int PONT_DEBUT = 14;
    private static final int RIVIERE_DEBUT = 28;
    private static final int RIVIERE_FIN = 55;

    private static final String RESET = "\033[0m";
    private static final String ROUGE = "\033[31m";
    private static final String VERT = "\033[32m";
    private static final String JAUNE = "\033[33m";
    private static final String BLEU = "\033[34m";
    private static final String MAGENTA = "\033[35m";
    private static final String CYAN = "\033[36m";
    private static final String GRAS = "\033[1m";

    private final Pont pont;
    private volatile boolean premiereImage = true;
    private volatile boolean simulationTerminee = false;
    private int tick = 0;

    public MoteurAffichageConsole(Pont pont) {
        super("MoteurAffichageConsole");
        this.pont = pont;
    }

    public void marquerTerminee() {
        this.simulationTerminee = true;
    }

    @Override
    public void run() {
        try {
            while (!simulationTerminee) {
                rafraichirEcran();
                tick++;
                Thread.sleep(150);
            }
            rafraichirEcran();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Réafficher le curseur à la fin
            System.out.print("\033[?25h");
            System.out.flush();
        }
    }

    private void rafraichirEcran() {
        StringBuilder sb = new StringBuilder();
        if (premiereImage) {
            sb.append("\033[2J\033[?25l");
            premiereImage = false;
        }
        sb.append("\033[H");

        EtatPont etat = pont.getEtat();
        BandeUrgence bande = BandeUrgence.de(pont);
        ControleCirculation ctrl = ControleCirculation.de(pont);
        List<Vehicule> gares = bande.getVehiculesGares();
        List<VehiculePrioritaire> prioritaires = bande.getPrioritairesActifs();
        List<?> bateaux = pont.getFileAttenteFIFO();
        boolean urgence = bande.urgenceEnCours();

        Set<Integer> idsGares = new HashSet<>();
        for (Vehicule v : gares) {
            idsGares.add(v.getIdVehicule());
        }

        // Bandeau d'état
        String couleurEtat = etat == EtatPont.FERME ? VERT : (etat == EtatPont.OUVERT ? ROUGE : JAUNE);
        sb.append(GRAS).append("PONT MOBILE").append(RESET)
          .append(" | Etat: ").append(couleurEtat).append(GRAS).append(etat).append(RESET)
          .append(" | Sur pont: ").append(pont.getNbVehiculesEngages())
          .append(" | Attente G->D: ").append(ctrl.getNbEnAttente(Vehicule.GAUCHE_DROITE))
          .append(" D->G: ").append(ctrl.getNbEnAttente(Vehicule.DROITE_GAUCHE));
        if (urgence) {
            sb.append(" | ").append(ROUGE).append(GRAS).append("!! URGENCE !!").append(RESET);
        }
        sb.append("\033[K\n");

        // Grille ASCII de la scène
        char[][] grille = new char[HAUTEUR_SCENE][LARGEUR];
        for (char[] ligne : grille) {
            Arrays.fill(ligne, ' ');
        }

        dessinerRiviereEtBerge(grille);
        dessinerPont(grille, etat, ctrl);
        dessinerVehiculesGares(grille, gares);
        dessinerVehiculesSurVoie(grille, Vehicule.GAUCHE_DROITE, 7, idsGares);
        dessinerVehiculesSurVoie(grille, Vehicule.DROITE_GAUCHE, 9, idsGares);
        dessinerPrioritaires(grille, prioritaires);
        dessinerBateaux(grille, bateaux);

        // Rendu des lignes avec coloration simple
        for (int r = 0; r < HAUTEUR_SCENE; r++) {
            sb.append(colorierLigne(new String(grille[r]), r, etat)).append("\033[K\n");
        }

        // Légende
        sb.append(CYAN).append("[V#>] Voiture").append(RESET).append("  ")
          .append(JAUNE).append("[=C#=>] Camion").append(RESET).append("  ")
          .append(MAGENTA).append("[==CONV#==>] Convoi").append(RESET).append("  ")
          .append(ROUGE).append("[+AMB+/POMP] Prioritaire").append(RESET).append("  ")
          .append(VERT).append("[B#v/^] Bateau").append(RESET)
          .append("\033[K\n");

        System.out.print(sb.toString());
        System.out.flush();
    }

    private void dessinerRiviereEtBerge(char[][] g) {
        int dec = tick % 4;
        for (int r = 0; r < HAUTEUR_SCENE; r++) {
            g[r][RIVIERE_DEBUT - 1] = '|';
            g[r][RIVIERE_FIN + 1] = '|';
            for (int c = RIVIERE_DEBUT + 1; c < RIVIERE_FIN; c += 4) {
                int col = c + ((r + dec) % 2 == 0 ? 0 : 1);
                if (col < RIVIERE_FIN) {
                    g[r][col] = '~';
                }
            }
        }
        ecrire(g, 0, RIVIERE_DEBUT + 8, "NORD (v)");
        ecrire(g, HAUTEUR_SCENE - 1, RIVIERE_DEBUT + 8, "SUD (^)");
    }

    private void dessinerPont(char[][] g, EtatPont etat, ControleCirculation ctrl) {
        int pontFin = PONT_DEBUT + pont.getNbSegments() * SEG_W;

        // Routes d'accès gauche et droite
        for (int c = 0; c < PONT_DEBUT; c++) {
            g[6][c] = '-';
            g[10][c] = '-';
        }
        for (int c = pontFin; c < LARGEUR; c++) {
            g[6][c] = '-';
            g[10][c] = '-';
        }

        // Feux d'entrée
        boolean vertGD = etat == EtatPont.FERME && !ctrl.estBloque(Vehicule.GAUCHE_DROITE);
        boolean vertDG = etat == EtatPont.FERME && !ctrl.estBloque(Vehicule.DROITE_GAUCHE);
        ecrire(g, 7, PONT_DEBUT - 4, vertGD ? "(V)" : "(R)");
        ecrire(g, 9, pontFin + 1, vertDG ? "(V)" : "(R)");

        // Tablier du pont
        for (int c = PONT_DEBUT; c < pontFin; c++) {
            boolean surRiviere = c >= RIVIERE_DEBUT && c <= RIVIERE_FIN;
            if (etat == EtatPont.FERME) {
                g[6][c] = '=';
                g[8][c] = (c % 2 == 0) ? '-' : ' ';
                g[10][c] = '=';
            } else if (etat == EtatPont.OUVERT) {
                if (!surRiviere) {
                    g[6][c] = '=';
                    g[10][c] = '=';
                }
            } else {
                // OUVERTURE ou FERMETURE
                if (!surRiviere) {
                    g[6][c] = '=';
                    g[10][c] = '=';
                } else {
                    g[6][c] = (c < (RIVIERE_DEBUT + RIVIERE_FIN) / 2) ? '/' : '\\';
                    g[10][c] = (c < (RIVIERE_DEBUT + RIVIERE_FIN) / 2) ? '/' : '\\';
                }
            }
        }

        if (etat == EtatPont.OUVERT) {
            for (int r = 5; r <= 11; r++) {
                g[r][RIVIERE_DEBUT - 1] = '|';
                g[r][RIVIERE_FIN + 1] = '|';
            }
        }
    }

    private void dessinerVehiculesSurVoie(char[][] g, String sens, int row, Set<Integer> idsGares) {
        int nb = pont.getNbSegments();
        boolean gd = Vehicule.GAUCHE_DROITE.equals(sens);
        int i = 0;
        while (i < nb) {
            Integer id = pont.getSegment(sens, i).getIdOccupant();
            if (id == null || idsGares.contains(id)) {
                i++;
                continue;
            }
            int debut = i;
            while (i < nb && id.equals(pont.getSegment(sens, i).getIdOccupant())) {
                i++;
            }
            int taille = i - debut;
            int colDebut = PONT_DEBUT + debut * SEG_W;
            int largeur = taille * SEG_W - 1;
            String texte = formaterVehicule(id, taille, gd, largeur);
            ecrire(g, row, colDebut, texte);
        }
    }

    private String formaterVehicule(int id, int taille, boolean gd, int largeur) {
        String corps;
        if (taille >= 3) {
            corps = gd ? ("==CONV" + id + "==>") : ("<==CONV" + id + "==");
        } else if (taille == 2) {
            corps = gd ? ("=C" + id + "=>") : ("<=C" + id + "=");
        } else {
            corps = gd ? ("V" + id + ">") : ("<" + id + "V");
        }
        StringBuilder res = new StringBuilder("[");
        int pad = Math.max(0, largeur - 2 - corps.length());
        int gauche = pad / 2;
        int droite = pad - gauche;
        char fill = taille >= 2 ? '=' : ' ';
        for (int k = 0; k < gauche; k++) res.append(fill);
        res.append(corps);
        for (int k = 0; k < droite; k++) res.append(fill);
        res.append("]");
        return res.toString();
    }

    private void dessinerVehiculesGares(char[][] g, List<Vehicule> gares) {
        int nb = pont.getNbSegments();
        for (Vehicule v : gares) {
            boolean gd = Vehicule.GAUCHE_DROITE.equals(v.getSens());
            int pos = Math.max(0, v.getPositionActuelle());
            int seg = gd ? Math.max(0, pos - v.getTaille() + 1) : Math.max(0, nb - 1 - pos);
            int row = gd ? 5 : 11;
            int col = PONT_DEBUT + Math.min(seg, nb - 1) * SEG_W;
            ecrire(g, row, col, "[G:V" + v.getIdVehicule() + "]");
        }
    }

    private void dessinerPrioritaires(char[][] g, List<VehiculePrioritaire> prioritaires) {
        int nb = pont.getNbSegments();
        for (VehiculePrioritaire vp : prioritaires) {
            boolean gd = Vehicule.GAUCHE_DROITE.equals(vp.getSens());
            int pos = Math.min(Math.max(0, vp.getPositionActuelle()), nb - 1);
            int seg = gd ? pos : (nb - 1 - pos);
            int row = gd ? 7 : 9;
            int col = PONT_DEBUT + seg * SEG_W;
            String tag = vp.getCategorie().toLowerCase().contains("pompier") ? "POMP" : "AMB";
            String txt = gd ? ("+" + tag + ">") : ("<" + tag + "+");
            ecrire(g, row, col, "[" + txt + "]");
        }
    }

    private void dessinerBateaux(char[][] g, List<?> fileBateaux) {
        int attenteNord = 0;
        int attenteSud = 0;
        List<Bateau> liste = new ArrayList<>();
        for (Object obj : fileBateaux) {
            if (obj instanceof Bateau) {
                liste.add((Bateau) obj);
            }
        }
        for (Bateau b : liste) {
            boolean ns = Bateau.NORD_SUD.equals(b.getDirection());
            int col = ns ? (RIVIERE_DEBUT + 3) : (RIVIERE_FIN - 11);
            int row;
            if (b.getEtat() == Bateau.EtatBateau.EN_TRAVERSEE) {
                double ratio = (double) b.getProgression() / Bateau.ETAPES_TRAVERSEE;
                row = ns ? (int) Math.round(3 + ratio * 9) : (int) Math.round(12 - ratio * 9);
            } else {
                if (ns) {
                    row = Math.max(1, 3 - attenteNord);
                    attenteNord++;
                } else {
                    row = Math.min(HAUTEUR_SCENE - 2, 12 + attenteSud);
                    attenteSud++;
                }
            }
            String icone = "[B" + b.getIdBateau() + (ns ? "v" : "^") + "]";
            ecrire(g, row, col, icone);
        }
    }

    private void ecrire(char[][] g, int row, int col, String txt) {
        if (row < 0 || row >= HAUTEUR_SCENE) {
            return;
        }
        for (int i = 0; i < txt.length(); i++) {
            int c = col + i;
            if (c >= 0 && c < LARGEUR) {
                g[row][c] = txt.charAt(i);
            }
        }
    }

    private String colorierLigne(String ligne, int row, EtatPont etat) {
        // Colorisation légère des éléments clés
        String out = ligne
                .replace("~", BLEU + "~" + RESET)
                .replace("(V)", VERT + "(V)" + RESET)
                .replace("(R)", ROUGE + "(R)" + RESET);
        if (row == 5 || row == 11) {
            out = out.replace("[G:", JAUNE + "[G:").replace("]", "]" + RESET);
        }
        if ((row == 6 || row == 10) && etat == EtatPont.OUVERT) {
            out = out.replace("=", ROUGE + "=" + RESET);
        }
        return out;
    }
}
