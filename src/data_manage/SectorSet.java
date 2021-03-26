package data_manage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Contains data about multiple sectors
 */
public class SectorSet {
  /**
   * Data about the sectors, sorted according to the sector identifiers
   */
  public TreeMap<String,OneSectorData> sectors=null;
  /**
   * For each flight: the sequence of visited sectors, in chronological order
   */
  public TreeMap<String,ArrayList<FlightInSector>> flights=null;
  
  public OneSectorData getSectorData(String sectorId) {
    if (sectorId==null || sectors==null || sectors.isEmpty())
      return null;
    return sectors.get(sectorId);
  }
  
  public void addSector(OneSectorData sector) {
    if (sector==null)
      return;
    if (sectors==null)
      sectors=new TreeMap<String,OneSectorData>();
    sectors.put(sector.sectorId,sector);
  }
  
  public ArrayList<OneSectorData> getSectorsSortedByNFlights(){
    if (sectors==null || sectors.isEmpty())
      return null;
    ArrayList<OneSectorData> sorted=new ArrayList<OneSectorData>(sectors.size());
    Collection values=sectors.values();
    for (Iterator<OneSectorData> it=values.iterator(); it.hasNext();) {
      OneSectorData s=it.next();
      int idx;
      for (idx=0; idx<sorted.size() && s.getNFlights()<sorted.get(idx).getNFlights(); idx++);
      sorted.add(idx,s);
    }
    return sorted;
  }
  
  public ArrayList<OneSectorData> getSectorsSortedByIdentifiers(){
    if (sectors==null || sectors.isEmpty())
      return null;
    ArrayList<OneSectorData> sorted=new ArrayList<OneSectorData>(sectors.size());
    Collection values=sectors.values();
    for (Iterator<OneSectorData> it=values.iterator(); it.hasNext();)
      sorted.add(it.next());
    return sorted;
  }

  public LocalTime[] getTimeRange(){
    if (sectors==null || sectors.isEmpty())
      return null;
    LocalTime range[]={null,null};
    Collection values=sectors.values();
    for (Iterator<OneSectorData> it=values.iterator(); it.hasNext();) {
      OneSectorData s = it.next();
      if (range[0]==null || range[0].compareTo(s.tFirst)>0)
        range[0]=s.tFirst;
      if (range[1]==null || range[1].compareTo(s.tLast)<0)
        range[1]=s.tLast;
    }
    return range;
  }
  
  public int getNSectors() {
    if (sectors==null)
      return 0;
    return sectors.size();
  }
  
  /**
   * Tries to get data about a flight visit to a sector from the given data record.
   * If successful, constructs and an instance of data_manage.FlightInSector and adds it to the
   * corresponding OneSectorDat. When necessary, creates an instance of data_manage.OneSectorData
   * and adds it to the set of already available sectors..
   * @param data - data record, an array of attribute values
   * @param attrNames - an array of attribute names
   * @return true if successfully added
   */
  public boolean addFlightData(Object data[], String attrNames[]) {
    FlightInSector f=FlightInSector.getFlightData(data,attrNames);
    if (f==null || f.sectorId==null)
      return false;
    OneSectorData s=getSectorData(f.sectorId);
    if (s==null) {
      s=new OneSectorData();
      s.sectorId=f.sectorId;
      addSector(s);
    }
    s.addFlight(f);
    if (flights==null) {
      flights=new TreeMap<String, ArrayList<FlightInSector>>();
    }
    ArrayList<FlightInSector> sequence=flights.get(f.flightId);
    if (sequence==null) {
      sequence = new ArrayList<FlightInSector>(20);
      flights.put(f.flightId,sequence);
    }
    int idx;
    for (idx=0; idx<sequence.size() && f.entryTime.compareTo(sequence.get(idx).exitTime)>=0; idx++);
    sequence.add(idx,f);
    return true;
  }
  
  /**
   * Returns the sequence of sector visits of a given flight
   * @param flightId - identifier of the flight
   * @return sequence of chronologically ordered sector visits
   */
  public ArrayList<FlightInSector> getSectorVisitSequence(String flightId){
    if (flightId==null || flights==null)
      return null;
    return flights.get(flightId);
  }
}
