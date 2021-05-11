package TapasSectorExplorer;

import TapasSectorExplorer.data_manage.*;
import TapasSectorExplorer.ui.SectorShowPanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
      String path=null;
      Hashtable<String,String> fNames=new Hashtable<String,String>(10);
      try {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(new File("params.txt")))) ;
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

      DataStore baseline=new DataStore();
      System.out.println("Tryng to get baseline data ...");
      String fName=fNames.get("baseline");
      if (fName==null)
        fName=fNames.get("data_baseline");
      if (fName==null) {
        System.out.println("No baseline file name in the parameters!");
        return;
      }
      if (!baseline.readData(fName))
        return;
      System.out.println("Successfully got baseline data!");
      
      SectorSet sectorsBaseline=new SectorSet();
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
  
      SectorSet sectorsSolution=null;
      TapasSectorExplorer.data_manage.DataStore solution=new TapasSectorExplorer.data_manage.DataStore();
      fName=fNames.get("solution");
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
  
      fName=fNames.get("sector_capacities");
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
}
