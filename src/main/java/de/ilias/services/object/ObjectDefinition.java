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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class ObjectDefinition implements DocumentHandler {

  public static final String TYPE_FULL = "full";
  public static final String TYPE_INCREMENTAL = "incremental";

  private Logger logger = LogManager.getLogger(ObjectDefinition.class);

  private String type;
  private String indexType = "full";
  private Vector<DocumentDefinition> documentDefinitions = new Vector<>();

  public ObjectDefinition(String type, String indexType) {

    this.type = type;
    this.indexType = indexType;
  }

  public ObjectDefinition(String type) {
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
  public String getIndexType() {
    return indexType;
  }

  /**
   * @return the document definitions
   */
  public Vector<DocumentDefinition> getDocumentDefinitions() {
    return documentDefinitions;
  }

  public void addDocumentDefinition(DocumentDefinition docDefinition) {
    documentDefinitions.add(docDefinition);
  }

  public void removeDocumentDefinition(DocumentDefinition doc) {

    int index;

    while ((index = documentDefinitions.indexOf(doc)) != -1) {
      documentDefinitions.remove(index);
    }
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    out.append("Object Definition for type = ").append(getType());
    out.append("\n");

    for (Object doc : getDocumentDefinitions()) {

      out.append(doc);
      out.append("\n");
    }
    return out.toString();
  }

  /**
   * create/write documents for this element.
   *
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement)
   */
  public List<DocumentHolder> createDocument(CommandQueueElement el) throws DocumentHandlerException, IOException {

    DocumentHolder docs = new DocumentHolder();
    List<DocumentHolder> documentHolderList = new LinkedList<>();
    for (DocumentDefinition def : getDocumentDefinitions()) {
      logger.debug("1. New document definition");
      documentHolderList.addAll(def.createDocument(el));
    }
    documentHolderList.add(docs);
    return documentHolderList;
  }

  /**
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement, java.sql.ResultSet)
   */
  public List<DocumentHolder> createDocument(CommandQueueElement el, ResultSet res) {
    // TODO Auto-generated method stub
    return null;
  }

}
