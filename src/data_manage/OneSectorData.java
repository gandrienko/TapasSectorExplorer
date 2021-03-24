package data_manage;

import java.time.LocalTime;
import java.util.ArrayList;
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
    flights.put(f.flightId,idx);
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
}
