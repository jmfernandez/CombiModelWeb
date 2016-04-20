package es.csic.cnb.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.EmailException;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

import biz.source_code.miniTemplator.MiniTemplator;
import biz.source_code.miniTemplator.MiniTemplator.TemplateSyntaxException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import es.csic.cnb.ModelManager;
import es.csic.cnb.util.PropertiesParser;
import es.csic.cnb.client.rpc.MergeService;
import es.csic.cnb.shared.ClientData;
import es.csic.cnb.shared.JobStatus;
import es.csic.cnb.shared.ModelData;
import es.csic.cnb.shared.error.FieldVerifier;
import es.csic.cnb.shared.error.ServerException;
import es.csic.cnb.ws.ChebiException;

public class MergeServiceImpl extends RemoteServiceServlet implements MergeService {
  private static final long serialVersionUID = 1L;

  private static Logger log = Logger.getLogger(MergeServiceImpl.class.getName());

  private static final String NEWLINE = System.getProperty("line.separator");

  private static final String DATEFORMAT = "yyyy/MM/dd HH:mm:ss";

  private static final String CMODEL_PROPERTIES_FILE = "/WEB-INF/cmodel.properties";

  // Hash with the session-ids and the status associated
  private Map<String, JobStatus> statusList = new HashMap<String, JobStatus>();

