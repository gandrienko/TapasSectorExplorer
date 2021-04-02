package ui;

import data_manage.FlightInSector;
import data_manage.SectorSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class SelectedFlightsInfoShow extends JPanel
    implements ItemListener, MouseListener, MouseMotionListener {
  public static Color cbBkgColor=new Color(255,255,204);
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * Identifiers of selected flights and identifiers of those of them that are currently shown
   */
  public ArrayList<String> selectedFlIds=null, shownFlIds=null;
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
  
  protected JPopupMenu popupMenu =null;
  protected JCheckBoxMenuItem popupItem =null;
  /**
   * Listeners of selections
   */
  protected ArrayList<ActionListener> listeners=null;
  
  public SelectedFlightsInfoShow(SectorSet sectors){
    super();
    this.sectors=sectors;
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    addMouseMotionListener(this);
    addMouseListener(this);
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
  protected ArrayList<JPanel> flPanels=null; //each panel corresponds to one flight,
                                             // includes a checkbox and several labels
  
  protected JPanel hlPanel=null; //highlighted panel
  
  protected void makeInterior() {
    removeAll();
    popupMenu=null;
    hlPanel=null;
    if (flCB!=null)
      flCB.clear();
    if (svLabels!=null)
      svLabels.clear();
    if (sectors!=null && selectedFlIds!=null && !selectedFlIds.isEmpty()) {
      boolean shown[]=(shownFlIds==null || shownFlIds.isEmpty())?null:new boolean[selectedFlIds.size()];
      if (shown!=null) {
        for (int i=0; i<shown.length; i++)
          shown[i]=false;
        for (int i=shownFlIds.size()-1; i>=0; i--) {
          int idx=selectedFlIds.indexOf(shownFlIds.get(i));
          if (idx>=0) {
            shown[idx]=true;
            JPanel pan=flPanels.get(i);
            pan.setBackground(this.getBackground());
            add(pan);
            add(Box.createRigidArea(new Dimension(0, 3)));
            ArrayList<FlightInSector> seq=sectors.getSectorVisitSequence(shownFlIds.get(i));
            int lIdx=-1;
            boolean passedFocusSector=false;
            for (int j=0; j<pan.getComponentCount(); j++)
              if (pan.getComponent(j) instanceof JCheckBox)
                flCB.add((JCheckBox)pan.getComponent(j));
              else
              if (pan.getComponent(j) instanceof JLabel) {
                JLabel lab=(JLabel)pan.getComponent(j);
                String id=lab.getText();
                int ii=id.indexOf(':');
                if (ii>0) {
                  id = id.substring(0, ii);
                  FlightInSector f=seq.get(lIdx+1);
                  if (id.equals(f.sectorId)) {
                    ++lIdx;
                    if (id.equals(focusSectorId)) {
                      lab.setForeground(SectorShowCanvas.focusSectorColor);
                      passedFocusSector=true;
                    }
                    else
                    if (!passedFocusSector && fromSectorIds != null && fromSectorIds.contains(id))
                      lab.setForeground(SectorShowCanvas.fromSectorColor);
                    else
                    if (passedFocusSector && toSectorIds != null && toSectorIds.contains(id))
                      lab.setForeground(SectorShowCanvas.toSectorColor);
                    else
                      lab.setForeground(getForeground());
                  }
                  else
                    lab.setForeground(getForeground());
                }
                svLabels.add(lab);
              }
          }
          else {
            flPanels.remove(i);
            shownFlIds.remove(i);
          }
        }
      }
      for (int i=0; i<selectedFlIds.size(); i++)
        if (shown==null || !shown[i]) {
          ArrayList<FlightInSector> seq=sectors.getSectorVisitSequence(selectedFlIds.get(i));
          if (seq==null || seq.isEmpty())
            continue;
          if (flCB==null)
            flCB=new ArrayList<JCheckBox>(50);
          if (svLabels==null)
            svLabels=new ArrayList<JLabel>(300);
          if (flPanels==null)
            flPanels=new ArrayList<JPanel>(300);
          
          JPanel pan=new JPanel();
          flPanels.add(pan);
          pan.setLayout(new BoxLayout(pan,BoxLayout.Y_AXIS));
          
          JCheckBox cb=new JCheckBox(seq.get(0).flightId,true);
          cb.addItemListener(this);
          cb.setBackground(cbBkgColor);
          flCB.add(cb);
          pan.add(cb);
          pan.add(Box.createRigidArea(new Dimension(0, 5)));
          
          boolean passedFocusSector=false;
          for (int j=0; j<seq.size(); j++) {
            FlightInSector f=seq.get(j);
            JLabel lab=new JLabel(f.sectorId+": "+f.entryTime+".."+f.exitTime);
            if (f.sectorId.equals(focusSectorId)) {
              lab.setForeground(SectorShowCanvas.focusSectorColor);
              passedFocusSector=true;
            }
            else
            if (!passedFocusSector && fromSectorIds!=null && fromSectorIds.contains(f.sectorId))
              lab.setForeground(SectorShowCanvas.fromSectorColor);
            else
            if (passedFocusSector && toSectorIds!=null && toSectorIds.contains(f.sectorId))
              lab.setForeground(SectorShowCanvas.toSectorColor);
            svLabels.add(lab);
            pan.add(lab);
            lab.addMouseListener(this);
            pan.add(Box.createRigidArea(new Dimension(0, 3)));
          }
          add(pan);
          add(Box.createRigidArea(new Dimension(0, 3)));
          if (shownFlIds==null)
            shownFlIds=new ArrayList(100);
          shownFlIds.add(selectedFlIds.get(i));
        }
    }
    else {
      if (flPanels!=null)
        flPanels.clear();
      if (shownFlIds!=null)
        shownFlIds.clear();
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
    clearPanelHighlighting();
    if (e.getClickCount()==2) {
      if (e.getSource() instanceof JLabel) {//possibly, selection of a sector
        String txt=((JLabel)e.getSource()).getText();
        int idx=txt.indexOf(':');
        if (idx>0)
          sendActionEvent("select_sector:"+txt.substring(0,idx));
      }
    }
  }
  
  public void mousePressed(MouseEvent e) {
    if (e.getButton()>MouseEvent.BUTTON1) { //right button pressed
      Point p=getMousePosition();
      if (p!=null) {
        if (flPanels==null || flPanels.isEmpty())
          return;
        int pIdx=-1;
        for (int i=0; i<flPanels.size() && pIdx<0; i++)
          if (flPanels.get(i).getBounds().contains(p.x,p.y))
            pIdx=i;
        if (pIdx<0) {
          if (popupMenu !=null)
            popupMenu.show(this,p.x,p.y);
          return;
        }
        if (popupMenu ==null) {
          popupMenu = new JPopupMenu();
          popupMenu.add("Flight " + shownFlIds.get(pIdx));
          popupItem = new JCheckBoxMenuItem("Show whole path");
          popupItem.setActionCommand(shownFlIds.get(pIdx));
          popupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
              if (item.getState())
                sendActionEvent("show_path:" + e.getActionCommand());
              else
                sendActionEvent("cancel_path_show:" + e.getActionCommand());
            }
          });
          popupMenu.add(popupItem);
        }
        else {
          if (!popupItem.getActionCommand().equals(shownFlIds.get(pIdx))) {
            popupItem.setEnabled(false);
            popupItem.setState(false);
            popupItem.setActionCommand(shownFlIds.get(pIdx));
            popupItem.setEnabled(true);
            ((JMenuItem) popupMenu.getComponent(0)).setText("Flight " + shownFlIds.get(pIdx));
          }
        }
        popupMenu.show(this,p.x,p.y);
      }
    }
  }
  
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {
    if (e.getSource().equals(this)) {
      if (flPanels!=null && !flPanels.isEmpty()) {
        Point p=getMousePosition();
        if (p!=null)
          for (int i=0; i<flPanels.size(); i++)
            if (flPanels.get(i).getBounds().contains(p.x,p.y))
              return;
      }
      clearPanelHighlighting();
    }
  }
  
  protected void clearPanelHighlighting(){
    if (hlPanel!=null) {
      JPanel pan=hlPanel;
      hlPanel=null;
      int pIdx=-1;
      for (int i=0; i<flPanels.size() && pIdx<0; i++)
        if (pan.equals(flPanels.get(i)))
          pIdx=i;
      if (pIdx>=0)
        sendActionEvent("dehighlight_object:"+shownFlIds.get(pIdx));
      pan.setBackground(getBackground());
      pan.repaint();
    }
  }
  
  public void mouseMoved(MouseEvent e) {
    Point p=getMousePosition();
    if (p!=null) {
      if (flPanels==null || flPanels.isEmpty())
        return;
      int pIdx=-1;
      for (int i=0; i<flPanels.size() && pIdx<0; i++)
        if (flPanels.get(i).getBounds().contains(p.x,p.y))
          pIdx=i;
      if (pIdx<0)
        clearPanelHighlighting();
      else {
        JPanel pan=flPanels.get(pIdx);
        if (!pan.equals(hlPanel)) {
          sendActionEvent("highlight_object:"+shownFlIds.get(pIdx));
          clearPanelHighlighting();
          hlPanel=pan;
          pan.setBackground(Color.pink);
          pan.repaint();
        }
      }
    }
    else
      clearPanelHighlighting();
  }
  
  public void mouseDragged(MouseEvent e) {
    //
  }
}
