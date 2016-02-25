package es.csic.cnb.server;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import es.csic.cnb.db.DbManager;
import es.csic.cnb.log.LogMgr;

/**
 * <p>Start scheduler automatically when the application is launched
 * <p>This code opens the database.
 *
 * @author Pablo D. Sánchez
 *
 */
public class DbStarter implements ServletContextListener {
	
	private static Logger log = Logger.getLogger(DbStarter.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Log
        new LogMgr().configureLog();

        DbManager.INSTANCE.connect();

        log.info("CMODEL > open db");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    	// Cerrar conexiones con la base de datos
        DbManager.INSTANCE.closeDbConections();
        // Cerrar el servidor de la base de datos
        DbManager.INSTANCE.shutdown();

        log.info("CMODEL > stop db");
    }
}
