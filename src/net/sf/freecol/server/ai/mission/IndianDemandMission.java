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

package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;

/**
 * Mission for demanding goods from a specified player.
 */
public class IndianDemandMission extends Mission {

    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    /** The <code>Colony</code> receiving the demand. */
    private Colony target;

    /** Whether this mission has been completed or not. */
    private boolean completed;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The <code>Colony</code> receiving the gift.
     */
    public IndianDemandMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);

        this.target = target;

        if (!getUnit().getOwner().isIndian() || !getUnit().canCarryGoods()) {
            logger.warning("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
            throw new IllegalArgumentException("Only an indian which can carry goods can be given the mission: IndianBringGiftMission");
        }
    }

    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public IndianDemandMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>IndianDemandMission</code> and reads the given
     * element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public IndianDemandMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Performs the mission.
     * 
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        if (!isValid()) {
            return;
        }

        if (!hasGift()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                Direction r = moveTowards(connection, getUnit().getIndianSettlement().getTile());
                moveButDontAttack(connection, r);
            } else {
                // Load the goods:
                ArrayList<Goods> goodsList = new ArrayList<Goods>();
                GoodsContainer gc = getUnit().getIndianSettlement().getGoodsContainer();
                for (GoodsType goodsType : FreeCol.getSpecification().getNewWorldGoodsTypeList()) {
                    if (gc.getGoodsCount(goodsType) >= IndianSettlement.KEEP_RAW_MATERIAL + 25) {
                        goodsList.add(new Goods(getGame(), getUnit().getIndianSettlement(),
                                                goodsType,
                                                getRandom().nextInt(15) + 10));
                    }
                }

                if (goodsList.size() > 0) {
                    Goods goods = goodsList.get(getRandom().nextInt(goodsList.size()));
                    goods.setLocation(getUnit());
                }
            }
        } else {
            // Move to the target's colony and deliver
            Unit unit = getUnit();
            Direction r = moveTowards(connection, target.getTile());
            if (r != null &&
                getGame().getMap().getNeighbourOrNull(r, unit.getTile()) == target.getTile()
                && unit.getMovesLeft() > 0) {
                // We have arrived.
                Element demandElement = Message.createNewRootElement("indianDemand");
                demandElement.setAttribute("unit", unit.getId());
                demandElement.setAttribute("colony", target.getId());

                Player enemy = target.getOwner();
                Goods goods = selectGoods(target);
                if (goods == null) {
                    if (enemy.getGold() == 0) {
                        // give up
                        completed = true;
                        return;
                    }
                    demandElement.setAttribute("gold", String.valueOf(enemy.getGold() / 20));
                } else {
                    demandElement.appendChild(goods.toXMLElement(null, demandElement.getOwnerDocument()));
                }
                if (!unit.isVisibleTo(enemy)) {
                    demandElement.appendChild(unit.toXMLElement(enemy, demandElement.getOwnerDocument()));
                }

                Element reply;
                try {
                    reply = connection.ask(demandElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"demand\"-message!");
                    completed = true;
                    return;
                }

                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                int tension = 0;
                int unitTension = unit.getOwner().getTension(enemy).getValue();
                if (unit.getIndianSettlement() != null) {
                    unitTension += unit.getIndianSettlement().getOwner().getTension(enemy).getValue();
                }
                // TODO: make this work with DifficultyLevel
                int difficulty = enemy.getDifficulty().getIndex();
                if (accepted) {
                    // TODO: if very happy, the brave should convert
                    tension = -(5 - difficulty) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                    if (unitTension <= Tension.Level.HAPPY.getLimit() &&
                        (goods == null || goods.getType().isFoodType())) {
                        Element deliverGiftElement = Message.createNewRootElement("deliverGift");
                        deliverGiftElement.setAttribute("unit", getUnit().getId());
                        deliverGiftElement.setAttribute("settlement", target.getId());
                        deliverGiftElement.appendChild(getUnit().getGoodsIterator().next().toXMLElement(null,
                                deliverGiftElement.getOwnerDocument()));

                        try {
                            connection.sendAndWait(deliverGiftElement);
                        } catch (IOException e) {
                            logger.warning("Could not send \"deliverGift\"-message!");
                        }
                    }
                } else {
                    tension = (difficulty + 1) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                    if (unitTension >= Tension.Level.CONTENT.getLimit()) {
                        // if we didn't get what we wanted, attack
                        Element element = Message.createNewRootElement("attack");
                        element.setAttribute("unit", unit.getId());
                        element.setAttribute("direction", r.toString());

                        try {
                            connection.ask(element);
                        } catch (IOException e) {
                            logger.warning("Could not send message!");
                        }
                    }
                }
                completed = true;
            }
        }

        // Walk in a random direction if we have any moves left:
        moveRandomly(connection);
    }

    /**
     * Selects the most desirable goods from the colony.
     * 
     * @param target The colony.
     * @return The goods to demand.
     */
    public Goods selectGoods(Colony target) {
        Tension.Level tension = getUnit().getOwner().getTension(target.getOwner()).getLevel();
        int dx = target.getOwner().getDifficulty().getIndex() + 1;
        GoodsType food = FreeCol.getSpecification().getGoodsType("model.goods.food");
        Goods goods = null;
        GoodsContainer warehouse = target.getGoodsContainer();
        if (tension.compareTo(Tension.Level.CONTENT) <= 0 &&
            warehouse.getGoodsCount(food) >= 100) {
            int amount = (warehouse.getGoodsCount(food) * dx) / 6;
            if (amount > 0) {
                return new Goods(getGame(), target, food, amount);
            }
        } else if (tension.compareTo(Tension.Level.DISPLEASED) <= 0) {
            Market market = target.getOwner().getMarket();
            int value = 0;
            List<Goods> warehouseGoods = warehouse.getCompactGoods();
            for (Goods currentGoods : warehouseGoods) {
                int goodsValue = market.getSalePrice(currentGoods);
                if (currentGoods.getType().isFoodType() ||
                    currentGoods.getType().isMilitaryGoods()) {
                    continue;
                } else if (goodsValue > value) {
                    value = goodsValue;
                    goods = currentGoods;
                }
            }
            if (goods != null) {
                goods.setAmount(Math.max((goods.getAmount() * dx) / 6, 1));
                return goods;
            }
        } else {
            // military goods
            for (GoodsType preferred : FreeCol.getSpecification().getGoodsTypeList()) {
                if (preferred.isMilitaryGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, Math.max((amount * dx) / 6, 1));
                    }
                }
            }
            // storable building materials (what do the natives need tools for?)
            for (GoodsType preferred : FreeCol.getSpecification().getGoodsTypeList()) {
                if (preferred.isBuildingMaterial() && preferred.isStorable()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, Math.max((amount * dx) / 6, 1));
                    }
                }
            }
            // trade goods
            for (GoodsType preferred : FreeCol.getSpecification().getGoodsTypeList()) {
                if (preferred.isTradeGoods()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, Math.max((amount * dx) / 6, 1));
                    }
                }
            }
            // refined goods
            for (GoodsType preferred : FreeCol.getSpecification().getGoodsTypeList()) {
                if (preferred.isRefined() && preferred.isStorable()) {
                    int amount = warehouse.getGoodsCount(preferred);
                    if (amount > 0) {
                        return new Goods(getGame(), target, preferred, Math.max((amount * dx) / 6, 1));
                    }
                }
            }
        }

        // haven't found what we want
        Market market = target.getOwner().getMarket();
        int value = 0;
        List<Goods> warehouseGoods = warehouse.getCompactGoods();
        for (Goods currentGoods : warehouseGoods) {
            int goodsValue = market.getSalePrice(currentGoods);
            if (goodsValue > value) {
                value = goodsValue;
                goods = currentGoods;
            }
        }
        if (goods != null) {
            goods.setAmount(Math.max((goods.getAmount() * dx) / 6, 1));
        }
        return goods;
    }

    /**
     * Checks if the unit is carrying a gift (goods).
     * 
     * @return <i>true</i> if <code>getUnit().getSpaceLeft() == 0</code> and
     *         false otherwise.
     */
    private boolean hasGift() {
        return (getUnit().getSpaceLeft() == 0);
    }

    /**
     * Checks if this mission is still valid to perform.
     * 
     * <BR>
     * <BR>
     * 
     * This mission will be invalidated when the demand has been delivered.
     * 
     * @return <code>true</code> if this mission is still valid.
     */
    public boolean isValid() {
        // The last check is to ensure that the colony have not been burned to
        // the ground.
        return (!completed && target != null && !target.isDisposed() && target.getTile().getColony() == target &&
                getUnit().getIndianSettlement() != null);
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("unit", getUnit().getId());
        out.writeAttribute("target", target.getId());
        out.writeAttribute("completed", Boolean.toString(completed));

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));

        target = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "target"));
        completed = Boolean.valueOf(in.getAttributeValue(null, "completed")).booleanValue();

        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "indianDemandMission".
     */
    public static String getXMLElementTagName() {
        return "indianDemandMission";
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     * 
     * @return The <code>String</code>: "[ColonyName] GIFT_TYPE" or
     *         "[ColonyName] Getting gift: (x, y)".
     */
    public String getDebuggingInfo() {
        if (getUnit().getIndianSettlement() == null) {
            return "invalid";
        }
        final String targetName = (target != null) ? target.getName() : "null";
        if (!hasGift()) {
            return "[" + targetName + "] Getting gift: "
                    + getUnit().getIndianSettlement().getTile().getPosition();
        } else {
            return "[" + targetName + "] " + getUnit().getGoodsIterator().next().getName();
        }
    }
}
