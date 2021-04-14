package data_manage;

import TapasDataReader.Record;

import java.time.LocalTime;

/**
 * Contains information about a single visit of a sector by a flight.
 */
public class FlightInSector implements Comparable<FlightInSector>{
  /**
   * Sector identifier
   */
  public String sectorId=null;
  /**
   * Flight identifier
   */
  public String flightId=null;
  /**
   * Times of entering and exiting the sector
   */
  public LocalTime entryTime =null, exitTime=null;
  /**
   * Delay, in minutes
   */
  public int delay=0;
  /**
   * Identifiers of the previous and next sectors
   */
  public String prevSectorId=null, nextSectorId=null;
  
  public boolean equals (Object obj) {
    if (obj==null)
      return false;
    if (!(obj instanceof FlightInSector))
      return false;
    if (this==obj)
      return true;
    FlightInSector f=(FlightInSector)obj;
    if (!this.flightId.equals(f.flightId)) return false;
    if (!this.sectorId.equals(f.sectorId)) return false;
    if (!this.entryTime.equals(f.entryTime)) return false;
    if (!this.exitTime.equals(f.exitTime)) return false;
    return true;
  }
  
  /**
   * Tries to get values of internal variables from the given data record.
   * If successful, constructs and returns an instance of the class.
   * @param data - data record, an array of attribute values
   * @param attrNames - an array of attribute names
   * @return an instance of data_manage.FlightInSector if successfully got values
   *         for mandatory variables: sectorId, flightId, entryTime, exitTime
   */
  public static FlightInSector getFlightData(Object data[], String attrNames[]) {
    if (data==null || data.length<4 || attrNames==null || attrNames.length<4)
      return null;
    FlightInSector fis=new FlightInSector();
    for (int i=0; i<attrNames.length && i<data.length; i++) {
      if (attrNames[i]==null)
        continue;
      if (attrNames[i].equalsIgnoreCase("FlightID")) {
        if (data[i]==null)
          return null;
        fis.flightId=data[i].toString();
      }
      else
      if (attrNames[i].equalsIgnoreCase("Sector")) {
        if (data[i]==null)
          return null;
        fis.sectorId=data[i].toString();
      }
      else
      if (attrNames[i].equalsIgnoreCase("FromSector"))
        fis.prevSectorId=(data[i]==null)?null:data[i].toString();
      else
      if (attrNames[i].equalsIgnoreCase("ToSector"))
        fis.nextSectorId=(data[i]==null)?null:data[i].toString();
      else
      if (attrNames[i].equalsIgnoreCase("Delays")) {
        if (data[i]!=null && (data[i] instanceof Integer))
          fis.delay=((Integer)data[i]).intValue();
      }
      else
      if (attrNames[i].equalsIgnoreCase("EntryTime") ||
          attrNames[i].equalsIgnoreCase("ExitTime")) {
        if (data[i]==null)
          return null;
        LocalTime t=LocalTime.parse(data[i].toString());
        if (t!=null)
          if (attrNames[i].equalsIgnoreCase("EntryTime"))
            fis.entryTime =t;
          else
            fis.exitTime=t;
      }
    }
    if (fis.sectorId!=null && fis.flightId!=null && fis.entryTime !=null && fis.exitTime!=null)
      return fis;
    return null;
  }

  public static FlightInSector getFlightData(Record record) {
    if (record==null)
      return null;
    else {
      FlightInSector fis=new FlightInSector();
      fis.flightId=record.flight;
      fis.sectorId=record.sector;
      fis.delay=record.delay;
      fis.entryTime=LocalTime.parse(record.FromT);
      fis.exitTime=LocalTime.parse(record.ToT);
      fis.prevSectorId=record.FromS;
      fis.nextSectorId=record.ToS;
      return fis;
    }
  }
  
  @Override
  public int compareTo(FlightInSector f2) {
    if (this==null || this.entryTime==null)
      return (f2==null || f2.entryTime==null)?0:1;
    if (f2==null || f2.entryTime==null)
      return -1;
    int c=this.entryTime.compareTo(f2.entryTime);
    if (c!=0)
      return c;
    c=this.exitTime.compareTo(f2.exitTime);
    if (c!=0)
      return c;
    return this.flightId.compareTo(f2.flightId);
  }
}
