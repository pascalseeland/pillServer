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

package de.ilias.services.lucene.index;

import java.util.Vector;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
@ApplicationScoped
public class FieldInfo {

  private Vector<String> fields = new Vector<>();

  protected FieldInfo() {

    initDefaultFields();
  }

  /**
   * Add field (if not already appended)
   */
  public void addField(String field) {

    if (!fields.contains(field)) {
      fields.add(field);
    }
  }

  /**
   * Get fields as Vector
   *
   * @return Vector fields
   */
  public Vector<String> getFields() {
    return fields;
  }

  /**
   * Return fields as string array
   */
  public String[] getFieldsAsStringArray() {
    return fields.toArray(new String[0]);
  }

  public int getFieldSize() {

    return fields.size();
  }

  /**
   * Load default fields
   */
  protected void initDefaultFields() {

    addField("title");
    addField("description");
    addField("lomKeyword");
    addField("metaData");
    addField("tag");
    addField("propertyHigh");
    addField("propertyMedium");
    addField("propertyLow");
    addField("content");

  }

}
