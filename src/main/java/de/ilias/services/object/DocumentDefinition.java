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
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class DocumentDefinition {

  private final Logger logger = LogManager.getLogger(DocumentDefinition.class);

  private final String type;
  private final Vector<DataSource> dataSource = new Vector<>();

  /**
   *
   */
  public DocumentDefinition(String type) {
    this.type = type;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the dataSource
   */
  private Vector<DataSource> getDataSource() {
    return dataSource;
  }

  public void addDataSource(DataSource source) {
    this.dataSource.add(source);
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    out.append("Document of type = ").append(getType());
    out.append("\n");

    for (Object doc : getDataSource()) {

      out.append(doc.toString());
      out.append("\n");
    }
    return out.toString();
  }

  /**
   * @see DocumentExtractor#extractDocument(CommandQueueElement, DocumentHolder, ResultSet)
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh) {
    for (int i = 0; i < getDataSource().size(); i++) {
      try {
        getDataSource().get(i).extractDocument(el, dh, null);
      } catch (DocumentHandlerException e) {
        logger.warn(e);
      }
    }


  }
}
