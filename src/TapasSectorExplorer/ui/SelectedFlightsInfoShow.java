package TapasSectorExplorer.ui;

import TapasSectorExplorer.data_manage.FlightInSector;
import TapasSectorExplorer.data_manage.SectorSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;

public class SelectedFlightsInfoShow extends JPanel
    implements ItemListener, MouseListener, MouseMotionListener {
  public static Color cbBkgColor=new Color(255,255,204);
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * In comparison mode, this is a set with alternative data
   */
  public SectorSet altSectors=null;
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
  /**
   * In the mode of showing only selected flights, this hash set contains identifiers of "marked" flights
   */
  protected HashSet<String> markedObjIds=null;
  /**
   * The identifier of the flight in focus (the whole path is shown)
   */
  protected String focusFlightId =null;
  /**
   * Whether to allow marking and unmarking of the items in the list
   */
  public boolean allowMarking=false;
  
  protected JPopupMenu popupMenu =null;
  protected JCheckBoxMenuItem mitShowPath =null, mitMark=null;
  /**
   * Listeners of selections
   */
  protected ArrayList<ActionListener> listeners=null;
  
  public SelectedFlightsInfoShow(SectorSet sectors){
    super();
    this.sectors=sectors;
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
  
  public void setDataToCompare(SectorSet altSectors) {
    this.altSectors=altSectors;
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
    if (changedSelected || changedVisible) {
      if (focusFlightId !=null && (selectedFlIds==null || !selectedFlIds.contains(focusFlightId)))
        focusFlightId =null;
      if (markedObjIds!=null && !markedObjIds.isEmpty())
        if (selectedFlIds==null || selectedFlIds.isEmpty())
          markedObjIds.clear();
        else
          for (String id:markedObjIds)
            if (!selectedFlIds.contains(id))
              markedObjIds.remove(id);
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
  public ArrayList<String> panelFlIds =null; //identifiers of flights represented in the panels
  public ArrayList<String> labelSectorIds=null; //identifiers of sectors appearing in the labels
  
  protected JPanel hlPanel=null; //highlighted panel
  
  protected boolean keepScrollPosition=false;
  
  public static String sFocusSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.focusSectorColor.getRGB()).substring(2);
  public static String sFromSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.fromSectorColor.getRGB()).substring(2);
  public static String sToSectorColor =
      "#"+Integer.toHexString(SectorShowCanvas.toSectorColor.getRGB()).substring(2);
  
  protected void makeInterior() {
    removeAll();
    popupMenu=null;
    hlPanel=null;
    if (flCB!=null)
      flCB.clear();
    if (svLabels!=null)
      svLabels.clear();
    if (labelSectorIds!=null)
      labelSectorIds.clear();
    if (flPanels!=null)
      flPanels.clear();
    if (panelFlIds !=null)
      panelFlIds.clear();
  
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    if (sectors!=null && selectedFlIds!=null && !selectedFlIds.isEmpty()) {
      int nAdded=0, nLoops=(visibleFlIds==null || visibleFlIds.isEmpty())?1:2;
      for (int n=0; n<nLoops && nAdded<selectedFlIds.size(); n++) {
        boolean addVisible=n<nLoops-1;
        if (!addVisible) {
          JPanel pan = new JPanel();
          pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
          add(pan);
          if (nAdded>0) {
            pan.add(Box.createRigidArea(new Dimension(0, 1)));
            JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
            sep.setBackground(Color.blue);
            sep.setForeground(Color.blue);
            pan.add(sep);
          }
          pan.add(Box.createRigidArea(new Dimension(0,1)));
          JLabel lab=new JLabel("Not in current view:");
          lab.setForeground(Color.blue);
          pan.add(lab);
          pan.add(Box.createRigidArea(new Dimension(0,1)));
          JSeparator sep=new JSeparator(JSeparator.HORIZONTAL);
          sep.setBackground(Color.blue);
          sep.setForeground(Color.blue);
          pan.add(sep);
          pan.add(Box.createRigidArea(new Dimension(0,2)));
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
          if (labelSectorIds==null)
            labelSectorIds=new ArrayList<String>(300);
    
          JPanel pan = new JPanel();
          flPanels.add(pan);
          pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
          
          if (fId.equals(focusFlightId))
            pan.setBorder(BorderFactory.createLineBorder(Color.ORANGE,3));
          else
          if (markedObjIds!=null && markedObjIds.contains(fId))
            pan.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
          
          JCheckBox cb = new JCheckBox(seq.get(0).flightId, true);
          cb.addItemListener(this);
          cb.setBackground(cbBkgColor);
          flCB.add(cb);
          pan.add(cb);
          pan.add(Box.createRigidArea(new Dimension(0, 5)));
          
          ArrayList<FlightInSector> altSeq=(altSectors==null)?null:altSectors.getSectorVisitSequence(fId);
          if (altSeq!=null && SectorSet.sameSequence(seq,altSeq))
            altSeq=null;
          
          if (altSeq!=null) {
            cb.setForeground(Color.red.darker());
            LocalTime t1=seq.get(0).entryTime, t2=altSeq.get(0).entryTime;
            if (!t1.equals(t2)) {
              Duration dur=Duration.between(t1,t2);
              long diff=dur.getSeconds();
              diff/=60;
              cb.setText(cb.getText()+" ("+((diff>0)?"+":"")+diff+"\')");
            }
          }
    
          boolean passedFocusSector = false;
          for (int j = 0; j < seq.size(); j++) {
            FlightInSector f = seq.get(j);
            boolean sectorVisible=(fromSectorIds != null && fromSectorIds.contains(f.sectorId)) ||
                                      (toSectorIds != null && toSectorIds.contains(f.sectorId));
            String colorTxt=(sectorVisible)?"black":"gray";
            if (addVisible) {
              if (f.sectorId.equals(focusSectorId)) {
                colorTxt= sFocusSectorColor;
                passedFocusSector = true;
              }
              else
                if (!passedFocusSector && fromSectorIds != null && fromSectorIds.contains(f.sectorId))
                  colorTxt= sFromSectorColor;
                else
                  if (passedFocusSector && toSectorIds != null && toSectorIds.contains(f.sectorId))
                    colorTxt= sToSectorColor;
            }
            //String txt=f.sectorId + ": " + f.entryTime + ".." + f.exitTime;
            String txt="<html><body><font color=\""+colorTxt+"\">"+f.sectorId+"</font>"+
                ": "+f.entryTime+".."+f.exitTime;
            if (altSeq!=null && j<altSeq.size()) {
              FlightInSector fAlt=altSeq.get(j);
              if (!fAlt.equals(f)) {
                txt += "<font color=\"#BB0000\">" + " >>> ";
                if (!fAlt.sectorId.equals(f.sectorId))
                  txt+="<u>"+fAlt.sectorId + "</u>: ";
                txt += fAlt.entryTime + ".." + fAlt.exitTime + "</font>";
              }
            }
            txt+="</body></html>";
            JLabel lab = new JLabel(txt);
            svLabels.add(lab);
            labelSectorIds.add(f.sectorId);
            pan.add(lab);
            lab.addMouseListener(this);
            pan.add(Box.createRigidArea(new Dimension(0, 3)));
          }
          if (altSeq!=null && altSeq.size()>seq.size())
            for (int j=seq.size(); j<altSeq.size(); j++) {
              FlightInSector fAlt=altSeq.get(j);
              String txt="<html><body>&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;"+
                             "&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;"+
                             "&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;"+
                             "<font color=\"#BB0000\">"+">>> "+"<u>"+fAlt.sectorId + "</u>: "+
                             fAlt.entryTime + ".." + fAlt.exitTime + "</font></body></html>";
              JLabel lab = new JLabel(txt);
              svLabels.add(lab);
              labelSectorIds.add(fAlt.sectorId);
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
  
  public void setMarkedObjIds(HashSet<String> marked) {
    if (markedObjIds==null && marked==null)
      return;
    this.markedObjIds=(HashSet<String>)marked.clone();
    updateMarked();
  }
  
  public void clearMarking() {
    if (markedObjIds!=null && !markedObjIds.isEmpty()) {
      markedObjIds.clear();
      updateMarked();
    }
  }
  
  protected void updateMarked() {
    if (panelFlIds==null || panelFlIds.isEmpty())
      return;
    int y=-1;
    for (int i=0; i<panelFlIds.size(); i++) {
      JPanel pan = flPanels.get(i);
      if (markedObjIds != null && markedObjIds.contains(panelFlIds.get(i))) {
        pan.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        pan.invalidate();
        pan.repaint();
        if (y < 0)
          y = pan.getY();
      }
      else {
        if (pan.getBorder()!=null) {
          pan.setBorder(null);
          pan.invalidate();
          pan.repaint();
        }
      }
    }
    if (y>=0 && (getParent() instanceof JViewport) &&
            (getParent().getParent() instanceof JScrollPane)) {
      JScrollPane sp=(JScrollPane)getParent().getParent();
      sp.getVerticalScrollBar().setValue(y);
    }
  }
  
  public void setAllowMarking(boolean allow) {
    this.allowMarking=allow;
  }
  
  public void setFocusFlightId(String fId) {
    if (fId==null)
      if (focusFlightId ==null)
        return;
      else;
    else
      if (fId.equals(focusFlightId))
        return;
    if (focusFlightId !=null)
      for (int i=0; i<panelFlIds.size(); i++)
        if (panelFlIds.get(i).equals(focusFlightId)) {
          JPanel pan = flPanels.get(i);
          pan.setBorder(null);
          pan.invalidate();
          pan.repaint();
          break;
        }
    focusFlightId =fId;
    int y=-1;
    if (focusFlightId !=null)
      for (int i=0; i<panelFlIds.size(); i++)
        if (panelFlIds.get(i).equals(focusFlightId)) {
          JPanel pan = flPanels.get(i);
          pan.setBorder(BorderFactory.createLineBorder(Color.orange, 3));
          pan.invalidate();
          pan.repaint();
          y = pan.getY();
        }
    if (y>=0 && (getParent() instanceof JViewport) &&
            (getParent().getParent() instanceof JScrollPane)) {
      JScrollPane sp=(JScrollPane)getParent().getParent();
      sp.getVerticalScrollBar().setValue(y);
    }
  }
  
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() instanceof JCheckBox) {
      JCheckBox cb=(JCheckBox)e.getSource();
      if (!cb.isSelected()) {
        keepScrollPosition=true;
        String id=cb.getText();
        int idx=id.indexOf(" (");
        if (idx>0)
          id=id.substring(0,idx);
        sendActionEvent("deselect_object:" + id);
      }
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    clearPanelHighlighting();
    if (e.getClickCount()==2) {
      if (e.getSource() instanceof JLabel) {//possibly, selection of a sector
        int idx=svLabels.indexOf((JLabel)e.getSource());
        if (idx>=0 && idx<labelSectorIds.size())
          sendActionEvent("select_sector:"+labelSectorIds.get(idx));
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
          mitShowPath = new JCheckBoxMenuItem("Show whole path");
          mitShowPath.setActionCommand(fId);
          mitShowPath.setState(fId.equals(focusFlightId));
          mitShowPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
              if (item.getState())
                sendActionEvent("show_path:" + e.getActionCommand());
              else
                sendActionEvent("cancel_path_show:" + e.getActionCommand());
            }
          });
          popupMenu.add(mitShowPath);
          
          mitMark=new JCheckBoxMenuItem("Mark in black",markedObjIds!=null && markedObjIds.contains(fId));
          mitMark.setEnabled(allowMarking);
          mitMark.setActionCommand(fId);
          mitMark.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
              if (item.getState())
                sendActionEvent("mark:" + e.getActionCommand());
              else
                sendActionEvent("unmark:" + e.getActionCommand());
            }
          });
          popupMenu.add(mitMark);
        }
        else {
          String fId=panelFlIds.get(pIdx);
          if (!mitShowPath.getActionCommand().equals(fId)) {
            mitShowPath.setEnabled(false);
            mitShowPath.setState(fId.equals(focusFlightId));
            mitShowPath.setActionCommand(panelFlIds.get(pIdx));
            mitShowPath.setEnabled(true);
            ((JMenuItem) popupMenu.getComponent(0)).setText("Flight " + fId);
  
            mitMark.setEnabled(allowMarking);
            mitMark.setState(markedObjIds!=null && markedObjIds.contains(fId));
            mitMark.setActionCommand(fId);
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
