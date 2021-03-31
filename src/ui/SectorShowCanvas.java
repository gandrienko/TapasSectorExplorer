package ui;

import data_manage.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.util.ArrayList;

public class SectorShowCanvas extends JPanel implements MouseListener, MouseMotionListener {
  public static final int secondsInDay =86400, minutesInDay=1440;
  
  public static Color
      focusSectorColor=Color.red.darker(),
      focusSectorBkgColor=new Color(128,0,0,30),
      fromSectorColor=Color.cyan.darker().darker(),
      fromSectorBkgColor=new Color(0,128,128,30),
      toSectorColor=Color.blue.darker(),
      toSectorBkgColor=new Color(0,0,128,30);
  
  public static Color
      flightCountColor=new Color(0, 0, 0, 40),
      highFlightCountColor=new Color(90, 0, 0, 60),
      entryCountColor=new Color(255, 255, 255, 40),
      highEntryCountColor=new Color(255, 128, 128, 60),
      capacityColor=new Color(128, 0, 0, 128);
  
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * Time step, in minutes, for aggregating flights in sectors
   */
  public int tStepAggregates=1;
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
  /**
   * Index of the currently highlighted object
   */
  protected int hlIdx=-1;
  /**
   * Identifiers of selected objects
   */
  protected ArrayList<String> selectedObjIds=null;
  /**
   * Whether to show only selected flights or all flights
   */
  public boolean showOnlySelectedFlights=false;
  
