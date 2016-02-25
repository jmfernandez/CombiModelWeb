package es.csic.cnb.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import es.csic.cnb.shared.JobStatus;
import es.csic.cnb.shared.error.ServerException;

public class PerlWrapper {
  private static Logger log = Logger.getLogger(PerlWrapper.class.getName());

  private static final String NEWLINE = System.getProperty("line.separator");

  private static final String PERL_ERROR =
          "An error occurred while attempting to contact the server." +
          " Perl exception in 'genreac2.pl' module.";

  private static final int BUFFER_SIZE = 4096;

  // "/usr/bin/perl"
  private static final String PERL = "/usr/bin/perl";

  private Properties properties;
  private JobStatus status;
  private String perlPath;

  private ProcessBuilder pb;

  public PerlWrapper(Properties properties, JobStatus status) {
    this.properties = properties;
    this.status = status;

    String rootPath = properties.getProperty("rootpath");
    perlPath = rootPath + "WEB-INF/lib/perl/";

    pb = new ProcessBuilder();
    pb.redirectErrorStream(true);
  }

  public String runMergeModels(List<File> modelList, boolean mimedia, boolean newobj) throws ServerException {
    // Create commands
    List<String> commandList = new ArrayList<String>();
    commandList.add(properties.getProperty("perl", PERL));
    commandList.add(perlPath + "genreac2.pl");
    if (mimedia) {
      commandList.add("-minmedia");
    }
    if (newobj) {
      commandList.add("-newobj");
    }
    for (File f : modelList) {
      commandList.add(f.getAbsolutePath());
    }
    
    //System.out.println("---> " + commandList);
    log.fine("Perl command: " + commandList);

    pb.command(commandList);

    StringBuilder sb = new StringBuilder();
//    StringBuilder sbErr = new StringBuilder();

    Process pr = null;
    BufferedReader br = null;
//    BufferedReader brErr = null;
    try {
      // Ejecutar el programa externo
      pr = pb.start();

      br = new BufferedReader(new InputStreamReader(pr.getInputStream()), BUFFER_SIZE);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append(NEWLINE);
      }
      
//      brErr = new BufferedReader(new InputStreamReader(pr.getErrorStream()), BUFFER_SIZE);
//      String lineErr;
//      while ((lineErr = brErr.readLine()) != null) {
//        sbErr.append(lineErr).append(NEWLINE);
//      }

      // Termination with error
      if (pr.waitFor() != 0) {
        throw new ServerException(PERL_ERROR, status);
      }

    } catch (Exception e) {
      log.severe("Perl error: " + e.getMessage());
    	
      status.setInfo(sb.toString());
      throw new ServerException(PERL_ERROR, status, e);

    } finally {
      // Cerrar conexiones
      try {
        if (br != null) {
          br.close();
        }

        if (pr != null) {
          pr.getInputStream().close();
          pr.getOutputStream().close();
          pr.getErrorStream().close();
        }

      } catch (IOException e) {
        throw new ServerException(PERL_ERROR, status, e);
      }
    }

    return sb.toString();
  }
}
