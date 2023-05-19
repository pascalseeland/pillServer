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

package de.ilias.services.lucene.index.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public abstract class ZipBasedOfficeHandler {

  private static Logger logger = LogManager.getLogger(ZipBasedOfficeHandler.class);
  protected static final int BUFFER = 2048;

  /**
   * get name of content file
   * E.g content.xml for .odt files
   */
  protected abstract String getContentFileName();

  protected abstract String getXPath();

  protected InputStream extractContentStream(InputStream is) throws FileHandlerException {

    try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
      ZipInputStream zip = new ZipInputStream(is);
      ZipEntry entry;

      while ((entry = zip.getNextEntry()) != null) {

        if (entry.getName().equalsIgnoreCase(getContentFileName())) {
          int count;
          byte[] data = new byte[BUFFER];
          while ((count = zip.read(data, 0, BUFFER)) != -1) {
            bout.write(data, 0, count);
          }
          break;
        }
      }
      is.close();
      return new ByteArrayInputStream(bout.toByteArray());
    } catch (IOException e) {
      logger.info("Cannot extract " + getContentFileName() + " " + e.getMessage());
      throw new FileHandlerException(e);
    }
  }

  public String extractContent(InputStream is) {

    SAXBuilder builder = new SAXBuilder();
    StringBuilder content = new StringBuilder();

    try {
      org.jdom2.Document doc = builder.build(is);
      XPathExpression<Element> xpath =
          XPathFactory.instance().compile(getXPath(), Filters.element());
      List<Element> elements = xpath.evaluate(doc);
      for (Element element : elements) {
        content.append(" ");
        content.append(element.getTextTrim());
      }
      return content.toString();

    } catch (NullPointerException e) {
      logger.warn("Caught NullPointerException: " + e);
    } catch (JDOMException | IOException e) {
      logger.info("Cannot parse OO content: " + e);
    }
    return "";
  }

}
