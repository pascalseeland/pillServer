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

package de.ilias.services.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.util.prefs.Preferences;

/**
 * Parser for ini files.
 * Stores ini values in ServerSettings and ClientSettings
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class IniFileParser {

  private static final Logger logger = LogManager.getLogger(IniFileParser.class);

  public void parseServerSettings(String path, boolean parseClientSettings) throws ConfigurationException {
    ServerSettings serverSettings = ServerSettings.getInstance();
    ClientSettings clientSettings;
    try {
      for (Ini.Section section : new Ini(new FileReader(path)).values()) {

        if (section.getName().equals("Server")) {
          if (section.containsKey("IpAddress")) {
            serverSettings.setHost(purgeString(section.get("IpAddress")));
          }
          if (section.containsKey("Port")) {
            serverSettings.setPort(purgeString(section.get("Port")));
          }
          if (section.containsKey("IndexPath")) {
            serverSettings.setIndexPath(purgeString(section.get("IndexPath")));
          }
          if (section.containsKey("NumThreads")) {
            serverSettings.setThreadNumber(purgeString(section.get("NumThreads")));
          }
          if (section.containsKey("RAMBufferSize")) {
            serverSettings.setRAMSize(purgeString(section.get("RAMBufferSize")));
          }
          if (section.containsKey("IndexMaxFileSizeMB")) {
            serverSettings.setMaxFileSizeMB(purgeString(section.get("IndexMaxFileSizeMB")));
          }
          if (section.containsKey("IgnoreDocAndXlsFiles")) {
            serverSettings.setIgnoreDocAndXlsFiles(Boolean.parseBoolean(section.get("IgnoreDocAndXlsFiles")));
          }
        }
        if (section.getName().startsWith("Client") && parseClientSettings) {
          if (section.containsKey("ClientId")) {
            String client = purgeString(section.get("ClientId"));
            String nic;
            if (section.containsKey("NicId")) {
              nic = purgeString(section.get("NicId"));
            } else {
              nic = "0";
            }
            clientSettings = ClientSettings.getInstance(client, nic);
            if (section.containsKey("IliasIniPath")) {
              clientSettings.setIliasIniFile(purgeString(section.get("IliasIniPath")));

              // Now parse the ilias.ini file
              parseClientData(clientSettings);
            }
          } else {
            logger.error("No ClientId given for section: " + section.getName());
            throw new ConfigurationException("No ClientId given for section: " + section.getName());
          }
        }
      }

    } catch (ConfigurationException | IOException e) {
      logger.error("Cannot parse server settings: " + e.getMessage());
      throw new ConfigurationException(e);
    }
  }

  public void parseClientData(ClientSettings clientSettings) throws ConfigurationException {

    Preferences prefs;
    try {
      // parse ilias.ini.php
      prefs = new IniPreferences(convertIniFile(clientSettings.getIliasIniFile()));
      clientSettings.setDataDirectory(purgeString(prefs.node("clients").get("datadir", ""), true));
      clientSettings.setAbsolutePath(purgeString(prefs.node("server").get("absolute_path", ""), true));

      String dataName = purgeString(prefs.node("clients").get("path", ""), true);
      String iniFileName = purgeString(prefs.node("clients").get("inifile", ""), true);

      clientSettings.setClientIniFile(
          clientSettings.getAbsolutePath().getCanonicalPath() + FileSystems.getDefault().getSeparator() + dataName
              + FileSystems.getDefault().getSeparator() + clientSettings.getClient() + FileSystems.getDefault().getSeparator()
              + iniFileName);
      clientSettings.setIndexPath(ServerSettings.getInstance().getIndexPath() + FileSystems.getDefault().getSeparator()
          + clientSettings.getClientKey());
      // now parse client.ini.php
      prefs = new IniPreferences(convertIniFile(clientSettings.getClientIniFile()));

      clientSettings.setDbType(purgeString(prefs.node("db").get("type", ""), true));
      clientSettings.setDbHost(purgeString(prefs.node("db").get("host", ""), true));
      clientSettings.setDbPort(purgeString(prefs.node("db").get("port", ""), true));
      clientSettings.setDbUser(purgeString(prefs.node("db").get("user", ""), true));
      clientSettings.setDbPass(purgeString(prefs.node("db").get("pass", ""), true));
      clientSettings.setDbName(purgeString(prefs.node("db").get("name", ""), true));

      logger.debug("Client ID: " + clientSettings.getClient());
      logger.debug("DB Type: " + clientSettings.getDbType());
      logger.debug("DB Host: " + clientSettings.getDbHost());
      logger.debug("DB Port: " + clientSettings.getDbPort());
      logger.debug("DB Name: " + clientSettings.getDbName());
      logger.debug("DB User: " + clientSettings.getDbUser());
      logger.debug("DB Pass: " + clientSettings.getDbPass());

    } catch (IOException e) {
      logger.error("Caught IOException when trying to parse client data: " + e.getMessage());
      throw new ConfigurationException(e);
    } catch (ConfigurationException e) {
      logger.error("Caught ConfigurationException when trying to parse client data.");
      throw e;
    }

  }

  public String purgeString(String dirty, boolean replaceQuotes) {

    if (replaceQuotes) {
      return dirty.replace('"', ' ').trim();
    } else {
      return dirty.trim();
    }
  }

  public String purgeString(String dirty) {

    return purgeString(dirty, false);
  }

  private StringReader convertIniFile(File iniFile) throws ConfigurationException {

    try {
      String output;
      InputStreamReader reader = new InputStreamReader(new FileInputStream(iniFile));

      int c;
      StringBuilder builder = new StringBuilder();

      while ((c = reader.read()) != -1) {
        builder.append((char) c);
      }
      reader.close();
      output = builder.toString();
      output = output.replaceFirst("<\\?php /\\*", "");
      output = output.replaceFirst("\\*/ \\?>", "");
      return new StringReader(output);
    } catch (FileNotFoundException e) {
      logger.fatal("Cannot find ini file: " + e.getMessage());
      throw new ConfigurationException(e);
    } catch (IOException e) {
      logger.error("Caught IOException when trying to convert ini file: " + e.getMessage());
      throw new ConfigurationException(e);
    }
  }

}
