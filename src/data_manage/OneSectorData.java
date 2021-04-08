package data_manage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Contains data describing flights visiting one sector
 */
public class OneSectorData {
  public static final int minutesInDay=1440;
  /**
   * Sector identifier
   */
  public String sectorId=null;
  /**
   * Capacity of this sector
   */
  public int capacity=0;
  /**
   * Data about the flights that visited this sector, sorted by the entry and exit times
   */
  public ArrayList<FlightInSector> sortedFlights=null;
  /**
   * Used for sorting the flights according to the flight identifiers
   * and for finding flights by their identifiers
   */
  public TreeMap<String,Integer> flights=null;
  public HashSet<String> repeatedVisits=null;
  
  public LocalTime tFirst=null, tLast=null;
  /**
   * Last computed hourly counts of the flights
   */
  protected int flightCounts[]=null;
  /**
   * The time step of the last computed hourly flight counts
   */
  protected int tStepFlightCounts =0;
  /**
   * Last computed hourly counts of the sector entries
   */
  protected int entryCounts[]=null;
  /**
   * The time step of the last computed hourly entry counts
   */
  protected int tStepEntryCounts=0;
  /**
   * Whether in the last computation of the entry counts re-entries were ignored
   */
  protected boolean reEntriesIgnored=false;
  
  public synchronized void addFlight(FlightInSector f) {
    if (f==null || f.entryTime==null)
      return;
    if (sortedFlights==null)
      sortedFlights=new ArrayList<FlightInSector>(500);
    int idx;
    for (idx=0; idx<sortedFlights.size() && f.compareTo(sortedFlights.get(idx))>=0; idx++);
    sortedFlights.add(idx,f);

    if (tFirst==null || f.entryTime.compareTo(tFirst)<0)
      tFirst=f.entryTime;
    if (tLast==null || f.exitTime.compareTo(tLast)>0)
      tLast=f.exitTime;
  }
  
  public int getNFlights() {
    if (sortedFlights==null)
      return 0;
    return sortedFlights.size();
  }
  
  /**
   * @param binWidth - always in minutes
   */
  public int getMinuteOfDayBinIndex(int minuteOfDay, int binWidth){
    if (minuteOfDay<0 || binWidth<=0)
      return -1;
    return minuteOfDay/binWidth;
  }
  
  public int getTimeBinIndex(LocalTime t, int binWidth) {
    if (t==null || binWidth<=0)
      return -1;
    return getMinuteOfDayBinIndex(t.getHour()*60+t.getMinute(),binWidth);
  }
  
  public LocalTime[] getTimeBinRange(int binIdx, int binWidth) {
    if (binWidth<=0)
      return null;
    LocalTime tt[]=new LocalTime[2];
    int m=binIdx*binWidth;
    tt[0]=LocalTime.of((m/60)%24,m%60,0);
    m+=59;
    tt[1]=LocalTime.of((m/60)%24,m%60,59);
    return tt;
  }
  
  public int getAggregationTimeStep(){
    return tStepFlightCounts;
  }
  /**
   * Computes hourly flight counts with the given time step, in minutes
   */
  public int[] getHourlyFlightCounts(int tStep) {
    if (tStep<=0)
      return null;
    if (flightCounts!=null && tStepFlightCounts ==tStep)
      return flightCounts;
    int nSteps=minutesInDay/tStep;
    if (nSteps*tStep<minutesInDay)
      ++nSteps;
    int counts[]=new int[nSteps];
    for (int i=0; i<nSteps; i++)
      counts[i]=0;
    HashSet<String> done=(repeatedVisits==null)?null:
                             new HashSet<String>(Math.round(repeatedVisits.size()*1.5f));
    if (sortedFlights!=null)
      for (int i=0; i<sortedFlights.size(); i++) {
        FlightInSector f=sortedFlights.get(i);
        if (f.entryTime==null || f.exitTime==null)
          continue;
        if (done!=null && done.contains(f.flightId))
          continue;
        int m1 = f.entryTime.getHour() * 60 + f.entryTime.getMinute(),
            m2 = f.exitTime.getHour() * 60 + f.exitTime.getMinute();
        int idx1 = (m1-59)/tStep+1, idx2 = m2/tStep;
        if (idx1<0 || idx2<idx1)
          continue;
        for (int j = Math.max(0,idx1); j < idx2 && j < counts.length; j++)
          ++counts[j];
        if (repeatedVisits!=null && repeatedVisits.contains(f.flightId) &&
                !done.contains(f.flightId)){
          FlightInSector visits[]=getAllVisits(f.flightId);
          for (int k=1; k<visits.length; k++) {
            m1 = visits[k].entryTime.getHour() * 60 + visits[k].entryTime.getMinute();
            m2 = visits[k].exitTime.getHour() * 60 + visits[k].exitTime.getMinute();
            int nextIdx1 = Math.max((m1-59)/tStep+1, idx2+1), nextIdx2 = m2/ tStep;
            if (nextIdx2<nextIdx1) //already counted
              continue;
            for (int j = Math.max(0,nextIdx1); j < nextIdx2 && j < counts.length; j++)
              ++counts[j];
            idx2=nextIdx2;
          }
          done.add(f.flightId);
        }
      }
    flightCounts=counts;
    tStepFlightCounts =tStep;
    return counts;
  }
  /**
   * Computes hourly counts of sector entries with the given time step, in minutes
   */
  public int[] getHourlyEntryCounts(int tStep, boolean ignoreReEntries) {
    if (tStep<=0)
      return null;
    /*
    if (ignoreReEntries && tStep==20 && sectorId.equals("LECMTLL")) {
      entryCounts=null;
    }
    */
    if (entryCounts!=null && tStepEntryCounts ==tStep && reEntriesIgnored==ignoreReEntries)
      return entryCounts;
    int nSteps=minutesInDay/tStep;
    if (nSteps*tStep<minutesInDay)
      ++nSteps;
    int counts[]=new int[nSteps];
    for (int i=0; i<nSteps; i++)
      counts[i]=0;
    HashSet<String> done=(repeatedVisits==null || !ignoreReEntries)?null:
                             new HashSet<String>(Math.round(repeatedVisits.size()*1.5f));
    if (sortedFlights!=null)
      for (int i=0; i<sortedFlights.size(); i++) {
        FlightInSector f=sortedFlights.get(i);
        if (f.entryTime==null)
          continue;
        if (done!=null && done.contains(f.flightId))
          continue;
        int m=f.entryTime.getHour()*60+f.entryTime.getMinute();
        int idx1=(m-59)/tStep+1, idx2=m/tStep;
        for (int j=Math.max(0,idx1); j<idx2 && j<counts.length; j++)
          ++counts[j];
        if (ignoreReEntries &&
                repeatedVisits!=null && repeatedVisits.contains(f.flightId) &&
                !done.contains(f.flightId)){
          FlightInSector visits[]=getAllVisits(f.flightId);
          for (int k=1; k<visits.length; k++) {
            m = visits[k].entryTime.getHour() * 60 + visits[k].entryTime.getMinute();
            int nextIdx1 = (m-59)/tStep+1, nextIdx2 = m/tStep;
            if (nextIdx2<=idx2) //already counted
              continue;
            for (int j = Math.max(idx2+1,nextIdx1); j < nextIdx2 && j < counts.length; j++)
              ++counts[j];
            idx2=nextIdx2;
          }
          done.add(f.flightId);
        }
      }
    entryCounts=counts;
    tStepEntryCounts =tStep;
    reEntriesIgnored=ignoreReEntries;
    return counts;
  }
  
