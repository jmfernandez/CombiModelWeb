package es.csic.cnb.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import es.csic.cnb.shared.JobStatus;
import es.csic.cnb.shared.error.ServerException;

public class ShWrapper {
  //private static Logger log = Logger.getLogger(ShWrapper.class.getName());

  private static final String NEWLINE = System.getProperty("line.separator");

  private static final String SH_ERROR =
          "An error occurred while attempting to contact the server." +
          " sh exception in '.sh' module.";

  private static final int BUFFER_SIZE = 4096;

  private JobStatus status;
  private String shPath;
  private String mcrRoot;

  private ProcessBuilder pb;

  public ShWrapper(Properties properties, JobStatus status) {
    this.status = status;

    String rootPath = properties.getProperty("rootpath");
    shPath = rootPath + "WEB-INF/lib/sh/";

    mcrRoot = properties.getProperty("mcr");

    pb = new ProcessBuilder();
    pb.redirectErrorStream(true);
  }

  public String runMinMedia(File model, File out) throws ServerException {
    // XXX
    //String mcrRoot = "/usr/local/MATLAB/MATLAB_Compiler_Runtime/v714";

    // Create commands
    List<String> commandList = new ArrayList<String>();
    commandList.add(shPath + "run_minmedia.sh");
    commandList.add(mcrRoot);
    commandList.add(model.getAbsolutePath());
    commandList.add(out.getAbsolutePath());
    pb.command(commandList);

    System.out.println("COMAND LIST: "+commandList);

//    // Variables de entorno XXX
//    StringBuilder varLdLibPath = new StringBuilder();
//    varLdLibPath.append(mcrRoot).append("runtime/glnxa64:");
//    varLdLibPath.append(mcrRoot).append("sys/os/glnxa64:");
//    varLdLibPath.append(mcrRoot).append("sys/java/jre/glnxa64/jre/lib/amd64/native_threads:");
//    varLdLibPath.append(mcrRoot).append("sys/java/jre/glnxa64/jre/lib/amd64/server:");
//    varLdLibPath.append(mcrRoot).append("sys/java/jre/glnxa64/jre/lib/amd64");
//
//    Map<String, String> env = pb.environment();
//    env.put("LD_LIBRARY_PATH", varLdLibPath.toString());
//    env.put("XAPPLRESDIR", mcrRoot + "X11/app-defaults");


    StringBuilder sb = new StringBuilder();

    Process pr = null;
    BufferedReader br = null;
    try {
      // Ejecutar el programa externo
      pr = pb.start();

      br = new BufferedReader(new InputStreamReader(pr.getInputStream()), BUFFER_SIZE);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append(NEWLINE);
      }

      // Termination with error
      if (pr.waitFor() != 0) {
        throw new ServerException(SH_ERROR, status);
      }

    } catch (Exception e) {
      status.setInfo(sb.toString());
      throw new ServerException(SH_ERROR, status, e);

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
        throw new ServerException(SH_ERROR, status, e);
      }
    }

    return sb.toString();
  }
}
