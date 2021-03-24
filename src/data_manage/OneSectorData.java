package data_manage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Contains data describing flights visiting one sector
 */
public class OneSectorData {
  /**
   * Sector identifier
   */
  public String sectorId=null;
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
  
  public synchronized void addFlight(FlightInSector f) {
    if (f==null || f.entryTime==null)
      return;
    if (sortedFlights==null)
      sortedFlights=new ArrayList<FlightInSector>(500);
    int idx;
    for (idx=0; idx<sortedFlights.size() && f.compareTo(sortedFlights.get(idx))>=0; idx++);
    sortedFlights.add(idx,f);
    if (flights==null)
      flights=new TreeMap<String,Integer>();
    if (!flights.containsKey(f.flightId))
      flights.put(f.flightId, idx);
    else {
      if (repeatedVisits==null)
        repeatedVisits=new HashSet<String>();
      repeatedVisits.add(f.flightId);
      if (flights.get(f.flightId)>idx)
        flights.put(f.flightId, idx);
    }
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
  
  public FlightInSector getFlightData(String flightId, LocalTime tBefore, LocalTime tAfter) {
    if (flightId==null || flights==null || flights.isEmpty())
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
