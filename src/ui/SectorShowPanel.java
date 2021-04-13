package ui;

import data_manage.FlightInSector;
import data_manage.OneSectorData;
import data_manage.SectorSet;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;

public class SectorShowPanel extends JPanel
    implements ActionListener, ItemListener, ChangeListener, MouseListener {
  /**
   * The canvas(es) with the visual representation. There may be several canvases
   * representing different scenarios
   */
  protected SectorShowCanvas sectorsFlightsViews[]=null;
  /**
   * A panel with information about selected flights
   */
  protected SelectedFlightsInfoShow flInfoPanel=null;
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
  /**
   * Used for setting the aggregation time step
   */
  protected JComboBox chAggrStep=null;
  /**
   * Used for switching between showing all flights and only selected ones
   */
  protected JCheckBox cbShowOnlySelected=null;
  /**
   * Controls for selecting the time range to view
   */
  protected RangeSlider timeFocuser=null;
  protected JTextField tfTStart=null, tfTEnd=null;
  protected JButton bFullRange=null;
  /**
   * Controls for highlighting excesses of capacity
   */
  protected JCheckBox cbHighlightExcess=null;
  protected JTextField tfPercentExcess=null;
  /**
   * What to count: entries or presence
   */
  protected JComboBox chEntriesOrPresence=null;
  /**
   * Whether to ignore repeated entries
   */
  protected JCheckBox cbIgnoreReEntries =null;
  /**
   * Shows how many flights are currently selected
   */
  protected JLabel labSelFlights=null;
  
  /**
   * Each SectorSet corresponds to one scenario
   * @param scenarios - sector sets corresponding to different scenarios
   */
  public SectorShowPanel(SectorSet scenarios[]) {
    super();
    if (scenarios==null || scenarios.length<1 || scenarios[0]==null || scenarios[0].getNSectors()<1)
      return;
    
    JPanel mainP=new JPanel(new BorderLayout());
    
    JPanel bp=new JPanel(new GridLayout(0,1));
    mainP.add(bp,BorderLayout.SOUTH);

    sortedSectors=SectorSet.getSectorList(scenarios);
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
    
    JPanel p=new JPanel(new BorderLayout(10,2));
    bp.add(p);
  
    JPanel pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    p.add(pp,BorderLayout.WEST);
    pp.add(new JLabel("Sectors:"));
    pp.add(chSectors);
    backButton=new JButton("show previous");
    backButton.setActionCommand("back");
    backButton.setEnabled(false);
    backButton.addActionListener(this);
    pp.add(backButton);
  
    pp.add(Box.createRigidArea(new Dimension(10, 0)));
    pp.add(new JLabel("Show time interval:"));

    timeFocuser=new RangeSlider();
    timeFocuser.setPreferredSize(new Dimension(240,timeFocuser.getPreferredSize().height));
    timeFocuser.setMinimum(0);
    timeFocuser.setMaximum(SectorShowCanvas.minutesInDay);
    timeFocuser.setValue(0);
    timeFocuser.setUpperValue(SectorShowCanvas.minutesInDay);
    timeFocuser.addChangeListener(this);
    tfTStart=new JTextField("00:00");
    tfTEnd=new JTextField("24:00");
    tfTStart.addActionListener(this);
    tfTEnd.addActionListener(this);
    pp=new JPanel(new BorderLayout(5,2));
    p.add(pp,BorderLayout.CENTER);
    pp.add(tfTStart,BorderLayout.WEST);
    pp.add(timeFocuser,BorderLayout.CENTER);
    pp.add(tfTEnd,BorderLayout.EAST);
  
    pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    p.add(pp,BorderLayout.EAST);
    bFullRange=new JButton("Restore full range");
    bFullRange.setActionCommand("full_time_range");
    bFullRange.addActionListener(this);
    bFullRange.setEnabled(false);
    pp.add(bFullRange);
  
    pp.add(Box.createRigidArea(new Dimension(10, 0)));
    cbShowOnlySelected=new JCheckBox("Show only selected flights",false);
    cbShowOnlySelected.addItemListener(this);
    pp.add(cbShowOnlySelected);
  
    p=new JPanel(new FlowLayout(FlowLayout.CENTER,20,2));
    bp.add(p);
  
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,2));
    p.add(pp);
    pp.add(new JLabel("Aggregation time step:"));
    chAggrStep=new JComboBox();
    chAggrStep.addItem(new Integer(1));
    chAggrStep.addItem(new Integer(5));
    chAggrStep.addItem(new Integer(10));
    chAggrStep.addItem(new Integer(15));
    chAggrStep.addItem(new Integer(20));
    chAggrStep.addItem(new Integer(30));
    chAggrStep.addItem(new Integer(60));
    chAggrStep.setSelectedIndex(4);
    chAggrStep.addActionListener(this);
    pp.add(chAggrStep);
    pp.add(new JLabel("minutes"));
  
    cbIgnoreReEntries=new JCheckBox("Ignore re-entries",true);
    cbIgnoreReEntries.addItemListener(this);
    p.add(cbIgnoreReEntries);
  
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,2));
    p.add(pp);
    cbHighlightExcess=new JCheckBox("Highlight excess of sector capacity by over",true);
    pp.add(cbHighlightExcess);
    cbHighlightExcess.addItemListener(this);
    tfPercentExcess=new JTextField("10",4);
    pp.add(tfPercentExcess);
    tfPercentExcess.addActionListener(this);
    pp.add(new JLabel("% regarding"));
    chEntriesOrPresence=new JComboBox();
    pp.add(chEntriesOrPresence);
    chEntriesOrPresence.addItem("entries");
    chEntriesOrPresence.addItem("presence");
    chEntriesOrPresence.setSelectedIndex(0);
    chEntriesOrPresence.addActionListener(this);
    
    sectorsFlightsViews =new SectorShowCanvas[scenarios.length];
    JTabbedPane tabbedPane = (scenarios.length>1)?new JTabbedPane():null;
    if (tabbedPane!=null)
      mainP.add(tabbedPane,BorderLayout.CENTER);
    for (int i=0; i<scenarios.length; i++) {
      sectorsFlightsViews[i]=new SectorShowCanvas(scenarios[i]);
      sectorsFlightsViews[i].setFocusSector(sortedSectors.get(maxIdx).sectorId);
      sectorsFlightsViews[i].addActionListener(this);
      if (i==0)
        chAggrStep.setSelectedItem(Integer.toString(sectorsFlightsViews[i].getAggregationTimeStep()));
      if (tabbedPane!=null)
        tabbedPane.addTab("scenario "+(i+1), sectorsFlightsViews[i]);
      else
        mainP.add(sectorsFlightsViews[i], BorderLayout.CENTER);
    }
  
    flInfoPanel=new SelectedFlightsInfoShow(scenarios[0]);
    if (scenarios.length>1)
      flInfoPanel.setDataToCompare(scenarios[1]);
    flInfoPanel.addActionListener(this);
    flInfoPanel.setCurrentSectors(sectorsFlightsViews[0].getFocusSectorId(),
        sectorsFlightsViews[0].getFromSectorIds(), sectorsFlightsViews[0].getToSectorIds());
    JScrollPane scp=new JScrollPane(flInfoPanel);
    
    p=new JPanel(new BorderLayout());
    p.add(scp,BorderLayout.CENTER);
    
    labSelFlights=new JLabel("0 flights selected",JLabel.CENTER);
    p.add(labSelFlights,BorderLayout.NORTH);
    labSelFlights.addMouseListener(this);
    labSelFlights.setToolTipText("Press right mouse button to put selected flights to clipboard");
    ToolTipManager.sharedInstance().registerComponent(labSelFlights);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    JSplitPane spl=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,mainP,p);
    setLayout(new BorderLayout());
    add(spl,BorderLayout.CENTER);
    spl.setDividerLocation(sectorsFlightsViews[0].getPreferredSize().width);
  
    sectorSelections=new ArrayList<Integer>(100);
    sectorSelections.add(maxIdx);
    //setPreferredSize(new Dimension(canvas.getPreferredSize().width+150,
        //canvas.getPreferredSize().height+50));
  }
  
  public SectorShowCanvas getVisibleCanvas() {
    if (sectorsFlightsViews==null || sectorsFlightsViews.length<1)
      return null;
    if (sectorsFlightsViews.length<2)
      return sectorsFlightsViews[0];
    for (int i=0; i<sectorsFlightsViews.length; i++)
      if (sectorsFlightsViews[i].isShowing())
        return sectorsFlightsViews[i];
    return  sectorsFlightsViews[0];
  }
  
  
  public void actionPerformed (ActionEvent ae) {
    SectorShowCanvas shownCanvas=getVisibleCanvas();
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
    if (ae.getSource().equals(chAggrStep)) {
      if (sectorsFlightsViews !=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setAggregationTimeStep((Integer)chAggrStep.getSelectedItem());
    }
    else
    if (ae.getSource().equals(chEntriesOrPresence)) {
      if (sectorsFlightsViews !=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setToCountEntries(chEntriesOrPresence.getSelectedIndex()==0);
    }
    else
    if (ae.getSource().equals(chSectors) && shownCanvas !=null) {
      int selIdx=chSectors.getSelectedIndex();
      if (selIdx!=sectorSelections.get(sectorSelections.size()-1)) {
        sectorSelections.add(selIdx);
        if (sectorSelections.size()==2)
          backButton.setEnabled(true);
      }
      for (int i = 0; i< sectorsFlightsViews.length; i++)
        sectorsFlightsViews[i].setFocusSector(sortedSectors.get(selIdx).sectorId);
      flInfoPanel.setCurrentSectors(shownCanvas.getFocusSectorId(),
          shownCanvas.getFromSectorIds(), shownCanvas.getToSectorIds());
      flInfoPanel.setSelectedFlights(shownCanvas.getSelectedObjectIds(),
          shownCanvas.getSelectedVisibleObjectIds());
    }
    else
    if ((ae.getSource() instanceof SectorShowCanvas) || ae.getSource().equals(flInfoPanel)) {
      String cmd=ae.getActionCommand();
      if (cmd.equals("object_selection")) {
        SectorShowCanvas canvas=(SectorShowCanvas)ae.getSource();
        ArrayList<String> fIds=canvas.getSelectedObjectIds();
        flInfoPanel.setSelectedFlights(fIds, canvas.getSelectedVisibleObjectIds());
        if (sectorsFlightsViews.length>1)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            if (!canvas.equals(sectorsFlightsViews[i]))
              sectorsFlightsViews[i].setSelectedObjIds(fIds);
      }
      else
      if (cmd.equals("object_marking")) {
        SectorShowCanvas canvas=(SectorShowCanvas)ae.getSource();
        HashSet<String> fIds=canvas.getMarkedObjIds();
        flInfoPanel.setMarkedObjIds(fIds);
        if (sectorsFlightsViews.length>1)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            if (!canvas.equals(sectorsFlightsViews[i]))
              sectorsFlightsViews[i].setMarkedObjIds(fIds);
      }
      else
      if (cmd.startsWith("deselect_object:"))  {
        String oId=cmd.substring(16);
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].deselectObject(oId);
      }
      else
      if (cmd.startsWith("highlight_object:"))  {
        String oId=cmd.substring(17);
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          if (sectorsFlightsViews[i].isShowing())
            sectorsFlightsViews[i].highlightObject(oId);
      }
      else
      if (cmd.startsWith("dehighlight_object:"))  {
        String oId=cmd.substring(19);
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          if (sectorsFlightsViews[i].isShowing())
            sectorsFlightsViews[i].dehighlightObject(oId);
      }
      else
      if (cmd.startsWith("select_sector:")) {
        cmd=cmd.substring(14);
        int sIdx=-1;
        for (int i=0; i<sortedSectors.size() && sIdx<0; i++)
          if (cmd.equals(sortedSectors.get(i).sectorId))
            sIdx=i;
        if (sIdx>=0 && sIdx!=chSectors.getSelectedIndex())
          chSectors.setSelectedIndex(sIdx);
      }
      else
      if (cmd.startsWith("show_path:")) {
        if (sectorsFlightsViews !=null)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            sectorsFlightsViews[i].setFocusFlight(cmd.substring(10));
      }
      else
      if (cmd.startsWith("cancel_path_show:"))  {
        if (sectorsFlightsViews !=null)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            sectorsFlightsViews[i].setFocusFlight(null);
      }
      else
      if (cmd.startsWith("mark:") || cmd.startsWith("unmark:")) {
        if (sectorsFlightsViews==null)
          return;
        String fId=cmd.substring(cmd.indexOf(':')+1);
        boolean mark=cmd.startsWith("m");
        HashSet<String> fIds=sectorsFlightsViews[0].getMarkedObjIds();
        if (mark) {
          if (fIds!=null && fIds.contains(fId))
            return;
          if (fIds==null)
            fIds=new HashSet<String>(50);
          fIds.add(fId);
        }
        else {
          if (fIds==null || !fIds.contains(fId))
            return;
          fIds.remove(fId);
        }
        flInfoPanel.setMarkedObjIds(fIds);
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setMarkedObjIds(fIds);
      }
    }
    else
    if (ae.getSource() instanceof JTextField)  {
      JTextField tf=(JTextField)ae.getSource();
      if (tf.equals(tfTStart) || tf.equals(tfTEnd)) {
        String txt=tf.getText();
        int idx=txt.indexOf(':');
        int h=-1, m=-1;
        try {
          h=Integer.parseInt((idx<0)?txt:txt.substring(0,idx));
        } catch (Exception ex) {}
        if (h>=0 && idx>0)
          try {
            m=Integer.parseInt(txt.substring(idx+1));
          } catch (Exception ex) {}
        if (h<0 || m<0 || h>24 || m>59) {
          int val=(tf.equals(tfTStart))?timeFocuser.getValue():timeFocuser.getUpperValue();
          tf.setText(String.format("%02d:%02d",val/60,val%60));
        }
        else {
          m+=h*60;
          boolean ok=(tf.equals(tfTStart))?m<timeFocuser.getUpperValue():m>timeFocuser.getValue();
          if (!ok) {
            int val=(tf.equals(tfTStart))?timeFocuser.getValue():timeFocuser.getUpperValue();
            tf.setText(String.format("%02d:%02d",val/60,val%60));
          }
          else
            if (tf.equals(tfTStart))
              timeFocuser.setValue(m);
            else
              timeFocuser.setUpperValue(m);
        }
      }
      else
      if (tf.equals(tfPercentExcess)) {
        float perc=-1;
        try {
          perc=Float.parseFloat(tf.getText());
        } catch (Exception ex) {}
        if (perc<0) {
          perc=(shownCanvas ==null)?10: shownCanvas.getMinExcessPercent();
          tf.setText(String.valueOf(perc));
        }
        else
          if (sectorsFlightsViews !=null)
            for (int i = 0; i< sectorsFlightsViews.length; i++)
              sectorsFlightsViews[0].setMinExcessPercent(perc);
      }
    }
    else
    if (ae.getActionCommand().equals("full_time_range"))  {
      if (timeFocuser.getValue()>timeFocuser.getMinimum() || timeFocuser.getUpperValue()<timeFocuser.getMaximum())
        timeFocuser.setFullRange();
    }
    if (shownCanvas !=null) {
      ArrayList<String> selected = shownCanvas.getSelectedObjectIds(),
                        visible = shownCanvas.getSelectedVisibleObjectIds();
      String txt = ((selected == null) ? "0" : Integer.toString(selected.size())) + " flights selected; " +
                       ((visible == null) ? "0" : Integer.toString(visible.size())) + " visible";
      labSelFlights.setText(txt);
      labSelFlights.setSize(labSelFlights.getPreferredSize());
    }
  }
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource().equals(cbShowOnlySelected)) {
      if (sectorsFlightsViews !=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setShowOnlySelectedFlights(cbShowOnlySelected.isSelected());
      flInfoPanel.setAllowMarking(cbShowOnlySelected.isSelected());
    }
    else
    if (e.getSource().equals(cbIgnoreReEntries)) {
      if (sectorsFlightsViews !=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setToIgnoreReEntries(cbIgnoreReEntries.isSelected());
    }
    else
    if (e.getSource().equals(cbHighlightExcess))  {
      if (sectorsFlightsViews !=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setToHighlightCapExcess(cbHighlightExcess.isSelected());
    }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(timeFocuser))
      getTimeRange();
  }
  
  protected void getTimeRange() {
    int m1=timeFocuser.getValue(), m2=timeFocuser.getUpperValue();
    if (m2-m1<60) {
      if (sectorsFlightsViews !=null)
        if (m1== sectorsFlightsViews[0].getMinuteStart()) {
          m2=m1+60;
          if (m2>SectorShowCanvas.minutesInDay) {
            m2=SectorShowCanvas.minutesInDay;
            m1=m2-60;
          }
        }
        else {
          m1=m2-60;
          if (m1<0) {
            m1=0; m2=60;
          }
        }
      else {
        m2=m1+60;
        if (m2>SectorShowCanvas.minutesInDay) {
          m2=SectorShowCanvas.minutesInDay;
          m1=m2-60;
        }
      }
      timeFocuser.setValue(m1);
      timeFocuser.setUpperValue(m2);
    }
    tfTStart.setText(String.format("%02d:%02d",m1/60,m1%60));
    tfTEnd.setText(String.format("%02d:%02d",m2/60,m2%60));
    if (sectorsFlightsViews !=null)
      for (int i = 0; i< sectorsFlightsViews.length; i++)
        sectorsFlightsViews[i].setTimeRange(m1,m2);
    bFullRange.setEnabled(m1>timeFocuser.getMinimum() || m2<timeFocuser.getMaximum());
  }
  
  public void putSelectedIdsToClipboard(){
    if (sectorsFlightsViews==null || sectorsFlightsViews.length<1)
      return;
    ArrayList<String> ids=sectorsFlightsViews[0].getSelectedVisibleObjectIds();
    if (ids==null || ids.isEmpty())
      return;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    if (clipboard==null)
      return;
    StringBuffer txt=new StringBuffer();
    for (int i=0; i<ids.size(); i++)
      txt.append(ids.get(i)+"\n");
    clipboard.setContents(new StringSelection(txt.toString()),null);
  }
  
  public void putSelectedFlightPathsToClipboard(){
    if (sectorsFlightsViews==null || sectorsFlightsViews.length<1)
      return;
    ArrayList<String> ids=sectorsFlightsViews[0].getSelectedVisibleObjectIds();
    if (ids==null || ids.isEmpty())
      return;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    if (clipboard==null)
      return;
    StringBuffer txt=new StringBuffer();
    for (int i=0; i<ids.size(); i++) {
      txt.append(ids.get(i) + "\n");
      ArrayList<FlightInSector> seq = sectorsFlightsViews[0].sectors.getSectorVisitSequence(ids.get(i));
      if (seq==null || seq.isEmpty()) {
        if (i+1<ids.size())
          txt.append("\n");
        continue;
      }
      for (int j = 0; j < seq.size(); j++) {
        FlightInSector f = seq.get(j);
        txt.append(f.sectorId + ": " + f.entryTime + ".." + f.exitTime+"\n");
      }
      if (i+1<ids.size())
        txt.append("\n");
    }
    clipboard.setContents(new StringSelection(txt.toString()),null);
  }
  
  protected JPopupMenu popupMenu=null;
  
  public void mousePressed(MouseEvent e) {
    if (sectorsFlightsViews==null || sectorsFlightsViews.length<1)
      return;
    if (sectorsFlightsViews[0].hasSelectedVisibleObjects() &&
        e.getSource().equals(labSelFlights) && e.getButton()>MouseEvent.BUTTON1) {
      Point p=getMousePosition();
      if (p==null)
        return;
      if (popupMenu !=null) {
        popupMenu.show(this, p.x, p.y);
        return;
      }
      popupMenu = new JPopupMenu();
      JMenuItem mit=new JMenuItem("Put identifiers to clipboard");
      popupMenu.add(mit);
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu.setVisible(false);
          putSelectedIdsToClipboard();
        }
      });
      mit=new JMenuItem("Put paths to clipboard");
      popupMenu.add(mit);
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupMenu.setVisible(false);
          putSelectedFlightPathsToClipboard();
        }
      });
      popupMenu.show(this, p.x, p.y);
    }
    else
      if (popupMenu!=null && popupMenu.isVisible())
        popupMenu.setVisible(false);
  }
  
  public void mouseClicked(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
}