  @Override
  public ClientData run(String sessionId, ClientData cdata) throws ServerException {
    List<ModelData> modelDataList = cdata.getModelDataList();

    // Verify that the input is valid.
    // If the input is not valid, throw a ServerException back to the client.
    if (!FieldVerifier.isModelUploaded(modelDataList.size())) {
      throw new ServerException("Please upload the model");
    }

    // Create a JobStatus for this session-id
    JobStatus status = new JobStatus();
    statusList.put(sessionId, status);

    final String ip = getThreadLocalRequest().getRemoteAddr();
    StringBuilder sblog = new StringBuilder();
    sblog.append("SESSION_ID ").append(sessionId);
    sblog.append(" [").append(ip).append("]: ");
    final String logId = sblog.toString();

    // Log - start process
    String userAgent = getThreadLocalRequest().getHeader("User-Agent");
    log.info(new StringBuilder().append("START ").append(logId).append(userAgent).toString());

    // Retrieve properties file
    Properties properties = new PropertiesParser(System.getProperties());
    try {
      properties.load(getServletContext().getResourceAsStream(CMODEL_PROPERTIES_FILE));
    } catch (IOException e) {
      log.log(Level.SEVERE, logId + e.getMessage(), e);
      throw new ServerException("An error occurred while "
              + "attempting to contact the server", status, e);
    }

    // Store in properties file the base url and the root path
    String baseUrl = null;
    HttpServletRequest request = this.getThreadLocalRequest();
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443)) {
      baseUrl =  request.getScheme() + "://" + request.getServerName() + request.getContextPath();
    } else {
      baseUrl =  request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
              + request.getContextPath();
    }
    properties.setProperty("baseurl", baseUrl);

    String rootPath = getServletContext().getRealPath(File.separator);
    properties.setProperty("rootpath", rootPath);

    // Minmedia
    int mm = cdata.getMinmedia();
    boolean useMinMedia = false; // true en basic minmedia e intersection minmedia

    // Crear directorio de trabajo
    File dir = new File(rootPath, "output");
    dir = new File(dir, sessionId);
    dir.mkdir();

    File idxHtml = new File(dir, "index.html");

    // Plantilla HTML
    MiniTemplator templator = null;

    // Capturar todas las excepciones para poder notificar al user
    try {
      // Cargar template.html
      MiniTemplator.Builder tsp = new MiniTemplator.Builder();
      try {
        Set<String> flags = new HashSet<String>();
        if (cdata.getMail() != null) {
          flags.add("existEmail");
        }
        if (cdata.isNewobj()) {
          flags.add("existObj");
        }

        if (mm == ClientData.BASICMM) {
          flags.add("existBMM");
        }
        else if (mm == ClientData.INTERSECMM) {
          flags.add("existIMM");
        }
        else if (mm == ClientData.COMBIMM) {
          flags.add("existCMM");
        }
        tsp.setConditionFlags(flags);

        InputStreamReader templateTextReader = new InputStreamReader(getServletContext().getResourceAsStream("/resources/template.html"), "UTF-8");
        templator = tsp.build(templateTextReader);

        StringBuilder chtml = new StringBuilder();
        chtml.append("<p><img src=\"../../images/icon/loader.gif\" alt=\"running\"/> ");
        chtml.append("Your job is running</p>");
        chtml.append("<p>The results will be available in a few minutes.");
        if (cdata.getMail() != null) {
        chtml.append("<br><small>On completion an email containing the results will be sent to <strong>");
        chtml.append(cdata.getMail()).append("</strong></small>");
        }
        chtml.append("</p>");
        updateTemplate(templator, sessionId, cdata, baseUrl, "running", chtml.toString());
        templator.generateOutput(idxHtml.getPath());
      }
      catch (MalformedURLException e) {
        e.printStackTrace();
        log.log(Level.SEVERE, logId + e.getMessage(), e);

      } catch (TemplateSyntaxException e) {
        e.printStackTrace();
        log.log(Level.SEVERE, logId + e.getMessage(), e);
      } catch (IOException e) {
        e.printStackTrace();
        log.log(Level.SEVERE, logId + e.getMessage(), e);
      }


      //      File idxHtml = new File(dir, "index.html");
      //      URL htmlUrl = getServletContext().getResource("/resources/template.html");
      //      FileUtils.copyURLToFile(htmlUrl, idxHtml);
      //
      //      // Crear html
      //      //FileUtils.writeStringToFile(new File(dir, "index.html"), "html - running");


      // Guarda Strings a volcar en el zip
      Map<String, CharSequence> info = new HashMap<String, CharSequence>(4);

      // Equivalencias entre compuestos
      Set<String> equivList = new HashSet<String>();

      // Guarda los modelos normalizados
      List<File> modelList = new LinkedList<File>();
      // Guarda datos pendientes de curacion manual (warning para el user)
      Set<String> warnList = new HashSet<String>();

      ModelManager mdl = new ModelManager();
      for (ModelData data : modelDataList) {
        File fileIn = new File(data.getFilepath());
        File fileOut = new File(fileIn.getParent(), "normal_" + fileIn.getName());
        try {
          Map<String, String> atts = new HashMap<String, String>();
          atts.put("ph", String.valueOf(data.getPh()));
          atts.put("coef", String.valueOf(data.getCoef()));

          //fileOut = mdl.writeNormalizedModel(fileIn);
          List<String> equivs = mdl.writeNormalizedModel(fileIn, fileOut, atts);
          equivList.addAll(equivs);
        }
        catch (ChebiException e) {
          log.log(Level.SEVERE, logId + e.getMessage(), e);
          throw new ServerException("An error occurred while attempting to normalize the model: " + e.getMessage(), status, e);
        }

        // Contemplar casos a corregir manualmente
        List<Species> spList = mdl.writeManualCurationModel();
        for (Species sp : spList) {
          warnList.add(sp.getId()+" - "+sp.getName());
        }

        modelList.add(fileOut);

        // Shell Script (Solo si hay que calcular el medio minimo de la interseccion)
        if (mm == ClientData.INTERSECMM) {
          log.info("Shell Script: calculate minimum media");
        	
          ShWrapper shw = new ShWrapper(properties, status);
          String mmfile = fileOut.getName().replace(".xml", ".minmedia_ul");

          File newfile = new File(fileOut.getParent(), mmfile);
          //String s = shw.runMinMedia(fileOut, newfile);
          shw.runMinMedia(fileOut, newfile);

          String txt = null;
          try {
            txt = FileUtils.readFileToString(newfile);
          } catch (IOException e) {
            log.log(Level.SEVERE, logId + "An error occurred while attempting to read the minmedia file", e);
          }
          data.setMinmedia(txt);
          //info.put("MM", txt); // Habria que concatenar los mm de los diferentes modelos

          useMinMedia = true;
        }
        // Shell Script (Solo si hay que calcular el medio minimo basico)
        else if (mm == ClientData.BASICMM) {
          log.info("Shell Script: calculate basic minimum media");
          
          String mmfile = fileOut.getName().replace(".xml", ".minmedia_ul");
          File newfile = new File(fileOut.getParent(), mmfile);

          final DefaultMinMedia defMinMedia = new DefaultMinMedia(fileOut);
          defMinMedia.createMinMediaFile(newfile);

          String txt = null;
          try {
            txt = FileUtils.readFileToString(newfile);
          } catch (IOException e) {
            log.log(Level.SEVERE, logId + "An error occurred while attempting to read the minmedia file", e);
          }
          data.setMinmedia(txt);
          //info.put("MM", txt); // Habria que concatenar los mm de los diferentes modelos

          useMinMedia = true;
        }
      }

      // Perl
      log.info("Perl process");
      
      PerlWrapper perlw = new PerlWrapper(properties, status);
      String cmodel = perlw.runMergeModels(modelList, useMinMedia, cdata.isNewobj());
      info.put("CMODEL", cmodel);

      // Shell Script (Solo si hay que calcular el medio minimo del modelo combinado)
      if (mm == ClientData.COMBIMM) {
    	log.info("Shell Script: calculate combined model minimum media");
    	  
        // Crear fichero cmodel
        String cmodelFilename = "cmodel-"+ sessionId +".xml";
        File cmodelFile = new File(modelDataList.get(0).getFilepath());
        cmodelFile = new File(cmodelFile.getParentFile(), cmodelFilename);

        try {
          // Volcar el contenido de txt al fichero
          FileUtils.writeStringToFile(cmodelFile, cmodel);

          ShWrapper shw = new ShWrapper(properties, status);
          String mmfile = cmodelFilename.replace(".xml", ".minmedia_ul");

          File shOutFile = new File(cmodelFile.getParent(), mmfile);
          //String s = shw.runMinMedia(fileOut, newfile);
          shw.runMinMedia(cmodelFile, shOutFile);

          StringBuilder sb = new StringBuilder();
          sb.append("Reaction_ID").append("\t").append("Reaction_Name").append(NEWLINE).append(NEWLINE);

          // Lista de ids de las reacciones del medio minimo
          Set<String> mmIdList = new HashSet<String>();
          String txt = FileUtils.readFileToString(shOutFile);
          String[] vlines = txt.split("\n");
          for (String line : vlines) {
            String[] reg = line.split("\t");
            mmIdList.add(reg[0]);

            sb.append(reg[0]).append("\t").append(reg[1]).append(NEWLINE);
          }
          info.put("MM", sb);

          // Cargar modelo para cambiar LB
          SBMLDocument cdoc = SBMLReader.read(cmodelFile);
          for (Reaction r : cdoc.getModel().getListOfReactions()) {
            boolean generic = false;

            List<SpeciesReference> listCpds = new ArrayList<SpeciesReference>();
            listCpds.addAll(r.getListOfProducts());
            listCpds.addAll(r.getListOfReactants());

            checkcpds:
              for (SpeciesReference psr : listCpds) {
                if (psr.getSpeciesInstance().isBoundaryCondition()) {
                  generic = true;
                  break checkcpds;
                }
              }

            if (generic) {
              if (!mmIdList.contains(r.getId())) {
		KineticLaw kl = r.getKineticLaw();
		if(kl != null) {
			kl.getLocalParameter("LOWER_BOUND").setValue(0.0);
		}
              }
            }
          }
          SBMLWriter wr = new SBMLWriter();
          cmodel = wr.writeSBMLToString(cdoc);
          info.put("CMODEL", cmodel);

        } catch (IOException e) {
          log.log(Level.SEVERE, logId + "An error occurred while attempting to read the minmedia file", e);
        } catch (XMLStreamException e) {
          log.log(Level.SEVERE, logId + "An error occurred while attempting to read the minmedia file", e);
        }
      }


      // Guardar salida en fichero
      if (!warnList.isEmpty()) {
        // Crear txt warnings
        StringBuilder sb = new StringBuilder();
        sb.append("List of compounds that could not be normalized.").append(NEWLINE);
        sb.append("You may want to check these, because these compounds were retained as unique and not mapped between the models.").append(NEWLINE);
        sb.append("If you find any equivalence, please change the model manually.").append(NEWLINE).append(NEWLINE);
        for (String s : warnList) {
          sb.append(s).append(NEWLINE);
        }
        info.put("NOMAP", sb);
      }
      if (!equivList.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("List of compounds normalized.").append(NEWLINE).append(NEWLINE);

        List<String> list = new ArrayList<String>();
        list.addAll(equivList);
        Collections.sort(list);
        for (String s : list) {
          sb.append(s).append(NEWLINE);
        }
        info.put("MAP", sb.toString());
      }

      // Readme
      SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
      String initDate = sdf.format(cdata.getInitDate());

      StringBuilder sb = new StringBuilder();
      sb.append("cmodel-").append(sessionId).append(NEWLINE).append(NEWLINE); // cmodelId
      sb.append("Creation date: ").append(initDate).append(NEWLINE).append(NEWLINE);
      for (ModelData md : cdata.getModelDataList()) {
        sb.append("Model: ").append(md.getFilename()).append(NEWLINE);
      }

      if (mm == ClientData.BASICMM) {
        sb.append(NEWLINE).append("- Basic minimum media selected").append(NEWLINE);
      }
      else if (mm == ClientData.INTERSECMM) {
        sb.append(NEWLINE).append("- Intersection minimum media selected").append(NEWLINE);
      }
      else if (mm == ClientData.COMBIMM) {
        sb.append(NEWLINE).append("- Combination minimum media selected").append(NEWLINE);
      }

      if (cdata.isNewobj()) {
        sb.append(NEWLINE).append("- Biomass joint function selected").append(NEWLINE);
      }
      info.put("README", sb.toString());

      String zipName = null;
      try {
        // XML
        //outputPath = createXml(sessionId, rootPath, cmodel);

        // Zip
        zipName = createZip(sessionId, dir, modelDataList.get(0).getFilepath(), info);

        // Actualizar html
        String path = baseUrl + "/output/" + sessionId + "/" + zipName;

        // TODO Cambiar 72 por dato del properties (schedule.limit.hours)
        StringBuilder okhtml = new StringBuilder();
        okhtml.append("<p>The process has been completed successfully</p>");
        okhtml.append("<a href=\"").append(path).append("\">");
        okhtml.append("Click here to download the model</a><br>");
        okhtml.append("<small>Right-click the link and choose 'Save Link As...' to save ");
        okhtml.append("the document to your local disk</small>");
        okhtml.append("<p style=\"font-family: Arial, Monospace; color: #ff0000;\">This link will be available for 72 hours</p>");
        updateTemplate(templator, sessionId, cdata, baseUrl, "finished", okhtml.toString());
        templator.generateOutput(idxHtml.getPath());

        // Enviar mail
        if (cdata.getMail() != null) {
          SendMail mail = new SendMail(properties);
          //mail.sendMail(cdata, rootPath + zipName);
          mail.sendMail(cdata, new File(dir, zipName).getPath());
        }

      }
      catch (IOException e) {
        log.log(Level.SEVERE, logId + "An error occurred while attempting to create the model", e);
        throw new ServerException("An error occurred while attempting to create the model", e);
      }
      catch (EmailException e) {
        log.log(Level.SEVERE, logId + "An error occurred while attempting to send the email", e);
        throw new ServerException("An error occurred while attempting to send the email", status, e);
      }

      // Remove JobStatus object for this session-id
      statusList.remove(sessionId);
    }
    catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage(), e);
      
      try {
        StringBuilder chtml = new StringBuilder();
        chtml.append("<img src=\"../../images/icon/warn.png\" alt=\"${status}\"/>");
        chtml.append("<p>An error occurred while attempting to create the model: ");
        chtml.append(e.getMessage());
        chtml.append("</p>");
        updateTemplate(templator, sessionId, cdata, baseUrl, "error", chtml.toString());

        templator.generateOutput(idxHtml.getPath());
      } catch (IOException e1) {
        log.log(Level.SEVERE, logId + "An error occurred while attempting to generate the index.html", e1);
      }

      SendErrorMail errmail = new SendErrorMail(properties);
      errmail.notifyDeveloper(sessionId, e, cdata);
      if (cdata.getMail() != null) {
        errmail.sendMail("An error occurred while attempting to create the model: " + e.getMessage(), cdata);
      }

      throw new ServerException("An error occurred while attempting to create the model: " + e.getMessage(), e);
    }

    return cdata;
  }

  //private String createXml(String sessionId, String rootPath, CharSequence> info) throws IOException {
  //  String outputPath = "/output/" + "cmodel-"+ sessionId +".xml";
  //  File cmodelFile = new File(rootPath, outputPath);
  //
  //  FileUtils.writeStringToFile(cmodelFile, info.get("CMODEL").toString());
  //
  //  return outputPath;
  //}

  private String createZip(String sessionId, File dir, String path, Map<String, CharSequence> info) throws IOException {
    String filename = "cmodel-"+ sessionId +".xml";
    File cmodelFile = new File(path);
    cmodelFile = new File(cmodelFile.getParentFile(), filename);
    
    log.info("Create zip: " + filename);

    // Volcar el contenido de txt al fichero
    FileUtils.writeStringToFile(cmodelFile, info.get("CMODEL").toString());

    // Create a buffer for reading the files
    byte[] buf = new byte[1024];

    // Create the ZIP file
    //String outputPath = "/output/" + "cmodel-"+ sessionId +".zip";
    String zipName = "/cmodel-"+ sessionId +".zip";
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(dir, zipName)));

    // Add ZIP entry to output stream.
    out.putNextEntry(new ZipEntry(filename));

    // Compress the files
    FileInputStream in = new FileInputStream(cmodelFile);

    // Transfer bytes from the file to the ZIP file
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    // Complete the entry
    out.closeEntry();
    in.close();

    // Another entry (README)
    if (info.get("README") != null) {
      String wfilename = "readme.txt";
      File wFile = new File(path);
      wFile = new File(wFile.getParentFile(), wfilename);

      FileUtils.writeStringToFile(wFile, info.get("README").toString());

      out.putNextEntry(new ZipEntry(wfilename));

      in = new FileInputStream(wFile);

      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }

      out.closeEntry();
      in.close();
    }

    // Another entry (warnings)
    if (info.get("MAP") != null) {
      String wfilename = "compounds_mapped.txt";
      File wFile = new File(path);
      wFile = new File(wFile.getParentFile(), wfilename);

      FileUtils.writeStringToFile(wFile, info.get("MAP").toString());

      out.putNextEntry(new ZipEntry(wfilename));

      in = new FileInputStream(wFile);

      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }

      out.closeEntry();
      in.close();
    }

    // Another entry (warnings)
    if (info.get("NOMAP") != null) {
      String wfilename = "compounds_not_mapped.txt";
      File wFile = new File(path);
      wFile = new File(wFile.getParentFile(), wfilename);

      FileUtils.writeStringToFile(wFile, info.get("NOMAP").toString());

      out.putNextEntry(new ZipEntry(wfilename));

      in = new FileInputStream(wFile);

      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }

      out.closeEntry();
      in.close();
    }

