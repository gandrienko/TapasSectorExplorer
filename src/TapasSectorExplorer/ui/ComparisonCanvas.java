package TapasSectorExplorer.ui;

import TapasSectorExplorer.data_manage.FlightInSector;
import TapasSectorExplorer.data_manage.OneSectorData;
import TapasSectorExplorer.data_manage.ScenarioDistinguisher;
import TapasSectorExplorer.data_manage.SectorSet;

import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;

public class ComparisonCanvas extends SectorShowCanvas {
  public ScenarioDistinguisher scDiff=null;
  
  
  public ComparisonCanvas(ScenarioDistinguisher scDiff) {
    super(scDiff);
    this.scDiff=scDiff;
  }
  
  protected void showSectorVisitAggregates(Graphics g, String sectorId, int y0, int fullH, int fullW) {
    if (sectors==null || sectorId==null)
      return;
    OneSectorData s1=scDiff.scenario1.getSectorData(sectorId),
        s2=scDiff.scenario2.getSectorData(sectorId);
    if (s1==null || s2==null)
      return;
    int fCounts1[]=s1.getHourlyFlightCounts(tStepAggregates);
    if (fCounts1==null)
      return;
    int fCounts2[]=s2.getHourlyFlightCounts(tStepAggregates);
    int eCounts1[]=s1.getHourlyEntryCounts(tStepAggregates,toIgnoreReEntries);
    int eCounts2[]=s2.getHourlyEntryCounts(tStepAggregates,toIgnoreReEntries);
    int min=Integer.MAX_VALUE, max=Integer.MIN_VALUE;
    int fCounts[]=new int[fCounts1.length], eCounts[]=new int[fCounts1.length];
    for (int j=0; j<fCounts.length; j++) {
      fCounts[j]=(fCounts2!=null)?fCounts2[j]-fCounts1[j]:fCounts1[j];
      eCounts[j]=(eCounts2!=null)?eCounts2[j]-eCounts1[j]:eCounts1[j];
      if (max < fCounts[j])
        max = fCounts[j];
      if (min>fCounts[j])
        min=fCounts[j];
    }
    if (min>=max) return;
    
    float capToHighlight=(100+minExcessPercent)*s1.capacity/100;
    
    int maxBH=fullH-2;
    int yAxis=(max<=0)?1:(min>=0)?maxBH+1:Math.round(((float)maxBH)/(max-min)*max);
    yAxis+=y0;
    g.setColor(new Color(255,255,255,128));
    g.drawLine(0,yAxis,fullW,yAxis);
    
    for (int j = 0; j < fCounts.length; j++)
      if (fCounts[j] !=0) {
        int t=j*tStepAggregates;
        int x1 = tMarg+getXPos(t, tWidth), x2 = tMarg+getXPos(t +tStepAggregates, tWidth);
        int bh = Math.round(((float) fCounts[j]) / (max-min) * maxBH);
        if (toHighlightCapExcess && !toCountEntries && s1.capacity > 0 && fCounts2[j] > capToHighlight)
          g.setColor(highFlightCountColor);
        else
          g.setColor(flightCountColor);
        g.fillRect(x1, (bh>0)?yAxis - bh:yAxis, x2 - x1 + 1, Math.abs(bh));
        if (eCounts[j]>0) {
          bh=Math.round(((float) eCounts[j]) / (max-min) * maxBH);
          if (toHighlightCapExcess && toCountEntries && s1.capacity > 0 && eCounts2[j] > capToHighlight)
            g.setColor(highEntryCountColor);
          else
            g.setColor(entryCountColor);
          g.fillRect(x1, (bh>0)?yAxis - bh:yAxis, x2 - x1 + 1, Math.abs(bh));
        }
      }
  }
  
