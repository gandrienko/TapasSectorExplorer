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
   * Sector descriptors keeping data about original and modified versions of the flights
   */
  public TreeMap<String,OneSectorData> origSectors=null, altSectors=null;
  /**
   * Original and modified versions of the flights.
   * For each flight: the sequence of visited sectors, in chronological order
   */
  public TreeMap<String,ArrayList<FlightInSector>> origFlights=null, altFlights=null;
  
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
      for (int i=0; i<3; i++) {
        OneSectorData sDiff = new OneSectorData();
        sDiff.sectorId = sectorId;
        sDiff.capacity = entry.getValue().capacity;
        switch (i) {
          case 0:
            addSector(sDiff);
            break;
          case 1:
            addOrigSector(sDiff);
            break;
          case 2:
            addAltSector(sDiff);
            break;
        }
      }
    }
    for (Map.Entry<String,OneSectorData> entry:sc2.sectors.entrySet()) {
      String sectorId = entry.getKey();
      if (!sectors.containsKey(sectorId))
        for (int i=0; i<3; i++) {
          OneSectorData sDiff = new OneSectorData();
          sDiff.sectorId = sectorId;
          sDiff.capacity = entry.getValue().capacity;
          switch (i) {
            case 0:
              addSector(sDiff);
              break;
            case 1:
              addOrigSector(sDiff);
              break;
            case 2:
              addAltSector(sDiff);
              break;
          }
        }
    }
    
    //for each flight, get the full path and add data to the sector descriptors in both sets of sectors
    flights=new TreeMap<String, ArrayList<FlightInSector>>();
    origFlights=new TreeMap<String, ArrayList<FlightInSector>>();
    altFlights=new TreeMap<String, ArrayList<FlightInSector>>();
    
    for (Map.Entry<String,ArrayList<FlightInSector>> entry:sc1.flights.entrySet()) {
      String fId=entry.getKey();
      ArrayList<FlightInSector> seq=entry.getValue(), altSeq=sc2.getSectorVisitSequence(fId);
      if (sameSequence(seq,altSeq)) //the flight remains unchanged
        continue;
      for (int i=0; i<seq.size(); i++) {
        FlightInSector f=seq.get(i);
        OneSectorData sector=sectors.get(f.sectorId);
        sector.addFlight(f);
        sector=origSectors.get(f.sectorId);
        sector.addFlight(f);
      }
      flights.put(fId,seq);
      origFlights.put(fId,seq);
      if (altSeq!=null) {
        ArrayList<FlightInSector> altSeqCopy=new ArrayList<FlightInSector>(altSeq.size());
        for (int i = 0; i < altSeq.size(); i++) {
          FlightInSector f=altSeq.get(i), fCopy=f.makeCopy();
          fCopy.isModifiedVersion=true;
          fCopy.origFlightId=f.flightId;
          fCopy.flightId+="_mod";
          altSeqCopy.add(fCopy);
          OneSectorData sector = sectors.get(f.sectorId);
          sector.addFlight(fCopy);
          sector=altSectors.get(f.sectorId);
          sector.addFlight(f);
        }
        flights.put(fId+"_mod",altSeqCopy);
        altFlights.put(fId,altSeq);
      }
    }
    //perhaps, there are some flights in the second scenario that are absent in the first one
    for (Map.Entry<String,ArrayList<FlightInSector>> entry:sc2.flights.entrySet()) {
      String fId = entry.getKey();
      if (!sc1.flights.containsKey(fId)) {
        ArrayList<FlightInSector> altSeq=sc2.getSectorVisitSequence(fId);
        if (altSeq!=null) {
          ArrayList<FlightInSector> altSeqCopy=new ArrayList<FlightInSector>(altSeq.size());
          for (int i = 0; i < altSeq.size(); i++) {
            FlightInSector f=altSeq.get(i), fCopy=f.makeCopy();
            fCopy.isModifiedVersion=true;
            fCopy.origFlightId=f.flightId;
            fCopy.flightId+="_mod";
            altSeqCopy.add(fCopy);
            OneSectorData sector = sectors.get(f.sectorId);
            sector.addFlight(fCopy);
            sector=altSectors.get(f.sectorId);
            sector.addFlight(f);
          }
          flights.put(fId+"_mod",altSeqCopy);
          altFlights.put(fId,altSeq);
        }
      }
    }
    
    return sectors!=null && !sectors.isEmpty() && flights!=null && !flights.isEmpty();
  }
  
  public int getNChangedFlights() {
    if (origFlights==null || altFlights==null)
      return 0;
    return Math.max(origFlights.size(),altFlights.size());
  }
  
  public int getNChangedFlights(String sectorId) {
    if (origSectors==null && altSectors==null)
      return 0;
    OneSectorData s1=origSectors.get(sectorId), s2=altSectors.get(sectorId);
    if (s1==null)
      if (s2==null)
        return 0;
      else
        return s2.getNFlights();
    return Math.max(s1.getNFlights(),s2.getNFlights());
  }
  
  public void addOrigSector(OneSectorData sector) {
    if (sector==null)
      return;
    if (origSectors==null)
      origSectors=new TreeMap<String,OneSectorData>();
    origSectors.put(sector.sectorId,sector);
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
