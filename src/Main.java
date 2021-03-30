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
      
      SectorSet sectors=new SectorSet();
      int nFlights=0, nFailed=0;
      for (int i=0; i<baseline.data.size(); i++) {
        if (sectors.addFlightData(baseline.data.elementAt(i), baseline.attrNames))
          ++nFlights;
        else
          ++nFailed;
      }
      System.out.println(nFlights+" successfully added; "+nFailed+" failed.");
      if (nFlights<1)
        return;
      ArrayList<OneSectorData> sortedSectors=sectors.getSectorsSortedByNFlights();
      if (sortedSectors==null) {
        System.out.println("Failed to retrieve flight data!");
        return;
      }
      System.out.println("Got data about "+sortedSectors.size()+" sectors!");
      if (fileNameCapacities!=null) {
        System.out.println("Reading file with sector capacities...");
        DataStore capData=new DataStore();
        if (!capData.readData(fileNameCapacities))
          System.out.println("Failed to get capacity data!");
        else {
          System.out.println("Got "+capData.data.size()+" data records; trying to get capacities...");
          sectors.getSectorCapacities(capData);
        }
      }
      /*
      for (int i=0; i<sortedSectors.size(); i++) {
        OneSectorData s=sortedSectors.get(i);
        System.out.println(s.sectorId + " : " +s.getNFlights() + " flights; time range: "+
                              s.tFirst+".."+s.tLast);
      }
      */
      LocalTime range[]=sectors.getTimeRange();
      System.out.println("Overall time range: "+range[0]+".."+range[1]);
      
      /*
      data_manage.DataStore solution=new data_manage.DataStore();
      System.out.println("Tryng to get solution data ...");
      if (solution.readData(fileNameSolution)) {
        System.out.println("Successfully got solution data!");
      }
      else {
        System.out.println("Failed to get solution data!");
      }
      */
      JFrame frame = new JFrame("TAPAS Sector Explorer");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      SectorShowPanel sectorShow=new SectorShowPanel(sectors);
      frame.getContentPane().add(sectorShow, BorderLayout.CENTER);
      //Display the window.
      frame.pack();
      frame.setLocation(50,50);
      frame.setVisible(true);
    }
}
