/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when renaming a FreeColGameObject.
 */
public class RenameMessage extends Message {

    /**
     * The id of the object to be renamed.
     */
    private String id;

    /**
     * The new name.
     */
    private String newName;

    /**
     * Create a new <code>RenameMessage</code> with the
     * supplied name.
     *
     * @param id The id of the object to rename.
     * @param newName The new name for the object.
     */
    public RenameMessage(String id, String newName) {
        this.id = id;
        this.newName = newName;
    }

    /**
     * Create a new <code>RenameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public RenameMessage(Game game, Element element) {
        this.id = element.getAttribute("nameable");
        this.newName = element.getAttribute("name");
    }

    /**
     * Handle a "rename"-message.
     *
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param player The <code>Player</code> who has declared independence.
     * @param element The element containing the request.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverplayer = server.getPlayer(connection);
        Nameable object = (Nameable) player.getGame().getFreeColGameObject(id);
        if (object == null) {
            throw new IllegalStateException("Tried to rename an object with id " + id + " which could not be found");
        }
        if (!(object instanceof Ownable) || ((Ownable) object).getOwner() != player) {
            throw new IllegalStateException("Not the owner of the nameable.");
        }
        object.setName(newName);
        return null;
    }

    /**
     * Convert this RenameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("nameable", id);
        result.setAttribute("name", newName);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "rename".
     */
    public static String getXMLElementTagName() {
        return "rename";
    }
}
