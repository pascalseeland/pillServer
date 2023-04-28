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
import de.ilias.services.object.ObjectDefinitionParser;
import de.ilias.services.object.ObjectDefinitionReader;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.LocalSettings;
import de.ilias.services.settings.ServerSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

import javax.inject.Inject;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class RPCIndexHandler {

  @Inject
  private DBFactory dbFactory;

  private static Logger logger = LogManager.getLogger(RPCIndexHandler.class);

  /**
   * Update index for a vector of obj ids
   */
  public boolean indexObjects(String clientKey, Vector<Integer> objIds) {

    // Set client key
    LocalSettings.setClientKey(clientKey);
    ClientSettings client;
    ServerSettings server;
    ObjectDefinitionReader properties;
    ObjectDefinitionParser parser;

    CommandController controller;

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

      client = ClientSettings.getInstance(LocalSettings.getClientKey());
      server = ServerSettings.getInstance();

      properties = ObjectDefinitionReader.getInstance(client.getAbsolutePath());
      parser = new ObjectDefinitionParser(properties.getObjectPropertyFiles());
      parser.parse();

      //controller = CommandController.getInstance();
      controller = new CommandController();
      controller.initObjects(objIds);

      // Start threads
      Vector<CommandControllerThread> threads = new Vector<CommandControllerThread>();
      for (int i = 0; i < server.getNumThreads(); i++) {

        CommandControllerThread t = new CommandControllerThread(clientKey, controller);
        t.start();
        threads.add(t);
      }
      // Join threads
      for (int i = 0; i < server.getNumThreads(); i++) {
        threads.get(i).join();
      }
      controller.writeToIndex();
      controller.closeIndex();

      long s_end = new java.util.Date().getTime();
      logger.info("Index time: " + ((s_end - s_start) / (1000)) + " seconds");
      logger.debug(client.getIndexPath());
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
  public boolean index(String clientKey, boolean incremental) {

    Boolean doPurge = true;

    // Set client key
    LocalSettings.setClientKey(clientKey);
    ClientSettings client;
    ServerSettings server;
    ObjectDefinitionReader properties;
    ObjectDefinitionParser parser;

    CommandController controller;

    try {
      long s_start = new java.util.Date().getTime();

      logger.info("Checking if indexer is running for client: " + clientKey);
      // Return if indexer is already running for this clientKey
      if (ILServerStatus.isIndexerActive(clientKey)) {
        logger.error("An Indexer is already running for this client. Aborting!");
        doPurge = false;
        return false;
      }

      // Set status
      ILServerStatus.addIndexer(clientKey);

      client = ClientSettings.getInstance(LocalSettings.getClientKey());
      server = ServerSettings.getInstance();

      if (!incremental) {
        IndexHolder.deleteIndex();
      }

      properties = ObjectDefinitionReader.getInstance(client.getAbsolutePath());
      parser = new ObjectDefinitionParser(properties.getObjectPropertyFiles());
      parser.parse();

      //controller = CommandController.getInstance();
      controller = new CommandController();
      if (incremental) {
        controller.initRefresh();
      } else {
        controller.initCreate();
      }
      // Start threads
      Vector<CommandControllerThread> threads = new Vector<CommandControllerThread>();
      for (int i = 0; i < server.getNumThreads(); i++) {

        CommandControllerThread t = new CommandControllerThread(clientKey, controller);
        t.start();
        threads.add(t);
      }
      // Join threads
      for (int i = 0; i < server.getNumThreads(); i++) {
        threads.get(i).join();
      }
      controller.writeToIndex();
      controller.closeIndex();

      long s_end = new java.util.Date().getTime();
      logger.info("Index time: " + ((s_end - s_start) / (1000)) + " seconds");
      logger.debug(client.getIndexPath());
      return true;

    } catch (Exception e) {
      logger.error("Unknown error", e);
    } finally {
      // Purge resources
      if (doPurge) {
        ILServerStatus.removeIndexer(clientKey);
        dbFactory.closeAll();
      }
    }

    return false;
  }

}
