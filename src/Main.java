import java.io.*;

public class Main {

    public static void main(String[] args) {
      String fileNameBaseline=null, fileNameSolution=null;
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("params.txt")))) ;
        String strLine;
        try {
          while ((strLine = br.readLine()) != null) {
            String str=strLine.replaceAll("\"","").replaceAll(" ","");
            String[] tokens=str.split("=");
            if (tokens==null || tokens.length<2)
              continue;
            String parName=tokens[0].trim().toLowerCase();
            if (parName.equals("data_baseline"))
              fileNameBaseline=tokens[1].trim();
            else
              if (parName.equals("data_solution"))
                fileNameSolution=tokens[1].trim();
          }
        } catch (IOException io) {
          System.out.println(io);
        }
      } catch (IOException io) {
        System.out.println(io);
      }
      if (fileNameBaseline==null)
        return;
      DataStore baseline=new DataStore();
      System.out.println("Tryng to get baseline data ...");
      if (!baseline.readData(fileNameBaseline))
        return;
      System.out.println("Successfully got baseline data!");
      
      DataStore solution=new DataStore();
      System.out.println("Tryng to get solution data ...");
      if (solution.readData(fileNameSolution)) {
        System.out.println("Successfully got solution data!");
      }
      else {
        System.out.println("Failed to get solution data!");
      }
    }
}
