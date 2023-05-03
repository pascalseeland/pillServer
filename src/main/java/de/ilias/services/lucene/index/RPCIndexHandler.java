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

import de.ilias.ILServerStatus;
import de.ilias.services.db.DBFactory;
import de.ilias.services.object.ObjectDefinition;
import de.ilias.services.object.ObjectDefinitionParser;
import de.ilias.services.object.ObjectDefinitionReader;
import de.ilias.services.object.ObjectDefinitions;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.ServerSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
@ApplicationScoped
public class RPCIndexHandler {

  @Inject
  DBFactory dbFactory;
  @Inject
  IndexHolder indexHolder;
  @Inject
  ClientSettings clientSettings;
  @Inject
  ObjectDefinitions objectDefinitions;
  @Inject
  CommandController commandController;

  private ObjectDefinitionReader objectDefinitionReader;

  private static Logger logger = LogManager.getLogger(RPCIndexHandler.class);

  @PostConstruct
  public void init() throws ConfigurationException {
    this.objectDefinitionReader = new ObjectDefinitionReader(clientSettings.getAbsolutePath());
  }

  /**
   * Update index for a vector of obj ids
   */
  public boolean indexObjects(String clientKey, Vector<Integer> objIds) {

    // Set client key
    ServerSettings server;
    ObjectDefinitionParser parser;

    try {
      long s_start = new java.util.Date().getTime();

      logger.info("Checking if indexer is running for client: " + clientKey);
      // Return if indexer is already running for this clientKey
      if (ILServerStatus.isIndexerActive(clientKey)) {
        logger.error("An Indexer is already running for this client. Aborting!");
        return false;
      }

      // Set status
      //ilServerStatus.addIndexer(clientKey);

      server = ServerSettings.getInstance();
      parser = new ObjectDefinitionParser(objectDefinitionReader.getObjectPropertyFiles(), dbFactory, clientSettings);
      parser.parse();

      objectDefinitions.reset();
      for (ObjectDefinition def : parser.getDefinitions()) {
        objectDefinitions.addDefinition(def);
      }
      commandController.initObjects(objIds);

      // Start threads
      Vector<CommandControllerThread> threads = new Vector<>();
      for (int i = 0; i < server.getNumThreads(); i++) {

        CommandControllerThread t = new CommandControllerThread(clientKey, commandController, dbFactory);
        t.start();
        threads.add(t);
      }
      // Join threads
      for (int i = 0; i < server.getNumThreads(); i++) {
        threads.get(i).join();
      }
      commandController.writeToIndex();

      long s_end = new java.util.Date().getTime();
      logger.info("IndexController time: " + ((s_end - s_start) / (1000)) + " seconds");
      logger.debug(clientSettings.getIndexPath());
      return true;

    } catch (Exception e) {
      logger.error("Unknown error", e);
    } finally {
      // Purge resources
      dbFactory.closeAll();
    }

    return false;
  }

  /**
   * Refresh index
   */
  public boolean index(boolean incremental) {

    ServerSettings server;

    try {
      long s_start = new java.util.Date().getTime();

      server = ServerSettings.getInstance();

      if (!incremental) {
        this.indexHolder.deleteIndex();
      }

      ObjectDefinitionParser parser = new ObjectDefinitionParser(objectDefinitionReader.getObjectPropertyFiles(), dbFactory, clientSettings);
      parser.parse();
      objectDefinitions.reset();
      for (ObjectDefinition def : parser.getDefinitions()) {
        objectDefinitions.addDefinition(def);
      }

      if (incremental) {
        commandController.initRefresh();
      } else {
        commandController.initCreate();
      }
      // Start threads
      Vector<CommandControllerThread> threads = new Vector<>();
      for (int i = 0; i < server.getNumThreads(); i++) {

        CommandControllerThread t = new CommandControllerThread("clientkey", commandController, dbFactory);
        t.start();
        threads.add(t);
      }
      // Join threads
      for (int i = 0; i < server.getNumThreads(); i++) {
        threads.get(i).join();
      }
      commandController.writeToIndex();

      long s_end = new java.util.Date().getTime();
      logger.info("IndexController time: " + ((s_end - s_start) / (1000)) + " seconds");
      logger.debug(clientSettings.getIndexPath());
      return true;

    } catch (Exception e) {
      logger.error("Unknown error", e);
    } finally {
      dbFactory.closeAll();
    }

    return false;
  }

}
