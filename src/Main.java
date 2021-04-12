import data_manage.*;
import ui.SectorShowPanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
      String fileNameBaseline=null, fileNameSolution=null, fileNameCapacities=null;
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("params.txt")))) ;
        String strLine;
        try {
          while ((strLine = br.readLine()) != null) {
            String str=strLine.replaceAll("\"","").replaceAll(" ","");
            String[] tokens=str.split("=");
            if (tokens==null || tokens.length<2)
              continue;
            String parName=tokens[0].trim().toLowerCase();
            if (parName.equals("data_baseline"))
              fileNameBaseline=tokens[1].trim();
            else
            if (parName.equals("data_solution"))
              fileNameSolution=tokens[1].trim();
            else
            if (parName.equals("sector_capacities"))
              fileNameCapacities=tokens[1].trim();
          }
        } catch (IOException io) {
          System.out.println(io);
        }
      } catch (IOException io) {
        System.out.println(io);
      }
      if (fileNameBaseline==null)
        return;

      DataStore baseline=new DataStore();
      System.out.println("Tryng to get baseline data ...");
      if (!baseline.readData(fileNameBaseline))
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
      data_manage.DataStore solution=new data_manage.DataStore();
      System.out.println("Tryng to get solution data ...");
      if (solution.readData(fileNameSolution)) {
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
      
      if (fileNameCapacities!=null) {
        System.out.println("Reading file with sector capacities...");
        DataStore capData=new DataStore();
        if (!capData.readData(fileNameCapacities))
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
      if (sectorsSolution!=null)
        scenarios[1]=sectorsSolution;
      
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
