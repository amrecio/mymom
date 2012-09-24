/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit.
 */
public class MoveMessage extends DOMMessage {
    /**
     * The id of the object to be moved.
     */
    private String unitId;

    /**
     * The direction to move.
     */
    private String directionString;

    /**
     * Create a new <code>MoveMessage</code> for the supplied unit and
     * direction.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> to move in.
     */
    public MoveMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>MoveMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MoveMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "move"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the moved unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        MoveType moveType = unit.getMoveType(tile);
        if (!moveType.isProgress()) {
            return DOMMessage.clientError("Illegal move for: " + unitId
                + " type: " + moveType
                + " from: " + unit.getLocation().getId()
                + " to: " + tile.getId());
        }

        // Proceed to move.
        return server.getInGameController()
            .move(serverPlayer, unit, tile);
    }

    /**
     * Convert this MoveMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", this.unitId,
            "direction", this.directionString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "move".
     */
    public static String getXMLElementTagName() {
        return "move";
    }
}
