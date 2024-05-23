/*
+-----------------------------------------------------------------------------------------+
| ILIAS open source                                                                       |
+-----------------------------------------------------------------------------------------+
| Copyright (c) 1998-2001 ILIAS open source, University of Cologne                        |
|                                                                                         |
| This program is free software; you can redistribute it and/or                           |
| modify it under the terms of the GNU General Public License                             |
| as published by the Free Software Foundation; either version 2                          |
| of the License, or (at your option) any later version.                                  |
|                                                                                         |
| This program is distributed in the hope that it will be useful,                         |
| but WITHOUT ANY WARRANTY; without even the implied warranty of                          |
| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                           |
| GNU General Public License for more details.                                            |
|                                                                                         |
| You should have received a copy of the GNU General Public License                       |
| along with this program; if not, write to the Free Software                             |
| Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             |
+-----------------------------------------------------------------------------------------+
*/

package de.ilias.services.transformation;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.PageSequenceResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class FO2PDF {

  private static final Logger logger = LogManager.getLogger(FO2PDF.class);
  private String foString = null;
  private byte[] pdfByteArray = null;
  private FopFactory fopFactory = null;

  public FO2PDF() {
    try {
      // add font config
      URL fopConfigUrl = getClass().getResource("/de/ilias/config/fopConfig.xml");
      logger.info("Using config uri: " + fopConfigUrl.toURI());

      fopFactory = FopFactory.newInstance(getClass().getResource("/de/ilias/config/fopConfig.xml").toURI());
    } catch (URISyntaxException ex) {
      logger.error("Cannot load fop configuration:", ex);
    }
  }

  /**
   * clear fop uri cache
   */
  public void clearCache() {

    fopFactory.getImageManager().getCache().clearCache();
  }

  /**
   * Transform
   */
  public void transform() throws TransformationException {

    try {

      logger.info("Starting fop transformation...");
      logger.debug(foString);
      FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setNamespaceAware(true);
      saxParserFactory.setValidating(false);
      SAXParser sp = saxParserFactory.newSAXParser();
      InputStream is = new ByteArrayInputStream(foString.getBytes(StandardCharsets.UTF_8));
      sp.parse(is, new ILIASFopDhAdapter(fop.getDefaultHandler()));

      FormattingResults foResults = fop.getResults();
      if (logger.isDebugEnabled()) {
        java.util.List pageSequences = foResults.getPageSequences();
        for (Object pageSequence : pageSequences) {
          PageSequenceResults pageSequenceResults = (PageSequenceResults) pageSequence;
          logger.debug("PageSequenze " + (!String.valueOf(pageSequenceResults.getID()).isEmpty()
              ? pageSequenceResults.getID()
              : "<no id>") + " generated " + pageSequenceResults.getPageCount() + " pages.");
        }
      }
      logger.info("Generated " + foResults.getPageCount() + " pages in total.");

      this.setPdf(out.toByteArray());

    } catch (SAXException | IOException ex) {
      logger.error("Cannot load fop configuration", ex);
    } catch (ParserConfigurationException e) {
      logger.error("Cannot configure Sax parser", e);
      throw new TransformationException(e);
    }
  }

  /**
   * @param foString The foString to set.
   */
  public void setFoString(String foString) {
    this.foString = foString;
  }

  public byte[] getPdf() {
    return this.pdfByteArray;
  }

  public void setPdf(byte[] ba) {

    this.pdfByteArray = ba;
  }

}
