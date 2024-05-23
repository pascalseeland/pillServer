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

/**
 * Represents a single entry from table search_command_queue
 * Read only: Updates should be handled in class CommandQueue
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class CommandQueueElement {

  public enum Command {
    RESET("reset"),
    RESET_ALL("reset_all"),
    UPDATE("update"),
    CREATE("create"),
    DELETE("delete");
    public final String label;

    Command(String label) {
      this.label = label;
    }

    public static Command valueOfLabel(String label) {
      for (Command c : values()) {
        if (c.label.equals(label)) {
          return c;
        }
      }
      return null;
    }
  }
  private int objId;
  private String objType;

  private Command command;

  /**
   *
   */
  public CommandQueueElement() {

  }

  /**
   * @return the objId
   */
  public int getObjId() {
    return objId;
  }

  /**
   * @param objId the objId to set
   */
  public void setObjId(int objId) {
    this.objId = objId;
  }

  /**
   * @return the objType
   */
  public String getObjType() {
    return objType;
  }

  /**
   * @param objType the objType to set
   */
  public void setObjType(String objType) {
    this.objType = objType;
  }

  /**
   * @return the command
   */
  public Command getCommand() {
    return command;
  }

  /**
   * @param command the command to set
   */
  public void setCommand(Command command) {
    this.command = command;
  }
}
