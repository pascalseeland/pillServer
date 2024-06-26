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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import java.util.HashMap;

/**
 * Highlight results (top most xml element)
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class HighlightHits implements ResultExport {

  private final HashMap<Integer, HighlightObject> objects = new HashMap<>();

  private double maxScore = 0;

  public HighlightObject initObject(int objId) {

    if (objects.containsKey(objId)) {
      //logger.debug("Reusing object with id: " + String.valueOf(objId));
      return objects.get(objId);
    }
    //logger.debug("New object with id: " + String.valueOf(objId));
    objects.put(objId, new HighlightObject(objId));
    return objects.get(objId);
  }

  /**
   * Set score
   */
  public void setMaxScore(double score) {
    maxScore = score;
  }

  /**
   * Get max score
   */
  public double getMaxScore() {
    return maxScore;
  }

  public String toXML() {

    Document doc = new Document(addXML());

    XMLOutputter outputter = new XMLOutputter();
    return outputter.outputString(doc);

  }

  /**
   * Add xml
   *
   * @see ResultExport#addXML()
   */
  public Element addXML() {

    Element hits = new Element("Hits");
    hits.setAttribute("maxScore", String.valueOf(getMaxScore()));

    for (ResultExport obj : objects.values()) {

      hits.addContent(obj.addXML());
    }
    return hits;
  }
}
