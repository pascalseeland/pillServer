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

import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class HighlightItem implements ResultExport {
  private int subId;
  private double absoluteScore = 0;
  private final Vector<HighlightField> fields = new Vector<>();

  public HighlightItem(int subId) {

    this.setSubId(subId);
  }

  /**
   * @param subId the subId to set
   */
  public void setSubId(int subId) {
    this.subId = subId;
  }

  /**
   * @return the subId
   */
  public int getSubId() {
    return subId;
  }

  /**
   * Score
   */
  public void setAbsoluteScore(double score) {
    this.absoluteScore = score;
  }

  /**
   * Get absolute score
   */
  public double getAbsoluteScore() {
    return absoluteScore;
  }

  public void addField(HighlightField field) {
    fields.add(field);
  }

  /**
   * Add xml
   *
   * @see ResultExport#addXML()
   */
  public Element addXML() {

    Element item = new Element("Item").setAttribute("id", String.valueOf(getSubId()));
    item.setAttribute("id", String.valueOf(getSubId()));
    item.setAttribute("absoluteScore", String.valueOf(getAbsoluteScore()));

    for (ResultExport field : fields) {

      item.addContent(field.addXML());
    }
    return item;
  }

}
