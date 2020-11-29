import fr.dgac.ivy.Ivy;
import fr.dgac.ivy.IvyException;

import java.awt.geom.Point2D;
import java.util.Timer;
import java.util.TimerTask;

public class IvyFusion {

    enum Etat {
        ATTENTE,
        CREER,
        CREER_ATTENTE_VOIX,
        CREER_ATTENTE_CLIC,
        CREER_ATTENTE_PALETTE,
        DEPLACER,
        DEPLACER_ATTENTE_VOIX,
        DEPLACER_ATTENTE_CLIC,
        DEPLACER_ATTENTE_PALETTE,
        SUPPRIMER,
        SUPPRIMER_ATTENTE_VOIX,
        SUPPRIMER_ATTENTE_CLIC,
        SUPPRIMER_ATTENTE_PALETTE
    }

    enum Forme {
        ELLIPSE,
        RECTANGLE
    }

    Ivy bus;

    Etat etat;
    Forme forme;
    String couleur;
    Point2D.Double position;
    String objet;

    Timer timer;

    Point2D.Double positionClic;

    public IvyFusion() {
        bus = new Ivy("IvyFusion", "", null);

        etat = Etat.ATTENTE;

        try {
            bus.start("127.255.255.255:2010");

            // Gestes
            bus.bindMsg("Geste:Clic x=(.*) y=(.*)", (ivyClient, strings) -> {
                int x = Integer.parseInt(strings[0]);
                int y = Integer.parseInt(strings[1]);
                positionClic = new Point2D.Double(x, y);

                switch (etat) {
                    case CREER:
                        timer.cancel();
                        etat = Etat.CREER_ATTENTE_VOIX;
                        break;

                    case CREER_ATTENTE_CLIC:
                        System.out.println("Position : (" + x + ", " + y + ")");
                        position = new Point2D.Double(positionClic.getX(), positionClic.getY());

                        timer = new Timer();
                        timer.schedule(new CreerObjet(), 3000);

                        etat = Etat.CREER;
                        break;

                    case DEPLACER:
                        etat = Etat.DEPLACER_ATTENTE_VOIX;
                        break;

                    case DEPLACER_ATTENTE_CLIC:
                        System.out.println("Position : (" + x + ", " + y + ")");
                        position = positionClic;
                        if (objet != null) {
                            timer = new Timer();
                            timer.schedule(new DeplacerObjet(), 0);
                        }
                        etat = Etat.DEPLACER;
                        break;

                    case SUPPRIMER:
                        etat = Etat.SUPPRIMER_ATTENTE_VOIX;
                        break;

                    case SUPPRIMER_ATTENTE_CLIC:
                        try {
                            bus.sendMsg("Palette:TesterPoint x=" + x + " y=" + y);
                            etat = Etat.SUPPRIMER_ATTENTE_PALETTE;
                        } catch (IvyException e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        break;
                }
            });

            bus.bindMsg("Geste:Creer(.*)", (ivyClient, strings) -> {
                switch (etat) {
                    case ATTENTE:
                        System.out.println("Créer...");
                        if (strings[0].startsWith("Rectangle")) {
                            System.out.println("Forme : rectangle");
                            forme = Forme.RECTANGLE;
                        } else if (strings[0].startsWith("Ellipse")) {
                            System.out.println("Forme : ellipse");
                            forme = Forme.ELLIPSE;
                        }

                        couleur = "Black";
                        position = new Point2D.Double(0, 0);

                        timer = new Timer();
                        timer.schedule(new CreerObjet(), 3000);

                        etat = Etat.CREER;
                        break;

                    default:
                        break;
                }
            });

            bus.bindMsg("Geste:Deplacer", (ivyClient, strings) -> {
                switch (etat) {
                    case ATTENTE:
                        System.out.println("Déplacer...");
                        etat = Etat.DEPLACER;
                        break;
                    default:
                        break;
                }
            });

            bus.bindMsg("Geste:Supprimer", (ivyClient, strings) -> {
                switch (etat) {
                    case ATTENTE:
                        System.out.println("Supprimer...");
                        etat = Etat.SUPPRIMER;
                        break;
                    default:
                        break;
                }
            });


            // Vocal
            bus.bindMsg("sra5 Parsed=Couleur:(.*) Confidence=(.*)", (ivyClient, strings) -> {
                switch (etat) {
                    case CREER:
                        if (!strings[0].equals("cetteCouleur")) {
                            couleur = strings[0];
                            System.out.println("Couleur : " + couleur);
                        }
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new CreerObjet(), 3000);
                        break;

                    case CREER_ATTENTE_VOIX:
                        if (strings[0].equals("cetteCouleur")) {
                            try {
                                bus.sendMsg("Palette:TesterPoint x=" + (int) positionClic.getX() + " y=" + (int) positionClic.getY());
                                etat = Etat.CREER_ATTENTE_PALETTE;
                            } catch (IvyException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    case DEPLACER:
                    case SUPPRIMER:
                        if (!strings[0].equals("cetteCouleur")) {
                            couleur = strings[0];
                            System.out.println("Couleur : " + couleur);
                        }
                        break;

                    default:
                        break;
                }
            });

            bus.bindMsg("sra5 Parsed=Objet:(.*) Confidence=(.*)", (ivyClient, strings) -> {
                switch (etat) {
                    case DEPLACER_ATTENTE_VOIX:
                        try {
                            if (strings[0].equals("rectangle")) {
                                forme = Forme.RECTANGLE;
                                System.out.println("Forme : Rectangle");
                            } else if (strings[0].equals("ellipse")) {
                                forme = Forme.ELLIPSE;
                                System.out.println("Forme : Ellipse");
                            }
                            bus.sendMsg("Palette:TesterPoint x=" + (int) positionClic.getX() + " y=" + (int) positionClic.getY());
                            etat = Etat.DEPLACER_ATTENTE_PALETTE;
                        } catch (IvyException e) {
                            e.printStackTrace();
                        }
                        break;

                    case SUPPRIMER:
                        if (strings[0].equals("rectangle")) {
                            forme = Forme.RECTANGLE;
                            System.out.println("Forme : Rectangle");
                        } else if (strings[0].equals("ellipse")) {
                            forme = Forme.ELLIPSE;
                            System.out.println("Forme : Ellipse");
                        }
                        etat = Etat.SUPPRIMER_ATTENTE_CLIC;
                        break;

                    case SUPPRIMER_ATTENTE_VOIX:
                        try {
                            if (strings[0].equals("rectangle")) {
                                forme = Forme.RECTANGLE;
                                System.out.println("Forme : Rectangle");
                            } else if (strings[0].equals("ellipse")) {
                                forme = Forme.ELLIPSE;
                                System.out.println("Forme : Ellipse");
                            }
                            bus.sendMsg("Palette:TesterPoint x=" + (int) positionClic.getX() + " y=" + (int) positionClic.getY());
                            etat = Etat.SUPPRIMER_ATTENTE_PALETTE;
                        } catch (IvyException e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        break;
                }
            });

            bus.bindMsg("sra5 Parsed=Position:(.*) Confidence=(.*)", (ivyClient, strings) -> {
                switch (etat) {
                    case CREER:
                        timer.cancel();
                        etat = Etat.CREER_ATTENTE_CLIC;
                        break;

                    case DEPLACER:
                        etat = Etat.DEPLACER_ATTENTE_CLIC;
                        break;

                    default:
                        break;
                }
            });

            bus.bindMsg("sra5 Parsed=Annuler:(.*) Confidence=(.*)", (ivyClient, strings) -> {
                if (etat != Etat.ATTENTE) {
                    System.out.println("Annulé.");
                    reinitialiser();
                }
            });

            // Palette
            bus.bindMsg("Palette:ResultatTesterPoint x=(.*) y=(.*) nom=(.*)", (ivyClient, strings) -> {
                try {
                    bus.sendMsg("Palette:DemanderInfo nom=" + strings[2]);
                } catch (IvyException e) {
                    e.printStackTrace();
                }
            });

            bus.bindMsg("Palette:Info nom=(.*) x=(.*) y=(.*) longueur=(.*) hauteur=(.*) couleurFond=(.*) couleurContour=(.*)", (ivyClient, strings) -> {
                objet = strings[0];

                switch (etat) {
                    case CREER_ATTENTE_PALETTE:
                        couleur = strings[6];
                        System.out.println("Couleur : " + couleur);

                        timer = new Timer();
                        timer.schedule(new CreerObjet(), 3000);

                        etat = Etat.CREER;
                        break;

                    case DEPLACER_ATTENTE_PALETTE:
                        if (couleur != null) {
                            if (!couleur.toLowerCase().equals(strings[6].toLowerCase())) {
                                System.out.println("La couleur ne correspond pas !");
                                reinitialiser();
                                break;
                            }
                        }
                        if (forme != null) {
                            if (forme == Forme.RECTANGLE && !objet.startsWith("R")) {
                                System.out.println("La forme ne correspond pas !");
                                reinitialiser();
                                break;
                            } else if (forme == Forme.ELLIPSE && !objet.startsWith("E")) {
                                System.out.println("La forme ne correspond pas !");
                                reinitialiser();
                                break;
                            }
                        }

                        System.out.println("Objet : " + objet);
                        if (position != null) {
                            timer = new Timer();
                            timer.schedule(new DeplacerObjet(), 0);
                        }
                        etat = Etat.DEPLACER;
                        break;

                    case SUPPRIMER_ATTENTE_PALETTE:
                        if (couleur != null) {
                            if (!couleur.toLowerCase().equals(strings[6].toLowerCase())) {
                                System.out.println("La couleur ne correspond pas !");
                                reinitialiser();
                                break;
                            }
                        }
                        if (forme != null) {
                            if (forme == Forme.RECTANGLE && !objet.startsWith("R")) {
                                System.out.println("La forme ne correspond pas !");
                                reinitialiser();
                                break;
                            } else if (forme == Forme.ELLIPSE && !objet.startsWith("E")) {
                                System.out.println("La forme ne correspond pas !");
                                reinitialiser();
                                break;
                            }
                        }
                        System.out.println("Objet : " + objet);
                        timer = new Timer();
                        timer.schedule(new SupprimerObjet(), 0);

                        etat = Etat.SUPPRIMER;
                        break;

                    default:
                        break;
                }
            });
        } catch (IvyException e) {
            e.printStackTrace();
        }
    }


    void reinitialiser() {
        forme = null;
        couleur = null;
        position = null;
        objet = null;
        positionClic  = null;
        timer = null;

        etat = Etat.ATTENTE;
        System.out.println();
    }


    class CreerObjet extends TimerTask {

        @Override
        public void run() {
            String message = "Palette:";
            if (forme == Forme.RECTANGLE) {
                message += "CreerRectangle ";
            } else if (forme == Forme.ELLIPSE) {
                message += "CreerEllipse ";
            }
            message += "x=" + (int) position.getX() + " y=" + (int) position.getY() + " ";
            message += "longueur=100 hauteur=50 ";
            message += "couleurFond=0:0:0:0 couleurContour=" + couleur;

            try {
                bus.sendMsg(message);
            } catch (IvyException e) {
                e.printStackTrace();
            }
            System.out.println("Terminé.");
            reinitialiser();
        }
    }


    class DeplacerObjet extends TimerTask {

        @Override
        public void run() {
            String message = "Palette:DeplacerObjetAbsolu ";
            message += "nom=" + objet + " ";
            message += "x=" + (int) position.getX() + " y=" + (int) position.getY();

            try {
                bus.sendMsg(message);
            } catch (IvyException e) {
                e.printStackTrace();
            }
            System.out.println("Terminé.");
            reinitialiser();
        }
    }


    class SupprimerObjet extends TimerTask {

        @Override
        public void run() {
            String message = "Palette:SupprimerObjet ";
            message += "nom=" + objet;

            try {
                bus.sendMsg(message);
            } catch (IvyException e) {
                e.printStackTrace();
            }
            System.out.println("Terminé.");
            reinitialiser();
        }
    }


    public static void main(String[] args) {
        new IvyFusion();
    }
}
