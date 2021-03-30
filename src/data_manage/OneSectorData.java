package data_manage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

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
    if (sortedFlights!=null)
      for (int i=0; i<sortedFlights.size(); i++) {
        FlightInSector f=sortedFlights.get(i);
        if (f.entryTime==null || f.exitTime==null)
          continue;
        int m1=f.entryTime.getHour()*60+f.entryTime.getMinute(),
            m2=f.exitTime.getHour()*60+f.exitTime.getMinute()+60;
        int idx1=m1/tStep, idx2=m2/tStep;
        for (int j=idx1; j<=idx2 && j<counts.length; j++)
          ++counts[j];
      }
    flightCounts=counts;
    tStepFlightCounts =tStep;
    return counts;
  }
  /**
   * Computes hourly counts of sector entries with the given time step, in minutes
   */
  public int[] getHourlyEntryCounts(int tStep) {
    if (tStep<=0)
      return null;
    if (entryCounts!=null && tStepEntryCounts ==tStep)
      return entryCounts;
    int nSteps=minutesInDay/tStep;
    if (nSteps*tStep<minutesInDay)
      ++nSteps;
    int counts[]=new int[nSteps];
    for (int i=0; i<nSteps; i++)
      counts[i]=0;
    if (sortedFlights!=null)
      for (int i=0; i<sortedFlights.size(); i++) {
        FlightInSector f=sortedFlights.get(i);
        if (f.entryTime==null)
          continue;
        int m1=f.entryTime.getHour()*60+f.entryTime.getMinute(), m2=m1+60;
        int idx1=m1/tStep, idx2=m2/tStep;
        for (int j=idx1; j<=idx2 && j<counts.length; j++)
          ++counts[j];
      }
    entryCounts=counts;
    tStepEntryCounts =tStep;
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
}
