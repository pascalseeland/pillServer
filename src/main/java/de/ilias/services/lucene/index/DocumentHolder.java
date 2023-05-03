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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class DocumentHolder {


  private Document globalDoc = null;
  private Document doc = null;


  public DocumentHolder() {
    // new string fields are in contrast to TextFields not analyzed. Ensure STORE.YES
    globalDoc = new Document();
    globalDoc.add(new StringField("docType", "combined", Field.Store.YES));
    doc = new Document();
    doc.add(new StringField("docType", "separated", Field.Store.YES));
  }

  /**
   * @return get current global document
   */
  public Document getGlobalDocument() {
    return globalDoc;
  }

  /**
   * @return return current document
   */
  public Document getDocument() {
    return doc;
  }

  public void add(String name, String value, boolean isGlobal, Field.Store store, boolean indexed) {

    if (indexed) {
      doc.add(new TextField(name, value, store));
    } else {
      doc.add(new StringField(name, value, store));
    }

    if (isGlobal) {
      if (indexed) {
        globalDoc.add(new TextField(name, value, store));
      } else {
        globalDoc.add(new StringField(name, value, store));
      }
    }
  }
}
