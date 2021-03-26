package ui;

import data_manage.SectorSet;

import javax.swing.*;
import java.util.ArrayList;

public class SelectedFlightsInfoShow extends JPanel {
  /**
   * Information about all sectors
   */
  public SectorSet sectors=null;
  /**
   * Identifiers of selected flights
   */
  protected ArrayList<String> selectedFlIds=null;
}