//    // Another entry (minmedia)
//    if (info.get("MM") != null) {
//      String wfilename = "minimum_media.txt";
//      File wFile = new File(path);
//      wFile = new File(wFile.getParentFile(), wfilename);
//
//      FileUtils.writeStringToFile(wFile, info.get("MM").toString());
//
//      out.putNextEntry(new ZipEntry(wfilename));
//
//      in = new FileInputStream(wFile);
//
//      while ((len = in.read(buf)) > 0) {
//        out.write(buf, 0, len);
//      }
//
//      out.closeEntry();
//      in.close();
//    }


    // Complete the ZIP file
    out.close();

    return zipName;
  }

  private void updateTemplate(MiniTemplator templator, String sessionId, ClientData cdata, String baseUrl, String status, String html) {
    templator.reset();
    templator.setVariable("baseUrl", baseUrl);
    templator.setVariable("status", status);
    templator.setVariable("chtml", html);
    templator.setVariableOpt("email", cdata.getMail());

    SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    String initDate = sdf.format(cdata.getInitDate());
    templator.setVariable("initDate", initDate);

    String cmodelId = "cmodel-" + sessionId;
    templator.setVariable("cmodelId", cmodelId);

    for (ModelData md : cdata.getModelDataList()) {
      templator.setVariable("modelname", md.getFilename());
      templator.addBlock("modelList");
    }
  }
}
