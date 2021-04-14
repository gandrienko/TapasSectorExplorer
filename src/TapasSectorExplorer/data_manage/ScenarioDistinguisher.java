package TapasSectorExplorer.data_manage;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes and stores differences between two scenarios represented by
 * two instances of SectorSet
 */
public class ScenarioDistinguisher extends SectorSet {
  /**
   * The scenarios (sector sets) that are compared.
   * The first scenario is considered as baseline, to which the second scenario is compared.
   */
  public SectorSet scenario1=null, scenario2=null;
  /**
   * Sector descriptors keepind data about modified versions of the flights
   */
  public TreeMap<String,OneSectorData> altSectors=null;
  /**
   * Modified versions of the flights.
   * For each flight: the sequence of visited sectors, in chronological order
   */
  public TreeMap<String,ArrayList<FlightInSector>> altFlights=null;
  
  /**
   * Computes differences between the two scenarios. Stores the scenarios and the
   * results of the comparison in its internal structures.
   * Returns true if successful.
   */
  public boolean compareScenarios(SectorSet sc1, SectorSet sc2) {
    if (sc1==null || sc2==null ||
            sc1.sectors==null || sc2.sectors==null || sc1.sectors.isEmpty() || sc2.sectors.isEmpty() ||
            sc1.flights==null || sc2.flights==null || sc1.flights.isEmpty() || sc2.flights.isEmpty())
      return false;
    scenario1=sc1; scenario2=sc2;
  
    //create empty sector records for all sectors occurring in any of the scenarios
    for (Map.Entry<String,OneSectorData> entry:sc1.sectors.entrySet()) {
      String sectorId = entry.getKey();
      OneSectorData sDiff=new OneSectorData();
      sDiff.sectorId=sectorId;
      sDiff.capacity=entry.getValue().capacity;
      addSector(sDiff);
      sDiff=new OneSectorData();
      sDiff.sectorId=sectorId;
      sDiff.capacity=entry.getValue().capacity;
      addAltSector(sDiff);
    }
    for (Map.Entry<String,OneSectorData> entry:sc2.sectors.entrySet()) {
      String sectorId = entry.getKey();
      if (!sectors.containsKey(sectorId)) {
        OneSectorData sDiff = new OneSectorData();
        sDiff.sectorId = sectorId;
        sDiff.capacity=entry.getValue().capacity;
        addSector(sDiff);
        sDiff=new OneSectorData();
        sDiff.sectorId=sectorId;
        sDiff.capacity=entry.getValue().capacity;
        addAltSector(sDiff);
      }
    }
    
    //for each flight, get the full path and add data to the sector descriptors in both sets of sectors
    flights=new TreeMap<String, ArrayList<FlightInSector>>();
    altFlights=new TreeMap<String, ArrayList<FlightInSector>>();
    
    for (Map.Entry<String,ArrayList<FlightInSector>> entry:sc1.flights.entrySet()) {
      String fId=entry.getKey();
      ArrayList<FlightInSector> seq=entry.getValue(), altSeq=sc2.getSectorVisitSequence(fId);
      if (sameSequence(seq,altSeq)) //the flight remains unchanged
        continue;
      for (int i=0; i<seq.size(); i++) {
        OneSectorData sector=sectors.get(seq.get(i).sectorId);
        sector.addFlight(seq.get(i));
      }
      flights.put(fId,seq);
      if (altSeq!=null) {
        for (int i = 0; i < altSeq.size(); i++) {
          OneSectorData sector = altSectors.get(altSeq.get(i).sectorId);
          sector.addFlight(altSeq.get(i));
        }
        altFlights.put(fId,altSeq);
      }
    }
    if (flights.size()>altFlights.size() && altFlights.size()<sc2.flights.size())
      //there are some flights in the second scenario that are absent in the first one
      for (Map.Entry<String,ArrayList<FlightInSector>> entry:sc2.flights.entrySet()) {
        String fId = entry.getKey();
        if (!altFlights.containsKey(fId)) {
          ArrayList<FlightInSector> altSeq=sc2.getSectorVisitSequence(fId);
          if (altSeq!=null) {
            for (int i = 0; i < altSeq.size(); i++) {
              OneSectorData sector = altSectors.get(altSeq.get(i).sectorId);
              sector.addFlight(altSeq.get(i));
            }
            altFlights.put(fId,altSeq);
          }
        }
      }
    
    return sectors!=null && !sectors.isEmpty() && flights!=null && !flights.isEmpty();
  }
  
  public void addAltSector(OneSectorData sector) {
    if (sector==null)
      return;
    if (altSectors==null)
      altSectors=new TreeMap<String,OneSectorData>();
    altSectors.put(sector.sectorId,sector);
  }
  
  public OneSectorData getAltSectorData(String sectorId) {
    if (sectorId==null || altSectors==null || altSectors.isEmpty())
      return null;
    return altSectors.get(sectorId);
  }
}
