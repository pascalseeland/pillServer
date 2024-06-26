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
import de.ilias.services.lucene.index.file.path.PathCreatorException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.ResultSet;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class DirectoryDataSource extends FSDataSource {

  private static final Logger logger = LogManager.getLogger(DirectoryDataSource.class);

  public DirectoryDataSource(TYPE type) {
    super(type);
  }

  /**
   * write Document
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh, ResultSet res) throws DocumentHandlerException {

    File start;
    ExtensionFileHandler handler = new ExtensionFileHandler();
    StringBuilder content = new StringBuilder();

    Vector<File> files;

    logger.info("Start scanning directory...");

    try {
      if (getPathCreator() == null) {
        logger.info("No path creator defined");
        return;
      }
      start = getPathCreator().buildFile(el, res);

      FileReader reader = new FileReader();
      reader.traverse(start);
      files = reader.getFiles();

      logger.info("Found " + files.size() + " new files.");

      for (File file : files) {
        // Analyze encoding (transfer encoding), parse file extension
        // and finally read content
        try {
          content.append(" ").append(handler.getContent(file, ""));
        } catch (FileHandlerException e) {
          logger.warn("Cannot parse file " + file.getAbsolutePath());
        }
      }

      // Write content
      for (FieldDefinition field : getFields()) {
        field.writeDocument(content.toString(), dh);
      }
      logger.debug("Content is : " + content);
    } catch (PathCreatorException e) {
      throw new DocumentHandlerException(e);
    }
  }

  /**
   * Read all files in a directory
   */
  static class FileReader {
    private final Vector<File> files = new Vector<>();

    public Vector<File> getFiles() {
      return files;
    }

    public void traverse(File dir) {

      File[] entries = dir.listFiles(path -> {

        if (path.isDirectory()) {
          return !path.getName().equals(".svn");
        } else {
          //getCandidates().add(path);
          files.add(path);
          return false;
        }
      });

      if (entries != null) {
        for (File entry : entries) {
          // there are only directories
          traverse(entry);
        }
      }
    }
  }
}
