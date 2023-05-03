/* Copyright (c) 1998-2016 ILIAS open source, Extended GPL, see docs/LICENSE */

package de.ilias.services.lucene.index.file.path;

import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.settings.ClientSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class FileObjectPathCreator7 implements PathCreator {
  private static final Logger logger = LogManager.getLogger(FileObjectPathCreator.class);

  protected String basePath = "storage";
  protected static final String BIN_NAME = "data";

  ClientSettings clientSettings;

  public FileObjectPathCreator7(ClientSettings clientSettings) {
    this.clientSettings = clientSettings;
  }

  /**
   * Get base path
   *
   * @return String basePath
   */
  public String getBasePath() {

    return this.basePath;
  }

  /**
   * @see de.ilias.services.lucene.index.file.path.PathCreator#buildFile(de.ilias.services.lucene.index.CommandQueueElement, java.sql.ResultSet)
   */
  public File buildFile(CommandQueueElement el, ResultSet res) throws PathCreatorException {

    StringBuilder fullPath = new StringBuilder();
    StringBuilder versionPath = new StringBuilder();

    File file;

    try {
      int versionCode = 1;
      int resVersion = res.getInt("version");
      if (resVersion > 0) {
        versionCode = resVersion;
      }

      fullPath.append(clientSettings.getDataDirectory().getAbsolutePath());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(clientSettings.getClient());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(getBasePath());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(res.getString("resource_path"));
      versionPath.append(fullPath);
      versionPath.append(System.getProperty("file.separator"));
      versionPath.append(versionCode);
      versionPath.append(System.getProperty("file.separator"));
      versionPath.append(BIN_NAME);

      logger.info("Detected file object path is: " + versionPath);

      file = new File(versionPath.toString());
      if (!file.exists()) {
        throw new PathCreatorException("Cannot find file: " + fullPath);
      }

      if (!file.canRead()) {
        throw new PathCreatorException("Cannot read file: " + fullPath);
      }

      return file;
    } catch (SQLException | NullPointerException e) {
      throw new PathCreatorException(e);
    }
  }

  @Override
  public String getExtension(CommandQueueElement el, ResultSet res) throws PathCreatorException {

    StringBuilder extension = new StringBuilder();
    try {
      String fileName = res.getString("file_name");
      if (null == fileName) {
        logger.error("Cannot split NULL filename for objId: " + el.getObjId());
        throw new PathCreatorException("Cannot split NULL filename for objId: " + el.getObjId());
      }
      int dotIndex = fileName.lastIndexOf(".");
      if (dotIndex > 0) {
        extension.append(fileName.substring(dotIndex + 1));
      }
      logger.info("Extraced extension: " + extension + " from file name: " + fileName);

    } catch (SQLException ex) {
      logger.error(ex.toString());
    }
    return extension.toString();
  }
}
