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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when cashing in a treasure train.
 */
public class CashInTreasureTrainMessage extends Message {
    /**
     * The id of the object to be cashed in.
     */
    private String unitId;

    /**
     * Create a new <code>CashInTreasureTrainMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> to cash in.
     */
    public CashInTreasureTrainMessage(Unit unit) {
        this.unitId = unit.getId();
    }

    /**
     * Create a new <code>CashInTreasureTrainMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public CashInTreasureTrainMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
    }

    /**
     * Handle a "cashInTreasureTrain"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return An update resulting from cashing in the treasure train,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (!unit.canCarryTreasure()) {
            return Message.clientError("Can not cash in unit " + unitId + " because it can not carry treasure.");
        } else if (!unit.canCashInTreasureTrain()) {
            return Message.clientError("Can not cash in unit " + unitId + " because it is not in a suitable location.");
        }

        // Cash in
        ModelMessage m = serverPlayer.cashInTreasureTrain(unit);
        Tile tile = unit.getTile();
        server.getInGameInputHandler().sendUpdatedTileToAll(tile, serverPlayer);

        // Only need the partial player update, as the sole action on
        // the tile is to remove the treasure train, which will be
        // done by the remove anyway.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element messages = doc.createElement("addMessages");
        reply.appendChild(messages);
        messages.appendChild(m.toXMLElement(player, doc));
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(player.toXMLElementPartial(doc, "gold", "score"));
        Element remove = doc.createElement("remove");
        reply.appendChild(remove);
        unit.addToRemoveElement(remove);
        return reply;
    }

    /**
     * Convert this CashInTreasureTrainMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "cashInTreasureTrain".
     */
    public static String getXMLElementTagName() {
        return "cashInTreasureTrain";
    }
}
