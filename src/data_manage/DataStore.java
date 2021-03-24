package data_manage;

import java.io.*;
import java.util.Vector;

public class DataStore {
  public String fileName=null;
  public String attrNames[]=null;
  public Vector<Object[]> data=null;
  
  public boolean readData(String fname) {
    fileName=fname;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fname)))) ;
      String strLine;
    
      int n=0;
      try {
        while ((strLine = br.readLine()) != null) {
          n++;
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split(",");
          if (tokens!=null && tokens.length>0) {
            if (attrNames==null) {
              attrNames = tokens;
              System.out.println("Got attribute names:");
              for (int i=0; i<attrNames.length; i++)
                System.out.println(attrNames[i]);
            }
            else {
              if (data==null)
                data=new Vector<>(100000,50000);
              Object values[]=new Object[attrNames.length];
              for (int i=0; i<attrNames.length; i++) {
                values[i]=null;
                if (i>=tokens.length || tokens[i]==null) continue;
                String tok=tokens[i].trim();
                if (tok.length()<1) continue;
                if (!tok.contains(".") && !tok.contains("E") && !tok.contains("e"))
                  try {
                    values[i]=Integer.valueOf(tok);
                    continue;
                  } catch (Exception ex) {}
                try {
                  values[i]=Double.valueOf(tok);
                  continue;
                } catch (Exception ex) {}
                values[i]=tok;
              }
              data.addElement(values);
            }
          }
          if (n%5000==0)
            System.out.println("lines processed: "+n);
        }
        br.close();
      } catch (IOException io) {
        System.out.println(io);
      }
      int nData=(data==null)?0:data.size();
      System.out.println("lines processed: "+n+"; data records stored: "+nData);
    } catch (FileNotFoundException ex) {
      System.out.println(ex);
    }
    return attrNames!=null && data!=null && data.size()>0;
  }
}
