package de.ilias.services.lucene.index.file.path;

import de.ilias.services.db.DBFactory;
import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.settings.ClientSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Creates the filesystem path to exercise assignments.
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
@ApplicationScoped
public class ExerciseAssignmentPathCreator implements PathCreator {

  private Logger logger = LogManager.getLogger(ExerciseAssignmentPathCreator.class);

  @Inject
  DBFactory dbFactory;
  @Inject
  ClientSettings clientSettings;

  public File buildFile(CommandQueueElement el, ResultSet res) throws PathCreatorException {

    int objId = el.getObjId();
    StringBuilder fullPath = new StringBuilder();

    File file;

    try {

      fullPath.append(clientSettings.getDataDirectory().getAbsolutePath());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(clientSettings.getClient());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append("ilExercise");
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(PathUtils.buildSplittedPathFromId(objId, "exc"));
      fullPath.append("ass_").append(this.dbFactory.getInt(res, "id"));

      logger.info("Try to read from path: " + fullPath.toString());

      file = new File(fullPath.toString());
      if (file.exists() && file.canRead()) {
        return file;
      }
      throw new PathCreatorException("Cannot access directory: " + fullPath.toString());
    } catch (SQLException | NullPointerException e) {
      throw new PathCreatorException(e);
    }
  }

  @Override
  public String getExtension(CommandQueueElement el, ResultSet res) {
    return "";
  }

}
