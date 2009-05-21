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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when negotiating a purchase at an IndianSettlement.
 */
public class BuyPropositionMessage extends Message {
    /**
     * The ID of the unit that is buying.
     */
    private String unitId;

    /**
     * The ID of the settlement that is selling.
     */
    private String settlementId;

    /**
     * The goods to be bought.
     */
    private Goods goods;

    /**
     * The price being negotiated.
     */
    private int gold;

    /**
     * Create a new <code>BuyPropositionMessage</code>.
     *
     * @param unit The <code>Unit</code> that is buying.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The price of the goods (negative if unknown).
     */
    public BuyPropositionMessage(Unit unit, Settlement settlement,
                                 Goods goods, int gold) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goods = goods;
        this.gold = gold;
    }

    /**
     * Create a new <code>BuyPropositionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public BuyPropositionMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.gold = Integer.parseInt(element.getAttribute("gold"));
        this.goods = new Goods(game, Message.getChildElement(element, Goods.getXMLElementTagName()));
    }

    /**
     * What is the price currently negotiated for this transaction?
     *
     * @return The current price.
     */
    public int getGold() {
        return gold;
    }

    /**
     * Handle a "buyProposition"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return This <code>BuyProposition</code> message as an
     *         <code>Element</code> with updated gold value,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        IndianSettlement settlement;

        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
            settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        // Make sure we are trying to buy something that is there
        if (goods.getLocation() != settlement) {
            return Message.createError("server.trade.noGoods", "Goods " + goods.getId()
                                       + " are not in settlement " + settlementId);
        }

        InGameController controller = (InGameController) server.getController();
        if (!controller.isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trading without opening a transaction session?!");
        }
        java.util.Map<String,Object> session = controller.getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canBuy")
            || !(Boolean) session.get("hasSpaceLeft")) {
            return Message.clientError("Trying to buy in a session where buying is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) server.getAIMain().getAIObject(settlement.getOwner());
        gold = ai.buyProposition(unit, goods, gold);
        return this.toXMLElement();
    }

    /**
     * Convert this BuyPropositionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        result.setAttribute("gold", Integer.toString(gold));
        result.appendChild(goods.toXMLElement(null, doc));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "buyProposition".
     */
    public static String getXMLElementTagName() {
        return "buyProposition";
    }
}
