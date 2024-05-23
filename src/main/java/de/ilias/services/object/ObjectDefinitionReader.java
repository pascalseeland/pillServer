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

import de.ilias.services.settings.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class ObjectDefinitionReader {

  private static final Logger logger = LogManager.getLogger(ObjectDefinitionReader.class);
  private static final HashMap<File, ObjectDefinitionReader> instances = new HashMap<>();

  public static final String objectPropertyName = "LuceneObjectDefinition.xml";
  public static final String pluginPath = "Customizing/global/plugins";

  private final Vector<File> objectPropertyFiles = new Vector<>();

  private final File absolutePath;

  private ObjectDefinitionReader(File absolutePath) throws ConfigurationException {
    this.absolutePath = absolutePath;
    read();
  }

  public static ObjectDefinitionReader getInstance(File absolutePath) throws ConfigurationException {

    if (instances.containsKey(absolutePath)) {
      logger.debug("Using cached properties.");
      return instances.get(absolutePath);
    }
    instances.put(absolutePath, new ObjectDefinitionReader(absolutePath));
    return instances.get(absolutePath);
  }

  /**
   * @return the objectPropertyFiles
   */
  public Vector<File> getObjectPropertyFiles() {
    return objectPropertyFiles;
  }

  private void read() throws ConfigurationException {

    logger.debug("Start reading search index definitions...");
    if (!absolutePath.isDirectory()) {
      throw new ConfigurationException("Absolute path required. Path: " + absolutePath.getAbsolutePath());
    }

    // Traverse through Modules
    File start = new File(absolutePath.getAbsoluteFile() + FileSystems.getDefault().getSeparator() + "Modules");
    logger.debug("Start path is : " + start.getAbsoluteFile());
    traverse(start);

    // Traverse through Modules
    File services = new File(absolutePath.getAbsoluteFile() + FileSystems.getDefault().getSeparator() + "Services");
    logger.debug("Start path is : " + start.getAbsoluteFile());
    traverse(services);

    // Traverse through Plugins
    File plugin = new File(
        absolutePath.getAbsoluteFile() + FileSystems.getDefault().getSeparator() + ObjectDefinitionReader.pluginPath);
    logger.debug("Start path is : " + plugin.getAbsoluteFile());
    traverse(plugin);
  }

  private void traverse(File dir) {

    if (dir == null) {
      return;
    }
    logger.debug("Start path is : " + dir.getAbsoluteFile());

    File[] entries = dir.listFiles(path -> {

      if (path.isDirectory()) {
        //logger.debugQ"Found new directory: " + path.getAbsolutePath());
        return !path.getName().equals(".git");
      }
      //logger.debug(path.getName() + " <-> " + objectPropertyName);
      if (path.getName().equalsIgnoreCase(objectPropertyName)) {
        logger.info("Found: " + path.getAbsolutePath());
        objectPropertyFiles.add(path);
      }
      return false;
    });

    if (entries == null) {
      return;
    }
    for (File entry : entries) {
      // there are only directories
      traverse(entry);
    }
  }

}
