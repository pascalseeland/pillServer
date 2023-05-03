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
import de.ilias.services.lucene.index.DocumentHandler;
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public abstract class DataSource implements DocumentHandler {

  public static final int TYPE_JDBC = 1;
  public static final int TYPE_FILE = 2;
  public static final int TYPE_DIRECTORY = 3;

  public static final String ACTION_APPEND = "append";
  public static final String ACTION_CREATE = "create";

  private int type;
  private String action;
  Vector<FieldDefinition> fields = new Vector<>();
  Vector<DataSource> ds = new Vector<>();

  public DataSource(int type) {

    this.type = type;
  }

  /**
   * @return the type
   */
  public int getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(int type) {
    this.type = type;
  }

  /**
   * @param action the action to set
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * @return the action
   */
  public String getAction() {
    return action;
  }

  /**
   * @return the fields
   */
  public Vector<FieldDefinition> getFields() {
    return fields;
  }

  /**
   * @param fields the fields to set
   */
  public void setFields(Vector<FieldDefinition> fields) {
    this.fields = fields;
  }

  public void addField(FieldDefinition field) {
    this.fields.add(field);
  }

  /**
   * return nested data sources
   *
   * @return the data sources
   */
  public Vector<DataSource> getDataSources() {
    return ds;
  }

  /**
   * Set DataSource elements
   */
  public void setDataSources(Vector<DataSource> ds) {
    this.ds = ds;
  }

  /**
   * Add DataSource element to vector
   */
  public void addDataSource(DataSource ds) {
    this.getDataSources().add(ds);
  }

  /**
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement)
   */
  public abstract List<DocumentHolder> createDocument(CommandQueueElement el) throws DocumentHandlerException, IOException;

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    for (Object field : getFields()) {

      out.append(field.toString());
    }

    return out.toString();
  }

}
