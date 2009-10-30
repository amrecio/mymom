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
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when inciting a native settlement.
 */
public class InciteMessage extends Message {
    /**
     * The id of the unit inciting.
     */
    private String unitId;

    /**
     * The direction to the settlement.
     */
    private String directionString;

    /**
     * The id of the enemy to incite against.
     */
    private String enemyId;

    /**
     * The amount of gold in the bribe.
     */
    private String goldString;

    /**
     * Create a new <code>InciteMessage</code> with the
     * supplied name.
     *
     * @param unit The inciting <code>Unit</code>.
     * @param direction The <code>Direction</code> to the settlement.
     * @param enemy The enemy <code>Player</code>.
     * @param gold The amount of gold in the bribe (negative for the
     *             initial inquiry).
     */
    public InciteMessage(Unit unit, Direction direction, Player enemy, int gold) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
        this.enemyId = enemy.getId();
        this.goldString = Integer.toString(gold);
    }

    /**
     * Create a new <code>InciteMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public InciteMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
        this.enemyId = element.getAttribute("enemyId");
        this.goldString = element.getAttribute("gold");
    }

    /**
     * Handle a "incite"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An element containing the result of the incite,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getTile() == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Map map = game.getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        if (tile == null) {
            return Message.clientError("Could not find tile"
                                       + " in direction: " + direction
                                       + " from unit: " + unitId);
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null || !(settlement instanceof IndianSettlement)) {
            return Message.clientError("There is no native settlement at: "
                                       + tile.getId());
        }
        IndianSettlement indianSettlement = (IndianSettlement) settlement;
        Player settlementPlayer = indianSettlement.getOwner();
        Player enemy;
        if (enemyId == null || enemyId.length() == 0) {
            return Message.clientError("Empty enemyId.");
        }
        FreeColGameObject obj = game.getFreeColGameObjectSafely(enemyId);
        if (!(obj instanceof Player)) {
            return Message.clientError("Not a player: " + enemyId);
        }
        enemy = (Player) obj;
        if (enemy == player) {
            return Message.clientError("Inciting against oneself!");
        }
        if (!enemy.isEuropean()) {
            return Message.clientError("Inciting against non-European!");
        }
        int gold = Integer.parseInt(goldString);

        // Valid, proceed to incite.
        int goldToPay;
        if (gold < 0) { // Initial inquiry.
            goldToPay = server.getInGameController()
                .getInciteAmount(player, enemy, settlementPlayer);
        } else if (server.getInGameController()
                   .inciteIndianSettlement(indianSettlement, player,
                                           enemy, gold)) {
            goldToPay = gold; // Pay this amount.
        } else {
            goldToPay = 0;
        }
        unit.setMovesLeft(0);

        // Build the reply, updating unit for moves left, and player
        // gold if it changed.  The settlement itself does not need to
        // be updated as alarm wrt other players is not public.
        // Always add the goldToPay value as an attribute.
        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.setAttribute("gold", Integer.toString(goldToPay));
        reply.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
        if (gold > 0 && goldToPay > 0) {
            reply.appendChild(player.toXMLElementPartial(doc, "gold"));
        }
        return reply;
    }

    /**
     * Convert this InciteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unitId", unitId);
        result.setAttribute("direction", directionString);
        result.setAttribute("enemyId", enemyId);
        result.setAttribute("gold", goldString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "incite".
     */
    public static String getXMLElementTagName() {
        return "incite";
    }
}
