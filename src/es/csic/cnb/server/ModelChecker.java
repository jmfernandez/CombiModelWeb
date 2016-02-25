package es.csic.cnb.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ModelChecker {
  private static final String NEWLINE = System.getProperty("line.separator");

  private DocumentBuilder builder;

  private Map<String, String> traza;

  public ModelChecker() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);

    try {
      builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new SimpleErrorHandler());
    }
    catch (ParserConfigurationException e) {
      e.printStackTrace();
    }

    traza = new HashMap<String, String>();
  }

  public boolean isValid(File fileIn) {
    boolean ok = true;

    try {
      // 1::: Validar XML (wellformed XML)
      builder.parse(new InputSource(new FileReader(fileIn)));

      // 2::: Validar modelo
      SBMLDocument doc = SBMLReader.read(fileIn);
      Model docModel = doc.getModel();

      // Sacar lista de compuestos
      Set<String> idListFromSp = new HashSet<String>();
      for (Species sp : docModel.getListOfSpecies()) {
        if (sp.getId().isEmpty()) {
          traza.put(fileIn.getName(), "Compuesto sin Id: " + sp);
          return false;
        }
        idListFromSp.add(sp.getId());
      }

      // Sacar lista de compuestos de las reacciones
      Set<String> idListFromRx = new HashSet<String>();
      for (Reaction r : docModel.getListOfReactions()) {
        if (r.getId().isEmpty()) {
          traza.put(fileIn.getName(), "Reaccion sin Id: " + r);
          return false;
        }

        // Guardar compuestos
        for (SpeciesReference spr : r.getListOfReactants()) {
          idListFromRx.add(spr.getSpecies());
        }
        for (SpeciesReference spr : r.getListOfProducts()) {
          idListFromRx.add(spr.getSpecies());
        }
      }

      // Comparar
      if (! idListFromSp.equals(idListFromRx)) {
        List<String> list = new ArrayList<String>();
        for (String id : idListFromRx) {
          if (!idListFromSp.contains(id)) {
            list.add(id);
            ok = false;
          }
        }
        if (!ok) {
          String msg = "Reacciones con compuestos no presentes en la lista de especies: " + list.toString();
          traza.put(fileIn.getName(), msg);
        }
      }
    }
    catch (SAXException e) {
      traza.put(fileIn.getName(), e.getMessage());
      return false;
    }
    catch (IOException e) {
      traza.put(fileIn.getName(), e.getMessage());
      return false;
    }
    catch (XMLStreamException e) {
      traza.put(fileIn.getName(), e.getMessage());
      return false;
    }

    return ok;
  }

  public String getTraza() {
    StringBuilder sb = new StringBuilder();
    for (String filename : traza.keySet()) {
      sb.append(filename).append(" >> ").append(traza.get(filename)).append(NEWLINE);
    }

    return sb.toString();
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/ModelRepository/bigg/nmodels/n_Ecoli_iAF1260.minmedia_ul");
    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/ModelRepository/prb/Seed458817.3.xml");
    //File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/ModelRepository/prb/Seed595537.3.xml");
    File fileIn = new File("/home/pdsanchez/Proyectos/TestCModel/Prb2013/models/n_iAbaylyiv4.xml");



    ModelChecker mc = new ModelChecker();
    if (mc.isValid(fileIn)) {
      System.out.println("VALIDO");
    }
    else {
      System.out.println("NO VALIDO");
      System.out.println(mc.getTraza());
    }


  }
}
