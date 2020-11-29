import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;

public class Stroke implements Serializable
{
    private static final int NB_POINTS = 32;
    private static final int SQUARE_SIZE = 100;

    ArrayList<Point2D.Double> listePoint;
    Point2D.Double centroid;

    public Stroke()
    {
        listePoint = new ArrayList<Point2D.Double>();
    }

    public Stroke(Stroke s)
    {
        listePoint = new ArrayList<Point2D.Double>();
        for(int i=0;i<s.listePoint.size();i++)
            listePoint.add(s.listePoint.get(i));
    }

    public void init()
    {
        listePoint = null;
        centroid = null;
        listePoint = new ArrayList<Point2D.Double>();
    }

    public boolean isEmpty()
    {
        if(listePoint==null || listePoint.size()==0)
            return true;
        return false;
    }

    public int size()
    {
        return listePoint.size();
    }

    public Point2D.Double getPoint(int i)
    {
        return listePoint.get(i);
    }

    public Point2D.Double getCentroid()
    {
        return centroid;
    }

    public double getCentroidX()
    {
        return centroid.x;
    }

    public double getCentroidY()
    {
        return centroid.y;
    }

    public void addPoint(Point2D.Double p)
    {
        listePoint.add(p);
    }

    public void addPoint(int x, int y)
    {
        listePoint.add(new Point2D.Double(x, y));
    }

    public void normalize()
    {
        resample();
        rotateToZero();
        scaleToSquare();
        translateToOrigin();
    }

    public double getPathLength()
    {
        double dist = 0.0;
        for(int i=1;i<listePoint.size();i++)
        {
            Point2D.Double p0 = listePoint.get(i-1);
            Point2D.Double p1 = listePoint.get(i);
            dist+=p0.distance(p1);
        }
        return dist;
    }

    public ArrayList<Point2D.Double> resample()
    {
        double l = getPathLength()/(NB_POINTS-1);
        double D=0;
        ArrayList<Point2D.Double> newPoints = new ArrayList<Point2D.Double>();
        newPoints.add(listePoint.get(0));
        for(int i=1;i<listePoint.size();i++)
        {
            Point2D.Double p0 = listePoint.get(i-1);
            Point2D.Double p1 = listePoint.get(i);
            double d=p0.distance(p1);
            if(D+d>=l)
            {
                double x = p0.getX()+((l-D)/d)*(p1.getX()-p0.getX());
                double y = p0.getY()+((l-D)/d)*(p1.getY()-p0.getY());
                Point2D.Double q = new Point2D.Double(x,y);
                newPoints.add(q);
                listePoint.add(i, q);
                D=0;
            }
            else
            {
                D+=d;
            }
        }
        if(newPoints.size()<NB_POINTS)
            newPoints.add(listePoint.get(listePoint.size()-1));

        listePoint = newPoints;
        return newPoints;
    }

    public Point2D.Double calculCentroid()
    {
        double x=0.0;
        double y=0.0;
        for(int i=0;i<listePoint.size();i++)
        {
            Point2D.Double p = listePoint.get(i);
            x+=p.getX();
            y+=p.getY();
        }
        Point2D.Double c = new Point2D.Double(x/listePoint.size(),y/listePoint.size());
        return c;
    }

    public ArrayList<Point2D.Double> rotateToZero()
    {
        ArrayList<Point2D.Double> newPoints = new ArrayList<Point2D.Double>();
        centroid = calculCentroid();
        Point2D.Double p0 = listePoint.get(0);
        double theta = Math.atan2(centroid.getY()-p0.getY(),centroid.getX()-p0.getX());
        theta*=-1;
        for(int i=0;i<listePoint.size();i++)
        {
            Point2D.Double p = listePoint.get(i);
            double x = ((p.getX()-centroid.getX())*Math.cos(theta))-((p.getY()-centroid.getY())*Math.sin(theta))+centroid.getX();
            double y = ((p.getX()-centroid.getX())*Math.sin(theta))+((p.getY()-centroid.getY())*Math.cos(theta))+centroid.getY();
            Point2D.Double q = new Point2D.Double(x,y);
            newPoints.add(q);
        }
        listePoint = newPoints;
        return newPoints;
    }

    public Rectangle2D.Double getBoundingBox()
    {
        Point2D.Double p0 = listePoint.get(0);
        double minX = p0.getX();
        double maxX = p0.getX();
        double minY = p0.getY();
        double maxY = p0.getY();
        for(int i=0;i<listePoint.size();i++)
        {
            Point2D.Double p = listePoint.get(i);
            if(minX>p.getX())minX=p.getX();
            if(minY>p.getY())minY=p.getY();
            if(maxX<p.getX())maxX=p.getX();
            if(maxY<p.getY())maxY=p.getY();
        }
        Rectangle2D.Double bb = new Rectangle2D.Double(minX,minY,maxX-minX,maxY-minY);
        return bb;
    }

    public ArrayList<Point2D.Double> scaleToSquare()
    {
        ArrayList<Point2D.Double> newPoints = new ArrayList<Point2D.Double>();
        Rectangle2D.Double boundingBox = getBoundingBox();
        if(boundingBox.getHeight()>10)
        {
            for(int i=0;i<listePoint.size();i++)
            {
                Point2D.Double p = listePoint.get(i);
                double x = (p.getX()-boundingBox.getX())*(SQUARE_SIZE/boundingBox.getWidth());
                double y = (p.getY()-boundingBox.getY())*(SQUARE_SIZE/boundingBox.getHeight());
                Point2D.Double q = new Point2D.Double(x,y);
                newPoints.add(q);
            }
        }
        // Pour lisser une droite ...
        else
        {
            Point2D.Double p0 = listePoint.get(0);
            double y=p0.getY();
            for(int i=0;i<listePoint.size();i++)
            {
                Point2D.Double p = listePoint.get(i);
                double x = (p.getX()-boundingBox.getX())*(SQUARE_SIZE/boundingBox.getWidth());
                Point2D.Double q = new Point2D.Double(x,y);
                newPoints.add(q);
            }
        }
        listePoint = newPoints;
        return newPoints;

    }

    public ArrayList<Point2D.Double> translateToOrigin()
    {
        ArrayList<Point2D.Double> newPoints = new ArrayList<Point2D.Double>();
        centroid = calculCentroid();
        for(int i=0;i<listePoint.size();i++)
        {
            Point2D.Double p = listePoint.get(i);
            double x = p.getX()-centroid.getX()+SQUARE_SIZE;
            double y = p.getY()-centroid.getY()+SQUARE_SIZE;
            Point2D.Double q = new Point2D.Double(x,y);
            newPoints.add(q);
        }
        centroid = new Point2D.Double(SQUARE_SIZE,SQUARE_SIZE);
        listePoint = newPoints;
        return newPoints;
    }

    public ArrayList<Point2D.Double> getPoints()
    {
        return listePoint;
    }
}