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

package de.ilias.services.lucene.index;

import de.ilias.services.db.DBFactory;
import de.ilias.services.lucene.settings.LuceneSettings;
import de.ilias.services.object.ObjectDefinition;
import de.ilias.services.object.ObjectDefinitions;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.LocalSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Vector;

/**
 * TODO make this class thread safe
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @author Pascal Seeland <pascal.seeland@tik.uni-stuttgart.de>
 */
public class CommandQueue {

  private static final Logger logger = LogManager.getLogger(CommandQueue.class);

  private final Connection db;
  private final Vector<CommandQueueElement> elements;
  private Iterator<CommandQueueElement> elementsIter;

  public CommandQueue() throws SQLException {

    db = DBFactory.factory();
    elements = new Vector<>();
  }

  public void setFinished(Vector<Integer> objIds) throws SQLException {
    if (!objIds.isEmpty()) {
      PreparedStatement psta = DBFactory.getPreparedStatement(
          "UPDATE search_command_queue SET finished = 1 WHERE obj_id = ?");
      for (Integer i : objIds) {
        psta.setInt(1, i);
        psta.addBatch();
      }
      psta.executeBatch();
    }
  }

  public synchronized void loadFromDb() throws SQLException {

    logger.info("Start reading command queue");

    // Substitute all reset_all commands with reset command for each undeleted object id
    substituteResetCommands();

    PreparedStatement pst = DBFactory.getPreparedStatement(
        "SELECT * FROM search_command_queue " + "WHERE finished = 0 " + "OR last_update >= ? "
            + "ORDER BY last_update ");
    pst.setTimestamp(1, new java.sql.Timestamp(LuceneSettings.getInstance().getLastIndexTime().getTime()));
    ResultSet res = pst.executeQuery();

    int waitingUpdates = elements.size();
    while (res.next()) {

      CommandQueueElement element = new CommandQueueElement();

      logger.debug("Found type: " + res.getString("obj_type") + " with id " + res.getInt("obj_id"));
      element.setObjId(res.getInt("obj_id"));
      element.setObjType(DBFactory.getString(res, "obj_type"));
      element.setCommand(CommandQueueElement.Command.valueOfLabel(DBFactory.getString(res, "command")));

      elements.add(element);
    }
    try {
      res.close();
    } catch (SQLException e) {
      logger.warn(e);
    }
    //reset the iterator
    elementsIter = elements.iterator();

    logger.info("Found " + (elements.size() - waitingUpdates) + " new update events!");
  }

  private synchronized void substituteResetCommands() throws SQLException {

    String query = "SELECT * FROM search_command_queue WHERE command = ? AND obj_id = 0";

    try {
      logger.info("Substituting reset commands");

      PreparedStatement sta = DBFactory.getPreparedStatement(query);
      DBFactory.setString(sta, 1, CommandQueueElement.Command.RESET_ALL.label);
      logger.debug("Substitution query: " + sta.toString());
      ResultSet res = sta.executeQuery();
      while (res.next()) {

        logger.info("Start substituting obj_type " + res.getString("obj_type"));
        deleteCommandsByType(DBFactory.getString(res, "obj_type"));
        addCommandsByType(DBFactory.getString(res, "obj_type"));
        deleteResetCommandByType(DBFactory.getString(res, "obj_type"));
      }
      res.close();
    } catch (SQLException e) {
      logger.error("Invalid SQL statement: " + query);
      logger.error("Cannot substitute reset commands", e);
      throw e;
    } catch (Throwable e) {
      logger.fatal("Cannot substitute reset commands", e);
    }
  }

  private synchronized void deleteResetCommandByType(String objType) throws SQLException {

    try {

      logger.info("Deleting reset command");
      PreparedStatement sta = DBFactory.getPreparedStatement(
          "DELETE FROM search_command_queue " + "WHERE obj_type = ? " + "AND obj_id = 0 ");
      DBFactory.setString(sta, 1, objType);
      sta.executeUpdate();
    } catch (SQLException e) {
      logger.error("Cannot delete reset commands!", e);
      throw e;
    }
  }

  public synchronized void deleteCommandsByType(String objType) throws SQLException {

    try {

      logger.info("Deleting old commands from command queue");
      PreparedStatement sta = DBFactory.getPreparedStatement(
          "DELETE FROM search_command_queue " + "WHERE obj_type = ? " + "AND obj_id > 0");
      DBFactory.setString(sta, 1, objType);
      sta.executeUpdate();
    } catch (SQLException e) {
      logger.fatal("Cannot delete reset commands! ", e);
      throw e;
    }
  }

  private synchronized void addCommandsByType(String objType) throws SQLException {

    try {

      int res;
      PreparedStatement sta;

      if (objType.equalsIgnoreCase("help")) {

        sta = DBFactory.getPreparedStatement(
            "INSERT INTO search_command_queue(obj_id, obj_type, sub_id, sub_type, command, last_update, finished) "
                + "SELECT help_module.lm_id, ? as obj_type ,0,'','reset',now(),0 FROM help_module");
        DBFactory.setString(sta, 1, objType);
      } else if (!objType.equalsIgnoreCase("usr")) {
        sta = DBFactory.getPreparedStatement(
            "INSERT INTO search_command_queue(obj_id, obj_type, sub_id, sub_type, command, last_update, finished)"
                + "SELECT DISTINCT(oda.obj_id),oda.type as obj_type ,0,'','reset',now(),0  FROM object_data oda JOIN object_reference ore ON oda.obj_id = ore.obj_id "
                + "WHERE (deleted IS NULL) AND type = ? " + "GROUP BY oda.obj_id");
        DBFactory.setString(sta, 1, objType);
      } else {
        sta = DBFactory.getPreparedStatement(
            "INSERT INTO search_command_queue(obj_id, obj_type, sub_id, sub_type, command, last_update, finished) "
                + "SELECT  object_data.obj_id, object_data.type as obj_type ,0,'','reset',now(),0 FROM"
                + " object_data WHERE type = ? ");
        DBFactory.setString(sta, 1, objType);
      }

      res = sta.executeUpdate();

      logger.info("Added {} new commands for object type: {}", res, objType);

    } catch (SQLException e) {

      logger.fatal("Cannot build index ", e);
      throw e;
    }
  }

