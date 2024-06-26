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

import de.ilias.services.db.DBFactory;
import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.lucene.index.DocumentHandlerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class ParameterDefinition {

  public static final int FORMAT_LIST = 1;

  public static final int TYPE_INT = 1;
  public static final int TYPE_STRING = 2;

  private static final Logger logger = LogManager.getLogger(ParameterDefinition.class);

  private int format;
  private int type;
  private final String value;

  public ParameterDefinition(String format, String type, String value) {

    if (format.equals("format")) {
      this.format = FORMAT_LIST;
    }
    if (type.equals("int")) {
      this.type = TYPE_INT;
    }
    if (type.equals("string")) {
      this.type = TYPE_STRING;
    }
    this.value = value;
  }

  /**
   * @return the type
   */
  public int getType() {
    return type;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "Parameter " + format + " " + type + " " + value + "\n";
  }

  public void writeParameter(PreparedStatement pst, int index, CommandQueueElement el, ResultSet parentResult)
          throws SQLException, DocumentHandlerException {

    switch (getType()) {
      case TYPE_INT:
        logger.trace("ID: " + getParameterValue(el, parentResult));
        pst.setInt(index, getParameterValue(el, parentResult));
        break;

      case TYPE_STRING:
        logger.trace("ID: " + getParameterValue(el, parentResult));
        pst.setString(index, getParameterString(el, parentResult));
        break;

      default:
        throw new DocumentHandlerException("Invalid parameter type given. Type " + getType());
    }
  }

  private int getParameterValue(CommandQueueElement el, ResultSet parentResult) {

    // Check for parent result (e.g. pg,st)
    if (parentResult != null) {

      logger.trace("Trying to read parameter from parent result set...");
      try {
        logger.trace(parentResult.getInt(getValue()));
        return parentResult.getInt(getValue());
      } catch (SQLException e) {
        // ignoring this error
        // and trying to fetch objId and metaObjId
      }

    }

    if (getValue().equals("objId")) {
      logger.trace(el.getObjId());
      return el.getObjId();
    }

    if (getValue().equals("metaObjId")) {
      logger.trace(el.getObjId());
      return el.getObjId();
    }

    if (getValue().equals("metaRbacId")) {
      logger.trace(el.getObjId());
      return el.getObjId();
    }

    return 0;
  }

  private String getParameterString(CommandQueueElement el, ResultSet parentResult) {

    if (parentResult != null) {
      logger.debug("Trying to read parameter from parent result set...");

      try {
        logger.debug(parentResult.getString(getValue()).trim());
        return DBFactory.getString(parentResult, getValue());
      } catch (SQLException e) {
        // ignoring this error
        // and trying to fetch objId and metaObjId
      }
    }

    if (getValue().equals("objType")) {
      logger.trace(el.getObjType());
      return el.getObjType();
    }

    if (getValue().equals("metaType")) {
      logger.trace(el.getObjType());
      return el.getObjType();
    }
    return "";
  }

}
