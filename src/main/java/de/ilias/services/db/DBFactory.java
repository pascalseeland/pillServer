/*
        +-----------------------------------------------------------------------------+
        | ILIAS open source                                                           |
        +-----------------------------------------------------------------------------+
        | Copyright (c) 1998-2001 ILIAS open source, University of Cologne            |
        |                                                                             |
        | This program is free software; you can redistribute it and/or               |
        | modify it under the terms of the GNU General Public License                 |
        | as published by the Free Software Foundation; either version 2              |
        | of the License, or (at your option) any later version.                      |
        |                                                                             |
        | This program is distributed in the hope that it will be useful,             |
        | but WITHOUT ANY WARRANTY; without even the implied warranty of              |
        | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               |
        | GNU General Public License for more details.                                |
        |                                                                             |
        | You should have received a copy of the GNU General Public License           |
        | along with this program; if not, write to the Free Software                 |
        | Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. |
        +-----------------------------------------------------------------------------+
*/

package de.ilias.services.db;

import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.LocalSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * A thread local singleton for db connections
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class DBFactory {

  private static final Logger logger = LogManager.getLogger(DBFactory.class);

  private static final String MARIA_DB_CONNECTOR = "jdbc:mariadb://";

  private static final ThreadLocal<HashMap<String, PreparedStatement>> ps = ThreadLocal.withInitial(HashMap::new);

  private static final ThreadLocal<Connection> connection = ThreadLocal.withInitial(() -> {
    try {
      ClientSettings client = ClientSettings.getInstance(LocalSettings.getClientKey());

      logger.info("+++++++++++++++++++++++++++++++++++++++++++ New Thread local " + LocalSettings.getClientKey());

      // MySQL
      if (client.getDbType().equalsIgnoreCase("mysql")) {

        logger.info("Loading maria db driver...");
        logger.info("Using jdbc url: " + client.getDbUrl() + "/" + client.getDbUser() + "/" + "******"
            + "?autoReconnect=true");

        return DriverManager.getConnection(DBFactory.MARIA_DB_CONNECTOR + client.getDbUrl() + "?autoReconnect=true",
            client.getDbUser(), client.getDbPass());
      } else {
        logger.error("Unsupported db type given." + client.getDbType());
        throw new ConfigurationException("Unsupported db type given." + client.getDbType());
      }
    } catch (SQLException | ConfigurationException e) {
      logger.error("Cannot connect to database: " + e);
    }
    return null;
  });

  /**
   * get singleton db connection for each url
   */
  public static Connection factory() throws SQLException {

    logger.debug("====================================== Used cached DB connector.");
    if (!connection.get().isValid(10)) {
      init();
    }
    return connection.get();
  }

  public static void init() {

    logger.debug("------------------------------------- Destroying cached DB connector.");
    connection.remove();
    ps.remove();
  }

  /**
   * get prepared statement
   */
  public static PreparedStatement getPreparedStatement(String query) throws SQLException {

    if (ps.get().containsKey(query)) {

      logger.trace("Reusing prepared statement: " + query);
      return ps.get().get(query);
    }

    // Create new Prepared statement
    logger.trace("Creating new prepared statement: " + query);
    ps.get().put(query, DBFactory.factory().prepareStatement(query));
    return ps.get().get(query);
  }

  /**
   * Close prepared statement
   */
  public static void closePreparedStatement(String query) {

    try {
      if (ps.get().containsKey(query)) {
        ps.get().get(query).close();
      }
    } catch (SQLException e) {
      logger.warn("Cannot close prepared statement: " + query, e);
    } finally {
      ps.get().remove(query);
    }
  }

  /**
   * close all statements
   */
  public static void closeAll() {

    try {

      for (PreparedStatement pst : ps.get().values()) {

        // closing prepared statements
        logger.debug("Clossing prepared statement: " + pst.toString());
        try {
          // Close prepared statements
          pst.close();
          // Close connection
        } catch (SQLException e) {
          logger.warn("Cannot close prepared statement: " + pst);
          logger.warn(e);
        }
      }

    } finally {

      try {
        connection.get().close();
      } catch (Throwable e) {
        logger.error("Cannot release db connection: ", e);
      }
    }
  }

  /**
   * set string overwritten for oracle
   */
  public static void setString(PreparedStatement ps, int index, String str) throws SQLException {

    ClientSettings client;
    try {

      client = ClientSettings.getInstance(LocalSettings.getClientKey());
      if (client.getDbType().equals("mysql")) {

        ps.setString(index, str);
      }
    } catch (ConfigurationException e) {
      // shouldn't happen here
      logger.error(e);
    }
  }

  /**
   * get string overwritten for oracle
   */
  public static String getString(ResultSet res, String name) throws SQLException {

    ClientSettings client;
    try {

      client = ClientSettings.getInstance(LocalSettings.getClientKey());
      if (client.getDbType().equals("mysql")) {

        return res.getString(name);
      }
    } catch (ConfigurationException e) {
      // shouldn't happen here
      logger.error(e);
    }
    return "";
  }

  /**
   * Get clob value
   */
  public static String getCLOB(ResultSet res, String name) throws SQLException {
    return DBFactory.getString(res, name);
  }

  /**
   * Get integer value and parse it to int
   */
  public static String getInt(ResultSet res, String name) throws SQLException {
    return String.valueOf(res.getInt(name));
  }

}
