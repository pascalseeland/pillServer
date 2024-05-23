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
import de.ilias.services.lucene.index.DocumentExtractor;
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;
import de.ilias.services.lucene.index.file.ExtensionFileHandler;
import de.ilias.services.lucene.index.file.FileHandlerException;
import de.ilias.services.lucene.index.file.path.PathCreatorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.ResultSet;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class FileDataSource extends FSDataSource {

  private static final Logger logger = LogManager.getLogger(FileDataSource.class);


  public FileDataSource(TYPE type) {
    super(type);
  }

  /**
   * @see DocumentExtractor#extractDocument(CommandQueueElement, DocumentHolder, ResultSet)
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh, ResultSet res) throws DocumentHandlerException {

    File file = null;
    ExtensionFileHandler handler = new ExtensionFileHandler();

    try {
      if (getPathCreator() == null) {
        logger.info("No path creator defined");
        return;
      }
      file = getPathCreator().buildFile(el, res);
      String extension = getPathCreator().getExtension(el, res);

      // Analyze encoding (transfer encoding), parse file extension and finally read content
      for (FieldDefinition field : getFields()) {
        field.writeDocument(handler.getContent(file, extension), dh);
      }
      logger.debug("File path is: " + file.getAbsolutePath());
    } catch (PathCreatorException | FileHandlerException e) {
      logger.info("Current Files is: " + file.getAbsolutePath());
      throw new DocumentHandlerException(e);
    }
  }
}
