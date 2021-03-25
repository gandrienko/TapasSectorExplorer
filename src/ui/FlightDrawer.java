package ui;

import java.awt.*;
import java.awt.geom.Path2D;

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
    highlightColor=Color.yellow;
  
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
  
  public Path2D.Float path=null;
  
  public void setNoPrevious(){
    xPrev[0]=xPrev[1]=yPrev[0]=yPrev[1]=Integer.MIN_VALUE;
  }
  
  public void setNoNext(){
    xNext[0]=xNext[1]=yNext[0]=yNext[1]=Integer.MIN_VALUE;
  }
  
  public void setXYFocus(int x1, int x2, int y1, int y2) {
    xFocus[0]=x1; xFocus[1]=x2;
    yFocus[0]=y1; yFocus[1]=y2;
  }
  
  public void setXYPrevious(int x1, int x2, int y1, int y2) {
    xPrev[0]=x1; xPrev[1]=x2;
    yPrev[0]=y1; yPrev[1]=y2;
  }
  
  public void setXYNext(int x1, int x2, int y1, int y2) {
    xNext[0]=x1; xNext[1]=x2;
    yNext[0]=y1; yNext[1]=y2;
  }
  
  public void draw(Graphics g) {
    path=null;
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
    path=new Path2D.Float();
    if (xPrev[1]>=0) {
      path.moveTo(xPrev[0],yPrev[0]);
      path.lineTo(xPrev[1],yPrev[1]);
      path.lineTo(xFocus[0],yFocus[0]);
    }
    else
      path.moveTo(xFocus[0],yFocus[0]);
    if (xNext[1]>=0) {
      path.lineTo(xNext[0],yNext[0]);
      path.lineTo(xNext[1],yNext[1]);
    }
  }
  
  public boolean contains(int x, int y) {
    if (path==null)
      return false;
    return path.contains(x,y);
  }
  
  public void drawHighlighted(Graphics g) {
    if (path==null)
      return;
    g.setColor(highlightColor);
    Stroke origStroke=((Graphics2D)g).getStroke();
    ((Graphics2D)g).setStroke(thickStroke);
    g.drawLine(xFocus[0],yFocus[0],xFocus[1],yFocus[1]);
    ((Graphics2D)g).setStroke(origStroke);
    if (xPrev[1]>=0) {
      ((Graphics2D)g).setStroke(thickStroke);
      g.drawLine(xPrev[0],yPrev[0],xPrev[1],yPrev[1]);
      ((Graphics2D)g).setStroke(origStroke);
      g.drawLine(xPrev[1],yPrev[1],xFocus[0],yFocus[0]);
    }
    if (xNext[1]>=0) {
      ((Graphics2D)g).setStroke(thickStroke);
      g.drawLine(xNext[0],yNext[0],xNext[1],yNext[1]);
      ((Graphics2D)g).setStroke(origStroke);
      g.drawLine(xFocus[1],yFocus[1],xNext[0],yNext[0]);
    }
  }
}
