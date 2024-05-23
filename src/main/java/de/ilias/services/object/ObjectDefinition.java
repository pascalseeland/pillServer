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
import de.ilias.services.lucene.index.DocumentExtractor;
import de.ilias.services.lucene.index.DocumentHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class ObjectDefinition {

  public enum INDEX_TYPE {
    FULL,
    INCREMENTAL
  }

  private static final Logger logger = LogManager.getLogger(ObjectDefinition.class);

  private final String type;
  private INDEX_TYPE indexType = INDEX_TYPE.FULL;
  private final List<DocumentDefinition> documentDefinitions = new LinkedList<>();

  public ObjectDefinition(String type, INDEX_TYPE indexType) {

    this(type);
    this.indexType = indexType;
  }

  public ObjectDefinition(String type) {
    logger.debug("Found new definition for type: " + type);
    this.type = type;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the indexType
   */
  public INDEX_TYPE getIndexType() {
    return indexType;
  }

  public void addDocumentDefinition(DocumentDefinition docDefinition) {

    documentDefinitions.add(docDefinition);
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    out.append("Object Definition for type = ").append(getType());
    out.append("\n");

    for (DocumentDefinition doc : documentDefinitions) {

      out.append(doc);
      out.append("\n");
    }
    return out.toString();
  }

  /**
   * @see DocumentExtractor#extractDocument(CommandQueueElement, DocumentHolder, ResultSet)
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh) {
    for (DocumentDefinition def : documentDefinitions) {
      logger.debug("Extracting {} for DocumentDefinition {}", new Object[]{this.getType(), def});
      def.extractDocument(el, dh);
    }
  }

}
