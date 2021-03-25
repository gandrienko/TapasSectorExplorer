package ui;

import data_manage.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.util.ArrayList;

public class SectorShowCanvas extends JPanel implements MouseListener, MouseMotionListener {
  public static final int secondsinDay=86400;
  
  public static Color
      focusSectorColor=Color.red.darker(),
      focusSectorBkgColor=new Color(128,0,0,30),
      fromSectorColor=Color.cyan.darker().darker(),
      fromSectorBkgColor=new Color(0,128,128,30),
      toSectorColor=Color.blue.darker(),
      toSectorBkgColor=new Color(0,0,128,30);
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * The sector that is now in focus
   */
  public OneSectorData sInFocus=null;
  /**
   * The sectors from which flights are coming to the focus sector
   * and the sectors to which flights are going from this sector.
   */
  public SectorSet fromSectors=null, toSectors=null;
  /**
   * The "from" and "to" sectors sorted by the numbers of the flights.
   */
  public ArrayList<OneSectorData> fromSorted=null, toSorted=null;
  /**
   * Listeners of selections
   */
  protected ArrayList<ActionListener> listeners=null;
  /**
   * Used for drawing and calculating positions for times
   */
  protected int tMarg=10, tWidth=0, tStep=0;
  protected int yMarg=0, plotH=0;
  protected int hFocus=0, hOther=0, vSpace=0, hFrom=0, hTo=0;
  /**
   * Each element draws a line or polygon representing a single flight
   */
  protected FlightDrawer flightDrawers[]=null;
  
