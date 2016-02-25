package es.csic.cnb.server;

import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;

/**
 * <p>Start scheduler automatically on tomcat startup.
 * <p>This code open database.
 * <p>Deprecated. Use DbStarter
 *
 * @author Pablo D. Sánchez
 *
 */
@Deprecated
public class ServletInitializer extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static Logger log = Logger.getLogger(ServletInitializer.class.getName());

  public void init() throws ServletException {
    super.init();

    // Log
    new LogMgr().configureLog();

    DbManager.INSTANCE.connect();

    log.info("CMODEL > open db");
  }

  public void destroy() {
    super.destroy();

    // Cerrar conexiones con la base de datos
    DbManager.INSTANCE.closeDbConections();
    // Cerrar el servidor de la base de datos
    DbManager.INSTANCE.shutdown();

    log.info("CMODEL > stop db");
  }
}