  public CommandQueueElement nextElement() {
    synchronized (elements) {
      if (elementsIter.hasNext()) {
        return elementsIter.next();
      } else {
        return null;
      }
    }
  }

  public synchronized void debug(String type) throws SQLException {

    PreparedStatement resetType = DBFactory.getPreparedStatement(
        "INSERT INTO search_command_queue SET obj_id = ?,obj_type = ?, sub_id = ?, sub_type = ?, command = ?, last_update = ?, finished = ? ");
    resetType.setInt(1, 0);
    resetType.setString(2, type);
    resetType.setInt(3, 0);
    resetType.setString(4, "");
    resetType.setString(5, CommandQueueElement.Command.RESET_ALL.label);
    resetType.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
    resetType.setInt(7, 0);

    try {
      resetType.executeUpdate();
    } catch (SQLException e) {
      logger.error("Command queue update failed with message: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Delete and add all types
   */
  public synchronized void addAll() throws SQLException {

    try {

      Statement delete = db.createStatement();
      delete.executeUpdate("DELETE FROM search_command_queue");

      try {
        delete.close();
      } catch (SQLException e) {
        logger.warn(e);
      }

      ClientSettings client = ClientSettings.getInstance(LocalSettings.getClientKey());

      PreparedStatement pst = DBFactory.getPreparedStatement(
          "INSERT INTO search_command_queue (obj_id,obj_type,sub_id,sub_type,command,last_update,finished ) "
              + "VALUES ( ?, ?, ?, ?, ?, ?, ?) ");

      for (ObjectDefinition def : ObjectDefinitions.getInstance(client.getAbsolutePath()).getDefinitions()) {
        bindPreparedStatement(pst, def);
        try {
          pst.executeUpdate();
        } catch (SQLException e) {
          logger.error("Cannot add to command queue", e);
          throw e;
        }
      }
    } catch (ConfigurationException e) {
      logger.error("Cannot add to command queue", e);
    }
  }

  /**
   * Delete command queue
   */
  public synchronized void deleteAll() throws SQLException {

    logger.info("Deleting search_command_queue");
    Statement delete = db.createStatement();
    delete.execute("DELETE FROM search_command_queue");

    try {
      delete.close();
    } catch (SQLException e) {
      logger.warn(e);
      throw e;
    }
    logger.info("Search command queue deleted");
  }

  /**
   * Delete non incremental search command queue elements
   */
  public synchronized void deleteNonIncremental() throws SQLException, ConfigurationException {

    try {
      ClientSettings client = ClientSettings.getInstance(LocalSettings.getClientKey());

      PreparedStatement pst = DBFactory.getPreparedStatement(
          "DELETE FROM search_command_queue " + "WHERE obj_type = ?");
      for (ObjectDefinition def : ObjectDefinitions.getInstance(client.getAbsolutePath()).getDefinitions()) {

        if (def.getIndexType() == ObjectDefinition.INDEX_TYPE.FULL) {

          DBFactory.setString(pst, 1, def.getType());
          pst.executeUpdate();
        }
      }
    } catch (ConfigurationException | SQLException e) {
      logger.error("Error deleting from command queue", e);
      throw e;
    }

  }

  /**
   * Add non incremental search command queue elements
   */
  public synchronized void addNonIncremental() throws SQLException, ConfigurationException {

    try {

      ClientSettings client = ClientSettings.getInstance(LocalSettings.getClientKey());

      PreparedStatement pst = DBFactory.getPreparedStatement(
          "INSERT INTO search_command_queue " + "(obj_id, obj_type,sub_id,sub_type,command,last_update,finished) "
              + "VALUES (?,?,?,?,?,?,?)");
      for (ObjectDefinition def : ObjectDefinitions.getInstance(client.getAbsolutePath()).getDefinitions()) {

        if (def.getIndexType() == ObjectDefinition.INDEX_TYPE.FULL) {

          bindPreparedStatement(pst, def);
          try {
            pst.executeUpdate();
          } catch (SQLException e) {
            logger.info("Add non incremental failed failed with message: " + e.getMessage());
          }
        }
      }
    } catch (ConfigurationException | SQLException e) {
      logger.error("Error updating command queue", e);
      throw e;
    }
  }

  private void bindPreparedStatement(PreparedStatement pst, ObjectDefinition def) throws SQLException {
    logger.info("Adding reset command for " + def.getType());
    pst.setInt(1, 0);
    pst.setString(2, def.getType());
    pst.setInt(3, 0);
    pst.setString(4, "");
    pst.setString(5, CommandQueueElement.Command.RESET_ALL.label);
    pst.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
    pst.setInt(7, 0);
  }

}