  public String getSectorInfoText(OneSectorData s, boolean isBeforeFocus, LocalTime t){
    boolean isFocus=s.equals(sInFocus);
    OneSectorData sFull=(isFocus)?sInFocus:sectors.getSectorData(s.sectorId);
    
    String txt="<html><body style=background-color:rgb(255,255,204)>"+
                   "<font size=5><center>Sector "+s.sectorId+"</center></font>"+
                   "<center>Time = "+t+"</center>";
  
    if (s.getNFlights()<1) {
      txt+="<font color=\"#BB0000\"><u>No modified flights!</u></font>";
    }
    else {
      txt += "<table border=0 cellmargin=3 cellpadding=1 cellspacing=2>";
      txt += "<tr><td><font color=\"#BB0000\"><u>Modified flights</u></font></td></tr>";
      if (isFocus)
        txt += "<tr><td>N of flights:</td><td>" + s.getNFlights() + "</td></tr>" +
                   "<tr><td>Time range:</td>" + s.tFirst + " ..</td><td>" + s.tLast + "</td></tr>";
      else {
        txt += "<tr><td>N " + ((isBeforeFocus) ? "going directly to" : "coming directly from") +
                   "</td><td>" + sInFocus.sectorId + ":</td>" + s.getNFlights() + "</td></tr>" +
                   "<tr><td>Time range:</td>" + s.tFirst + " ..</td><td>" + s.tLast + "</td></tr>";
        ArrayList sList = (isBeforeFocus) ? fromSorted : toSorted;
        int idx = sList.indexOf(s);
        int n = (idx < 0) ? 0 : (isBeforeFocus) ? nComeFrom[idx] : nGoTo[idx];
        n -= s.getNFlights();
        if (n > 0)
          txt += "<tr><td>N " + ((isBeforeFocus) ? "going indirectly to" : "coming indirectly from") +
                     "</td><td>" + sInFocus.sectorId + ":</td>" + n + "</td></tr>";
        txt += "<tr><td>N of visits total:</td><td>" + sFull.getNFlights() + "</td></tr>" +
                   "<tr><td>Time range:</td>" + sFull.tFirst + " ..</td><td>" + sFull.tLast + "</td></tr>";
      }
      txt+="</table>";
    }
    txt+="<table border=0 cellmargin=3 cellpadding=1 cellspacing=2>";
    txt+="<tr><td>Sector capacity:</td><td>"+sFull.capacity+"</td><td>flights</td><td>per hour</td></tr>";
    
    OneSectorData s1=scDiff.scenario1.getSectorData(s.sectorId),
        s2=scDiff.scenario2.getSectorData(s.sectorId);

    int counts1[]=s1.getHourlyFlightCounts(tStepAggregates),
        counts2[]=s2.getHourlyFlightCounts(tStepAggregates);
    if (counts1!=null) {
      int idx=sFull.getTimeBinIndex(t,tStepAggregates);
      if (idx>=0 && idx<counts1.length) {
        LocalTime tt[]=sFull.getTimeBinRange(idx,tStepAggregates);
        if (tt!=null) {
          txt += "<tr><td>Time bin:</td><td>#"+idx+"</td><td>"+tt[0]+":00 ..</td><td>"+tt[1]+"</td></tr>";
          txt+="</table><table border=0 cellmargin=3 cellpadding=1 cellspacing=2>";
          txt += "<tr><td>Hourly occupancy:</td><td><font color=\"#0000BB\">" + counts1[idx]+"</font></td>"+
                     "<td> >>> </td><td><font color=\"#BB0000\">"+counts2[idx]+"</font></td></tr>";
          if (sFull.capacity>0 && (counts1[idx]>sFull.capacity || counts2[idx]>sFull.capacity)) {
            txt+="<tr><td>Excess of capacity:</td>";
            int diff=counts1[idx]-sFull.capacity;
            if (diff>0) {
              float percent = 100f * diff / sFull.capacity;
              txt +="<td><font color=\"#0000BB\">"+ diff + "</td><td>(" +
                        String.format("%.2f", percent) + "%)</font></td>";
            }
            else
              txt+="<td></td><td></td>";
            diff=counts2[idx]-sFull.capacity;
            if (diff>0) {
              float percent = 100f * diff / sFull.capacity;
              txt +="<td><font color=\"#BB0000\">"+ diff + "</td><td>(" +
                        String.format("%.2f", percent) + "%)</font></td>";
            }
            else
              txt+="<td></td><td></td>";
            txt+="</tr>";
          }
          txt+="<tr><td>Difference:</td><td>"+(counts2[idx]-counts1[idx])+"</td></tr>";
          counts1=s1.getHourlyEntryCounts(tStepAggregates,toIgnoreReEntries);
          counts2=s2.getHourlyEntryCounts(tStepAggregates,toIgnoreReEntries);
          if (counts1!=null) {
            txt += "<tr><td>Hourly entries:</td><td><font color=\"#0000BB\">" + counts1[idx] + "</font></td>" +
                       "<td> >>> </td><td><font color=\"#BB0000\">" + counts2[idx] + "</font></td></tr>";
            if (sFull.capacity>0 && (counts1[idx] > sFull.capacity || counts2[idx] > sFull.capacity)) {
              txt += "<tr><td>Excess of capacity:</td>";
              int diff = counts1[idx] - sFull.capacity;
              if (diff > 0) {
                float percent = 100f * diff / sFull.capacity;
                txt += "<td><font color=\"#0000BB\">" + diff + "</td><td>(" +
                           String.format("%.2f", percent) + "%)</font></td>";
              }
              else
                txt += "<td></td><td></td>";
              diff = counts2[idx] - sFull.capacity;
              if (diff > 0) {
                float percent = 100f * diff / sFull.capacity;
                txt += "<td><font color=\"#BB0000\">" + diff + "</td><td>(" +
                           String.format("%.2f", percent) + "%)</font></td>";
              }
              else
                txt += "<td></td><td></td>";
              txt += "</tr>";
            }
            txt += "<tr><td>Difference:</td><td>" + (counts2[idx] - counts1[idx]) + "</td></tr>";
          }
        }
      }
    }
    txt+="</table>";
    
    txt+="</body></html>";
    return txt;
  }
  
