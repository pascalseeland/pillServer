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

import de.ilias.services.lucene.index.DocumentExtractor;

import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public abstract class DataSource implements DocumentExtractor {

  public enum TYPE {
    JDBC,
    FILE,
    DIRECTORY
  }

  public enum ACTION {
    CREATE("create"),
    APPEND("append");
    public final String label;

    ACTION(String label) {
      this.label = label;
    }
    public static ACTION valueOfLabel(String label) {
      for (ACTION a : values()) {
        if (a.label.equals(label)) {
          return a;
        }
      }
      return null;
    }
  }

  private final TYPE type;
  private ACTION action;
  private final Vector<FieldDefinition> fields = new Vector<>();
  private final Vector<DataSource> ds = new Vector<>();

  public DataSource(TYPE type) {

    this.type = type;
  }

  /**
   * @return the type
   */
  public TYPE getType() {
    return type;
  }

  /**
   * @param action the action to set
   */
  public void setAction(ACTION action) {
    this.action = action;
  }

  /**
   * @return the action
   */
  public ACTION getAction() {
    return action;
  }

  /**
   * @return the fields
   */
  public Vector<FieldDefinition> getFields() {
    return fields;
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
   * Add DataSource element to vector
   */
  public void addDataSource(DataSource ds) {
    this.getDataSources().add(ds);
  }


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
