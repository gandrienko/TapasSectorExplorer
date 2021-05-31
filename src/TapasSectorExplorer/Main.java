package TapasSectorExplorer;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasSectorExplorer.data_manage.*;
import TapasSectorExplorer.ui.SectorShowPanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.util.*;

public class Main {

  public static void main(String[] args) {
    String parFileName=(args!=null && args.length>0)?args[0]:"params.txt";
    String path=null;
    Hashtable<String,String> fNames=new Hashtable<String,String>(10);
    try {
      BufferedReader br = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(new File(parFileName)))) ;
      String strLine;
      try {
        while ((strLine = br.readLine()) != null) {
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split("=");
          if (tokens==null || tokens.length<2)
            continue;
          String parName=tokens[0].trim().toLowerCase();
          if (parName.equals("path") || parName.equals("data_path"))
            path=tokens[1].trim();
          else
            fNames.put(parName,tokens[1].trim());
        }
      } catch (IOException io) {
        System.out.println(io);
      }
    } catch (IOException io) {
      System.out.println(io);
    }
    if (path!=null) {
      for (Map.Entry<String,String> e:fNames.entrySet()) {
        String fName=e.getValue();
        if (!fName.startsWith("\\") && !fName.contains(":\\")) {
          fName=path+fName;
          fNames.put(e.getKey(),fName);
        }
      }
    }
    else
      path="";

    SectorSet sectorsBaseline=null;
    SectorSet sectorsSolution=null;

    String fNameBaseline=fNames.get("baseline");
    if (fNameBaseline==null)
      fNameBaseline=fNames.get("data_baseline");
    
    if (fNameBaseline!=null) {
      DataStore baseline=new DataStore();
      System.out.println("Tryng to get baseline data ...");
      if (!baseline.readData(fNameBaseline))
        return;
      System.out.println("Successfully got baseline data!");

      sectorsBaseline=new SectorSet();
      int nFlights=0, nFailed=0;
      for (int i=0; i<baseline.data.size(); i++) {
        if (sectorsBaseline.addFlightData(baseline.data.elementAt(i), baseline.attrNames))
          ++nFlights;
        else
          ++nFailed;
      }
      System.out.println(nFlights+" successfully added; "+nFailed+" failed.");
      if (nFlights<1)
        return;
      if (sectorsBaseline.getNSectors()<1) {
        System.out.println("Failed to retrieve flight data!");
        return;
      }
      System.out.println("Got data about "+sectorsBaseline.getNSectors()+" sectors!");
      LocalTime range[]=sectorsBaseline.getTimeRange();
      System.out.println("Overall time range: "+range[0]+".."+range[1]);

      TapasSectorExplorer.data_manage.DataStore solution=new TapasSectorExplorer.data_manage.DataStore();
      String fName=fNames.get("solution");
      if (fName==null)
        fName=fNames.get("data_solution");
      if (fName!=null) {
        System.out.println("Tryng to get solution data ...");
        if (solution.readData(fName)) {
          System.out.println("Successfully got solution data!");
    
          sectorsSolution=new SectorSet();
          nFlights=0; nFailed=0;
          for (int i=0; i<solution.data.size(); i++) {
            if (sectorsSolution.addFlightData(solution.data.elementAt(i), solution.attrNames))
              ++nFlights;
            else
              ++nFailed;
          }
          System.out.println(nFlights+" successfully added; "+nFailed+" failed.");
          if (nFlights<1)
            return;
          if (sectorsSolution.getNSectors()<1) {
            System.out.println("Failed to retrieve flight data for the solution!");
            sectorsSolution=null;
          }
          else {
            System.out.println("Got data about " + sectorsSolution.getNSectors() + " sectors for the solution!");
            range = sectorsSolution.getTimeRange();
            System.out.println("Overall time range for the solution: " + range[0] + ".." + range[1]);
          }
        }
        else {
          System.out.println("Failed to get solution data!");
        }
      }
      else {
        System.out.println("No solution file name in the parameters!");
      }
    }
    else {
      String fnDecisions=fNames.get("decisions");
      if (fnDecisions==null) {
        System.out.println("No decisions file name in the parameters!");
        return;
      }
      System.out.println("Decisions file name = "+fnDecisions);
      String fnFlightPlans=fNames.get("flight_plans");
      if (fnFlightPlans==null) {
        System.out.println("No flight plans file name in the parameters!");
        return;
      }
      System.out.println("Flight plans file name = "+fnFlightPlans);
      String fnCapacities=fNames.get("sector_capacities");
      if (fnCapacities==null)
        fnCapacities=fNames.get("capacities");
      if (fnCapacities==null) {
        System.out.println("No capacities file name in the parameters!");
        //return;
      }
      System.out.println("Capacities file name = "+fnCapacities);

      TreeSet<Integer> steps=TapasDataReader.Readers.readStepsFromDecisions(fnDecisions);
      if (steps==null || steps.isEmpty()) {
        System.out.println("Failed to read the decision steps from file "+fnDecisions+" !");
        return;
      }
      System.out.println("Got "+steps.size()+" decision steps!");
      Hashtable<String, Flight> flights=
          TapasDataReader.Readers.readFlightDelaysFromDecisions(fnDecisions,steps);
      if (flights==null || flights.isEmpty()) {
        System.out.println("Failed to read flight data from file "+fnDecisions+" !");
        return;
      }
      System.out.println("Got "+flights.size()+" flights from the decisions file!");
      Hashtable<String, Vector<Record>> flightPlans=
          TapasDataReader.Readers.readFlightPlans(fnFlightPlans,flights);
      if (flightPlans==null || flightPlans.isEmpty()) {
        System.out.println("Failed to read flight plans from file "+fnFlightPlans+" !");
        return;
      }
      System.out.println("Got "+flights.size()+" flight plans!");

      sectorsBaseline=new SectorSet();
      sectorsBaseline.addFlightData(getRecordsForStep(flightPlans,0));
      sectorsBaseline.name="baseline";
      if (steps.size()>1) {
        sectorsSolution=new SectorSet();
        sectorsSolution.addFlightData(getRecordsForStep(flightPlans,steps.size()-1));
        sectorsSolution.name="step "+steps.last();
      }
    }
  
    String fName=fNames.get("sector_capacities");
    if (fName==null)
      fName=fNames.get("capacities");
    if (fName!=null) {
      System.out.println("Reading file with sector capacities...");
      DataStore capData=new DataStore();
      if (!capData.readData(fName))
        System.out.println("Failed to get capacity data!");
      else {
        System.out.println("Got "+capData.data.size()+" data records; trying to get capacities...");
        sectorsBaseline.getSectorCapacities(capData);
        if (sectorsSolution!=null)
          sectorsSolution.getSectorCapacities(capData);
      }
    }
    
    SectorSet scenarios[]=new SectorSet[(sectorsSolution==null)?1:2];
    scenarios[0]=sectorsBaseline;
    if (sectorsSolution!=null) {
      scenarios[0].name="baseline";
      scenarios[1] = sectorsSolution;
      scenarios[1].name="solution";
    }
    
    JFrame frame = new JFrame("TAPAS Sector Explorer");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    SectorShowPanel sectorShow=new SectorShowPanel(scenarios);
    frame.getContentPane().add(sectorShow, BorderLayout.CENTER);
    //Display the window.
    frame.pack();
    frame.setLocation(50,50);
    frame.setVisible(true);
  }
  
  public static Vector<Record> getRecordsForStep (Hashtable<String,Vector<Record>> records, int step) {
    Vector<Record> vr=new Vector(records.size()/100,records.size()/100);
    String stepAsStr=""+step;
    Set<String> keys=records.keySet();
    for (String key:keys) {
      String tokens[]=key.split("_");
      if (tokens!=null && tokens.length>1 && stepAsStr.equals(tokens[1]))
        for (Record r:records.get(key))
          vr.add(r);
    }
    return vr;
  }
}