  //generate a description of a flight showing data from two scenarios
  
  public String getFlightInfoText(int fIdx) {
    if (fIdx<0)
      return null;
    FlightInSector f0=sInFocus.sortedFlights.get(fIdx);
    ArrayList<FlightInSector> seq1 = scDiff.scenario1.getSectorVisitSequence(f0.flightId),
                             seq2= scDiff.scenario2.getSectorVisitSequence(f0.flightId);
    if (seq1 == null || seq1.isEmpty() || seq2==null || seq2.isEmpty() || SectorSet.sameSequence(seq1,seq2))
      return super.getFlightInfoText(fIdx);
    String str="<html><body style=background-color:rgb(255,255,204)>"+"Flight <b>"+f0.flightId+"</b><hr>";
    str += "<table border=0>";
    str+="<tr><td>Sector:</td><td>Time:</td><td></td><td>Change:</td>";
    boolean passedFocusSector = false;
    for (int j = 0; j < seq1.size(); j++) {
      FlightInSector f = seq1.get(j);
      String strColor=(sInFocus.sectorId.equals(f.sectorId))?sFocusSectorColor:
                          (!passedFocusSector && (fromSectors!=null && fromSectors.hasSector(f.sectorId)))?sFromSectorColor:
                          (passedFocusSector && (toSectors!=null && toSectors.hasSector(f.sectorId)))?sToSectorColor:
                          ((fromSectors!=null && fromSectors.hasSector(f.sectorId)) ||
                               (toSectors!=null && toSectors.hasSector(f.sectorId)))?"black":"gray";
      passedFocusSector=passedFocusSector || sInFocus.sectorId.equals(f.sectorId);
      str+="<tr style=\"color:"+strColor+"\"><td>"+f.sectorId+"</td><td>"+f.entryTime+".."+f.exitTime+"</td>";
      if (j<seq2.size()) {
        FlightInSector fAlt=seq2.get(j);
        if (!fAlt.equals(f)) {
          str+="<td>>>></td><td>";
          if (!fAlt.sectorId.equals(f.sectorId))
            str+="<font color=\"#BB0000\"><u>"+fAlt.sectorId+"</u></font> ";
          str+=fAlt.entryTime+".."+fAlt.exitTime+"</td>";
        }
      }
      str+="</tr>";
    }
    if (seq2.size()>seq1.size())
      for (int j=seq1.size(); j<seq2.size(); j++) {
        FlightInSector fAlt = seq2.get(j);
        str+="<tr style=\"color:#BB0000\"><td></td><td></td><td></td><u>"+
                 fAlt.sectorId+"</u> "+fAlt.entryTime+".."+fAlt.exitTime+"</td></tr>";
      }
    str+="</table>";
    str+="</body></html>";
    return str;
  }
}
