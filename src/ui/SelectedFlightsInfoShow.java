package ui;

import data_manage.FlightInSector;
import data_manage.SectorSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class SelectedFlightsInfoShow extends JPanel implements ItemListener, MouseListener {
  public static Color cbBkgColor=new Color(255,255,204);
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * Identifiers of selected flights
   */
  public ArrayList<String> selectedFlIds=null;
  /**
   * Identifier of the sector that is currently in focus 
   */
  public String focusSectorId=null;
  /**
   * Identifiers of currently shown "from" sectors (i.e., visited directly before the focus sector)
   */
  public ArrayList<String> fromSectorIds=null;
  /**
   * Identifiers of currently shown "to" sectors (i.e., visited directly after the focus sector)
   */
  public ArrayList<String> toSectorIds=null;
  /**
   * Listeners of selections
   */
  protected ArrayList<ActionListener> listeners=null;
  
  public SelectedFlightsInfoShow(SectorSet sectors){
    super();
    this.sectors=sectors;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
  }
  
  public void addActionListener(ActionListener l){
    if (l==null) return;
    if (listeners==null)
      listeners=new ArrayList<ActionListener>(10);
    if (!listeners.contains(l))
      listeners.add(l);
  }
  
  public void removeActionListener(ActionListener l) {
    if (l==null || listeners==null)
      return;
    listeners.remove(l);
  }
  
  public void sendActionEvent(String command) {
    if (listeners==null || listeners.isEmpty())
      return;
    ActionEvent ae=new ActionEvent(this,ActionEvent.ACTION_PERFORMED,command);
    for (int i=0; i<listeners.size(); i++)
      listeners.get(i).actionPerformed(ae);
  }

  public void setCurrentSectors(String focusSectorId,
                                ArrayList<String> fromSectorIds,
                                ArrayList<String> toSectorIds) {
    boolean changed=false;
    if (focusSectorId==null)
      changed=this.focusSectorId!=null;
    else
      changed=!focusSectorId.equals(this.focusSectorId);
    if (!changed)
      if (fromSectorIds==null)
        changed=this.fromSectorIds!=null;
      else
        changed=this.fromSectorIds==null || 
                    fromSectorIds.size()!=this.fromSectorIds.size() || 
                         !fromSectorIds.containsAll(this.fromSectorIds);
    if (!changed)
      if (toSectorIds==null)
        changed=this.toSectorIds!=null;
      else
        changed=this.toSectorIds==null ||
                    toSectorIds.size()!=this.toSectorIds.size() ||
                    !toSectorIds.containsAll(this.toSectorIds);
    if (!changed)  
      return;
    this.focusSectorId=focusSectorId;
    this.fromSectorIds=fromSectorIds;
    this.toSectorIds=toSectorIds;
    makeInterior();
  }
  
  public void setSelectedFlights(ArrayList<String> selectedFlIds) {
    boolean changed=false;
    if (selectedFlIds==null)
      changed=this.selectedFlIds!=null;
    else
      changed=this.selectedFlIds==null ||
                  selectedFlIds.size()!=this.selectedFlIds.size() ||
                  !selectedFlIds.containsAll(this.selectedFlIds);
    if (changed) {
      this.selectedFlIds=(selectedFlIds==null)?null:(ArrayList<String>)selectedFlIds.clone();
      makeInterior();
    }
  }
  
  /**
   * UI elements
   */
  protected ArrayList<JCheckBox> flCB=null; //each checkbox corresponds to one flight
  protected ArrayList<JLabel> svLabels=null; //each label corresponds to one visit of a sector by a flight
  
  protected void makeInterior() {
    removeAll();
    if (flCB!=null)
      flCB.clear();
    if (svLabels!=null)
      svLabels.clear();
    if (sectors!=null && selectedFlIds!=null && !selectedFlIds.isEmpty()) {
      for (int i=0; i<selectedFlIds.size(); i++) {
        ArrayList<FlightInSector> seq=sectors.getSectorVisitSequence(selectedFlIds.get(i));
        if (seq==null || seq.isEmpty())
          continue;
        if (flCB==null)
          flCB=new ArrayList<JCheckBox>(50);
        if (svLabels==null)
          svLabels=new ArrayList<JLabel>(300);
        JCheckBox cb=new JCheckBox(seq.get(0).flightId,true);
        cb.addItemListener(this);
        cb.setBackground(cbBkgColor);
        flCB.add(cb);
        add(cb);
        add(Box.createRigidArea(new Dimension(0, 5)));
        for (int j=0; j<seq.size(); j++) {
          FlightInSector f=seq.get(j);
          JLabel lab=new JLabel(f.sectorId+": "+f.entryTime+".."+f.exitTime);
          if (f.sectorId.equals(focusSectorId))
            lab.setForeground(SectorShowCanvas.focusSectorColor);
          else
          if (fromSectorIds!=null && fromSectorIds.contains(f.sectorId))
            lab.setForeground(SectorShowCanvas.fromSectorColor);
          else
          if (toSectorIds!=null && toSectorIds.contains(f.sectorId))
            lab.setForeground(SectorShowCanvas.toSectorColor);
          svLabels.add(lab);
          add(lab);
          add(Box.createRigidArea(new Dimension(0, 3)));
        }
      }
    }
    Dimension pSize=getPreferredSize();
    if (isShowing()) {
      invalidate();
      validate();
      setSize(Math.max(pSize.width,10),Math.max(pSize.height,50));
      repaint();
    }
  }
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() instanceof JCheckBox) {
      JCheckBox cb=(JCheckBox)e.getSource();
      if (!cb.isSelected())
        sendActionEvent("deselect_object:"+cb.getText());
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount()==2) {
      //
    }
  }
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
}
