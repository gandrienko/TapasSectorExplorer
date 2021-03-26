package ui;

import java.awt.*;

/**
 * Draws a line or polygon representing a flight
 */
public class FlightDrawer {
  public static Stroke thickStroke=new BasicStroke(3);
  public static Color focusSectorLineColour=new Color(128,0,0,70),
    fromSectorLineColor=new Color(0,128,128,70),
    fromConnectLineColor=new Color(0,128,128,40),
    toSectorLineColor=new Color(0,0,128,70),
    toConnectLineColor=new Color(0,0,128,40),
    highlightColor=Color.yellow,
      highlightBorderColor=new Color(255,255,0,192),
    selectColor=new Color(0,0,0,192),
      selectBorderColor=new Color(0,0,0,128),
    selectCandidateColor=new Color(255,255,255,192);
  
  public String flightId=null;
  /**
   * Screen coordinates of the line segment in the focus sector
   */
  public int xFocus[]={Integer.MIN_VALUE,Integer.MIN_VALUE}, yFocus[]={Integer.MIN_VALUE,Integer.MIN_VALUE};
  /**
   * Screen coordinates of the line segment in the previous sector
   */
  public int xPrev[]={Integer.MIN_VALUE,Integer.MIN_VALUE}, yPrev[]={Integer.MIN_VALUE,Integer.MIN_VALUE};
  /**
   * Screen coordinates of the line segment in the next sector
   */
  public int xNext[]={Integer.MIN_VALUE,Integer.MIN_VALUE}, yNext[]={Integer.MIN_VALUE,Integer.MIN_VALUE};
  
  public Polygon poly=null;
  
  public void setNoPrevious(){
    xPrev[0]=xPrev[1]=yPrev[0]=yPrev[1]=Integer.MIN_VALUE;
  }
  
  public void setNoNext(){
    xNext[0]=xNext[1]=yNext[0]=yNext[1]=Integer.MIN_VALUE;
  }
  
  public void setXYFocus(int x1, int x2, int y1, int y2) {
    if (x1==xFocus[0] && x2==xFocus[1] && y1==yFocus[0] && y2==yFocus[1])
      return;
    poly=null;
    xFocus[0]=x1; xFocus[1]=x2;
    yFocus[0]=y1; yFocus[1]=y2;
  }
  
  public void setXYPrevious(int x1, int x2, int y1, int y2) {
    if (x1==xPrev[0] && x2==xPrev[1] && y1==yFocus[0] && y2==yFocus[1])
      return;
    poly=null;
    xPrev[0]=x1; xPrev[1]=x2;
    yPrev[0]=y1; yPrev[1]=y2;
  }
  
  public void setXYNext(int x1, int x2, int y1, int y2) {
    if (x1==xNext[0] && x2==xNext[1] && y1==yFocus[0] && y2==yFocus[1])
      return;
    poly=null;
    xNext[0]=x1; xNext[1]=x2;
    yNext[0]=y1; yNext[1]=y2;
  }
  
  public void draw(Graphics g) {
    if (xFocus[1]<0)
      return;
    
    Stroke origStroke=((Graphics2D)g).getStroke();
    g.setColor(focusSectorLineColour);
    ((Graphics2D)g).setStroke(thickStroke);
    g.drawLine(xFocus[0],yFocus[0],xFocus[1],yFocus[1]);
    ((Graphics2D)g).setStroke(origStroke);
    if (xPrev[1]>=0) {
      g.setColor(fromSectorLineColor);
      ((Graphics2D)g).setStroke(thickStroke);
      g.drawLine(xPrev[0],yPrev[0],xPrev[1],yPrev[1]);
      ((Graphics2D)g).setStroke(origStroke);
      g.setColor(fromConnectLineColor);
      g.drawLine(xPrev[1],yPrev[1],xFocus[0],yFocus[0]);
    }
    if (xNext[1]>=0) {
      g.setColor(toSectorLineColor);
      ((Graphics2D)g).setStroke(thickStroke);
      g.drawLine(xNext[0],yNext[0],xNext[1],yNext[1]);
      ((Graphics2D)g).setStroke(origStroke);
      g.setColor(toConnectLineColor);
      g.drawLine(xFocus[1],yFocus[1],xNext[0],yNext[0]);
    }
    
    if (poly==null) {
      // points for a polygon
      int npAll=4;
      if (xPrev[1]>=0) 
        npAll+=8;
      if (xNext[1] >= 0)
        npAll+=8;
      int px[] = new int[npAll], py[] = new int[npAll];
      int np = 0, npLast=npAll-1;
      if (xPrev[1] >= 0) {
        px[np] = xPrev[0]-1;
        px[npLast-np] = px[np]+3;
        py[np] = py[npLast-np] = yPrev[0];
        ++np;
        px[np] = xPrev[1]-1;
        px[npLast-np] = px[np]+3;
        py[np] = py[npLast-np] = yPrev[1];
        ++np;
        px[np] = px[npLast-np] = xPrev[1];
        py[np] = py[npLast-np] = yPrev[1];
        ++np;
        px[np] = px[npLast-np] = xFocus[0];
        py[np] = py[npLast-np] = yFocus[0];
        ++np;
      }
      px[np] = xFocus[0]-1;
      px[npLast-np] = px[np]+3;
      py[np] = py[npLast-np] = yFocus[0];
      ++np;
      px[np] = xFocus[1]-1;
      px[npLast-np] = px[np]+3;
      py[np] = py[npLast-np] = yFocus[1];
      ++np;
      if (xNext[1] >= 0) {
        px[np] = px[npLast-np] = xFocus[1];
        py[np] = py[npLast-np] = yFocus[1];
        ++np;
        px[np] = px[npLast-np] = xNext[0];
        py[np] = py[npLast-np] = yNext[0];
        ++np;
        px[np] = xNext[0]-1;
        px[npLast-np] = px[np]+3;
        py[np] = py[npLast-np] = yNext[0];
        ++np;
        px[np] = xNext[1]-1;
        px[npLast-np] = px[np]+3;
        py[np] = py[npLast-np] = yNext[1];
        ++np;
      }
      poly=new Polygon(px,py,npAll);
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
  
  public void drawAsSelectionCandidate(Graphics g) {
    if (poly==null)
      return;
    g.setColor(selectCandidateColor);
    g.drawLine(xFocus[0],yFocus[0],xFocus[1],yFocus[1]);
    if (xPrev[1]>=0) {
      g.drawLine(xPrev[0],yPrev[0],xPrev[1],yPrev[1]);
      g.drawLine(xPrev[1],yPrev[1],xFocus[0],yFocus[0]);
    }
    if (xNext[1]>=0) {
      g.drawLine(xNext[0],yNext[0],xNext[1],yNext[1]);
      g.drawLine(xFocus[1],yFocus[1],xNext[0],yNext[0]);
    }
  }
}
