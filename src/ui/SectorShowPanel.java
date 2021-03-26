package ui;

import data_manage.OneSectorData;
import data_manage.SectorSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class SectorShowPanel extends JPanel  implements ActionListener {
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * The canvas with the visual representation
   */
  protected SectorShowCanvas canvas=null;
  /**
   * List of sectors ordered by their identifiers
   */
  protected ArrayList<OneSectorData> sortedSectors=null;
  /**
   * Used for selection of the sector in focus
   */
  protected JComboBox chSectors=null;
  /**
   * Keeps a history of sector selection to enable returning to previous views
   */
  protected ArrayList<Integer> sectorSelections=null;
  /**
   * Used for returning to previous sector selection
   */
  protected JButton backButton=null;
  
  public SectorShowPanel(SectorSet sectors) {
    super();
    if (sectors==null || sectors.getNSectors()<1)
      return;
    setLayout(new BorderLayout());

    sortedSectors=sectors.getSectorsSortedByIdentifiers();
    chSectors=new JComboBox();
    chSectors.addActionListener(this);
    
    int maxNFlights=0;
    int maxIdx=-1;
    for (int i=0; i<sortedSectors.size(); i++) {
      OneSectorData s=sortedSectors.get(i);
      int nFlights=s.getNFlights();
      chSectors.addItem(s.sectorId+" ("+nFlights+" flights)");
      if (maxNFlights<nFlights) {
        maxNFlights=nFlights;
        maxIdx=i;
      }
    }
    chSectors.setSelectedIndex(maxIdx);
    JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,20,5));
    add(p,BorderLayout.SOUTH);
    p.add(new JLabel("Sectors:"));
    p.add(chSectors);
    backButton=new JButton("show previous");
    backButton.setActionCommand("back");
    backButton.setEnabled(false);
    backButton.addActionListener(this);
    p.add(backButton);
    
    canvas=new SectorShowCanvas(sectors);
    add(canvas,BorderLayout.CENTER);
    canvas.setFocusSector(sortedSectors.get(maxIdx).sectorId);
    canvas.addActionListener(this);
  
    sectorSelections=new ArrayList<Integer>(100);
    sectorSelections.add(maxIdx);
  }
  
  
  public void actionPerformed (ActionEvent ae) {
    if (ae.getSource().equals(backButton)) {
      if (sectorSelections.size()>1) {
        sectorSelections.remove(sectorSelections.size()-1);
        if (sectorSelections.size()<2)
          backButton.setEnabled(false);
        int sIdx=sectorSelections.get(sectorSelections.size()-1);
        if (sIdx!=chSectors.getSelectedIndex())
          chSectors.setSelectedIndex(sIdx);
      }
    }
    else
    if (ae.getSource().equals(chSectors) && canvas!=null) {
      int selIdx=chSectors.getSelectedIndex();
      if (selIdx!=sectorSelections.get(sectorSelections.size()-1)) {
        sectorSelections.add(selIdx);
        if (sectorSelections.size()==2)
          backButton.setEnabled(true);
      }
      canvas.setFocusSector(sortedSectors.get(selIdx).sectorId);
    }
    else
    if (ae.getSource().equals(canvas)) {
      String id=ae.getActionCommand();
      if (id.startsWith("select_sector:")) {
        id=id.substring(14);
        int sIdx=-1;
        for (int i=0; i<sortedSectors.size() && sIdx<0; i++)
          if (id.equals(sortedSectors.get(i).sectorId))
            sIdx=i;
        if (sIdx>=0 && sIdx!=chSectors.getSelectedIndex())
          chSectors.setSelectedIndex(sIdx);
      }
    }
  }
}
