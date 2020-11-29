import fr.dgac.ivy.Ivy;
import fr.dgac.ivy.IvyException;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IvyGeste {
    HashMap<Stroke, String> dictionnaire;

    static final String dictionnairePath = "gestes";

    Ivy bus;
    Stroke stroke;

    public IvyGeste() {
        File dictionnaireFile = new File(dictionnairePath);

        if (dictionnaireFile.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(dictionnairePath);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                dictionnaire = (HashMap<Stroke, String>) objectInputStream.readObject();
                objectInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Aucun geste n'est défini dans le fichier \"gestes\" ou bien le fichier n'existe pas.");
            System.exit(-1);
        }

        bus = new Ivy("IvyGeste", "", null);
        stroke = new Stroke();

        try {
            bus.start("127.255.255.255:2010");

            bus.bindMsg("Palette:MousePressed x=(.*) y=(.*)", (ivyClient, strings) -> {
                int x = Integer.parseInt(strings[0]);
                int y = Integer.parseInt(strings[1]);

                // System.out.println("Clic souris (" + x + ", " + y + ")");

                stroke.init();
                stroke.addPoint(x, y);

                /*
                try {
                    bus.sendMsg("Palette:CreerEllipse " +
                            "x=" + x + " y=" + y +
                            " longueur=4 hauteur=4 couleurFond=Green couleurContour=Green");
                } catch (IvyException e) {
                    e.printStackTrace();
                }
                 */
            });

            bus.bindMsg("Palette:MouseDragged x=(.*) y=(.*)", (ivyClient, strings) -> {
                int x = Integer.parseInt(strings[0]);
                int y = Integer.parseInt(strings[1]);

                // System.out.println("Déplacement souris (" + x + ", " + y + ")");

                stroke.addPoint(x, y);

                /*
                try {
                    bus.sendMsg("Palette:CreerEllipse " +
                            "x=" + x + " y=" + y +
                            " longueur=2 hauteur=2 couleurFond=Gray couleurContour=Gray");
                } catch (IvyException e) {
                    e.printStackTrace();
                }
                 */
            });

            bus.bindMsg("Palette:MouseReleased x=(.*) y=(.*)", (ivyClient, strings) -> {
                int x = Integer.parseInt(strings[0]);
                int y = Integer.parseInt(strings[1]);

                // System.out.println("Relachement souris (" + x + ", " + y + ")");

                stroke.addPoint(x, y);

                /*
                try {
                    bus.sendMsg("Palette:CreerEllipse " +
                            "x=" + x + " y=" + y +
                            " longueur=4 hauteur=4 couleurFond=Red couleurContour=Red");
                } catch (IvyException e) {
                    e.printStackTrace();
                }
                 */

                if (stroke.getPoints().size() < 3) {
                    try {
                        System.out.println("Geste:Clic x=" + x + " y=" + y);
                        bus.sendMsg("Geste:Clic x=" + x + " y=" + y);
                    } catch (IvyException e) {
                        e.printStackTrace();
                    }
                } else {
                    stroke.normalize();
                    ArrayList<Point2D.Double> points = stroke.getPoints();
                    String gesteReconnu = "";
                    double meilleureDistance = Integer.MAX_VALUE;
                    for (Map.Entry<Stroke, String> element : dictionnaire.entrySet()) {
                        Stroke strokeCompare = element.getKey();
                        String geste = element.getValue();

                        ArrayList<Point2D.Double> points1 = strokeCompare.getPoints();

                        double distance = 0d;
                        for (int i = 0; i < points.size(); i++) {
                            distance += points.get(i).distance(points1.get(i));
                        }

                        if (distance < meilleureDistance) {
                            gesteReconnu = geste;
                            meilleureDistance = distance;
                        }
                    }

                    if (gesteReconnu.length() > 0) {
                        try {
                            System.out.println("Geste:" + gesteReconnu);
                            bus.sendMsg("Geste:" + gesteReconnu);
                        } catch (IvyException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IvyException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new IvyGeste();
    }
}
