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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
@ApplicationScoped
public class ObjectDefinitions {

  private static Logger logger = LogManager.getLogger(ObjectDefinitions.class);
  private Vector<ObjectDefinition> definitions = new Vector<>();

  /**
   * reset object definitons
   */
  public void reset() {
    logger.info("Resetting object definitions");
    definitions.clear();
  }

  /**
   * @return the definitions
   */
  public Vector<ObjectDefinition> getDefinitions() {
    return definitions;
  }

  public void addDefinition(ObjectDefinition def) {
    definitions.add(def);
  }

  /**
   * Get object definition by object type
   */
  public ObjectDefinition getDefinitionByType(String objType) throws ObjectDefinitionException {

    for (ObjectDefinition definition : definitions) {
      if (definition.getType().equals(objType)) {
        return definition;
      }
    }
    throw new ObjectDefinitionException("Invalid type given. Cannot find obj type " + objType);
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    for (Object defs : this.getDefinitions()) {

      out.append("Object definitions for: ");
      out.append("\n");
      out.append(defs);
    }
    return out.toString();
  }
}
