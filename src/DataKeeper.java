import java.io.*;
import java.util.Vector;

public class DataKeeper {
  public String fileName=null;
  public String attrNames[]=null;
  public Vector<Object[]> data=null;
  
  protected void readData(String fname) {
    fileName=fname;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fname+".csv")))) ;
      String strLine;
    
      int n=0;
      try {
        br.readLine();
        while ((strLine = br.readLine()) != null) {
          n++;
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split(",");
          if (tokens!=null && tokens.length>0) {
            if (attrNames==null)
              attrNames=tokens;
            else {
              if (data==null)
                data=new Vector<>(100000,50000);
            }
          }
          if (n%100000==0)
            System.out.println("lines processed: "+n);
        }
        br.close();
      } catch (IOException io) {
        System.out.println(io);
      }
      System.out.println("lines processed: "+n);
    } catch (FileNotFoundException ex) {
      System.out.println(ex);
    }
  }
}
