package es.csic.cnb.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

public class DefaultMinMedia {
  private static final String NEWLINE = System.getProperty("line.separator");
  private static final String TAB = "\t";

  private final static Matcher MTFM = Pattern.compile("FORMULA:\\s?([A-Za-z0-9]+)").matcher("");
  private final static Matcher MTNAMEFM = Pattern.compile("^(.+)_((?:[A-Z][a-z]?\\d*)+)$").matcher("");

  private final static Set<String> VMMDEFAULT = new HashSet<String>();

  private File cmodelFile;
  private SBMLDocument cdoc;

  // Identificadores de los compuestos del MM
  private Set<String> mmcpdIdList;

  public DefaultMinMedia(File cmodelFile) {
    // Formulas de los compuestos del medio minimo
    // Uso las formulas porque es cte (el nombre puede variar de un modelo a otro)
    VMMDEFAULT.add("C6H12O6"); // Glucosa
    VMMDEFAULT.add("C3H8O3");  // Glicerol
    VMMDEFAULT.add("H4N");     // Amonio
    VMMDEFAULT.add("HO4P");    // Fosfato
    VMMDEFAULT.add("O4S");     // Sulfato
    VMMDEFAULT.add("H2O");     // Agua
    VMMDEFAULT.add("Co");      // Co2+
    VMMDEFAULT.add("K");       // K+
    VMMDEFAULT.add("Mg");      // Mg
    VMMDEFAULT.add("Ca");      // Ca2+
    VMMDEFAULT.add("Cl");      // Cl-
    VMMDEFAULT.add("Cu");      // Cu2+
    VMMDEFAULT.add("Mn");      // Mn2+
    VMMDEFAULT.add("Zn");      // Zn2+
    VMMDEFAULT.add("Fe");      // Fe3+ Fe2+
    VMMDEFAULT.add("O2");      // O2
    VMMDEFAULT.add("MoO4");    // Molibdato
    VMMDEFAULT.add("Ni");      // Ni2+

    this.cmodelFile = cmodelFile;

    // Obtener los identificadores de los compuestos implicados en el medio minimo
    try {
      // Cargar modelo
      cdoc = SBMLReader.read(cmodelFile);

      // Identificadores de los compuestos del MM
      mmcpdIdList = new HashSet<String>();

      // Recorrer compuestos para encontrar los del MM
      ListOf<Species> spList = cdoc.getModel().getListOfSpecies();
      for (Species sp : spList) {
        if (!sp.isBoundaryCondition()) continue;

        String fm = getFormula(sp);
        if (fm != null && VMMDEFAULT.contains(fm)) {
          // Si aparece C6H12O6 guardar la glucosa (preferible a otros con la misma formula)
          if (fm.equals("C6H12O6")) {
            if (sp.getName().toLowerCase().contains("glucose")) {
              mmcpdIdList.add(sp.getId());
            }
          }
          else {
            mmcpdIdList.add(sp.getId());
          }
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (XMLStreamException e) {
      e.printStackTrace();
    }
  }

  public void createMinMediaFile(File mmfile) {
    StringBuilder sb = new StringBuilder();

    // Recorrer las reacciones
    ListOf<Reaction> rxlist = cdoc.getModel().getListOfReactions();
    for (Reaction r : rxlist) {
      List<SpeciesReference> listCpds = new ArrayList<SpeciesReference>();
      listCpds.addAll(r.getListOfProducts());
      listCpds.addAll(r.getListOfReactants());

      checkcpds:
        for (SpeciesReference psr : listCpds) {
          // El compuesto de intercambio esta en el medio minimo
          if (psr.getSpeciesInstance().isBoundaryCondition() &&
                  mmcpdIdList.contains(psr.getSpeciesInstance().getId())) {
            sb.append(r.getId()).append(TAB).append(r.getName()).append(TAB);
            sb.append("-").append(TAB).append("-").append(TAB);
            sb.append("-").append(TAB).append("-").append(NEWLINE);

            break checkcpds;
          }
        }
    }

    // Crear fichero
    try {
      FileUtils.writeStringToFile(mmfile, sb.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void adjustMinMedia() {
    // Recorrer las reacciones
    ListOf<Reaction> rxlist = cdoc.getModel().getListOfReactions();
    for (Reaction r : rxlist) {
      boolean exchange = false;
      boolean mmedia = false;

      List<SpeciesReference> listCpds = new ArrayList<SpeciesReference>();
      listCpds.addAll(r.getListOfProducts());
      listCpds.addAll(r.getListOfReactants());

      checkcpds:
        for (SpeciesReference psr : listCpds) {
          if (psr.getSpeciesInstance().isBoundaryCondition()) {
            exchange = true;

            // El compuesto de intercambio esta en el medio minimo
            if (mmcpdIdList.contains(psr.getSpeciesInstance().getId())) {
              mmedia = true;
            }

            break checkcpds;
          }
        }

      if (exchange) {
        if (!mmedia) {
          r.getKineticLaw().getLocalParameter("LOWER_BOUND").setValue(0.0);
        }
        // Activo cuando tiene que estar activo pero no lo esta
        else if (r.getKineticLaw().getLocalParameter("LOWER_BOUND").getValue() == 0) {
          r.getKineticLaw().getLocalParameter("LOWER_BOUND").setValue(-1000);
        }
      }
    }

    // Reescribir el modelo con el medio minimo ajustado
    try {
      // TRAZA XXX
      //File f = new File(cmodelFile.getParent(), "prb_" + cmodelFile.getName());
      //SBMLWriter.write(cdoc, f, "cmodel", "1.0");

      SBMLWriter.write(cdoc, cmodelFile, "cmodel", "1.0");
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (XMLStreamException e) {
      e.printStackTrace();
    }
  }

  private String getFormula(Species sp) {
    // Recuperar la formula que aparece en las notas del SBML
    try {
		MTFM.reset(sp.getNotesString());
	} catch (XMLStreamException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    if (MTFM.find()) {
      return MTFM.group(1);
    }
    // Recuperar la formula que aparece en el nombre y lo limpio
    else if (sp.getName().contains("_")) {
      // Recuperar formula
      MTNAMEFM.reset(sp.getName());
      if (MTNAMEFM.find()) {
        return MTNAMEFM.group(2);
      }
    }

    return null;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    //File file = new File("");
    //File file = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/n_Mtuberculosis_iNJ661.xml");
    //File file = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/n_Ecoli_iJR904.xml");
    //File file = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/n_Ecoli_iAF1260.xml");
    //File file = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/n_Seed85962.1.xml");

    File file = new File("/home/pdsanchez/Proyectos/TestCModel/Prb02/n2/error/n_Opt62977.3.xml");

    DefaultMinMedia mm = new DefaultMinMedia(file);
    mm.adjustMinMedia();

    File mmfile = new File(file.getParent(), file.getName().replace(".xml", ".minmedia_ul"));
    mm.createMinMediaFile(mmfile);
  }

}
