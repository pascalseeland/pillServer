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
import de.ilias.services.lucene.index.DocumentHolder;
import de.ilias.services.lucene.index.transform.ContentTransformer;
import de.ilias.services.lucene.index.transform.TransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Field;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class FieldDefinition {

  private static final Logger logger = LogManager.getLogger(FieldDefinition.class);

  /**
   * Possible index types
   */
  public static final int INDEX_NO = 0;
  public static final int INDEX_ANALYZED = 1;
  public static final int INDEX_NOT_ANALYZED = 2;

  /**
   * Is stored
   */
  private Field.Store store = Field.Store.NO;

  /**
   * Index field type
   */
  public int indexType = FieldDefinition.INDEX_NO;

  /**
   * Name if field
   */
  private final String name;

  /**
   * Column name
   */
  private final String column;

  /**
   * Type
   */
  private String type = "text";

  /**
   * Is global document type
   */
  private boolean global = false;

  /**
   * Is dynamic
   */
  private boolean isDynamic = false;

  private final Vector<TransformerDefinition> transformers = new Vector<>();

  public FieldDefinition(String store, String index, String name, String column, String type, String isGlobal,
      String dynamicName) {

    if (store.equalsIgnoreCase("YES")) {
      this.store = Field.Store.YES;
    }

    if (index.equalsIgnoreCase("NO")) {
      this.indexType = INDEX_NO;
    } else if (index.equalsIgnoreCase("ANALYZED")) {
      this.indexType = INDEX_ANALYZED;
    } else if (index.equalsIgnoreCase("NOT_ANALYZED")) {
      this.indexType = INDEX_NOT_ANALYZED;
    }
    if (isGlobal == null || isGlobal.equalsIgnoreCase("YES")) {
      this.global = true;
    }

    if (dynamicName != null) {
      this.name = dynamicName;
      this.isDynamic = true;
    } else {
      this.name = name;
    }

    this.column = column;

    if (type != null && !type.isEmpty()) {
      this.type = type;
    }
  }

  /**
   * @return String name of lucene field
   */
  public String parseName(ResultSet res) throws SQLException {

    if (!isDynamic) {
      return name;
    }
    if (res != null) {

      String value;

      if (type.equalsIgnoreCase("clob")) {
        value = DBFactory.getCLOB(res, name);
      } else if (type.equalsIgnoreCase("text")) {
        value = DBFactory.getString(res, name);
      } else if (type.equalsIgnoreCase("integer")) {
        value = DBFactory.getInt(res, name);
      } else {
        logger.warn("Unknown type given for Field name: " + name);
        return "";
      }

      if (value != null) {
        logger.debug("Dynamic name value: " + value);
        logger.debug("Dynamic name:" + name);
        return value;
      }
    }
    throw new SQLException("Invalid result set for dynamic field name: " + name);
  }

  /**
   * @return the transformers
   */
  public Vector<TransformerDefinition> getTransformers() {
    return transformers;
  }

  public void addTransformer(TransformerDefinition trans) {

    this.transformers.add(trans);
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    out.append(indexType).append(" ").append(column).append(" ").append(name);
    out.append("\n");

    for (Object tr : getTransformers()) {

      out.append(tr.toString());
      out.append("\n");
    }

    return out.toString();
  }

  public void writeDocument(ResultSet res, DocumentHolder dh) throws SQLException {

    try {
      String value;
      boolean indexed = INDEX_ANALYZED == indexType;

      if (type.equalsIgnoreCase("clob")) {
        value = DBFactory.getCLOB(res, column);
      } else if (type.equalsIgnoreCase("text")) {
        value = DBFactory.getString(res, column);
      } else if (type.equalsIgnoreCase("integer")) {
        value = DBFactory.getInt(res, column);
      } else {
        logger.warn("Unknown type given for Field name: " + name);
        return;
      }

      if (value != null && !value.isEmpty()) {
        String purged = callTransformers(value);
        String fieldName = parseName(res);

        logger.debug("Found value: " + purged + " for name: " + fieldName);

        dh.add(fieldName, purged, global, store, indexed);
      }
    } catch (NullPointerException e) {
      logger.error("Caught NullPointerException: " + e.getMessage());
    }
  }

  public void writeDocument(String content, DocumentHolder dh) {

    boolean indexed = (INDEX_ANALYZED == indexType);

    if (content != null && !content.isEmpty()) {

      String purged = callTransformers(content);

      dh.add(name, purged, global, store, indexed);
    }
  }

  private String callTransformers(String value) {

    // Default whitespace sanitizer
    ContentTransformer trans = TransformerFactory.factory("WhitespaceSanitizer");
    value = trans.transform(value);

    for (int i = 0; i < getTransformers().size(); i++) {

      logger.debug(getTransformers().get(i).getName());
      trans = TransformerFactory.factory(getTransformers().get(i).getName());
      if (trans != null) {
        value = trans.transform(value);
      }
    }

    // Delete html tags
    trans = TransformerFactory.factory("TagSanitizer");

    return trans.transform(value);
  }

}
