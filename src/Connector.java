import TapasDataReader.Record;
import data_manage.DataStore;
import data_manage.SectorSet;
import ui.SectorShowPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.Vector;

public class Connector {

  public void Connector(Vector<Record> records[]) {
    SectorSet sectorsBaseline=new SectorSet();
    sectorsBaseline.addFlightData(records[0]);
    SectorSet sectorsSolution=null;
    if (records.length>1) {
      sectorsSolution=new SectorSet();
      sectorsSolution.addFlightData(records[1]);
    }
    // capacities ...

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
