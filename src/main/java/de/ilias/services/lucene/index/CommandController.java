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

import de.ilias.services.lucene.settings.LuceneSettings;
import de.ilias.services.object.ObjectDefinition;
import de.ilias.services.object.ObjectDefinitionException;
import de.ilias.services.object.ObjectDefinitions;
import de.ilias.services.settings.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Handles command queue events
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @author Pascal Seeland <pascal.seeland@tik.uni-stuttgart.de>
 * @version $Id$
 */
@ApplicationScoped
public class CommandController {

  private static final int MAX_ELEMENTS = 100;

  private static Logger logger = LogManager.getLogger(CommandController.class);

  @Inject
  CommandQueue queue;

  @Inject
  ObjectDefinitions objDefinitions;

  @Inject
  IndexHolder holder;

  @Inject
  LuceneSettings luceneSettings;

  /**
   * Init command queue for new index
   */
  public void initCreate() throws SQLException {
    queue.deleteAll();
    queue.addAll();
    queue.loadFromDb();
  }

  public void initRefresh() throws SQLException, ConfigurationException {

    queue.deleteNonIncremental();
    queue.addNonIncremental();
    queue.loadFromDb();
  }

  /**
   * Load queue elements from given obj ids list
   */
  public void initObjects(Vector<Integer> objIds) throws SQLException {

    queue.loadFromObjectList(objIds);
  }

  /**
   * handle command queue.
   */
  public void start() {

    CommandQueueElement currentElement = null;
    Vector<Integer> finished = new Vector<>();
    try {
      while ((currentElement = queue.nextElement()) != null) {

        logger.info("Current element id: " + currentElement.getObjId() + " " + currentElement.getObjType());
        String command = currentElement.getCommand();

        logger.debug("Handling command: " + command + "!");

        switch (command) {
          case "reset":
          case "update":

            // content changed

            // Delete document
            deleteDocument(currentElement);
            try {
              addDocument(currentElement);
            } catch (ObjectDefinitionException e) {
              logger.warn("Ignoring deprecated object type " + currentElement.getObjType());
              getQueue().deleteCommandsByType(currentElement.getObjType());
            }
            break;
          case "create":

            // Create a new document
            // Called for new objects or objects restored from trash
            try {
              addDocument(currentElement);
            } catch (ObjectDefinitionException e) {
              logger.warn("Ignoring deprecated object type " + currentElement.getObjType());
              getQueue().deleteCommandsByType(currentElement.getObjType());
            }
            break;
          case "delete":
            // only delete it
            deleteDocument(currentElement);
            break;
        }
        finished.add(currentElement.getObjId());

        // Update command queue if MAX ELEMENTS is reached.
        if (finished.size() > MAX_ELEMENTS) {
          logger.debug("Finished queue is full, setting elements finished");
          queue.setFinished(finished);
          finished.clear();
          logger.debug("Finished queue cleared");
        }
      }
      queue.setFinished(finished);
      finished.clear();
    } catch (SQLException | IOException e) {
      logger.error(e);
    }

  }

  public synchronized void writeToIndex() {

    try {
      logger.info("Writer commitAndMerge.");
      holder.commitAndForceMerge();
      logger.info("Writer commited and forced merge");
      this.luceneSettings.writeLastIndexTime();
    } catch (IOException e) {
      logger.fatal("IndexController Corrupted. Aborting!" + e, e);
    } catch (SQLException e) {
      logger.error("Cannot update search_command_queue: " + e, e);
    }
  }

  private void addDocument(CommandQueueElement el) throws CorruptIndexException, ObjectDefinitionException {

    ObjectDefinition definition;

    try {
      logger.debug("Adding new document!");
      definition = objDefinitions.getDefinitionByType(el.getObjType());
      List<DocumentHolder> docs = definition.createDocument(el);
      logger.debug("{} generated {} documents:", el, docs.size());
      for (DocumentHolder doc: docs) {
        holder.addDocument(doc.getGlobalDocument());
        holder.addDocument(doc.getDocument());
      }
    } catch (DocumentHandlerException | IOException e) {
      logger.warn(e);
    }
  }

  private void deleteDocument(CommandQueueElement el) throws IOException {

    logger.debug("Deleteing document with objId: " + el.getObjId());
    holder.deleteDocument(String.valueOf(el.getObjId()));
  }

  /**
   * @return the queue
   */
  public CommandQueue getQueue() {
    return queue;
  }

}
