import fr.dgac.ivy.*;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyIvy {
    HashMap<Stroke, String> dictionnaire;
    int mode;

    static final String dictionnairePath = "gestes";
    static final String[] modes = {"Apprentissage", "Reconnaissance"};

    Ivy bus;
    Stroke stroke;

    public MyIvy() {
        File dictionnaireFile = new File(dictionnairePath);
        if (dictionnaireFile.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(dictionnairePath);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                dictionnaire = (HashMap<Stroke, String>) objectInputStream.readObject();
                objectInputStream.close();
                System.out.println(dictionnaire.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            dictionnaire = new HashMap<>();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(dictionnairePath);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(dictionnaire);
                    objectOutputStream.close();
                    System.out.println("Dictionnaire sauvegardé.");
                } catch (IOException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });

        choisirMode();

        bus = new Ivy("MyIvy", "", null);
        stroke = new Stroke();

        try {
            bus.start("127.0.0.1:2010");
            bus.bindMsg("Palette:MousePressed x=(.*) y=(.*)", new IvyMessageListener() {
                @Override
                public void receive(IvyClient ivyClient, String[] strings) {
                    int x = Integer.parseInt(strings[0]);
                    int y = Integer.parseInt(strings[1]);

                    System.out.println("Clic souris (" + x + ", " + y + ")");

                    stroke.init();
                    stroke.addPoint(x, y);

                    try {
                        bus.sendMsg("Palette:CreerEllipse " +
                                "x=" + x + " y=" + y +
                                " longueur=4 hauteur=4 couleurFond=Green couleurContour=Green");
                    } catch (IvyException e) {
                        e.printStackTrace();
                    }
                }
            });
            bus.bindMsg("Palette:MouseDragged x=(.*) y=(.*)", new IvyMessageListener() {
                @Override
                public void receive(IvyClient ivyClient, String[] strings) {
                    int x = Integer.parseInt(strings[0]);
                    int y = Integer.parseInt(strings[1]);

                    System.out.println("Déplacement souris (" + x + ", " + y + ")");

                    stroke.addPoint(x, y);

                    try {
                        bus.sendMsg("Palette:CreerEllipse " +
                                "x=" + x + " y=" + y +
                                " longueur=2 hauteur=2 couleurFond=Gray couleurContour=Gray");
                    } catch (IvyException e) {
                        e.printStackTrace();
                    }
                }
            });
            bus.bindMsg("Palette:MouseReleased x=(.*) y=(.*)", new IvyMessageListener() {
                @Override
                public void receive(IvyClient ivyClient, String[] strings) {
                    int x = Integer.parseInt(strings[0]);
                    int y = Integer.parseInt(strings[1]);

                    System.out.println("Relachement souris (" + x + ", " + y + ")");

                    stroke.addPoint(x, y);

                    try {
                        bus.sendMsg("Palette:CreerEllipse " +
                                "x=" + x + " y=" + y +
                                " longueur=4 hauteur=4 couleurFond=Red couleurContour=Red");
                    } catch (IvyException e) {
                        e.printStackTrace();
                    }

                    stroke.normalize();
                    ArrayList<Point2D.Double> points = stroke.getPoints();
                    for (Point2D.Double point : points) {
                        try {
                            bus.sendMsg("Palette:CreerEllipse " +
                                    "x=" + (int) point.x + " y=" + (int) point.y +
                                    " longueur=4 hauteur=4 couleurFond=Blue couleurContour=Blue");
                        } catch (IvyException e) {
                            e.printStackTrace();
                        }
                    }

                    // Mode Apprentissage
                    if (mode == 0) {
                        String forme = JOptionPane.showInputDialog(null, "Quelle forme a été tracée ?");
                        if (forme != null) {
                            dictionnaire.put(new Stroke(stroke), forme);
                            System.out.println("Forme apprise : " + forme);
                        }
                    }

                    // Mode Reconnaissance
                    else if (mode == 1) {
                        String formeReconnue = "";
                        double meilleureDistance = Integer.MAX_VALUE;
                        for (Map.Entry<Stroke, String> element : dictionnaire.entrySet()) {
                            Stroke strokeCompare = element.getKey();
                            String forme = element.getValue();

                            ArrayList<Point2D.Double> points1 = strokeCompare.getPoints();

                            double distance = 0d;
                            for (int i = 0; i < points.size(); i++) {
                                distance += points.get(i).distance(points1.get(i));
                            }

                            System.out.println(forme + " (" + distance + ")");

                            if (distance < meilleureDistance) {
                                formeReconnue = forme;
                                meilleureDistance = distance;
                            }
                        }

                        if (formeReconnue.length() > 0)
                            JOptionPane.showMessageDialog(null, "Forme reconnue : " + formeReconnue);
                    }
                }
            });
        } catch (IvyException e) {
            e.printStackTrace();
        }
    }

    void choisirMode() {
        mode = JOptionPane.showOptionDialog(null, "Choisir un mode",
                "Mode", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, modes, modes[0]);
        if (mode == -1) {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        MyIvy myIvy = new MyIvy();
    }
}