  protected BufferedImage off_Image=null;
  
  
  public SectorShowCanvas(SectorSet sectors) {
    super();
    this.sectors=sectors;
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.round(0.85f*size.width), Math.round(0.85f*size.height)));
    setBorder(BorderFactory.createLineBorder(Color.YELLOW,1));
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    addMouseListener(this);
    addMouseMotionListener(this);
  }
  
  public void setFocusSector(String sectorId) {
    flightDrawers=null;
    if (sectors==null) return;
    if (sectorId==null) {
      sInFocus = null;
      fromSectors=toSectors=null;
      fromSorted=toSorted=null;
    }
    else
      sInFocus=sectors.getSectorData(sectorId);
    if (sInFocus!=null && sInFocus.sortedFlights!=null && !sInFocus.sortedFlights.isEmpty()) {
      fromSectors=new SectorSet();
      toSectors=new SectorSet();
      for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
        FlightInSector f=sInFocus.sortedFlights.get(i);
        if (f.prevSectorId!=null) {
          FlightInSector ff=null;
          OneSectorData s=sectors.getSectorData(f.prevSectorId);
          if (s!=null)
            ff=s.getFlightData(f.flightId,f.entryTime,null);
          if (ff!=null) {
            s = fromSectors.getSectorData(ff.sectorId);
            if (s == null) {
              s = new OneSectorData();
              s.sectorId = ff.sectorId;
              fromSectors.addSector(s);
            }
            s.addFlight(ff);
          }
          ff=null;
          s=sectors.getSectorData(f.nextSectorId);
          if (s!=null)
            ff=s.getFlightData(f.flightId,null,f.exitTime);
          if (ff!=null) {
            s = toSectors.getSectorData(ff.sectorId);
            if (s == null) {
              s = new OneSectorData();
              s.sectorId = ff.sectorId;
              toSectors.addSector(s);
            }
            s.addFlight(ff);
          }
        }
      }
      fromSorted=fromSectors.getSectorsSortedByNFlights();
      toSorted=toSectors.getSectorsSortedByNFlights();
    }
    repaint();
  }
  
  public int getXPos(LocalTime t, int width) {
    if (t==null)
      return -1;
    int tSinceMidnight=t.getHour()*3600+t.getMinute()*60+t.getSecond();
    return Math.round(((float)width)*tSinceMidnight/secondsinDay);
  }
  
  public void paintComponent(Graphics gr) {
    int w=getWidth(), h=getHeight();
    if (off_Image!=null) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h)
        off_Image=null;
      else {
        gr.drawImage(off_Image,0,0,null);
        return;
      }
    }
    
    off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
    
    yMarg=g.getFontMetrics().getHeight();
    plotH=h-2*yMarg;
    int asc=g.getFontMetrics().getAscent();
    
    g.setColor(Color.lightGray);
    g.fillRect(0,0,w,h);
    
    tWidth=w-2*tMarg;
    tStep=tWidth/24;
    
    g.setColor(Color.darkGray);
    for (int i=0; i<=24; i++) {
      int x=tMarg+i*tStep;
      g.drawLine(x,yMarg,x,yMarg+plotH);
      String str=String.format("%02d:00",i);
      int sw=g.getFontMetrics().stringWidth(str);
      g.drawString(str,x-sw/2,yMarg-2);
      g.drawString(str,x-sw/2,h-yMarg+asc);
    }
    if (sInFocus==null)
      return;
    
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    ((Graphics2D)g).setRenderingHints(rh);
    
    int nFrom=(fromSorted==null)?0:fromSorted.size(),
        nTo=(toSorted==null)?0:toSorted.size();
    hFocus=Math.round(0.2f*plotH);
    hOther=(plotH-hFocus)/(nFrom+nTo);
    vSpace=Math.round(0.4f*hOther);
    hOther-=vSpace;
    
    hFrom=nFrom*(hOther+vSpace);
    hTo=nTo*(hOther+vSpace);
    hFocus=plotH-hFrom-hTo;
    if (hFocus<20)
      return;
    
    g.setColor(focusSectorColor);
    int y=yMarg+hFrom;
    g.drawLine(0,y,w,y);
    g.drawLine(0,y+hFocus,w,y+hFocus);
    g.drawString(sInFocus.sectorId+" ("+sInFocus.getNFlights()+")",tMarg,y+asc+2);
    g.setColor(focusSectorBkgColor);
    g.fillRect(0,y,w,hFocus);
    
    for (int i=0; i<nFrom; i++) {
      y=yMarg+i*(hOther+vSpace);
      g.setColor(fromSectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      OneSectorData s=fromSorted.get(nFrom-i-1);
      g.drawString(s.sectorId+" ("+s.getNFlights()+")",tMarg,y+asc+2);
      g.setColor(fromSectorBkgColor);
      g.fillRect(0,y,w,hOther);
    }
    
    for (int i=0; i<nTo; i++) {
      OneSectorData s=toSorted.get(i);
      y=yMarg+hFrom+hFocus+vSpace+i*(hOther+vSpace);
      g.setColor(toSectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      g.drawString(s.sectorId+" ("+s.getNFlights()+")",tMarg,y+asc+2);
      g.setColor(toSectorBkgColor);
      g.fillRect(0,y,w,hOther);
    }
    
    if (flightDrawers==null) {
      flightDrawers=new FlightDrawer[sInFocus.sortedFlights.size()];
      for (int i=0; i<flightDrawers.length; i++) {
        flightDrawers[i]=new FlightDrawer();
        flightDrawers[i].flightId=sInFocus.sortedFlights.get(i).flightId;
      }
    }
  
    y=yMarg+hFrom;
    for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
      FlightInSector f=sInFocus.sortedFlights.get(i);
      int x1=getXPos(f.entryTime,tWidth)+tMarg, x2=getXPos(f.exitTime,tWidth)+tMarg;
      flightDrawers[i].setXYFocus(x1,x2,y,y+hFocus);
      flightDrawers[i].setNoPrevious();
      flightDrawers[i].setNoNext();
      FlightInSector ff=null;
      if (f.prevSectorId!=null && fromSectors!=null) {
        OneSectorData s=fromSectors.getSectorData(f.prevSectorId);
        ff=(s==null)?null:s.getFlightData(f.flightId,f.entryTime,null);
        if (ff!=null) {
          int sIdx=-1;
          for (int j=0; j<fromSorted.size() && sIdx<0; j++)
            if (s.sectorId.equals(fromSorted.get(j).sectorId))
              sIdx=j;
          if (sIdx>=0) {
            int yy=yMarg+(nFrom-sIdx-1)*(hOther+vSpace);
            int xx1=getXPos(ff.entryTime,tWidth)+tMarg, xx2=getXPos(ff.exitTime,tWidth)+tMarg;
            flightDrawers[i].setXYPrevious(xx1,xx2,yy,yy+hOther);
          }
        }
      }
      if (f.nextSectorId!=null && toSectors!=null) {
        OneSectorData s=toSectors.getSectorData(f.nextSectorId);
        ff=(s==null)?null:s.getFlightData(f.flightId,null,f.exitTime);
        if (ff!=null) {
          int sIdx=-1;
          for (int j=0; j<toSorted.size() && sIdx<0; j++)
            if (s.sectorId.equals(toSorted.get(j).sectorId))
              sIdx=j;
          if (sIdx>=0) {
            int yy=yMarg+hFrom+hFocus+vSpace+sIdx*(hOther+vSpace);
            int xx1=getXPos(ff.entryTime,tWidth)+tMarg, xx2=getXPos(ff.exitTime,tWidth)+tMarg;
            flightDrawers[i].setXYNext(xx1,xx2,yy,yy+hOther);
          }
        }
      }
    }
    for (int i=0; i<flightDrawers.length; i++)
      flightDrawers[i].draw(g);
    gr.drawImage(off_Image,0,0,null);
  }
  
  public void redraw() {
    paintComponent(getGraphics());
  }
  
  public void addActionListener(ActionListener l){
    if (l==null) return;
    if (listeners==null)
      listeners=new ArrayList<ActionListener>(10);
    if (!listeners.contains(l))
      listeners.add(l);
  }
  
  public void removeActionListener(ActionListener l) {
    if (l==null || listeners==null)
      return;
    listeners.remove(l);
  }
  
  public void sendActionEvent(String command) {
    if (listeners==null || listeners.isEmpty())
      return;
    ActionEvent ae=new ActionEvent(this,ActionEvent.ACTION_PERFORMED,command);
    for (int i=0; i<listeners.size(); i++)
      listeners.get(i).actionPerformed(ae);
  }
  
  /**
   * Determines in what sector the given vertical position fits.
   * @param yPos - vertical position (particularly, of the mouse cursor)
   * @return array of 2 elements:
   *   [0]: -1 (previous sector), 0 (focus sector), or 1 (next sector)
   *   [1]: index of the sector in the corresponding list
   */
  protected int[] getSectorIdx(int yPos){
    if (yPos<=yMarg || yPos>=yMarg+plotH)
      return null;
    if (yPos>yMarg+hFrom && yPos<=yMarg+hFrom+hFocus) {
      int is[]={0,0};
      return is;
    }
    if (yPos<=yMarg+hFrom) {
      int sIdx=(yPos-yMarg)/(hOther+vSpace);
      if (yPos>yMarg+sIdx*(hOther+vSpace)+hOther) //the mouse is in a vertical space between sectors
        return null;
      int is[]={-1,fromSorted.size()-sIdx-1};
      return is;
    }
    if (yPos>yMarg+hFrom+hFocus+vSpace) {
      int sIdx=(yPos-yMarg-hFrom-hFocus)/(hOther+vSpace);
      if (yPos<yMarg+hFrom+hFocus+sIdx*(hOther+vSpace)+vSpace) //the mouse is in a vertical space between sectors
        return null;
      int is[]={1,sIdx};
      return is;
    }
    return null;
  }
  
  public int getFlightIdx(int x,int y){
    if (flightDrawers==null)
      return -1;
    for (int i=0; i<flightDrawers.length; i++)
      if (flightDrawers[i].contains(x,y))
        return i;
    return -1;
  }
  
  protected int hlIdx=-1;
  
  @Override
  public String getToolTipText(MouseEvent me) {
    hlIdx=-1;
    redraw();
    int fIdx=getFlightIdx(me.getX(),me.getY());
    if (fIdx>=0) {
      hlIdx=fIdx;
      flightDrawers[fIdx].drawHighlighted(getGraphics());
      FlightInSector f=sInFocus.sortedFlights.get(fIdx);
      String str="<html><body style=background-color:rgb(255,255,204)>"+"Flight <b>"+f.flightId+"</b><hr>";
      FlightInSector ff=null;
      boolean bTableSTarted=false;
      if (f.prevSectorId!=null && fromSectors!=null) {
        OneSectorData s=fromSectors.getSectorData(f.prevSectorId);
        ff=(s==null)?null:s.getFlightData(f.flightId,f.entryTime,null);
        if (ff!=null) {
          int r=fromSectorColor.getRed(), g=fromSectorColor.getGreen(), b=fromSectorColor.getBlue();
          if (!bTableSTarted) {
            str += "<table border=0>";
            bTableSTarted=true;
          }
          str+="<tr style=\"color:rgb("+r+","+g+","+b+")\"><td>Sector "+ff.sectorId+"</td><td>"+ff.entryTime+".."+ff.exitTime+"</td></tr>";
        }
      }
      int r=focusSectorColor.getRed(), g=focusSectorColor.getGreen(), b=focusSectorColor.getBlue();
      if (!bTableSTarted) {
        str += "<table border=0>";
        bTableSTarted=true;
      }
      str+="<tr style=\"color:rgb("+r+","+g+","+b+")\"><td>Sector "+f.sectorId+"</td><td>"+f.entryTime+".."+f.exitTime+"</td></tr>";
      if (f.nextSectorId!=null && toSectors!=null) {
        OneSectorData s=toSectors.getSectorData(f.nextSectorId);
        ff=(s==null)?null:s.getFlightData(f.flightId,null,f.exitTime);
        if (ff!=null) {
          r=toSectorColor.getRed(); g=toSectorColor.getGreen(); b=toSectorColor.getBlue();
          if (!bTableSTarted) {
            str += "<table border=0>";
            bTableSTarted=true;
          }
          str+="<tr style=\"color:rgb("+r+","+g+","+b+")\"><td>Sector "+ff.sectorId+"</td><td>"+ff.entryTime+".."+ff.exitTime+"</td></tr>";
        }
      }
      if (bTableSTarted)
        str+="</table>";
      str+="</body></html>";
      return str;
    }
    int is[]=getSectorIdx(me.getY());
    if (is==null || is[1]<0)
      return null;
    if (is[0]==0)
      return "<html><body style=background-color:rgb(255,255,204)>"+
                 "Sector "+sInFocus.sectorId+": "+sInFocus.getNFlights()+" flights;<br>"+
                 "time range = "+sInFocus.tFirst+".."+sInFocus.tLast+
                 "</body></html>";
    OneSectorData s=(is[0]<0)?fromSorted.get(is[1]):toSorted.get(is[1]),
      sAll=sectors.getSectorData(s.sectorId);
    return "<html><body style=background-color:rgb(255,255,204)>"+
               "Sector "+s.sectorId+":<br>"+s.getNFlights()+" flights "+(
               (is[0]<0)?"go to":"come from")+" sector "+sInFocus.sectorId+";<br>"+
               "time range = "+s.tFirst+".."+s.tLast+";<br>"+
               sAll.getNFlights()+" flights total;<br>" +
               "overall time range = "+sAll.tFirst+".."+sAll.tLast+
               "</body></html>";
  }
  
  //---------------------- MouseListener ----------------------------------------
  
  @Override
  public void mouseClicked(MouseEvent e) {
    hlIdx=-1;
    redraw();
    if (e.getClickCount()==2) {
      int is[]=getSectorIdx(e.getY());
      if (is==null || is[0]==0)
        return;
      if (is[0]==-1)
        sendActionEvent("select_sector:"+fromSorted.get(is[1]).sectorId);
      else
        sendActionEvent("select_sector:"+toSorted.get(is[1]).sectorId);
    }
  }
  
  @Override
  public void mousePressed(MouseEvent e) {
  }
  
  @Override
  public void mouseReleased(MouseEvent e) {}
  
  @Override
  public void mouseEntered(MouseEvent e) {}
  
  @Override
  public void mouseExited(MouseEvent e) {
    hlIdx=-1;
    redraw();
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {}
  
  @Override
  public void mouseMoved(MouseEvent me) {
    hlIdx=-1;
    redraw();
    int fIdx=getFlightIdx(me.getX(),me.getY());
    if (fIdx>=0)
      flightDrawers[fIdx].drawHighlighted(getGraphics());
  }
}
