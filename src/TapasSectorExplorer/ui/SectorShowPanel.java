package TapasSectorExplorer.ui;

import TapasSectorExplorer.data_manage.FlightInSector;
import TapasSectorExplorer.data_manage.OneSectorData;
import TapasSectorExplorer.data_manage.ScenarioDistinguisher;
import TapasSectorExplorer.data_manage.SectorSet;

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
   * One or two scenarios (sector sets). In case of two scenarios, they are compared
   */
  public SectorSet scenarios[]=null;
  /**
   * In case of comparison of two scenarios, here are the differences.
   */
  public ScenarioDistinguisher scDiff=null;
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
  protected ArrayList<OneSectorData> sectorList =null;
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
  protected JButton bFullRange=null, bUnselect=null;
  /**
   * Whether to show only the flights that changed
   */
  protected JCheckBox cbShowOnlyChanged=null;
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
    this.scenarios=scenarios;
    if (scenarios.length>1) {
      scDiff=new ScenarioDistinguisher();
      if (!scDiff.compareScenarios(scenarios[0],scenarios[1]))
        scDiff=null;
      else
        scDiff.name="difference";
    }
    
    setLayout(new BorderLayout());

    sectorList =SectorSet.getSectorList(scenarios);
    sortSectorsByNChanged();
    chSectors=new JComboBox();
    chSectors.addActionListener(this);
    
    int maxNFlights=0, maxNChanged=0;
    int maxIdx=-1, maxChangeIdx=-1;
    for (int i = 0; i< sectorList.size(); i++) {
      OneSectorData s= sectorList.get(i);
      int nFlights=s.getNFlights(), nChanged=0;
      if (scDiff!=null) {
        OneSectorData sDiff=scDiff.getSectorData(s.sectorId);
        if (sDiff!=null)
          nChanged=scDiff.getNChangedFlights(s.sectorId);
      }
      String txt=s.sectorId+" ("+nFlights+" flights";
      if (nChanged>0)
        txt+=", modified: "+nChanged;
      txt+=")";
      chSectors.addItem(txt);
      if (nChanged>maxNChanged) {
        maxNChanged=nChanged;
        maxChangeIdx=i;
      }
      if (maxNFlights<nFlights) {
        maxNFlights=nFlights;
        maxIdx=i;
      }
    }
    if (maxChangeIdx>=0)
      chSectors.setSelectedIndex(maxChangeIdx);
    else
      chSectors.setSelectedIndex(maxIdx);
    chSectors.addMouseListener(this);
    chSectors.setToolTipText("Press right mouse button to sort the list of sectors");
    ToolTipManager.sharedInstance().registerComponent(chSectors);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    
    JPanel p=new JPanel(new BorderLayout(10,2));
    JPanel pp=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
    p.add(pp,BorderLayout.WEST);
    JLabel lab=new JLabel("Sectors:");
    pp.add(lab);
    lab.addMouseListener(this);
    lab.setToolTipText("Press right mouse button to sort the list of sectors");
    ToolTipManager.sharedInstance().registerComponent(lab);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    pp.add(chSectors);
    backButton=new JButton("back to previous");
    backButton.setActionCommand("back");
    backButton.setEnabled(false);
    backButton.addActionListener(this);
    pp.add(backButton);
  
    pp.add(Box.createRigidArea(new Dimension(10, 0)));
    pp.add(new JLabel("Time:"));

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
    bFullRange=new JButton("Full range");
    bFullRange.setActionCommand("full_time_range");
    bFullRange.addActionListener(this);
    bFullRange.setEnabled(false);
    pp.add(bFullRange);
  
    JPanel bp=new JPanel(new GridLayout(0,1));
    add(bp,BorderLayout.SOUTH);
    bp.add(p);

    p=new JPanel(new FlowLayout(FlowLayout.CENTER,20,2));
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,2));
    p.add(pp);
    pp.add(new JLabel("Time step in histograms:"));
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
    p.add(pp);
    
    if (scDiff==null)
      bp.add(p);
    else {
      cbShowOnlyChanged=new JCheckBox("Show only differing flights",false);
      cbShowOnlyChanged.addItemListener(this);
      pp=new JPanel(new BorderLayout());
      pp.add(cbShowOnlyChanged,BorderLayout.WEST);
      pp.add(p,BorderLayout.EAST);
      bp.add(pp);
    }
    
    int nViews=scenarios.length;
    if (scDiff!=null)
      ++nViews;
    
    JPanel mainP=new JPanel(new BorderLayout());
    
    sectorsFlightsViews =new SectorShowCanvas[nViews];
    JTabbedPane tabbedPane = (nViews>1)?new JTabbedPane():null;
    if (tabbedPane!=null)
      mainP.add(tabbedPane,BorderLayout.CENTER);
    String focusSectorId= sectorList.get(chSectors.getSelectedIndex()).sectorId;
    for (int i=0; i<scenarios.length; i++) {
      sectorsFlightsViews[i]=new SectorShowCanvas(scenarios[i]);
      sectorsFlightsViews[i].setFocusSector(focusSectorId);
      if (scDiff!=null)
        sectorsFlightsViews[i].setChangedFlightsIds(scDiff.getModifiedFlightsIds());
      sectorsFlightsViews[i].addActionListener(this);
      if (i==0)
        chAggrStep.setSelectedItem(Integer.toString(sectorsFlightsViews[i].getAggregationTimeStep()));
      if (tabbedPane!=null)
        tabbedPane.addTab((scenarios[i].name==null)?"scenario "+(i+1):scenarios[i].name,
            sectorsFlightsViews[i]);
      else
        mainP.add(sectorsFlightsViews[i], BorderLayout.CENTER);
    }
    if (scDiff!=null) {
      int idx=nViews-1;
      sectorsFlightsViews[idx]=new ComparisonCanvas(scDiff);
      sectorsFlightsViews[idx].setFocusSector(focusSectorId);
      sectorsFlightsViews[idx].addActionListener(this);
      tabbedPane.addTab("differences", sectorsFlightsViews[idx]);
      tabbedPane.setSelectedIndex(idx);
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
    
    pp=new JPanel(new GridLayout(0,1,0,0));
    p.add(pp,BorderLayout.NORTH);
    labSelFlights=new JLabel("0 flights selected",JLabel.CENTER);
    pp.add(labSelFlights);
    labSelFlights.addMouseListener(this);
    labSelFlights.setToolTipText("Press right mouse button to put selected flights to clipboard");
    ToolTipManager.sharedInstance().registerComponent(labSelFlights);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  
    bp=new JPanel(new FlowLayout(FlowLayout.CENTER,20,0));
    pp.add(bp);
    cbShowOnlySelected=new JCheckBox("Show only selected",false);
    cbShowOnlySelected.addItemListener(this);
    bp.add(cbShowOnlySelected);
    bUnselect=new JButton("Unselect all");
    bUnselect.addActionListener(this);
    bUnselect.setActionCommand("unselect_all");
    bUnselect.setEnabled(false);
    bp.add(bUnselect);
    
    JSplitPane spl=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,mainP,p);
    add(spl,BorderLayout.CENTER);
    spl.setDividerLocation(sectorsFlightsViews[0].getPreferredSize().width);
  
    sectorSelections=new ArrayList<Integer>(100);
    sectorSelections.add(chSectors.getSelectedIndex());
    //setPreferredSize(new Dimension(canvas.getPreferredSize().width+150,
        //canvas.getPreferredSize().height+50));
    if (tabbedPane!=null)
      tabbedPane.addChangeListener(this);
  }
  
  protected void reOrderSectorChoice(ArrayList<Integer> sortedIndexes) {
    if (sortedIndexes==null || chSectors==null || chSectors.getItemCount()<2)
      return;
    Object items[]=new Object[chSectors.getItemCount()];
    for (int i=0; i<chSectors.getItemCount(); i++)
      items[i]=chSectors.getItemAt(i);
    Object selItem=chSectors.getSelectedItem();
    chSectors.invalidate();
    chSectors.removeActionListener(this);
    chSectors.removeAllItems();
    for (int i=0; i<sortedIndexes.size(); i++) {
      int idx=sortedIndexes.get(i);
      chSectors.addItem(items[idx]);
    }
    chSectors.setSelectedItem(selItem);
    chSectors.validate();
    chSectors.addActionListener(this);
  }
  
  public void sortSectorsByName() {
    if (sectorList==null || sectorList.size()<2)
      return;
    ArrayList<Integer> sortedIndexes=new ArrayList<Integer>(sectorList.size());
    for (int i = 0; i< sectorList.size(); i++) {
      OneSectorData s= sectorList.get(i);
      int idx;
      for (idx=0; idx<sortedIndexes.size() &&
                      s.sectorId.compareTo(sectorList.get(sortedIndexes.get(idx)).sectorId)>0;
           idx++);
      sortedIndexes.add(idx,i);
    }
    ArrayList<OneSectorData> sortedSectors=new ArrayList<OneSectorData>(sectorList.size());
    for (int i=0; i<sortedIndexes.size(); i++)
      sortedSectors.add(sectorList.get(sortedIndexes.get(i)));
    sectorList=sortedSectors;
    reOrderSectorChoice(sortedIndexes);
  }
  
  public void sortSectorsByNFlights () {
    if (sectorList==null || sectorList.size()<2)
      return;
    ArrayList<Integer> sortedIndexes=new ArrayList<Integer>(sectorList.size());
    for (int i = 0; i< sectorList.size(); i++) {
      OneSectorData s= sectorList.get(i);
      int idx;
      for (idx=0; idx<sortedIndexes.size() &&
                      s.getNFlights()<=sectorList.get(sortedIndexes.get(idx)).getNFlights();
           idx++);
      sortedIndexes.add(idx,i);
    }
    ArrayList<OneSectorData> sortedSectors=new ArrayList<OneSectorData>(sectorList.size());
    for (int i=0; i<sortedIndexes.size(); i++)
      sortedSectors.add(sectorList.get(sortedIndexes.get(i)));
    sectorList=sortedSectors;
    reOrderSectorChoice(sortedIndexes);
  }
  
  public void sortSectorsByNChanged () {
    if (sectorList==null || sectorList.size()<2 || scDiff==null)
      return;
    int nChanged[]=new int[sectorList.size()];
    for (int i=0; i<sectorList.size(); i++) {
      OneSectorData s= sectorList.get(i);
      nChanged[i]=scDiff.getNChangedFlights(s.sectorId);
    }
    ArrayList<Integer> sortedIndexes=new ArrayList<Integer>(sectorList.size());
    for (int i = 0; i< sectorList.size(); i++) {
      int idx=0;
      for (idx=0; idx<sortedIndexes.size() &&
                      nChanged[sortedIndexes.get(idx)]>nChanged[i];
           idx++);
      sortedIndexes.add(idx,i);
    }
    ArrayList<OneSectorData> sortedSectors=new ArrayList<OneSectorData>(sectorList.size());
    for (int i=0; i<sortedIndexes.size(); i++)
      sortedSectors.add(sectorList.get(sortedIndexes.get(i)));
    sectorList=sortedSectors;
    reOrderSectorChoice(sortedIndexes);
  }
  
  public void putSectorInFocus(String sectorId) {
    int sIdx=-1;
    for (int i = 0; i< sectorList.size() && sIdx<0; i++)
      if (sectorId.equals(sectorList.get(i).sectorId))
        sIdx=i;
    if (sIdx>=0 && sIdx!=chSectors.getSelectedIndex())
      chSectors.setSelectedIndex(sIdx);
  }
  
  public SectorShowCanvas getVisibleCanvas() {
    if (sectorsFlightsViews==null || sectorsFlightsViews.length<1)
      return null;
    if (sectorsFlightsViews.length<2)
      return sectorsFlightsViews[0];
    for (int i=0; i<sectorsFlightsViews.length; i++)
      if (sectorsFlightsViews[i]!=null && sectorsFlightsViews[i].isShowing())
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
        sectorsFlightsViews[i].setFocusSector(sectorList.get(selIdx).sectorId);
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
        for (int i = 0; i< sectorList.size() && sIdx<0; i++)
          if (cmd.equals(sectorList.get(i).sectorId))
            sIdx=i;
        if (sIdx>=0 && sIdx!=chSectors.getSelectedIndex())
          chSectors.setSelectedIndex(sIdx);
      }
      else
      if (cmd.startsWith("show_path:")) {
        String fId=cmd.substring(10);
        if (sectorsFlightsViews !=null)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            sectorsFlightsViews[i].setFocusFlight(fId);
        flInfoPanel.setFocusFlightId(fId);
      }
      else
      if (cmd.startsWith("cancel_path_show:"))  {
        if (sectorsFlightsViews !=null)
          for (int i = 0; i< sectorsFlightsViews.length; i++)
            sectorsFlightsViews[i].setFocusFlight(null);
        flInfoPanel.setFocusFlightId(null);
      }
      else
      if (cmd.startsWith("mark:") || cmd.startsWith("unmark:")) {
        SectorShowCanvas canvas=getVisibleCanvas();
        if (canvas==null)
          return;
        String fId=cmd.substring(cmd.indexOf(':')+1);
        boolean mark=cmd.startsWith("m");
        HashSet<String> fIds=canvas.getMarkedObjIds();
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
      SectorShowCanvas canvas=getVisibleCanvas();
      bUnselect.setEnabled(canvas!=null && canvas.hasSelectedObjects());
    }
    else
    if (ae.getActionCommand().equals("unselect_all")) {
      if (sectorsFlightsViews!=null)
        for (int i = 0; i< sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].clearSelection();
      flInfoPanel.setSelectedFlights(null, null);
      bUnselect.setEnabled(false);
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
              sectorsFlightsViews[i].setMinExcessPercent(perc);
      }
    }
    else
    if (ae.getActionCommand().equals("full_time_range"))  {
      if (timeFocuser.getValue()>timeFocuser.getMinimum() || timeFocuser.getUpperValue()<timeFocuser.getMaximum())
        timeFocuser.setFullRange();
    }
    updateSelectedFlightsCounts();
  }
  
  protected void updateSelectedFlightsCounts() {
    SectorShowCanvas canvas=getVisibleCanvas();
    if (canvas==null)
      return;
    ArrayList<String> selected = canvas.getSelectedObjectIds(),
        visible = canvas.getSelectedVisibleObjectIds();
    String txt = ((selected == null) ? "0" : Integer.toString(selected.size())) + " flights selected; " +
                     ((visible == null) ? "0" : Integer.toString(visible.size())) + " visible";
    labSelFlights.setText(txt);
    //labSelFlights.setSize(labSelFlights.getPreferredSize());
    labSelFlights.invalidate();
    labSelFlights.getParent().invalidate();
    labSelFlights.getParent().validate();
  }
  
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource().equals(cbShowOnlyChanged)) {
      if (sectorsFlightsViews !=null) {
        for (int i = 0; i < sectorsFlightsViews.length; i++)
          sectorsFlightsViews[i].setShowOnlyChanged(cbShowOnlyChanged.isSelected());
        SectorShowCanvas canvas=getVisibleCanvas();
        if (canvas!=null)
          flInfoPanel.setSelectedFlights(canvas.getSelectedObjectIds(),
              canvas.getSelectedVisibleObjectIds());
        updateSelectedFlightsCounts();
      }
    }
    else
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
    else
    if (e.getSource() instanceof JTabbedPane) {
      if (flInfoPanel!=null) {
        SectorShowCanvas canvas = getVisibleCanvas();
        if (canvas != null)
          flInfoPanel.setSelectedFlights(canvas.getSelectedObjectIds(),
              canvas.getSelectedVisibleObjectIds());
      }
      updateSelectedFlightsCounts();
    }
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
    SectorShowCanvas canvas=getVisibleCanvas();
    if (canvas==null)
      return;
    ArrayList<String> ids=canvas.getSelectedVisibleObjectIds();
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
    SectorShowCanvas canvas=getVisibleCanvas();
    if (canvas==null)
      return;
    ArrayList<String> ids=canvas.getSelectedVisibleObjectIds();
    if (ids==null || ids.isEmpty())
      return;
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    if (clipboard==null)
      return;
    StringBuffer txt=new StringBuffer();
    for (int i=0; i<ids.size(); i++) {
      txt.append(ids.get(i) + "\n");
      ArrayList<FlightInSector> seq = canvas.sectors.getSectorVisitSequence(ids.get(i));
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
  
  protected JPopupMenu clipboardMenu =null, sortMenu=null;
  
  public void mousePressed(MouseEvent e) {
    SectorShowCanvas canvas=getVisibleCanvas();
    if (canvas==null)
      return;
    if (e.getButton()>MouseEvent.BUTTON1) {
      Point p = getMousePosition();
      if (p == null)
        return;
      if (e.getSource().equals(labSelFlights)) {
        if (!canvas.hasSelectedVisibleObjects()) {
          if (clipboardMenu != null && clipboardMenu.isVisible())
            clipboardMenu.setVisible(false);
          return;
        }
        if (clipboardMenu != null) {
          clipboardMenu.show(this, p.x, p.y);
          return;
        }
        clipboardMenu = new JPopupMenu();
        JMenuItem mit;
        clipboardMenu.add(mit= new JMenuItem("Put identifiers to clipboard"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            clipboardMenu.setVisible(false);
            putSelectedIdsToClipboard();
          }
        });
        clipboardMenu.add(mit= new JMenuItem("Put paths to clipboard"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            clipboardMenu.setVisible(false);
            putSelectedFlightPathsToClipboard();
          }
        });
        clipboardMenu.show(this, p.x, p.y);
      }
      else {
        boolean sorting=false;
        if (e.getSource() instanceof JLabel)
          sorting=((JLabel)e.getSource()).getText().equals("Sectors:");
        else
          sorting=e.getSource().equals(chSectors);
        if (sorting) {
          if (sortMenu!=null) {
            sortMenu.show(this,p.x,p.y);
            return;
          }
        }
        sortMenu=new JPopupMenu();
        JMenuItem mit;
        sortMenu.add(mit= new JMenuItem("Sort by identifiers"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            sortMenu.setVisible(false);
            sortSectorsByName();
          }
        });
        sortMenu.add(mit= new JMenuItem("Sort by flight counts"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            sortMenu.setVisible(false);
            sortSectorsByNFlights();
          }
        });
        if (scDiff!=null) {
          sortMenu.add(mit= new JMenuItem("Sort by counts of changed flights"));
          mit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              sortMenu.setVisible(false);
              sortSectorsByNChanged();
            }
          });
        }
      }
    }
    else {
      if (clipboardMenu != null && clipboardMenu.isVisible())
        clipboardMenu.setVisible(false);
      if (sortMenu != null && sortMenu.isVisible())
        sortMenu.setVisible(false);
    }
  }
  
  public void mouseClicked(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
}
