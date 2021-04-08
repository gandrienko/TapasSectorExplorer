package ui;

import java.awt.*;
import java.util.ArrayList;

/**
 * Draws a line or polygon representing a flight
 */
public class FlightDrawer {
  public static Stroke thickStroke=new BasicStroke(3);
  public static Color focusSectorLineColour=new Color(128,80,0,50),
    fromSectorLineColor=new Color(0,128,128,70),
    fromConnectLineColor=new Color(0,128,128,60),
    toSectorLineColor=new Color(0,0,128,60),
    toConnectLineColor=new Color(0,0,128,50),
    highlightColor=Color.yellow,
      highlightBorderColor=new Color(255,255,0,192),
    selectColor=new Color(0,0,0,70),
      selectBorderColor=new Color(0,0,0,40);
  
  public String flightId=null;
  /**
   * Sequence of points representing the path on the screen
   */
  public ArrayList<Point> screenPath=null;
  /**
   * Index of the "focus" segment, when the flight is in the "focus" sector
   */
  public int focusSegmIdx=-1;
  
  public Polygon poly=null;
  
  public void clearPath() {
    if (screenPath!=null)
      screenPath.clear();
    poly=null;
  }
  
  public void addPathSegment (int x1, int x2, int y1, int y2, boolean isInFocus) {
    if (screenPath==null)
      screenPath=new ArrayList<Point>(12);
    if (isInFocus)
      focusSegmIdx=screenPath.size()/2;
    screenPath.add(new Point(x1,y1));
    screenPath.add(new Point(x2,y2));
  }
  
  public void draw (Graphics g) {
    if (screenPath==null || screenPath.isEmpty())
      return;
    Graphics2D g2d=(Graphics2D)g;
    Stroke origStroke=g2d.getStroke();
    
    boolean makePoly=poly==null;
    if (makePoly)
      poly=new Polygon();
    
    for (int k=0; k<screenPath.size(); k+=2) {
      int segmIdx=k/2;
      g2d.setStroke(thickStroke);
      g2d.setColor((segmIdx==focusSegmIdx)?focusSectorLineColour:
                       (segmIdx<focusSegmIdx)?fromSectorLineColor:toSectorLineColor);
      g2d.drawLine(screenPath.get(k).x,screenPath.get(k).y,screenPath.get(k+1).x,screenPath.get(k+1).y);
      g2d.setStroke(origStroke);
      if (makePoly) {
        poly.addPoint(screenPath.get(k).x - 1, screenPath.get(k).y);
        poly.addPoint(screenPath.get(k+1).x - 1, screenPath.get(k+1).y);
      }
      if (k+2<screenPath.size()) { //draw a connecting line
        g2d.setColor((segmIdx>=focusSegmIdx)?toConnectLineColor:fromConnectLineColor);
        g2d.drawLine(screenPath.get(k+1).x,screenPath.get(k+1).y,screenPath.get(k+2).x,screenPath.get(k+2).y);
        if (makePoly) {
          poly.addPoint(screenPath.get(k+1).x, screenPath.get(k+1).y);
          poly.addPoint(screenPath.get(k+2).x, screenPath.get(k+2).y);
        }
      }
    }
    if (makePoly)
      for (int k=screenPath.size()-2; k>=0; k-=2) {
        poly.addPoint(screenPath.get(k+1).x + 1, screenPath.get(k+1).y);
        poly.addPoint(screenPath.get(k).x + 1, screenPath.get(k).y);
        if (k-2>0) {
          poly.addPoint(screenPath.get(k).x, screenPath.get(k).y);
          poly.addPoint(screenPath.get(k-1).x, screenPath.get(k-1).y);
        }
      }
  }
  
  public boolean contains(int x, int y) {
    if (poly==null)
      return false;
    return poly.contains(x,y);
  }
  
  public boolean intersects(int x, int y, int w, int h) {
    if (poly==null)
      return false;
    return poly.intersects(x,y,w,h);
  }
  
  public void drawHighlighted(Graphics g) {
    if (poly==null)
      return;
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    ((Graphics2D)g).setRenderingHints(rh);
    g.setColor(highlightColor);
    g.fillPolygon(poly);
    g.setColor(highlightBorderColor);
    g.drawPolygon(poly);
  }
  
  public void drawSelected(Graphics g) {
    if (poly==null)
      return;
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    ((Graphics2D)g).setRenderingHints(rh);
    g.setColor(selectColor);
    g.fillPolygon(poly);
    g.setColor(selectBorderColor);
    g.drawPolygon(poly);
  }
}