  protected BufferedImage off_Image=null, off_Image_selected=null;
  protected boolean off_Valid=false, selection_Valid=false;
  
  
  public SectorShowCanvas(SectorSet sectors) {
    super();
    this.sectors=sectors;
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.round(0.7f*size.width), Math.round(0.85f*size.height)));
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
            OneSectorData sFrom = fromSectors.getSectorData(ff.sectorId);
            if (sFrom == null) {
              sFrom = new OneSectorData();
              sFrom.sectorId = ff.sectorId;
              sFrom.capacity=s.capacity;
              fromSectors.addSector(sFrom);
            }
            sFrom.addFlight(ff);
          }
          ff=null;
          s=sectors.getSectorData(f.nextSectorId);
          if (s!=null)
            ff=s.getFlightData(f.flightId,null,f.exitTime);
          if (ff!=null) {
            OneSectorData sTo = toSectors.getSectorData(ff.sectorId);
            if (sTo == null) {
              sTo = new OneSectorData();
              sTo.sectorId = ff.sectorId;
              sTo.capacity=s.capacity;
              toSectors.addSector(sTo);
            }
            sTo.addFlight(ff);
          }
        }
      }
      fromSorted=fromSectors.getSectorsSortedByNFlights();
      toSorted=toSectors.getSectorsSortedByNFlights();
    }
    off_Valid=false;
    selection_Valid=false;
    redraw();
  }
  
  public String getFocusSectorId(){
    return sInFocus.sectorId;
  }
  
  public ArrayList<String> getFromSectorIds() {
    if (fromSorted==null || fromSorted.isEmpty())
      return null;
    ArrayList<String> ids=new ArrayList<String>(fromSorted.size());
    for (int i=0; i<fromSorted.size(); i++)
      ids.add(fromSorted.get(i).sectorId);
    return ids;
  }
  
  public ArrayList<String> getToSectorIds() {
    if (toSorted==null || toSorted.isEmpty())
      return null;
    ArrayList<String> ids=new ArrayList<String>(toSorted.size());
    for (int i=0; i<toSorted.size(); i++)
      ids.add(toSorted.get(i).sectorId);
    return ids;
  }
  
  public int getAggregationTimeStep() {
    return tStepAggregates;
  }
  
  public void setAggregationTimeStep(int step) {
    if (step>0 && step!=tStepAggregates) {
      tStepAggregates=step;
      off_Valid=false;
      redraw();
    }
  }
  
  public void setShowOnlySelectedFlights(boolean only) {
    if (this.showOnlySelectedFlights != only) {
      this.showOnlySelectedFlights = only;
      off_Valid = false;
      redraw();
    }
  }
  
  public int getXPos(LocalTime t, int width) {
    if (t==null)
      return -1;
    int tSinceMidnight=t.getHour()*3600+t.getMinute()*60+t.getSecond();
    return Math.round(((float)width)*tSinceMidnight/ secondsInDay);
  }
  
  public int getXPos(int minute, int width) {
    return Math.round(((float)width)*minute/minutesInDay);
  }
  
  public int getMinuteOfDayForXPos(int xPos, int width) {
    if (xPos<0)
      return -1;
    return Math.round((float)xPos/width*minutesInDay);
  }
  
  public LocalTime getTimeForXPos(int xPos, int width) {
    if (xPos<0)
      return null;
    int secOfDay=Math.round((float)xPos/width*secondsInDay);
    int h=secOfDay/3600, mm=secOfDay%3600, m=mm/60, s=mm%60;
    if (h>23)
      return LocalTime.of(0,0,0);
    return LocalTime.of(h,m,s);
  }
  
  public void paintComponent(Graphics gr) {
    int w=getWidth(), h=getHeight();
    if (w<10 || h<10)
      return;
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false; selection_Valid=false;
      }
      else {
        //gr.drawImage(off_Image,0,0,null);
        if (showOnlySelectedFlights)
          gr.drawImage(off_Image,0,0,null);
        else
          drawSelected(gr);
        if (hlIdx>=0)
          flightDrawers[hlIdx].drawHighlighted(getGraphics());
        return;
      }
    }
    selection_Valid=false;
    
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
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
    g.setRenderingHints(rh);
    
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
      if (showOnlySelectedFlights && (selectedObjIds==null || !selectedObjIds.contains(f.flightId))) {
        flightDrawers[i].setXYFocus(-1,-1,-1,-1);
        flightDrawers[i].setNoPrevious();
        flightDrawers[i].setNoNext();
        continue;
      }
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
  
    showSectorVisitAggregates(g);

    off_Valid=true;
    if (showOnlySelectedFlights)
      gr.drawImage(off_Image,0,0,null);
    else
      drawSelected(gr);
    if (hlIdx>=0)
      flightDrawers[hlIdx].drawHighlighted(getGraphics());
  }
  
  protected void showSectorVisitAggregates(Graphics g) {
    if (sInFocus==null)
      return;
    int w=getWidth();

    showSectorVisitAggregates(g,sInFocus.sectorId,yMarg+hFrom,hFocus,w);

    int nFrom=(fromSorted==null)?0:fromSorted.size(),
        nTo=(toSorted==null)?0:toSorted.size();
    
    for (int i=0; i<nFrom; i++) {
      OneSectorData s=fromSorted.get(nFrom-i-1);
      int y=yMarg+i*(hOther+vSpace);
      showSectorVisitAggregates(g,s.sectorId,y,hOther,w);
    }
  
    for (int i=0; i<nTo; i++) {
      OneSectorData s=toSorted.get(i);
      int y=yMarg+hFrom+hFocus+vSpace+i*(hOther+vSpace);
      showSectorVisitAggregates(g,s.sectorId,y,hOther,w);
    }
  }
  
  protected void showSectorVisitAggregates(Graphics g, String sectorId, int y0, int fullH, int fullW) {
    if (sectors==null || sectorId==null)
      return;
    OneSectorData s=sectors.getSectorData(sectorId);
    if (s==null)
      return;
    int fCounts[]=s.getHourlyFlightCounts(tStepAggregates);
    if (fCounts==null)
      return;
    int eCounts[]=s.getHourlyEntryCounts(tStepAggregates);
    int max=0;
    for (int j=0; j<fCounts.length; j++)
      if (max<fCounts[j])
        max=fCounts[j];
    if (max<=0) return;
    max=Math.max(max,s.capacity);
    
    int maxBH=fullH-2;
    for (int j = 0; j < fCounts.length; j++)
      if (fCounts[j] > 0) {
        int t=j*tStepAggregates;
        int x1 = tMarg+getXPos(t, tWidth), x2 = tMarg+getXPos(t +tStepAggregates, tWidth);
        int bh = Math.round(((float) fCounts[j]) / max * maxBH);
        if (s.capacity > 0 && fCounts[j] > s.capacity)
          g.setColor(highFlightCountColor);
        else
          g.setColor(flightCountColor);
        g.fillRect(x1, y0 + fullH -1 - bh, x2 - x1 + 1, bh);
        if (eCounts[j]>0) {
          bh=Math.round(((float) eCounts[j]) / max * maxBH);
          if (s.capacity > 0 && eCounts[j] > s.capacity)
            g.setColor(highEntryCountColor);
          else
            g.setColor(entryCountColor);
          g.fillRect(x1, y0 + fullH -1 - bh, x2 - x1 + 1, bh);
        }
      }
    if (s.capacity<max) {
      g.setColor(capacityColor);
      int cy=y0+fullH-Math.round(((float) s.capacity) / max * maxBH);
      g.drawLine(0,cy,fullW,cy);
    }
  }
  
  protected int getDrawnObjIndex(String objId) {
    if (flightDrawers==null || objId==null)
      return -1;
    for (int i=0; i<flightDrawers.length; i++)
      if (objId.equals(flightDrawers[i].flightId))
        return i;
    return -1;
  }
  
  protected void drawSelected(Graphics g) {
    if (off_Image==null)
      return;
    boolean sameSize=off_Image_selected!=null &&
                     off_Image_selected.getWidth()==off_Image.getWidth() &&
                     off_Image_selected.getHeight()==off_Image.getHeight();
    if (sameSize && selection_Valid) {
      g.drawImage(off_Image_selected,0,0,null);
      return;
    }
    if (selectedObjIds==null || selectedObjIds.isEmpty()) {
      g.drawImage(off_Image,0,0,null);
      return;
    }
    if (!sameSize)
      off_Image_selected=new BufferedImage(off_Image.getWidth(),off_Image.getHeight(),
                                           BufferedImage.TYPE_INT_ARGB);
    Graphics2D gOff = off_Image_selected.createGraphics();
    gOff.drawImage(off_Image,0,0,null);
    for (int i=0; i<selectedObjIds.size(); i++) {
      int idx=getDrawnObjIndex(selectedObjIds.get(i));
      if (idx>=0)
        flightDrawers[idx].drawSelected(gOff);
    }
    selection_Valid=true;
    g.drawImage(off_Image_selected,0,0,null);
  }
  
  public void redraw() {
    paintComponent(getGraphics());
  }
  
  public ArrayList<String> getSelectedObjectIds() {
    return selectedObjIds;
  }
  
  public ArrayList<String> getSelectedVisibleObjectIds() {
    if (selectedObjIds==null || selectedObjIds.isEmpty())
      return null;
    ArrayList<String> drawn= new ArrayList<String>(selectedObjIds.size());
    for (int i=0; i<selectedObjIds.size(); i++) {
      int idx = getDrawnObjIndex(selectedObjIds.get(i));
      if (idx >= 0)
        drawn.add(flightDrawers[idx].flightId);
    }
    if (drawn.isEmpty())
      return null;
    return drawn;
  }

  public void deselectObject(String oId){
    if (oId==null || selectedObjIds==null || selectedObjIds.isEmpty())
      return;
    int idx=selectedObjIds.indexOf(oId);
    if (idx>=0) {
      selectedObjIds.remove(idx);
      selection_Valid=false;
      redraw();
      sendActionEvent("object_selection");
    }
  }
  
  public void highlightObject(String oId) {
    if (oId==null)
      return;
    int idx=getDrawnObjIndex(oId);
    if (hlIdx!=idx) {
      hlIdx=idx;
      redraw();
    }
  }
  
  public void dehighlightObject(String oId) {
    if (oId==null)
      return;
    int idx=getDrawnObjIndex(oId);
    if (idx>=0 && hlIdx==idx) {
      hlIdx=-1;
      redraw();
    }
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
      if (sIdx<0 || yPos>yMarg+sIdx*(hOther+vSpace)+hOther) //the mouse is in a vertical space between sectors
        return null;
      int is[]={-1,fromSorted.size()-sIdx-1};
      return is;
    }
    if (yPos>yMarg+hFrom+hFocus+vSpace) {
      int sIdx=(yPos-yMarg-hFrom-hFocus)/(hOther+vSpace);
      if (sIdx<0 || yPos<yMarg+hFrom+hFocus+sIdx*(hOther+vSpace)+vSpace) //the mouse is in a vertical space between sectors
        return null;
      int is[]={1,sIdx};
      return is;
    }
    return null;
  }
  
  public int getFlightIdxAtPosition(int x, int y){
    if (flightDrawers==null)
      return -1;
    for (int i=0; i<flightDrawers.length; i++)
      if (flightDrawers[i].contains(x,y))
        return i;
    return -1;
  }
  
  @Override
  public String getToolTipText(MouseEvent me) {
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx>=0) {
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
    OneSectorData s=(is[0]<0)?fromSorted.get(is[1]):(is[0]>0)?toSorted.get(is[1]):sInFocus;
    OneSectorData sFull=(is[0]==0)?sInFocus:sectors.getSectorData(s.sectorId);
    
    LocalTime tAtPos=getTimeForXPos(me.getX()-tMarg,tWidth);
    String txt="<html><body style=background-color:rgb(255,255,204)>"+
                   "Time = "+tAtPos+"<br>"+
                   "Sector "+s.sectorId+":<br>";
    
    if (is[0]==0)
      txt+=s.getNFlights()+" flights "+
                "during time range "+s.tFirst+".."+s.tLast+"<br>";
    else
      txt+=s.getNFlights()+" flights "+((is[0]<0)?"go to":"come from")+
               " sector "+sInFocus.sectorId+"<br>during "+
               "time range "+s.tFirst+".."+s.tLast+";<br>"+
               sFull.getNFlights()+" flights crossed this sector in total<br>" +
               "during time range "+sFull.tFirst+".."+sFull.tLast+"<br>";
    txt+="capacity = "+sFull.capacity+" flights per hour";
    
    int tStep=sFull.getAggregationTimeStep();
    int counts[]=sFull.getHourlyFlightCounts(tStep);
    if (counts!=null) {
      int idx=sFull.getTimeBinIndex(tAtPos,tStep);
      if (idx>=0 && idx<counts.length) {
        LocalTime tt[]=sFull.getTimeBinRange(idx,tStep);
        if (tt!=null) {
          txt += "<br>time bin: "+tt[0]+".."+tt[1]+" (#"+idx+")"+
                     "<br>Hourly occupancy: " + counts[idx];
          if (counts[idx]>sFull.capacity) {
            int diff=counts[idx]-sFull.capacity;
            float percent=100f*diff/sFull.capacity;
            txt+="; excess of capacity: "+diff+" flights ("+String.format("%.2f", percent)+"%)";
          }
          counts=sFull.getHourlyEntryCounts(tStep);
          if (counts!=null) {
            txt+="<br>Hourly entries: " + counts[idx];
            if (counts[idx]>sFull.capacity) {
              int diff=counts[idx]-sFull.capacity;
              float percent=100f*diff/sFull.capacity;
              txt+="; excess of capacity: "+diff+" entries ("+String.format("%.2f", percent)+"%)";
            }
          }
        }
      }
    }
    
    txt+="</body></html>";
    return txt;
  }
  
  //---------------------- MouseListener ----------------------------------------
  
  protected boolean clicked=false;
  
  @Override
  public void mouseClicked(MouseEvent e) {
    clicked=e.getClickCount()==1;
    if (e.getClickCount()==2) {
      int is[]=getSectorIdx(e.getY());
      if (is==null) {
        if (selectedObjIds!=null && !selectedObjIds.isEmpty()) {
          selectedObjIds.clear();
          selection_Valid=false;
          redraw();
          sendActionEvent("object_selection");
        }
        return;
      }
      if (is[0]==0)
        return;
      hlIdx = -1;
      if (is[0]==-1)
        sendActionEvent("select_sector:"+fromSorted.get(is[1]).sectorId);
      else
        sendActionEvent("select_sector:"+toSorted.get(is[1]).sectorId);
    }
  }
  
  protected int dragX0=-1, dragY0=-1;
  protected boolean dragged=false;
  
  @Override
  public void mousePressed(MouseEvent e) {
    dragX0=e.getX(); dragY0=e.getY();
    dragged=false;
  }
  
  @Override
  public void mouseReleased(MouseEvent e) {
    if (dragged) {
      if (dragX0>=0 && dragY0>=0 && flightDrawers != null) {
        int x0 = Math.min(e.getX(), dragX0), y0 = Math.min(e.getY(), dragY0);
        int w = Math.abs(e.getX() - dragX0), h = Math.abs(e.getY() - dragY0);
        if (w > 0 && h > 0) {
          for (int i = 0; i < flightDrawers.length; i++)
            if (flightDrawers[i].intersects(x0, y0, w, h)) {
              if (selectedObjIds == null || !selectedObjIds.contains(flightDrawers[i].flightId)) {
                if (selectedObjIds == null)
                  selectedObjIds = new ArrayList<String>(50);
                selectedObjIds.add(flightDrawers[i].flightId);
                selection_Valid=false;
              }
              else {
                selectedObjIds.remove(flightDrawers[i].flightId);
                selection_Valid=false;
              }
            }
        }
        sendActionEvent("object_selection");
      }
      dragX0=-1; dragY0=-1;
      dragged=false;
      redraw();
    }
    else {
      dragX0=-1; dragY0=-1;
      if (clicked) {
        clicked = false;
        int fIdx = getFlightIdxAtPosition(e.getX(), e.getY());
        if (fIdx >= 0)
          if (selectedObjIds == null || !selectedObjIds.contains(flightDrawers[fIdx].flightId)) {
            if (selectedObjIds == null)
              selectedObjIds = new ArrayList<String>(50);
            selectedObjIds.add(flightDrawers[fIdx].flightId);
            selection_Valid=false;
            redraw();
            sendActionEvent("object_selection");
          }
          else {
            selectedObjIds.remove(flightDrawers[fIdx].flightId);
            selection_Valid=false;
            redraw();
            sendActionEvent("object_selection");
          }
      }
    }
  }
  
  @Override
  public void mouseEntered(MouseEvent e) {
    dragX0=-1; dragY0=-1;
    dragged=false;
  }
  
  @Override
  public void mouseExited(MouseEvent e) {
    clicked=false;
    if (hlIdx>=0) {
      hlIdx = -1;
      redraw();
    }
    dragX0=-1; dragY0=-1;
    if (dragged) {
      redraw();
      dragged=false;
    }
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
    if (dragY0<0 || dragY0<0)
      return;
    if (dragged)
      redraw();
    int x0=Math.min(e.getX(),dragX0), y0=Math.min(e.getY(),dragY0);
    int w=Math.abs(e.getX()-dragX0), h=Math.abs(e.getY()-dragY0);
    if (w>0 && h>0) {
      dragged=true;
      Graphics g=getGraphics();
      g.setColor(new Color(0,0,0,96));
      g.drawRect(x0,y0,w,h);
      g.fillRect(x0,y0,w,h);
    }
  }
  
  @Override
  public void mouseMoved(MouseEvent me) {
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx!=hlIdx) {
      if (hlIdx>=0) {
        hlIdx=-1;
        redraw();
      }
      hlIdx=fIdx;
      if (fIdx >= 0)
        flightDrawers[fIdx].drawHighlighted(getGraphics());
    }
  }
}
