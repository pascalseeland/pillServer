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

package de.ilias.services.lucene.search.highlight;

import de.ilias.services.lucene.search.ResultExport;

import org.jdom2.Element;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class HighlightObject implements ResultExport, Comparator<Integer> {
  private final TreeMap<Integer, HighlightItem> items = new TreeMap<>();

  private int objId;

  public HighlightObject(int objId) {

    this.setObjId(objId);
  }

  public HighlightItem addItem(int subId) {

    if (items.containsKey(subId)) {
      return items.get(subId);
    }
    items.put(subId, new HighlightItem(subId));
    return items.get(subId);
  }

  /**
   * @param objId the objId to set
   */
  public void setObjId(int objId) {
    this.objId = objId;
  }

  /**
   * @return the objId
   */
  public int getObjId() {
    return objId;
  }

  /**
   * Add xml
   *
   * @see ResultExport#addXML()
   */
  public Element addXML() {

    Element obj = new Element("Object");
    obj.setAttribute("id", String.valueOf(getObjId()));

    TreeMap<Integer, HighlightItem> sortedItems = new TreeMap<>(this);
    sortedItems.putAll(items);

    for (ResultExport item : sortedItems.values()) {

      obj.addContent(item.addXML());
    }
    return obj;
  }

  /**
   * Compare items by absolute score
   */
  public int compare(Integer o1, Integer o2) {

    int index1 = o1;
    int index2 = o2;

    if (items.get(index1).getAbsoluteScore() < items.get(index2).getAbsoluteScore()) {
      return 1;
    }
    if (items.get(index1).getAbsoluteScore() > items.get(index2).getAbsoluteScore()) {
      return -1;
    }
    // returning zero, does not add a new element to TreeMap since its assumed to be equal
    //return 0;
    // ... sort by subitem
    return Integer.compare(items.get(index2).getSubId(), items.get(index1).getSubId());
  }
}
