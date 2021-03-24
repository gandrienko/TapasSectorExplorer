package ui;

import data_manage.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;

public class SectorShowCanvas extends JPanel {
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
  
  public SectorShowCanvas(SectorSet sectors) {
    super();
    this.sectors=sectors;
    setPreferredSize(new Dimension(1500, 1000));
    setBorder(BorderFactory.createLineBorder(Color.YELLOW,1));
    ToolTipManager.sharedInstance().registerComponent(this);
  }
  
  public void setFocusSector(String sectorId) {
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
    final int secondsinDay=86400;
    int tSinceMidnight=t.getHour()*3600+t.getMinute()*60+t.getSecond();
    return Math.round(((float)width)*tSinceMidnight/secondsinDay);
  }
  
  public void paintComponent(Graphics g) {
    int w=getWidth(), h=getHeight(),
        yMarg=g.getFontMetrics().getHeight(), plotH=h-2*yMarg,
        asc=g.getFontMetrics().getAscent();
    g.setColor(Color.lightGray);
    g.fillRect(0,0,w,h);
    int tMarg=10;
    int tWidth=w-2*tMarg, tStep=tWidth/24;
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
    
    int nFrom=(fromSorted==null)?0:fromSorted.size(),
        nTo=(toSorted==null)?0:toSorted.size();
    int hFocus=Math.round(0.25f*plotH), hOther=(plotH-hFocus)/(nFrom+nTo);
    int vSpace=Math.round(0.4f*hOther);
    hOther-=vSpace;
    
    int hFrom=nFrom*(hOther+vSpace), hTo=nTo*(hOther+vSpace);
    hFocus=plotH-hFrom-hTo;
    if (hFocus<20)
      return;
    
    g.setColor(Color.red.darker());
    int y=yMarg+hFrom;
    g.drawLine(0,y,w,y);
    g.drawLine(0,y+hFocus,w,y+hFocus);
    g.drawString(sInFocus.sectorId,tMarg,y+asc+2);
    g.setColor(new Color(128,0,0,30));
    g.fillRect(0,y,w,hFocus);
    
    for (int i=0; i<nFrom; i++) {
      y=yMarg+i*(hOther+vSpace);
      g.setColor(Color.cyan.darker().darker());
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      OneSectorData s=fromSorted.get(nFrom-i-1);
      g.drawString(s.sectorId,tMarg,y+asc+2);
      g.setColor(new Color(0,128,128,30));
      g.fillRect(0,y,w,hOther);
    }
    
    for (int i=0; i<nTo; i++) {
      OneSectorData s=toSorted.get(i);
      y=yMarg+hFrom+hFocus+vSpace+i*(hOther+vSpace);
      g.setColor(Color.blue.darker());
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      g.drawString(s.sectorId,tMarg,y+asc+2);
      g.setColor(new Color(0,0,128,30));
      g.fillRect(0,y,w,hOther);
    }
    
    Stroke origStroke=((Graphics2D)g).getStroke(), thickStroke=new BasicStroke(2);
  
    y=yMarg+hFrom;
    for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
      FlightInSector f=sInFocus.sortedFlights.get(i);
      int x1=getXPos(f.entryTime,tWidth)+tMarg, x2=getXPos(f.exitTime,tWidth)+tMarg;
      g.setColor(new Color(128,0,0,100));
      ((Graphics2D)g).setStroke(thickStroke);
      g.drawLine(x1,y,x2,y+hFocus);
      ((Graphics2D)g).setStroke(origStroke);
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
            g.setColor(new Color(0,128,128,100));
            int xx1=getXPos(ff.entryTime,tWidth)+tMarg, xx2=getXPos(ff.exitTime,tWidth)+tMarg;
            ((Graphics2D)g).setStroke(thickStroke);
            g.drawLine(xx1,yy,xx2,yy+hOther);
            ((Graphics2D)g).setStroke(origStroke);
            g.setColor(new Color(0,128,128,50));
            g.drawLine(xx2,yy+hOther,x1,y);
          }
        }
      }
    }
    
    
  }
  
}
