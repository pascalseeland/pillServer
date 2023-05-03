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

package de.ilias.services.object;

import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;
import de.ilias.services.lucene.index.file.ExtensionFileHandler;
import de.ilias.services.lucene.index.file.FileHandlerException;
import de.ilias.services.lucene.index.file.path.PathCreator;
import de.ilias.services.lucene.index.file.path.PathCreatorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class FileDataSource extends DataSource {

  private static Logger logger = LogManager.getLogger(FileDataSource.class);

  private PathCreator pathCreator = null;

  public FileDataSource(int type) {

    super(type);
  }

  /**
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement, java.sql.ResultSet)
   */
  public List<DocumentHolder> createDocument(CommandQueueElement el, ResultSet res) throws DocumentHandlerException {

    File file = null;
    ExtensionFileHandler handler = new ExtensionFileHandler();

    try {
      if (pathCreator == null) {
        logger.info("No path creator defined");
        return Collections.emptyList();
      }
      file = pathCreator.buildFile(el, res);
      String extension = pathCreator.getExtension(el, res);
      DocumentHolder documentHolder = new DocumentHolder();
      // Analyze encoding (transfer encoding), parse file extension and finally read content
      for (FieldDefinition field : fields) {
        field.writeDocument(documentHolder, handler.getContent(file, extension));
      }
      logger.debug("File path is: " + file.getAbsolutePath());
      List<DocumentHolder> documentHolders = new LinkedList<>();
      documentHolders.add(documentHolder);
      return documentHolders;
    } catch (PathCreatorException e) {
      if (file != null) {
        logger.info("Current Files is: " + file.getAbsolutePath());
      }
      throw new DocumentHandlerException(e);
    } catch (FileHandlerException e) {
      logger.info("Current Files is: " + file.getAbsolutePath());
      throw new DocumentHandlerException(e);
    }
  }

  /**
   * @param pathCreator the pathCreator to set
   */
  public void setPathCreator(PathCreator pathCreator) {
    this.pathCreator = pathCreator;
  }

  /**
   * @return the pathCreator
   */
  public PathCreator getPathCreator() {
    return pathCreator;
  }

  /**
   * @see de.ilias.services.object.DataSource#createDocument(de.ilias.services.lucene.index.CommandQueueElement)
   */
  @Override
  public List<DocumentHolder> createDocument(CommandQueueElement el) throws DocumentHandlerException {

    return createDocument(el, null);
  }
}
