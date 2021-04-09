package ui;

import data_manage.OneSectorData;
import data_manage.SectorSet;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

public class SectorShowPanel extends JPanel
    implements ActionListener, ItemListener, ChangeListener {
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * The canvas with the visual representation
   */
  protected SectorShowCanvas canvas=null;
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
   * Whether to list all selected flights or only visible
   */
  protected JRadioButton rbListAll=null, rbListVisible=null;
  
  public SectorShowPanel(SectorSet sectors) {
    super();
    if (sectors==null || sectors.getNSectors()<1)
      return;
    
    JPanel mainP=new JPanel(new BorderLayout());
    
    JPanel bp=new JPanel(new GridLayout(0,1));
    mainP.add(bp,BorderLayout.SOUTH);

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
    
    canvas=new SectorShowCanvas(sectors);
    canvas.setFocusSector(sortedSectors.get(maxIdx).sectorId);
    canvas.addActionListener(this);
    chAggrStep.setSelectedItem(Integer.toString(canvas.getAggregationTimeStep()));
    mainP.add(canvas,BorderLayout.CENTER);
  
    flInfoPanel=new SelectedFlightsInfoShow(sectors);
    flInfoPanel.addActionListener(this);
    flInfoPanel.setCurrentSectors(canvas.getFocusSectorId(),canvas.getFromSectorIds(),canvas.getToSectorIds());
    JScrollPane scp=new JScrollPane(flInfoPanel);
    
    p=new JPanel(new BorderLayout());
    p.add(scp,BorderLayout.CENTER);
    bp=new JPanel(new GridLayout(0,1));
    p.add(bp,BorderLayout.NORTH);
    
    labSelFlights=new JLabel("0 flights selected",JLabel.CENTER);
    bp.add(labSelFlights);
    
    rbListVisible=new JRadioButton("visible",true);
    rbListVisible.addActionListener(this);
    rbListAll=new JRadioButton("all",false);
    rbListAll.addActionListener(this);
    ButtonGroup g=new ButtonGroup();
    g.add(rbListVisible);
    g.add(rbListAll);
    pp=new JPanel(new FlowLayout(FlowLayout.CENTER,5,0));
    pp.add(new JLabel("List"));
    pp.add(rbListVisible);
    pp.add(rbListAll);
    bp.add(pp);
    
    JSplitPane spl=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,mainP,p);
    setLayout(new BorderLayout());
    add(spl,BorderLayout.CENTER);
    spl.setDividerLocation(canvas.getPreferredSize().width);
  
    sectorSelections=new ArrayList<Integer>(100);
    sectorSelections.add(maxIdx);
    //setPreferredSize(new Dimension(canvas.getPreferredSize().width+150,
        //canvas.getPreferredSize().height+50));
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
    if (ae.getSource().equals(chAggrStep)) {
      if (canvas!=null)
        canvas.setAggregationTimeStep((Integer)chAggrStep.getSelectedItem());
    }
    else
    if (ae.getSource().equals(chEntriesOrPresence)) {
      if (canvas!=null)
        canvas.setToCountEntries(chEntriesOrPresence.getSelectedIndex()==0);
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
      flInfoPanel.setCurrentSectors(canvas.getFocusSectorId(),canvas.getFromSectorIds(),canvas.getToSectorIds());
      flInfoPanel.setSelectedFlights(canvas.getSelectedVisibleObjectIds());
    }
    else
    if (ae.getSource().equals(canvas) || ae.getSource().equals(flInfoPanel)) {
      String cmd=ae.getActionCommand();
      if (cmd.equals("object_selection")) {
        flInfoPanel.setSelectedFlights(canvas.getSelectedVisibleObjectIds());
      }
      else
      if (cmd.startsWith("deselect_object:"))  {
        String oId=cmd.substring(16);
        canvas.deselectObject(oId);
      }
      else
      if (cmd.startsWith("highlight_object:"))  {
        String oId=cmd.substring(17);
        canvas.highlightObject(oId);
      }
      else
      if (cmd.startsWith("dehighlight_object:"))  {
        String oId=cmd.substring(19);
        canvas.dehighlightObject(oId);
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
        if (canvas!=null)
          canvas.setFocusFlight(cmd.substring(10));
      }
      else
      if (cmd.startsWith("cancel_path_show:"))  {
        if (canvas!=null)
          canvas.setFocusFlight(null);
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
          perc=(canvas==null)?10:canvas.getMinExcessPercent();
          tf.setText(String.valueOf(perc));
        }
        else
          if (canvas!=null)
            canvas.setMinExcessPercent(perc);
      }
    }
    else
    if (ae.getActionCommand().equals("full_time_range"))  {
      if (timeFocuser.getValue()>timeFocuser.getMinimum() || timeFocuser.getUpperValue()<timeFocuser.getMaximum())
        timeFocuser.setFullRange();
    }
    if (canvas!=null) {
      ArrayList<String> selected = canvas.getSelectedObjectIds(), visible = canvas.getSelectedVisibleObjectIds();
      String txt = ((selected == null) ? "0" : Integer.toString(selected.size())) + " flights selected; " +
                       ((visible == null) ? "0" : Integer.toString(visible.size())) + " visible";
      labSelFlights.setText(txt);
      labSelFlights.setSize(labSelFlights.getPreferredSize());
    }
  }
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource().equals(cbShowOnlySelected)) {
      if (canvas!=null)
        canvas.setShowOnlySelectedFlights(cbShowOnlySelected.isSelected());
    }
    else
    if (e.getSource().equals(cbIgnoreReEntries)) {
      if (canvas!=null)
        canvas.setToIgnoreReEntries(cbIgnoreReEntries.isSelected());
    }
    else
    if (e.getSource().equals(cbHighlightExcess))  {
      if (canvas!=null)
        canvas.setToHighlightCapExcess(cbHighlightExcess.isSelected());
    }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(timeFocuser))
      getTimeRange();
  }
  
  protected void getTimeRange() {
    int m1=timeFocuser.getValue(), m2=timeFocuser.getUpperValue();
    if (m2-m1<60) {
      if (canvas!=null)
        if (m1==canvas.getMinuteStart()) {
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
    if (canvas!=null)
      canvas.setTimeRange(m1,m2);
    bFullRange.setEnabled(m1>timeFocuser.getMinimum() || m2<timeFocuser.getMaximum());
  }
  
}
