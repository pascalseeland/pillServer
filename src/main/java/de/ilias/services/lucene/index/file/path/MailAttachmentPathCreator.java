/*
 *
 */
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
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
@ApplicationScoped
public class MailAttachmentPathCreator implements PathCreator {

  public static Logger logger = LogManager.getLogger(MailAttachmentPathCreator.class);

  @Inject
  DBFactory dbFactory;

  @Inject
  ClientSettings clientSettings;

  public File buildFile(CommandQueueElement el, ResultSet res) throws PathCreatorException {

    StringBuilder fullPath = new StringBuilder();

    File file;

    try {
      fullPath.append(clientSettings.getDataDirectory().getAbsolutePath());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append(clientSettings.getClient());
      fullPath.append(System.getProperty("file.separator"));
      fullPath.append("mail");
      fullPath.append(System.getProperty("file.separator"));

      fullPath.append(this.dbFactory.getString(res, "path"));

      logger.info("Try to read from path: " + fullPath);

      file = new File(fullPath.toString());
      if (file.exists() && file.canRead()) {
        return file;
      }
      throw new PathCreatorException("Cannot access directory: " + fullPath);
    } catch (SQLException | NullPointerException e) {
      throw new PathCreatorException(e);
    }

  }

  @Override
  public String getExtension(CommandQueueElement el, ResultSet res) {
    return "";
  }

}
