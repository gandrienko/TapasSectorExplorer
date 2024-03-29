package TapasSectorExplorer.ui;

import TapasSectorExplorer.data_manage.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;

public class SectorShowCanvas extends JPanel implements MouseListener, MouseMotionListener {
  public static final int secondsInDay =86400, minutesInDay=1440;
  
  public static Color
      focusSectorColor=new Color(128,70,0, 196),
      focusSectorBkgColor=new Color(255,165,0,30),
      fromSectorColor=new Color(0,128,128,196),
      fromSectorBkgColor=new Color(0,128,128,30),
      toSectorColor=new Color(0,40,128,196),
      toSectorBkgColor=new Color(0,40,128,30);
  
  public static Color
      flightCountColor=new Color(90,90,90,40),
      barBorderColor=new Color(90,90,90,90),
      highFlightCountColor=new Color(128, 0, 0, 40),
      entryCountColor=new Color(255, 255, 255, 40),
      highEntryCountColor=new Color(255, 128, 128, 60),
      capacityColor=new Color(128, 0, 0, 128);
  
  public static float alphaMin=0.075f, alphaMax=0.5f;
  
  public static float dash1[] = {10.0f,5.0f};
  public static Stroke dashedStroke = new BasicStroke(1.0f,BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,10.0f, dash1, 0.0f);
  
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * Time step, in minutes, for aggregating flights in sectors
   */
  public int tStepAggregates=20;
  /**
   * Whether to count entries (true) or presence (false)
   */
  public boolean toCountEntries=true;
  /**
   * Whether to ignore re-entries when counting entries
   */
  public boolean toIgnoreReEntries=true;
  /**
   * Whether to highlight excess of capacity
   */
  public boolean toHighlightCapExcess=true;
  /**
   * Threshold for the capacity excess to highlight, in percents
   */
  public float minExcessPercent=10;
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
   * For each "from" sector, the number of the flights that visit this sector before
   * the focus sector, possibly, with some sector visits in between.
   */
  public int nComeFrom[]=null;
  /**
   * For each "to" sector, the number of the flights that visit this sector after
   * the focus sector, possibly, with some sector visits in between.
   */
  public int nGoTo[]=null;
  /**
   * Identifiers of the flights that changed; used in the mode of showing only changed flights
   */
  public HashSet<String> changedFlightsIds=null;
  /**
   * Whether to show only the flights that changed
   */
  public boolean showOnlyChanged=false;
  /**
   * The identifier of a flight whose path is currently "in focus", i.e.,
   * the full path is shown.
   */
  public String focusFlightId=null;
  /**
   * Listeners of selections
   */
  protected ArrayList<ActionListener> listeners=null;
  /**
   * Used for drawing and calculating positions for times
   */
  protected int tMarg=10, tWidth=0;
  protected int yMarg=0, plotH=0;
  protected int hFocus=0, hOther=0, vSpace=0, hFrom=0, hTo=0;
  /**
   * Each element draws a line or polygon representing a single flight
   */
  protected FlightDrawer flightDrawers[]=null;
  /**
   * Indexes of the currently highlighted object
   */
  protected int hlIdxs[]=null;
  /**
   * Identifiers of selected objects
   */
  protected ArrayList<String> selectedObjIds=null;
  /**
   * Whether to show only selected flights or all flights
   */
  public boolean showOnlySelectedFlights=false;
  /**
   * In the mode of showing only selected flights, this hash set contains identifiers of "marked" flights
   */
  protected HashSet<String> markedObjIds=null;
  /**
   * Time range to show (minutes of the day)
   */
  public int minuteStart=0, minuteEnd=minutesInDay;
  /**
   * Time range length in minutes and in seconds
   */
  public int tLengthMinutes =minutesInDay, tLengthSeconds=tLengthMinutes*60;
  /**
   * Used to speed up redrawing
   */
  protected BufferedImage off_Image=null, off_Image_selected=null, offMarked=null;
  protected boolean off_Valid=false, selection_Valid=false;
  
  
  public SectorShowCanvas(SectorSet sectors) {
    super();
    this.sectors=sectors;
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.round(0.7f*size.width), Math.round(0.8f*size.height)));
    setBorder(BorderFactory.createLineBorder(Color.YELLOW,1));
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    addMouseListener(this);
    addMouseMotionListener(this);
  }
  
  protected void getSectorsForFlight(ArrayList<FlightInSector> seq) {
    if (seq==null || seq.isEmpty())
      return;
    if (fromSorted==null)
      fromSorted=new ArrayList<OneSectorData>(seq.size()*2);
    if (toSorted==null)
      toSorted=new ArrayList<OneSectorData>(seq.size()*2);
  
    int idxFocusSector=-1;
    for (int i=0; i<seq.size() && idxFocusSector<0; i++)
      if (sInFocus.sectorId.equals(seq.get(i).sectorId))
        idxFocusSector=i;
    if (idxFocusSector<0)
      return;
  
    for (int i=idxFocusSector-1; i>=0; i--) {
      FlightInSector f=seq.get(i);
      OneSectorData ss = fromSectors.getSectorData(f.sectorId);
      boolean toAddSector=ss==null;
      if (toAddSector) {
        OneSectorData s=sectors.getSectorData(f.sectorId);
        ss=new OneSectorData();
        ss.sectorId = s.sectorId;
        ss.capacity = s.capacity;
        fromSectors.addSector(ss);
        fromSorted.add(ss);
      }
      ss.addFlight(f);
    }
    for (int i=idxFocusSector+1; i<seq.size(); i++) {
      FlightInSector f=seq.get(i);
      OneSectorData ss = toSectors.getSectorData(f.sectorId);
      boolean toAddSector=ss==null;
      if (toAddSector) {
        OneSectorData s=sectors.getSectorData(f.sectorId);
        ss=new OneSectorData();
        ss.sectorId = s.sectorId;
        ss.capacity = s.capacity;
        toSectors.addSector(ss);
        toSorted.add(ss);
      }
      ss.addFlight(f);
    }
  }
  
  protected void putMissingFlightsInToFromSectors() {
    if (sInFocus==null || sInFocus.getNFlights()<1)
      return;
    if (fromSectors==null && toSectors==null)
      return;
    if (fromSectors.getNSectors()<1 && toSectors.getNSectors()<1)
      return;
    for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
      FlightInSector f = sInFocus.sortedFlights.get(i);
      if (f.prevSectorId!=null) {
        OneSectorData sFrom = fromSectors.getSectorData(f.prevSectorId);
        if (sFrom == null || sFrom.hasFlight(f.flightId))
          continue;
        FlightInSector ff = null;
        OneSectorData s = sectors.getSectorData(f.prevSectorId);
        if (s != null)
          ff = s.getFlightData(f.flightId, f.entryTime, null);
        if (ff != null)
          sFrom.addFlight(ff);
      }
      if (f.nextSectorId!=null) {
        OneSectorData sTo = toSectors.getSectorData(f.nextSectorId);
        if (sTo==null || sTo.hasFlight(f.flightId))
          continue;;
        FlightInSector ff=null;
        OneSectorData s=sectors.getSectorData(f.nextSectorId);
        if (s!=null)
          ff=s.getFlightData(f.flightId,null,f.exitTime);
        if (ff!=null)
          sTo.addFlight(ff);
      }
    }
  }
  
  protected void getPreviousAndNextSectors() {
    fromSectors=toSectors=null;
    fromSorted=toSorted=null;
    if (sInFocus==null || sInFocus.sortedFlights==null || sInFocus.sortedFlights.isEmpty())
      return;
    if (focusFlightId!=null && !sInFocus.hasFlight(focusFlightId))
      focusFlightId=null;
    fromSectors=new SectorSet();
    toSectors=new SectorSet();
    nComeFrom=nGoTo=null;
    if (focusFlightId!=null) {
      getSectorsForFlight(sectors.getSectorVisitSequence(focusFlightId));
      putMissingFlightsInToFromSectors();
    }
    if (fromSectors.getNSectors()<1 && toSectors.getNSectors()<1) {
      for (int i = 0; i < sInFocus.sortedFlights.size(); i++) {
        FlightInSector f = sInFocus.sortedFlights.get(i);
        if (f.prevSectorId != null) {
          FlightInSector ff = null;
          OneSectorData s = sectors.getSectorData(f.prevSectorId);
          if (s != null)
            ff = s.getFlightData(f.flightId, f.entryTime, null);
          if (ff != null) {
            OneSectorData sFrom = fromSectors.getSectorData(ff.sectorId);
            if (sFrom == null) {
              sFrom = new OneSectorData();
              sFrom.sectorId = ff.sectorId;
              sFrom.capacity = s.capacity;
              fromSectors.addSector(sFrom);
            }
            sFrom.addFlight(ff);
          }
          ff = null;
          s = sectors.getSectorData(f.nextSectorId);
          if (s != null)
            ff = s.getFlightData(f.flightId, null, f.exitTime);
          if (ff != null) {
            OneSectorData sTo = toSectors.getSectorData(ff.sectorId);
            if (sTo == null) {
              sTo = new OneSectorData();
              sTo.sectorId = ff.sectorId;
              sTo.capacity = s.capacity;
              toSectors.addSector(sTo);
            }
            sTo.addFlight(ff);
          }
        }
      }
      fromSorted=fromSectors.getSectorsSortedByNFlights();
      toSorted=toSectors.getSectorsSortedByNFlights();
    }
  }
  
  public void countFlightsToAndFrom() {
    nComeFrom=(fromSorted==null)?null:new int[fromSorted.size()];
    if (nComeFrom!=null)
      for (int i=0; i<nComeFrom.length; i++)
        nComeFrom[i]=0;
    nGoTo=(toSorted==null)?null:new int[toSorted.size()];
    if (nGoTo!=null)
      for (int i=0; i<nGoTo.length; i++)
        nGoTo[i]=0;
    if (nComeFrom!=null || nGoTo!=null)
      for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
        FlightInSector f = sInFocus.sortedFlights.get(i);
        if (f.prevSectorId!=null && sectors.hasSector(f.prevSectorId) && fromSorted!=null)
          for (int j=0; j<fromSorted.size(); j++) {
            OneSectorData s=sectors.getSectorData(fromSorted.get(j).sectorId);
            if (f.prevSectorId.equals(s.sectorId) ||
                    s.getFlightData(f.flightId,f.entryTime,null)!=null)
              ++nComeFrom[j];
          }
        if (f.nextSectorId!=null && sectors.hasSector(f.nextSectorId) && toSorted!=null)
          for (int j=0; j<toSorted.size(); j++) {
            OneSectorData s=sectors.getSectorData(toSorted.get(j).sectorId);
            if (f.nextSectorId.equals(s.sectorId) ||
                    s.getFlightData(f.flightId,null,f.exitTime)!=null)
              ++nGoTo[j];
          }
      }
  }
  
  public void setChangedFlightsIds(HashSet<String> changedFlightsIds) {
    this.changedFlightsIds = changedFlightsIds;
  }
  
  public void setShowOnlyChanged(boolean showOnlyChanged) {
    if (changedFlightsIds==null || changedFlightsIds.isEmpty())
      this.showOnlyChanged=false;
    else
    if (this.showOnlyChanged != showOnlyChanged) {
      this.showOnlyChanged = showOnlyChanged;
      off_Valid=false;
      selection_Valid=false;
      redraw();
    }  
  }
  
  public void setFocusSector(String sectorId) {
    setFocusSector(sectorId,true);
  }

  public void setFocusSector(String sectorId, boolean toRedraw) {
    flightDrawers=null;
    if (sectors==null) return;
    if (sectorId==null) {
      sInFocus = null;
      fromSectors=toSectors=null;
      fromSorted=toSorted=null;
      return;
    }
    sInFocus=sectors.getSectorData(sectorId);
    getPreviousAndNextSectors();
    countFlightsToAndFrom();
    createFlightDrawers();
    off_Valid=false;
    selection_Valid=false;
  
    if (toRedraw)
      redraw();
  }
  
  public String getFocusSectorId(){
    return sInFocus.sectorId;
  }
  
  public void setFocusFlight(String flightId) {
    if (flightId==focusFlightId)
      return;
    focusFlightId=flightId;
    getPreviousAndNextSectors();
    countFlightsToAndFrom();
    createFlightDrawers();
    off_Valid=false;
    selection_Valid=false;
    redraw();
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
  
  public void setToCountEntries(boolean entries) {
    if (this.toCountEntries != entries) {
      this.toCountEntries = entries;
      off_Valid = false;
      redraw();
    }
  }
  
  public void setMinExcessPercent(float percent) {
    if (this.minExcessPercent != percent) {
      this.minExcessPercent = percent;
      off_Valid = false;
      redraw();
    }
  }
  
  public float getMinExcessPercent() {
    return minExcessPercent;
  }
  
  public void setToIgnoreReEntries(boolean ignore) {
    if (this.toIgnoreReEntries != ignore) {
      this.toIgnoreReEntries = ignore;
      off_Valid = false;
      redraw();
    }
  }
  
  public void setToHighlightCapExcess(boolean hl) {
    if (this.toHighlightCapExcess != hl) {
      this.toHighlightCapExcess = hl;
      off_Valid = false;
      redraw();
    }
  }
  
  public void setShowOnlySelectedFlights(boolean only) {
    if (this.showOnlySelectedFlights != only) {
      this.showOnlySelectedFlights = only;
      off_Valid = false;
      if (markedObjIds!=null)
        markedObjIds.clear();
      redraw();
    }
  }
  
  public void setTimeRange(int minute1, int minute2) {
    if ((minute1!=minuteStart || minute2!=minuteEnd) && minute2-minute1>=60) {
      minuteStart=minute1; minuteEnd=minute2;
      tLengthMinutes =minuteEnd-minuteStart;
      tLengthSeconds=tLengthMinutes*60;
      off_Valid = false;
      //redraw();
      repaint();
    }
  }
  
  public int getMinuteStart() {
    return minuteStart;
  }
  public int getMinuteEnd() {
    return minuteEnd;
  }
  
  public int getXPos(LocalTime t, int width) {
    if (t==null)
      return -1;
    //int tSinceMidnight=t.getHour()*3600+t.getMinute()*60+t.getSecond();
    //return Math.round(((float)width)*tSinceMidnight/ secondsInDay);
    if (t.getSecond()>0) {
      int tSinceStart = (t.getHour() * 60 + t.getMinute() - minuteStart) * 60 + t.getSecond();
      return Math.round(((float) width) * tSinceStart / tLengthSeconds);
    }
    int tSinceStart = t.getHour() * 60 + t.getMinute() - minuteStart;
    return Math.round(((float) width) * tSinceStart / tLengthMinutes);
  }
  
  public int getXPos(int minute, int width) {
    return Math.round(((float)width)*(minute-minuteStart)/tLengthMinutes);
  }
  
  public int getMinuteOfDayForXPos(int xPos, int width) {
    if (xPos<0)
      return -1;
    return Math.round((float)xPos/width*tLengthMinutes)+minuteStart;
  }
  
  public LocalTime getTimeForXPos(int xPos, int width) {
    if (xPos<0)
      return null;
    int secOfDay=minuteStart*60+Math.round((float)xPos/width*tLengthSeconds);
    int h=secOfDay/3600, mm=secOfDay%3600, m=mm/60, s=mm%60;
    if (h>23)
      return LocalTime.of(0,0,0);
    return LocalTime.of(h,m,s);
  }
  
  protected int getSectorIndexInSortedList(String sectorId, ArrayList<OneSectorData> sorted) {
    if (sectorId==null || sorted==null)
      return -1;
    for (int i=0; i<sorted.size(); i++)
      if (sectorId.equals(sorted.get(i).sectorId))
        return i;
    return -1;
  }
  
  protected void createFlightDrawers(){
    if (sInFocus==null || sInFocus.sortedFlights==null || sInFocus.sortedFlights.isEmpty()) {
      flightDrawers=null;
      return;
    }
    if (flightDrawers==null || flightDrawers.length!=sInFocus.sortedFlights.size()) {
      flightDrawers=new FlightDrawer[sInFocus.sortedFlights.size()];
      for (int i=0; i<flightDrawers.length; i++) {
        flightDrawers[i] = new FlightDrawer();
        FlightInSector f=sInFocus.sortedFlights.get(i);
        flightDrawers[i].flightId = f.flightId;
        flightDrawers[i].isModifiedVersion=f.isModifiedVersion;
        flightDrawers[i].origFlightId=f.origFlightId;
      }
    }
  }
  
  protected void setupFlightDrawers(int nFrom, int y0) {
    createFlightDrawers();
    if (flightDrawers==null)
      return;
    int y=y0;
    for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
      flightDrawers[i].clearPath();
      FlightInSector f=sInFocus.sortedFlights.get(i);
      String fId=(f.isModifiedVersion)?f.origFlightId:f.flightId;
      if (showOnlySelectedFlights && (selectedObjIds==null || !selectedObjIds.contains(fId)))
        continue;
      ArrayList<FlightInSector> seq=sectors.getSectorVisitSequence(f.flightId);
      int fIdx=-1;
      for (int j=0; j<seq.size() && fIdx<0; j++)
        if (f.equals(seq.get(j)))
          fIdx=j;
      if (fIdx<0)
        continue;
      int idx1=fIdx, idx2=fIdx;
      for (int j=fIdx-1; j>=0 && fromSectors.hasSector(seq.get(j).sectorId); j--) {
        if (j+1<fIdx) {
          int k1=getSectorIndexInSortedList(seq.get(j).sectorId,fromSorted),
              k2=getSectorIndexInSortedList(seq.get(j+1).sectorId,fromSorted);
          if (k1<=k2)
            break;
        }
        idx1 = j;
      }
      for (int j=fIdx+1; j<seq.size() && toSectors.hasSector(seq.get(j).sectorId); j++) {
        if (j-1>fIdx) {
          int k1=getSectorIndexInSortedList(seq.get(j-1).sectorId,toSorted),
              k2=getSectorIndexInSortedList(seq.get(j).sectorId,toSorted);
          if (k2<=k1)
            break;
        }
        idx2 = j;
      }
      for (int j=idx1; j<=idx2; j++) {
        FlightInSector ff=seq.get(j);
        if (j==fIdx)  // path segment in the focus sector
          flightDrawers[i].addPathSegment(getXPos(ff.entryTime,tWidth)+tMarg,
              getXPos(ff.exitTime,tWidth)+tMarg,y,y+hFocus,true);
        else
          if (j<fIdx)  { // path segment in one of the previous sectors
            OneSectorData s=fromSectors.getSectorData(ff.sectorId);
            int sIdx=getSectorIndexInSortedList(s.sectorId,fromSorted);
            if (sIdx>=0) {
              int yy=yMarg+(nFrom-sIdx-1)*(hOther+vSpace);
              flightDrawers[i].addPathSegment(getXPos(ff.entryTime,tWidth)+tMarg,
                  getXPos(ff.exitTime,tWidth)+tMarg,yy,yy+hOther,false);
            }
          }
          else { // path segment in one of the following sectors
            OneSectorData s=toSectors.getSectorData(ff.sectorId);
            int sIdx=getSectorIndexInSortedList(s.sectorId,toSorted);
            if (sIdx>=0) {
              int yy=yMarg+hFrom+hFocus+vSpace+sIdx*(hOther+vSpace);
              int xx1=getXPos(ff.entryTime,tWidth)+tMarg, xx2=getXPos(ff.exitTime,tWidth)+tMarg;
              flightDrawers[i].addPathSegment(getXPos(ff.entryTime,tWidth)+tMarg,
                  getXPos(ff.exitTime,tWidth)+tMarg,yy,yy+hOther,false);
            }
          }
      }
    }
  }
  
  protected String makeTextForFocusSector() {
    return sInFocus.sectorId+" ("+sInFocus.getNFlights()+")";
  }
  
  protected String makeTextForFromSector (int idx) {
    OneSectorData s=fromSorted.get(idx);
    return s.sectorId+" ("+s.getNFlights()+"/"+nComeFrom[idx]+")";
  }
  
  protected String makeTextForToSector (int idx) {
    OneSectorData s=toSorted.get(idx);
    return s.sectorId+" ("+s.getNFlights()+"/"+nGoTo[idx]+")";
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
        if (showOnlySelectedFlights) {
          gr.drawImage(off_Image, 0, 0, null);
          drawMarked(gr);
        }
        else
          drawSelected(gr);
        if (hlIdxs!=null)
          for (int i:hlIdxs)
            flightDrawers[i].drawHighlighted(getGraphics());
        return;
      }
    }
    selection_Valid=false;
    offMarked=null;
  
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
    
    yMarg=g.getFontMetrics().getHeight();
    plotH=h-2*yMarg;
    int asc=g.getFontMetrics().getAscent();
    
    g.setColor(Color.lightGray);
    g.fillRect(0,0,w,h);
    
    tWidth=w-2*tMarg;
    
    Color tickColor=new Color(90,90,90, 90);
    Stroke origStroke=g.getStroke();
    g.setStroke(dashedStroke);
    for (int i=0; i<=24; i++) {
      int x=tMarg+getXPos(i*60,tWidth);
      if (x<0 || x>w)
        continue;
      g.setColor(tickColor);
      g.drawLine(x,yMarg,x,yMarg+plotH);
      String str=String.format("%02d:00",i);
      int sw=g.getFontMetrics().stringWidth(str);
      g.setColor(Color.darkGray);
      g.drawString(str,x-sw/2,yMarg-2);
      g.drawString(str,x-sw/2,h-yMarg+asc);
    }
    g.setStroke(origStroke);
    if (sInFocus==null)
      return;
    
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHints(rh);
    
    int nFrom=(fromSorted==null)?0:fromSorted.size(),
        nTo=(toSorted==null)?0:toSorted.size();
    hFocus=Math.round(0.15f*plotH);
    hOther=(nFrom+nTo>0)?(plotH-hFocus)/(nFrom+nTo):30;
    vSpace=Math.round(0.3f*hOther);
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
    g.drawString(makeTextForFocusSector(),tMarg,y+asc+2);
    g.setColor(focusSectorBkgColor);
    g.fillRect(0,y,w,hFocus);
    
    for (int i=0; i<nFrom; i++) {
      y=yMarg+i*(hOther+vSpace);
      g.setColor(fromSectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      int idx=nFrom-i-1;
      g.drawString(makeTextForFromSector(idx),tMarg,y+asc+2);
      g.setColor(fromSectorBkgColor);
      g.fillRect(0,y,w,hOther);
    }
    
    for (int i=0; i<nTo; i++) {
      y=yMarg+hFrom+hFocus+vSpace+i*(hOther+vSpace);
      g.setColor(toSectorColor);
      g.drawLine(0,y,w,y);
      g.drawLine(0,y+hOther,w,y+hOther);
      g.drawString(makeTextForToSector(i),tMarg,y+asc+2);
      g.setColor(toSectorBkgColor);
      g.fillRect(0,y,w,hOther);
    }
  
    y=yMarg+hFrom;
    setupFlightDrawers(nFrom,y);

    if (flightDrawers!=null)
      for (int i=0; i<flightDrawers.length; i++)
        if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
          flightDrawers[i].draw(g);
  
    showSectorVisitAggregates(g);

    off_Valid=true;
    if (showOnlySelectedFlights) {
      gr.drawImage(off_Image, 0, 0, null);
      drawMarked(gr);
    }
    else
      drawSelected(gr);
    if (hlIdxs!=null && flightDrawers!=null)
      for (int i:hlIdxs)
        if (i>=0 && i<flightDrawers.length)
          if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
            flightDrawers[i].drawHighlighted(getGraphics());
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
    int eCounts[]=s.getHourlyEntryCounts(tStepAggregates,toIgnoreReEntries);
    int max=0;
    for (int j=0; j<fCounts.length; j++)
      if (toCountEntries) {
        if (max<eCounts[j])
          max=eCounts[j];
      }
      else
      if (max<fCounts[j])
        max=fCounts[j];
    if (max<=0) return;
    if (s.capacity<999)
      max=Math.max(max,s.capacity);
  
    int nOverlap=Math.round(60/tStepAggregates)-1;
    float overlapRatio=nOverlap/59;
    float alpha=alphaMax-overlapRatio*(alphaMax-alphaMin);

    float capToHighlight=(100+minExcessPercent)*s.capacity/100;
    
    int maxBH=fullH-2;
    for (int j = 0; j < fCounts.length; j++)
      if (fCounts[j] > 0) {
        int t=j*tStepAggregates;
        int x1 = tMarg+getXPos(t, tWidth), x2 = tMarg+getXPos(t +/*tStepAggregates*/60, tWidth);
        if (!toCountEntries) {
          float ratio=Math.min(((float) fCounts[j]) / max,1);
          int bh = Math.round(ratio * maxBH);
          if (toHighlightCapExcess && s.capacity > 0 && fCounts[j] > capToHighlight)
            g.setColor(new Color(ratio,0,0,alpha));
          else
            g.setColor(new Color(1-ratio,1-ratio,1-ratio,alpha));
          g.fillRect(x1, y0 + fullH - 1 - bh, x2 - x1 + 1, bh);
          g.drawRect(x1, y0 + fullH - 1 - bh, x2 - x1 + 1, bh);
        }
        else
          if (eCounts[j]>0) {
            float ratio=Math.min(((float) eCounts[j]) / max,1);
            int bh=Math.round(ratio * maxBH);
            if (toHighlightCapExcess && s.capacity > 0 && eCounts[j] > capToHighlight)
              g.setColor(new Color(ratio,0,0,alpha));
            else
              g.setColor(new Color(1-ratio,1-ratio,1-ratio,alpha));
            g.fillRect(x1, y0 + fullH -1 - bh, x2 - x1 + 1, bh);
            g.drawRect(x1, y0 + fullH -1 - bh, x2 - x1 + 1, bh);
          }
      }
    if (s.capacity<max) {
      g.setColor(capacityColor);
      int cy=y0+fullH-Math.round(((float) s.capacity) / max * maxBH);
      g.drawLine(0,cy,fullW,cy);
    }
  }
  
  protected int[] getDrawnObjIndexes(String objId) {
    if (flightDrawers==null || objId==null)
      return null;
    ArrayList<Integer> iList=null;
    for (int i=0; i<flightDrawers.length; i++)
      if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
        if (objId.equals(flightDrawers[i].flightId) ||
                objId.equals(flightDrawers[i].origFlightId)) {
          if (iList==null)
            iList=new ArrayList<Integer>(10);
          iList.add(i);
        }
    if (iList==null)
      return null;
    int idxs[]=new int[iList.size()];
    for (int i=0; i<iList.size(); i++)
      idxs[i]=iList.get(i);
    return idxs;
  }
  
  public void setMarkedObjIds(HashSet<String> marked) {
    if (marked==null && markedObjIds==null)
      return;
    if (marked==null)
      markedObjIds.clear();
    else
      markedObjIds=(HashSet<String>)marked.clone();
    updateMarked();
  }
  
  public HashSet<String> getMarkedObjIds () {
    return markedObjIds;
  }
  
  public void clearMarking() {
    if (markedObjIds!=null && !markedObjIds.isEmpty()) {
      markedObjIds.clear();
      updateMarked();
    }
  }
  
  protected void updateMarked() {
    offMarked=null;
    if (isShowing() && showOnlySelectedFlights && off_Image!=null) {
      Graphics g=getGraphics();
      g.drawImage(off_Image,0,0,null);
      drawMarked(g);
    }
  }
  
  protected void drawMarked(Graphics g) {
    if (showOnlySelectedFlights && markedObjIds!=null && !markedObjIds.isEmpty()) {
      if (offMarked==null) {
        offMarked=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = offMarked.createGraphics();
        RenderingHints rh = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setRenderingHints(rh);
        for (int i = 0; i < flightDrawers.length; i++)
          if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
            if (markedObjIds.contains(flightDrawers[i].flightId) ||
                    markedObjIds.contains(flightDrawers[i].origFlightId))
              flightDrawers[i].drawSelected(gr);
      }
      g.drawImage(offMarked,0,0,null);
    }
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
    for (int i=0; i<flightDrawers.length; i++)
      if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
        if (selectedObjIds.contains(flightDrawers[i].flightId) ||
                selectedObjIds.contains(flightDrawers[i].origFlightId))
          flightDrawers[i].drawSelected(gOff);
    selection_Valid=true;
    g.drawImage(off_Image_selected,0,0,null);
  }
  
  public void redraw() {
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  public void sortSelectedObjects() {
    if (selectedObjIds==null || selectedObjIds.size()<2)
      return;
    ArrayList sorted=new ArrayList(selectedObjIds.size());
    if (sInFocus!=null)
      for (int i=0; i<sInFocus.sortedFlights.size(); i++) {
        FlightInSector f=sInFocus.sortedFlights.get(i);
        if (selectedObjIds.contains(f.flightId))
          sorted.add(f.flightId);
      }
    synchronized (selectedObjIds) {
      if (sorted.size() == selectedObjIds.size()) {
        selectedObjIds = sorted;
        return;
      }
    }
    for (int i=0; i<selectedObjIds.size(); i++)
      if (!sorted.contains(selectedObjIds.get(i)))
        sorted.add(selectedObjIds.get(i));
    synchronized (selectedObjIds) {
      selectedObjIds = sorted;
    }
  }
  
  public void setSelectedObjIds(ArrayList<String> newSelection) {
    if (selectedObjIds==null)
      if (newSelection==null) return;
      else;
    else
      if (newSelection!=null &&
              selectedObjIds.size()==newSelection.size() &&
              selectedObjIds.containsAll(newSelection))
        return;
    
    if (newSelection==null)
      if (selectedObjIds!=null)
        selectedObjIds.clear();
      else;
    else
      selectedObjIds=(ArrayList<String>)newSelection.clone();
    if (markedObjIds != null && !markedObjIds.isEmpty())
      if (selectedObjIds == null || selectedObjIds.isEmpty())
        markedObjIds.clear();
    selection_Valid=false;
    if (showOnlySelectedFlights)
      off_Valid=false;
    redraw();
  }
  
  public ArrayList<String> getSelectedObjectIds() {
    return selectedObjIds;
  }
  
  public ArrayList<String> getSelectedVisibleObjectIds() {
    if (flightDrawers==null || selectedObjIds==null || selectedObjIds.isEmpty())
      return null;
    ArrayList<String> drawn= new ArrayList<String>(selectedObjIds.size());
    for (int i=0; i<flightDrawers.length && drawn.size()<selectedObjIds.size(); i++)
      if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
        if (selectedObjIds.contains(flightDrawers[i].flightId))
          drawn.add(flightDrawers[i].flightId);
    if (drawn.isEmpty())
      return null;
    return drawn;
  }
  
  public boolean hasSelectedObjects(){
    return selectedObjIds!=null && !selectedObjIds.isEmpty();
  }
  
  public boolean hasSelectedVisibleObjects(){
    if (hasSelectedObjects())
      for (int i=0; i<flightDrawers.length; i++)
        if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
          if (selectedObjIds.contains(flightDrawers[i].flightId))
            return true;
    return false;
  }

  public void deselectObject(String oId){
    if (oId==null || selectedObjIds==null || selectedObjIds.isEmpty())
      return;
    int idx=selectedObjIds.indexOf(oId);
    if (idx>=0) {
      selectedObjIds.remove(idx);
      if (!showOnlyChanged || changedFlightsIds.contains(oId)) {
        selection_Valid = false;
        if (showOnlySelectedFlights)
          off_Valid = false;
        redraw();
      }
      sendActionEvent("object_selection");
    }
  }
  
  public void highlightObject(String oId) {
    if (oId==null)
      return;
    int idxs[]=getDrawnObjIndexes(oId);
    if (idxs!=null && (hlIdxs==null || hlIdxs[0]!=idxs[0])) {
      hlIdxs=idxs;
      redraw();
    }
  }
  
  public void dehighlightObject(String oId) {
    if (oId==null)
      return;
    int idxs[]=getDrawnObjIndexes(oId);
    if (idxs!=null && hlIdxs==null) {
      hlIdxs=null;
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
    if (yPos<=yMarg+hFrom && fromSorted!=null && !fromSorted.isEmpty()) {
      int sIdx=(yPos-yMarg)/(hOther+vSpace);
      if (sIdx<0 || yPos>yMarg+sIdx*(hOther+vSpace)+hOther) //the mouse is in a vertical space between sectors
        return null;
      int is[]={-1,Math.max(fromSorted.size()-sIdx-1,0)};
      return is;
    }
    if (yPos>yMarg+hFrom+hFocus+vSpace && toSorted!=null && !toSorted.isEmpty()) {
      int sIdx=(yPos-yMarg-hFrom-hFocus)/(hOther+vSpace);
      if (sIdx<0 || yPos<yMarg+hFrom+hFocus+sIdx*(hOther+vSpace)+vSpace) //the mouse is in a vertical space between sectors
        return null;
      int is[]={1,Math.min(sIdx,toSorted.size()-1)};
      return is;
    }
    return null;
  }
  
  public int getFlightIdxAtPosition(int x, int y){
    if (flightDrawers==null)
      return -1;
    for (int i=0; i<flightDrawers.length; i++)
      if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
        if (flightDrawers[i].contains(x,y))
          return i;
    return -1;
  }
  
  public void selectFlights(HashSet<String> newSelection) {
    if (newSelection==null || newSelection.isEmpty())
      return;
    for (String id:newSelection)
      if (selectedObjIds == null || !selectedObjIds.contains(id)) {
        if (selectedObjIds == null)
          selectedObjIds = new ArrayList<String>(50);
        selectedObjIds.add(id);
        selection_Valid=false;
      }
    if (!selection_Valid && showOnlySelectedFlights)
      off_Valid = false;
    if (!selection_Valid) {
      sortSelectedObjects();
      sendActionEvent("object_selection");
    }
    redraw();
  }
  
  public void clearSelection() {
    selectedObjIds.clear();
    selection_Valid=false;
    if (showOnlySelectedFlights)
      off_Valid=false;
    redraw();
  }
  
  public void selectEntering(OneSectorData sector, LocalTime t1, LocalTime t2){
    if (sector==null || t1==null || t2==null || t1.compareTo(t2)>=0)
      return;
    HashSet<String> newSelection=new HashSet<String>(100);
    for (FlightInSector flight:sector.sortedFlights) {
      if (flight.entryTime.compareTo(t2)>=0)
        break;
      if (flight.entryTime.compareTo(t1)>=0)
        newSelection.add((flight.isModifiedVersion)?flight.origFlightId:flight.flightId);
    }
    selectFlights(newSelection);
  }
  
  public void selectVisiting(OneSectorData sector, LocalTime t1, LocalTime t2){
    if (sector==null || t1==null || t2==null || t1.compareTo(t2)>=0)
      return;
    HashSet<String> newSelection=new HashSet<String>(100);
    for (FlightInSector flight:sector.sortedFlights) {
      if (flight.entryTime.compareTo(t2)>=0)
        break;
      if (flight.exitTime.compareTo(t1)>=0)
        newSelection.add((flight.isModifiedVersion)?flight.origFlightId:flight.flightId);
    }
    selectFlights(newSelection);
  }
  
  public void selectExiting(OneSectorData sector, LocalTime t1, LocalTime t2){
    if (sector==null || t1==null || t2==null || t1.compareTo(t2)>=0)
      return;
    HashSet<String> newSelection=new HashSet<String>(100);
    for (FlightInSector flight:sector.sortedFlights) {
      if (flight.entryTime.compareTo(t2)>=0)
        break;
      if (flight.exitTime.compareTo(t1)>=0 && flight.exitTime.compareTo(t2)<0)
        newSelection.add((flight.isModifiedVersion)?flight.origFlightId:flight.flightId);
    }
    selectFlights(newSelection);
  }
  
  public void selectStaying(OneSectorData sector, LocalTime t1, LocalTime t2){
    if (sector==null || t1==null || t2==null || t1.compareTo(t2)>=0)
      return;
    HashSet<String> newSelection=new HashSet<String>(100);
    for (FlightInSector flight:sector.sortedFlights) {
      if (flight.entryTime.compareTo(t2)>=0)
        break;
      if (flight.exitTime.compareTo(t2)>=0)
        newSelection.add((flight.isModifiedVersion)?flight.origFlightId:flight.flightId);
    }
    selectFlights(newSelection);
  }
  
  public void selectFromTo(OneSectorData s1, OneSectorData s2,boolean direct,
                           LocalTime t1, LocalTime t2, boolean applyTimeLimitsToExits) {
    if (s1==null || s2==null || s1.getNFlights()<1 || s2.getNFlights()<1)
      return;
    HashSet<String> fIds=new HashSet<String>(100);
    for (FlightInSector f:s1.sortedFlights) {
      if (fIds.contains(f.flightId))
        continue;
      if (t1!=null && t2!=null) {
        if (f.entryTime.compareTo(t2) >= 0)
          break;
        if (applyTimeLimitsToExits && (f.exitTime.compareTo(t1)<0 || f.exitTime.compareTo(t2)>=0))
          continue;
      }
      if (s2.sectorId.equals(f.nextSectorId)) {
        if (!direct)
          continue;
        if (t1==null || applyTimeLimitsToExits) {
          fIds.add((f.isModifiedVersion)?f.origFlightId:f.flightId);
          continue;
        }
      }
      FlightInSector fNext=s2.getFlightData(f.flightId,null,f.exitTime);
      if (fNext==null)
        continue;;
      if (t1==null || applyTimeLimitsToExits ||
              (fNext.entryTime.compareTo(t1)>=0 && fNext.entryTime.compareTo(t2)<0))
        fIds.add((f.isModifiedVersion)?f.origFlightId:f.flightId);
    }
    selectFlights(fIds);
  }
  
  public static String sFocusSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.focusSectorColor.getRGB()).substring(2);
  public static String sFromSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.fromSectorColor.getRGB()).substring(2);
  public static String sToSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.toSectorColor.getRGB()).substring(2);

  public String getFlightInfoText(int fIdx) {
    if (fIdx<0)
      return null;
    FlightInSector f=sInFocus.sortedFlights.get(fIdx);
    String str="<html><body style=background-color:rgb(255,255,204)>"+"Flight <b>"+f.flightId+"</b><hr>";
    FlightInSector ff=null;
    boolean bTableSTarted=false;
    if (f.prevSectorId!=null && fromSectors!=null) {
      OneSectorData s=fromSectors.getSectorData(f.prevSectorId);
      ff=(s==null)?null:s.getFlightData(f.flightId,f.entryTime,null);
      if (ff!=null) {
        if (!bTableSTarted) {
          str += "<table border=0>";
          bTableSTarted=true;
        }
        str+="<tr style=\"color:"+sFromSectorColor+"\"><td>Sector "+ff.sectorId+"</td><td>"+
                 ff.entryTime+".."+ff.exitTime+"</td></tr>";
      }
    }
    if (!bTableSTarted) {
      str += "<table border=0>";
      bTableSTarted=true;
    }
    str+="<tr style=\"color:"+sFocusSectorColor+"\"><td>Sector "+f.sectorId+"</td><td>"+
             f.entryTime+".."+f.exitTime+"</td></tr>";
    if (f.nextSectorId!=null && toSectors!=null) {
      OneSectorData s=toSectors.getSectorData(f.nextSectorId);
      ff=(s==null)?null:s.getFlightData(f.flightId,null,f.exitTime);
      if (ff!=null) {
        if (!bTableSTarted) {
          str += "<table border=0>";
          bTableSTarted=true;
        }
        str+="<tr style=\"color:"+sToSectorColor+"\"><td>Sector "+ff.sectorId+"</td><td>"+
                 ff.entryTime+".."+ff.exitTime+"</td></tr>";
      }
    }
    if (bTableSTarted)
      str+="</table>";
    str+="</body></html>";
    return str;
  }
  
  public String getSectorInfoText(OneSectorData s, boolean isBeforeFocus, LocalTime t){
    boolean isFocus=s.equals(sInFocus);
    OneSectorData sFull=(isFocus)?sInFocus:sectors.getSectorData(s.sectorId);
    
    String txt="<html><body style=background-color:rgb(255,255,204)>"+
                   "<font size=5><center>Sector "+s.sectorId+"</center></font>"+
                   "<center>Time = "+t+"</center>";
  
    txt += "<table border=0 cellmargin=3 cellpadding=1 cellspacing=2>";
    if (isFocus)
      txt+="<tr><td>N of visits:</td><td>"+s.getNFlights()+"</td></tr>"+
               "<tr><td>Time range:</td>"+s.tFirst+" ..</td><td>"+s.tLast+"</td></tr>";
    else {
      txt+="<tr><td>N "+((isBeforeFocus) ? "going directly to" : "coming directly from")+
               "</td><td>" + sInFocus.sectorId+":</td>"+s.getNFlights()+"</td></tr>"+
               "<tr><td>Time range:</td>"+s.tFirst+" ..</td><td>"+s.tLast+"</td></tr>";
      ArrayList sList=(isBeforeFocus)?fromSorted:toSorted;
      int idx=sList.indexOf(s);
      int n=(idx<0)?0:(isBeforeFocus)?nComeFrom[idx]:nGoTo[idx];
      n-=s.getNFlights();
      if (n>0)
        txt+="<tr><td>N "+((isBeforeFocus) ? "going indirectly to" : "coming indirectly from")+
                 "</td><td>" + sInFocus.sectorId+":</td>"+n+"</td></tr>";
      txt+="<tr><td>N of visits total:</td><td>"+sFull.getNFlights()+"</td></tr>"+
               "<tr><td>Time range:</td>"+sFull.tFirst+" ..</td><td>"+sFull.tLast+"</td></tr>";
    }
    txt+="</table><table border=0 cellmargin=3 cellpadding=1 cellspacing=2>";
    txt+="<tr><td>Sector capacity:</td><td>"+sFull.capacity+"</td><td>flights</td><td>per hour</td></tr>";
  
    int tStep=sFull.getAggregationTimeStep();
    int counts[]=sFull.getHourlyFlightCounts(tStep);
    if (counts!=null) {
      int idx=sFull.getTimeBinIndex(t,tStep);
      if (idx>=0 && idx<counts.length) {
        LocalTime tt[]=sFull.getTimeBinRange(idx,tStep);
        if (tt!=null) {
          txt += "<tr><td>Time bin:</td><td>#"+idx+"</td><td>"+tt[0]+":00 ..</td><td>"+tt[1]+"</td></tr>";
          txt += "<tr><td>Hourly occupancy:</td><td>" + counts[idx]+"</td></tr>";
          if (sFull.capacity>0 && counts[idx]>sFull.capacity) {
            int diff=counts[idx]-sFull.capacity;
            float percent=100f*diff/sFull.capacity;
            txt+="<tr><td>Excess of capacity:</td><td>"+diff+"</td><td>flights</td><td>("+
                     String.format("%.2f", percent)+"%)</td></tr>";
          }
          counts=sFull.getHourlyEntryCounts(tStep,toIgnoreReEntries);
          if (counts!=null) {
            txt+="<tr><td>Hourly entries:</td><td>" + counts[idx]+"</td></tr>";
            if (sFull.capacity>0 && counts[idx]>sFull.capacity) {
              int diff=counts[idx]-sFull.capacity;
              float percent=100f*diff/sFull.capacity;
              txt+="<tr><td>Excess of capacity:</td><td>"+diff+"</td><td>entries</td><td>("+
                       String.format("%.2f", percent)+"%)</td></tr>";
            }
          }
        }
      }
    }
  
    txt+="</table>";
    txt+="</body></html>";
    return txt;
  }
  
  @Override
  public String getToolTipText(MouseEvent me) {
    if (me.getButton()!=MouseEvent.NOBUTTON)
      return null;
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx>=0)
      return getFlightInfoText(fIdx);
    
    int is[]=getSectorIdx(me.getY());
    if (is==null || is[1]<0)
      return null;
    OneSectorData s=(is[0]<0)?fromSorted.get(is[1]):(is[0]>0)?toSorted.get(is[1]):sInFocus;
    LocalTime tAtPos=getTimeForXPos(me.getX()-tMarg,tWidth);
    
    return getSectorInfoText(s,is[0]<0,tAtPos);
  }
  
  public void sectorEntriesToClipboard() {
    //
  }
  
  //---------------------- MouseListener ----------------------------------------
  
  protected boolean clicked=false;
  
  @Override
  public void mouseClicked(MouseEvent e) {
    if (!isShowing())
      return;
    if (e.getButton()>MouseEvent.BUTTON1)
      return;
    clicked=e.getClickCount()==1;
    if (e.getClickCount()==2) {
      int is[]=getSectorIdx(e.getY());
      if (is==null || (is!=null && is[0]==0)) {
        if (showOnlySelectedFlights && markedObjIds!=null && !markedObjIds.isEmpty()) {
          markedObjIds.clear();
          updateMarked();
          sendActionEvent("object_marking");
        }
        return;
      }
      if (is[0]==0)
        return;
      hlIdxs = null;
      synchronized (this) {
        if (is[0] == -1 && is[1] >= 0)
          sendActionEvent("select_sector:" + fromSorted.get(is[1]).sectorId);
        else
          sendActionEvent("select_sector:" + toSorted.get(is[1]).sectorId);
      }
    }
  }
  
  protected int dragX0=-1, dragY0=-1;
  protected boolean dragged=false;
  
  JPopupMenu popupMenu=null;
  
  @Override
  public void mousePressed(MouseEvent e) {
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    if (e.getButton()>MouseEvent.BUTTON1) {
      JMenuItem mitDeselect=(selectedObjIds!=null && !selectedObjIds.isEmpty())?
                             new JMenuItem("Unselect all flights"):null;
      if (mitDeselect!=null)
        mitDeselect.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            clearSelection();
            popupMenu=null;
            sendActionEvent("object_selection");
          }
        });
      int is[]=getSectorIdx(e.getY());
      if (is==null) {
        if (mitDeselect!=null) {
          popupMenu=new JPopupMenu();
          popupMenu.add(mitDeselect);
          popupMenu.show(this,e.getX(),e.getY());
        }
        return;
      }
      OneSectorData sector=(is[0]==0)?sInFocus:(is[0]<0)?fromSorted.get(is[1]):toSorted.get(is[1]);
      // find time interval
      int m=getMinuteOfDayForXPos(e.getX()-tMarg,tWidth);
      if (m<0 || m>=minutesInDay)
        return;
      int m1=(m/tStepAggregates)*tStepAggregates, m2=m1+tStepAggregates;
      LocalTime t1=LocalTime.of(m1/60,m1%60,0),
          t2=LocalTime.of(m2/60,m2%60,0);
      popupMenu=new JPopupMenu();
      JMenuItem mit;
      Color c=new Color(255,255,192);
      popupMenu.add(mit=new JMenuItem("<html><font size=+1 color=blue><center>Sector "+sector.sectorId+"</html"));
      mit.setBackground(c);
      mit.setEnabled(false);
      popupMenu.add(mit=new JMenuItem("Time interval "+t1+".."+t2));
      mit.setEnabled(false);
      mit.setBackground(c);
      popupMenu.addSeparator();
      popupMenu.add(mit=new JMenuItem("Select flights in this interval:"));
      mit.setEnabled(false);
      c=new Color(192,255,255);
      mit.setBackground(c);
      popupMenu.add(mit=new JMenuItem("entering the sector"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu=null;
          selectEntering(sector,t1,t2);
        }
      });
      popupMenu.add(mit=new JMenuItem("being in the sector"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu=null;
          selectVisiting(sector,t1,t2);
        }
      });
      popupMenu.add(mit=new JMenuItem("exiting the sector"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu=null;
          selectExiting(sector,t1,t2);
        }
      });
      popupMenu.add(mit=new JMenuItem("not exiting the sector"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu=null;
          selectStaying(sector,t1,t2);
        }
      });
      //for "before" and "after" sectors, select flights going directly/indirectly to/from focus sector
      if (is[0]!=0) {
        String prefix1=(is[0]<0)?"exiting to":"entering from",
            prefix2=(is[0]<0)?"going to":"coming from";
        String strSector=" sector "+sInFocus.sectorId;
        popupMenu.add(mit=new JMenuItem(prefix1+strSector+" directly"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            popupMenu=null;
            if (is[0]<0)
              selectFromTo(sector,sInFocus,true,t1,t2,true);
            else
              selectFromTo(sInFocus,sector,true,t1,t2,false);
          }
        });
        popupMenu.add(mit=new JMenuItem(prefix2+strSector+" indirectly"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            popupMenu=null;
            if (is[0]<0)
              selectFromTo(sectors.getSectorData(sector.sectorId),sInFocus,false,
                  t1,t2,true);
            else
              selectFromTo(sInFocus,sectors.getSectorData(sector.sectorId),false,
                  t1,t2,false);
          }
        });
        popupMenu.addSeparator();
        popupMenu.add(mit=new JMenuItem("Select flights in the whole time range:"));
        mit.setEnabled(false);
        mit.setBackground(c);
        popupMenu.add(mit=new JMenuItem(prefix1+strSector+" directly"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            popupMenu=null;
            if (is[0]<0)
              selectFromTo(sector,sInFocus,true,null,null,false);
            else
              selectFromTo(sInFocus,sector,true,null,null,false);
          }
        });
        popupMenu.add(mit=new JMenuItem(prefix2+strSector+" indirectly"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            popupMenu=null;
            if (is[0]<0)
              selectFromTo(sectors.getSectorData(sector.sectorId),sInFocus,false,
                  null,null,false);
            else
              selectFromTo(sInFocus,sectors.getSectorData(sector.sectorId),false,
                  null,null,false);
          }
        });
      }
      if (mitDeselect!=null) {
        popupMenu.addSeparator();
        popupMenu.add(mitDeselect);
      }
      popupMenu.addSeparator();
      if (sector==sInFocus && sector.sortedFlights!=null && !sector.sortedFlights.isEmpty()) {
        popupMenu.add(mit=new JMenuItem("Sector entries >>> clipboard"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard==null)
              return;
            StringBuffer txt=new StringBuffer();
            txt.append("Flight,Time\r\n");
            for (int i=0; i<sector.sortedFlights.size(); i++) {
              FlightInSector fis=sector.sortedFlights.get(i);
              String fid=fis.flightId;
              int pos=fid.lastIndexOf('-');
              if (pos>0) fid=fid.substring(0,pos);
              txt.append(fid+","+fis.entryTime + "\r\n");
            }
            clipboard.setContents(new StringSelection(txt.toString()),null);
          }
        });
        popupMenu.addSeparator();
      }
      popupMenu.add(mit=new JMenuItem("Cancel"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu=null;
        }
      });
      popupMenu.show(this,e.getX(),e.getY());
    }
    else {
      dragX0 = e.getX();
      dragY0 = e.getY();
    }
    dragged=false;
  }
  
  @Override
  public void mouseReleased(MouseEvent e) {
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    if (dragged) {
      if (dragX0>=0 && dragY0>=0 && flightDrawers != null) {
        int x0 = Math.min(e.getX(), dragX0), y0 = Math.min(e.getY(), dragY0);
        int w = Math.abs(e.getX() - dragX0), h = Math.abs(e.getY() - dragY0);
        if (w > 0 && h > 0) {
          HashSet<String> newSelection=new HashSet<String>(250);
          for (int i = 0; i < flightDrawers.length; i++)
            if (!showOnlyChanged || changedFlightsIds.contains(flightDrawers[i].flightId))
              if (flightDrawers[i].intersects(x0, y0, w, h))
                newSelection.add((flightDrawers[i].isModifiedVersion)?
                                     flightDrawers[i].origFlightId:flightDrawers[i].flightId);
          if (!newSelection.isEmpty())
            if (showOnlySelectedFlights) {
              for (String id : newSelection)
                if (markedObjIds==null || !markedObjIds.contains(id)) {
                  if (markedObjIds==null)
                    markedObjIds=new HashSet<String>(100);
                  markedObjIds.add(id);
                }
                else
                  markedObjIds.remove(id);
              updateMarked();
              sendActionEvent("object_marking");
            }
            else {
              for (String id : newSelection)
                if (selectedObjIds == null || !selectedObjIds.contains(id)) {
                  if (selectedObjIds == null)
                    selectedObjIds = new ArrayList<String>(50);
                  selectedObjIds.add(id);
                  selection_Valid = false;
                }
                else {
                  selectedObjIds.remove(id);
                  selection_Valid = false;
                }
              if (!selection_Valid) {
                sortSelectedObjects();
                sendActionEvent("object_selection");
              }
            }
        }
        redraw();
      }
      dragX0=-1; dragY0=-1;
      dragged=false;
    }
    else {
      dragX0=-1; dragY0=-1;
      if (clicked) {
        clicked = false;
        if (e.getButton()>MouseEvent.BUTTON1)
          return;
        int fIdx = getFlightIdxAtPosition(e.getX(), e.getY());
        if (fIdx >= 0) {
          String fId=(flightDrawers[fIdx].isModifiedVersion) ?
                          flightDrawers[fIdx].origFlightId : flightDrawers[fIdx].flightId;
          if (showOnlySelectedFlights) {
            if (markedObjIds == null || !markedObjIds.contains(fId)) {
              if (markedObjIds == null)
                markedObjIds = new HashSet<String>(100);
              markedObjIds.add(fId);
            }
            else
              markedObjIds.remove((flightDrawers[fIdx].isModifiedVersion) ?
                                      flightDrawers[fIdx].origFlightId : flightDrawers[fIdx].flightId);
            updateMarked();
            sendActionEvent("object_marking");
          }
          else {
            if (selectedObjIds == null || !selectedObjIds.contains(fId)) {
              if (selectedObjIds == null)
                selectedObjIds = new ArrayList<String>(50);
              selectedObjIds.add(fId);
              selection_Valid = false;
              if (showOnlySelectedFlights)
                off_Valid = false;
              sortSelectedObjects();
              redraw();
              sendActionEvent("object_selection");
            }
            else {
              selectedObjIds.remove(fId);
              selection_Valid = false;
              if (showOnlySelectedFlights)
                off_Valid = false;
              sortSelectedObjects();
              redraw();
              sendActionEvent("object_selection");
            }
          }
        }
      }
    }
  }
  
  @Override
  public void mouseEntered(MouseEvent e) {
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    dragX0=-1; dragY0=-1;
    dragged=false;
    clicked=false;
  }
  
  @Override
  public void mouseExited(MouseEvent e) {
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    clicked=false;
    if (hlIdxs!=null) {
      hlIdxs = null;
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
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    if (e.getButton()>MouseEvent.BUTTON1)
      return;
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
    if (!isShowing())
      return;
    if (popupMenu!=null && popupMenu.isVisible())
      return;
    if (me.getButton()!=MouseEvent.NOBUTTON)
      return;
    int fIdx= getFlightIdxAtPosition(me.getX(),me.getY());
    if (fIdx<0)
      if (hlIdxs!=null) {
        hlIdxs=null;
        redraw();;
      }
      else;
    else
      highlightObject((flightDrawers[fIdx].isModifiedVersion) ?
          flightDrawers[fIdx].origFlightId : flightDrawers[fIdx].flightId);
  }
}
