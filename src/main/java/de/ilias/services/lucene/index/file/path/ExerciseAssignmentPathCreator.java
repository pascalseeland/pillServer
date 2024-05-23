package de.ilias.services.lucene.index.file.path;

import de.ilias.services.db.DBFactory;
import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.LocalSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.FileSystems;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Creates the filesystem path to exercise assignments.
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */

public class ExerciseAssignmentPathCreator implements PathCreator {

  private static final Logger logger = LogManager.getLogger(ExerciseAssignmentPathCreator.class);

  public File buildFile(CommandQueueElement el, ResultSet res) throws PathCreatorException {

    int objId = el.getObjId();
    StringBuilder fullPath = new StringBuilder();

    File file;

    try {

      fullPath.append(ClientSettings.getInstance(LocalSettings.getClientKey()).getDataDirectory().getAbsolutePath());
      fullPath.append(FileSystems.getDefault().getSeparator());
      fullPath.append(ClientSettings.getInstance(LocalSettings.getClientKey()).getClient());
      fullPath.append(FileSystems.getDefault().getSeparator());
      fullPath.append("ilExercise");
      fullPath.append(FileSystems.getDefault().getSeparator());
      fullPath.append(PathUtils.buildSplittedPathFromId(objId, "exc"));
      fullPath.append("ass_").append(DBFactory.getInt(res, "id"));

      logger.info("Try to read from path: " + fullPath);

      file = new File(fullPath.toString());
      if (file.exists() && file.canRead()) {
        return file;
      }
      throw new PathCreatorException("Cannot access directory: " + fullPath);
    } catch (ConfigurationException | SQLException | NullPointerException e) {
      throw new PathCreatorException(e);
    }
  }

  @Override
  public String getExtension(CommandQueueElement el, ResultSet res) {
    return "";
  }

}
