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
  public ArrayList<String> selectedFlIds=null, visibleFlIds =null;
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
  
  public void setSelectedFlights(ArrayList<String> selectedFlIds, ArrayList<String> visibleFlIds) {
    boolean changedSelected=false, changedVisible=false;
    if (selectedFlIds==null)
      changedSelected=this.selectedFlIds!=null;
    else
      changedSelected=this.selectedFlIds==null ||
                  selectedFlIds.size()!=this.selectedFlIds.size() ||
                  !selectedFlIds.containsAll(this.selectedFlIds);
    if (changedSelected)
      this.selectedFlIds=(selectedFlIds==null)?null:(ArrayList<String>)selectedFlIds.clone();
    if (visibleFlIds==null)
      changedVisible=this.visibleFlIds!=null;
    else
      changedVisible=this.visibleFlIds==null ||
                         visibleFlIds.size()!=this.visibleFlIds.size() ||
                         !visibleFlIds.containsAll(this.visibleFlIds);
    if (changedVisible)
      this.visibleFlIds=(visibleFlIds==null)?null:(ArrayList<String>)visibleFlIds.clone();
    if (changedSelected || changedVisible)
      makeInterior();
    
  }
  
  /**
   * UI elements
   */
  protected ArrayList<JCheckBox> flCB=null; //each checkbox corresponds to one flight
  protected ArrayList<JLabel> svLabels=null; //each label corresponds to one visit of a sector by a flight
  protected ArrayList<JPanel> flPanels=null; //each panel corresponds to one flight,
                                             // includes a checkbox and several labels
  public ArrayList<String> panelFlIds =null; //identifiers of flights represented in the panels
  
  protected JPanel hlPanel=null; //highlighted panel
  
  boolean keepScrollPosition=false;
  
  protected void makeInterior() {
    removeAll();
    popupMenu=null;
    hlPanel=null;
    if (flCB!=null)
      flCB.clear();
    if (svLabels!=null)
      svLabels.clear();
    if (flPanels!=null)
      flPanels.clear();
    if (panelFlIds !=null)
      panelFlIds.clear();

    if (sectors!=null && selectedFlIds!=null && !selectedFlIds.isEmpty()) {
      int nAdded=0, nLoops=(visibleFlIds==null || visibleFlIds.isEmpty())?1:2;
      for (int n=0; n<nLoops && nAdded<selectedFlIds.size(); n++) {
        boolean addVisible=n<nLoops-1;
        if (!addVisible) {
          add(Box.createVerticalStrut(5));
          JSeparator sep=new JSeparator(JSeparator.HORIZONTAL);
          sep.setBackground(Color.blue);
          sep.setForeground(Color.blue);
          add(sep);
          add(Box.createVerticalStrut(5));
          JLabel lab=new JLabel("Not in current view:",JLabel.CENTER);
          lab.setForeground(Color.blue);
          add(lab);
          add(Box.createVerticalStrut(5));
          sep=new JSeparator(JSeparator.HORIZONTAL);
          sep.setBackground(Color.blue);
          sep.setForeground(Color.blue);
          add(sep);
          add(Box.createVerticalStrut(5));
        }
        for (int i = 0; i < selectedFlIds.size() && nAdded<selectedFlIds.size(); i++) {
          String fId=selectedFlIds.get(i);
          boolean visible=(nLoops<2)?false:visibleFlIds.contains(fId);
          if (visible!=addVisible)
            continue;
          ArrayList<FlightInSector> seq = sectors.getSectorVisitSequence(fId);
          if (seq == null || seq.isEmpty())
            continue;
          if (flCB == null)
            flCB = new ArrayList<JCheckBox>(50);
          if (svLabels == null)
            svLabels = new ArrayList<JLabel>(300);
          if (flPanels == null)
            flPanels = new ArrayList<JPanel>(300);
    
          JPanel pan = new JPanel();
          flPanels.add(pan);
          pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
    
          JCheckBox cb = new JCheckBox(seq.get(0).flightId, true);
          cb.addItemListener(this);
          cb.setBackground(cbBkgColor);
          flCB.add(cb);
          pan.add(cb);
          pan.add(Box.createRigidArea(new Dimension(0, 5)));
    
          boolean passedFocusSector = false;
          for (int j = 0; j < seq.size(); j++) {
            FlightInSector f = seq.get(j);
            JLabel lab = new JLabel(f.sectorId + ": " + f.entryTime + ".." + f.exitTime);
            boolean sectorVisible=(fromSectorIds != null && fromSectorIds.contains(f.sectorId)) ||
                                      (toSectorIds != null && toSectorIds.contains(f.sectorId));
            lab.setForeground((sectorVisible)?Color.black:Color.gray);
            if (addVisible) {
              if (f.sectorId.equals(focusSectorId)) {
                lab.setForeground(SectorShowCanvas.focusSectorColor);
                passedFocusSector = true;
              }
              else
                if (!passedFocusSector && fromSectorIds != null && fromSectorIds.contains(f.sectorId))
                  lab.setForeground(SectorShowCanvas.fromSectorColor);
                else
                  if (passedFocusSector && toSectorIds != null && toSectorIds.contains(f.sectorId))
                    lab.setForeground(SectorShowCanvas.toSectorColor);
            }
            svLabels.add(lab);
            pan.add(lab);
            lab.addMouseListener(this);
            pan.add(Box.createRigidArea(new Dimension(0, 3)));
          }
          add(pan);
          add(Box.createRigidArea(new Dimension(0, 3)));
          if (panelFlIds == null)
            panelFlIds = new ArrayList(100);
          panelFlIds.add(selectedFlIds.get(i));
          ++nAdded;
        }
      }
    }
    Dimension pSize=getPreferredSize();
    if (isShowing()) {
      invalidate();
      validate();
      setSize(Math.max(pSize.width,10),Math.max(pSize.height,50));
      repaint();
      if (!keepScrollPosition &&
              (getParent() instanceof JViewport) &&
              (getParent().getParent() instanceof JScrollPane)) {
        JScrollPane sp=(JScrollPane)getParent().getParent();
        sp.getVerticalScrollBar().setValue(0);
      }
      keepScrollPosition=false;
    }
  }
  
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() instanceof JCheckBox) {
      JCheckBox cb=(JCheckBox)e.getSource();
      if (!cb.isSelected()) {
        keepScrollPosition=true;
        sendActionEvent("deselect_object:" + cb.getText());
      }
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
          String fId=panelFlIds.get(pIdx);
          if (visibleFlIds==null || !visibleFlIds.contains(fId))
            return;
          popupMenu = new JPopupMenu();
          popupMenu.add("Flight " + fId);
          popupItem = new JCheckBoxMenuItem("Show whole path");
          popupItem.setActionCommand(panelFlIds.get(pIdx));
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
          if (!popupItem.getActionCommand().equals(panelFlIds.get(pIdx))) {
            popupItem.setEnabled(false);
            popupItem.setState(false);
            popupItem.setActionCommand(panelFlIds.get(pIdx));
            popupItem.setEnabled(true);
            ((JMenuItem) popupMenu.getComponent(0)).setText("Flight " + panelFlIds.get(pIdx));
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
        sendActionEvent("dehighlight_object:"+ panelFlIds.get(pIdx));
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
          sendActionEvent("highlight_object:"+ panelFlIds.get(pIdx));
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