  protected void makeFlightIndex() {
    if (flights!=null && !flights.isEmpty())
      return;
    if (sortedFlights==null || sortedFlights.isEmpty())
      return;
    flights=new TreeMap<String,Integer>();
    repeatedVisits=null;
    
    for (int i=0; i<sortedFlights.size(); i++) {
      FlightInSector f=sortedFlights.get(i);
      if (!flights.containsKey(f.flightId))
        flights.put(f.flightId, i);
      else {
        if (repeatedVisits==null)
          repeatedVisits=new HashSet<String>();
        repeatedVisits.add(f.flightId);
        if (flights.get(f.flightId)>i)
          flights.put(f.flightId, i);
      }
    }
  }
  
  public boolean hasFlight(String flightId) {
    if (flightId==null || sortedFlights==null || sortedFlights.isEmpty())
      return false;
    if (flights==null || flights.isEmpty())
      makeFlightIndex();
    if (flights==null || flights.isEmpty())
      return false;
    return flights.containsKey(flightId);
  }
  
  public FlightInSector getFlightData(String flightId, LocalTime tBefore, LocalTime tAfter) {
    if (flightId==null || sortedFlights==null || sortedFlights.isEmpty())
      return null;
    if (flights==null || flights.isEmpty())
      makeFlightIndex();
    if (flights==null || flights.isEmpty())
      return null;
    Integer idx=flights.get(flightId);
    if (idx==null)
      return null;
    FlightInSector f=sortedFlights.get(idx);
    if (f==null)
      return null;
    if (tBefore!=null) {
      if (f.exitTime.compareTo(tBefore)>0) //too late!
        return null;
      //perhaps, there is a later flight visit that happenedf.flightId))
      for (int i=idx+1; i<sortedFlights.size() && sortedFlights.get(i).entryTime.compareTo(tBefore)<=0; i++)
        if (sortedFlights.get(i).flightId.equals(flightId))
          if (sortedFlights.get(i).exitTime.compareTo(tBefore)<=0)
            f=sortedFlights.get(i);
          else
            break;
    }
    if (tAfter!=null) {
      if (f.entryTime.compareTo(tAfter)>=0)
        return f;
      f=null;
      //perhaps, there is a later flight visit that happened after tAfter
      if (repeatedVisits!=null && repeatedVisits.contains(flightId))
        for (int i=idx+1; i<sortedFlights.size() && f==null; i++)
          if (sortedFlights.get(i).flightId.equals(flightId))
            if (sortedFlights.get(i).entryTime.compareTo(tAfter)>=0)
              f=sortedFlights.get(i);
    }
    return f;
  }
  
  public FlightInSector[] getAllVisits(String flightId) {
    if (flightId==null || sortedFlights==null || sortedFlights.isEmpty())
      return null;
    if (flights==null || flights.isEmpty())
      makeFlightIndex();
    if (flights==null || flights.isEmpty())
      return null;
    Integer idx=flights.get(flightId);
    if (idx==null)
      return null;
    FlightInSector f=sortedFlights.get(idx);
    if (f==null)
      return null;
    if (repeatedVisits==null || !repeatedVisits.contains(flightId)) {
      FlightInSector result[]={f};
      return result;
    }
    ArrayList<FlightInSector> visits=new ArrayList<FlightInSector>(5);
    visits.add(f);
    for (int i=idx+1; i<sortedFlights.size(); i++)
      if (sortedFlights.get(i).flightId.equals(flightId))
        visits.add(sortedFlights.get(i));
    FlightInSector result[]=new FlightInSector[visits.size()];
    result=visits.toArray(result);
    return result;
  }
  
}
