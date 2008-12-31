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

package net.sf.freecol.server.ai;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;

/**
 *
 * Objects of this class contains AI-information for a single {@link Player} and
 * is used for controlling this player.
 *
 * <br />
 * <br />
 *
 * The method {@link #startWorking} gets called by the
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public abstract class NewAIPlayer extends AIObject {

    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());

    protected static EquipmentType muskets = FreeCol.getSpecification().getEquipmentType("model.equipment.muskets");
    protected static EquipmentType horses = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
    protected static EquipmentType toolsType = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");

    /*
     * Stores temporary information for sessions (trading with another player
     * etc).
     */
    protected HashMap<String, Integer> sessionRegister = new HashMap<String, Integer>();

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private ServerPlayer player;

    /** Temporary variable. */
    private ArrayList<AIUnit> aiUnits = new ArrayList<AIUnit>();

    /** Temporary variable. */
    private Connection debuggingConnection;

    public NewAIPlayer() {
        super(null);
    }

    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>AIPlayer</code>.
     */
    public NewAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player.getId());
        this.player = player;
    }

    /**
     *
     * Creates a new <code>AIPlayer</code> and reads the information from the
     * given <code>Element</code>.
     *
     * @param aiMain The main AI-class.
     * @param element The XML-element containing information.
     */
    public NewAIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public NewAIPlayer(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }

    /**
     *
     * Tells this <code>AIPlayer</code> to make decisions. The
     * <code>AIPlayer</code> is done doing work this turn when this method
     * returns.
     */
    public abstract void startWorking();

    /**
     * Determines the stances towards each player.
     *
     * That is: should we declare war?
     */
    protected void determineStances() {
        logger.finest("Entering method determineStances");
        for (Player p : getGame().getPlayers()) {
            if (p != player) {
                Stance stance = getPlayer().getStance(p);
                Tension tension = getPlayer().getTension(p);
                if (stance != null && tension != null) {
                    if (p.getREFPlayer() == getPlayer() && p.getPlayerType() == PlayerType.REBEL) {
                        tension.modify(1000);
                    }
                    if (stance != Stance.WAR &&
                        tension.getLevel() == Tension.Level.HATEFUL) {
                        getPlayer().changeRelationWithPlayer(p, Stance.WAR);
                    } else if (stance == Stance.WAR
                               && tension.getLevel().compareTo(Tension.Level.CONTENT) <= 0) {
                        getPlayer().changeRelationWithPlayer(p, Stance.CEASE_FIRE);
                    } else if (stance == Stance.CEASE_FIRE
                               && tension.getLevel().compareTo(Tension.Level.HAPPY) <= 0) {
                        getPlayer().changeRelationWithPlayer(p, Stance.PEACE);
                    }
                }
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidMissions() {
        logger.finest("Entering method abortInvalidMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid()) {
                aiUnit.setMission(null);
            }
        }
    }

    /**
     * Aborts all the missions which are no longer valid.
     */
    protected void abortInvalidAndOneTimeMissions() {
        logger.finest("Entering method abortInvalidAndOneTimeMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid() || aiUnit.getMission() instanceof UnitWanderHostileMission
                || aiUnit.getMission() instanceof UnitWanderMission
                // || aiUnit.getMission() instanceof DefendSettlementMission
                // || aiUnit.getMission() instanceof UnitSeekAndDestroyMission
                ) {
                aiUnit.setMission(null);
            }
        }
    }

    /**
     * Send an element and ignore IO exceptions. This was used all over the
     * place, no better use a single method.
     *
     * @param element The element.
     */
    protected void sendAndWaitSafely(Element element) {
        logger.finest("Entering method sendAndWaitSafely");
        try {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("AI player (" + this + ") sending " + element.getTagName() + "...");
            }
            getConnection().sendAndWait(element);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Sent and waited, returning.");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Couldn't send AI element " + element.getTagName() + "!", e);
        }
    }

    /**
     * Send some tiles to update to all players which can see them
     *
     * @param tiles The tiles to update.
     */
    protected void sendUpdatedTilesToAll(ArrayList<Tile> tiles) {
        Iterator<Player> enemyPlayerIterator = getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (equals(enemyPlayer) || enemyPlayer.getConnection() == null) {
                continue;
            }
            try {
                Element updateElement = Message.createNewRootElement("update");
                boolean send = false;
                for(Tile tile : tiles) {
                    if (enemyPlayer.canSee(tile)) {
                        updateElement.appendChild(tile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));
                        send = true;
                    }
                }
                if (send) {
                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection "
                               + enemyPlayer.getConnection());
            }
        }
    }

    /**
     *
     * Makes every unit perform their mission.
     *
     */
    protected void doMissions() {
        logger.finest("Entering method doMissions");
        Iterator<AIUnit> aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = aiUnitsIterator.next();
            if (aiUnit.hasMission() && aiUnit.getMission().isValid()
                && !(aiUnit.getUnit().isOnCarrier())) {
                try {
                    aiUnit.doMission(getConnection());
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());
                }
            }
        }
    }

    /**
     * Returns the treasure train carrying the largest treasure
     * located on the given <code>Tile</code>.
     *
     * @param tile a <code>Tile</code> value
     * @return The best treasure train or <code>null</code> if no treasure
     *         train is located on this <code>Tile</code>.
     */
    public Unit getBestTreasureTrain(Tile tile) {
        Unit bestTreasureTrain = null;
        for (Unit unit : tile.getUnitList()) {
            if (unit.canCarryTreasure() &&
                (bestTreasureTrain == null ||
                 bestTreasureTrain.getTreasureAmount() < unit.getTreasureAmount())) {
                bestTreasureTrain = unit;
            }
        }

        return bestTreasureTrain;
    }

    int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile, int turns) {
        logger.finest("Entering method getUnitSeekAndDestroyMissionValue");

        Unit defender = newTile.getDefendingUnit(unit);

        if(!isTargetValidForSeekAndDestroy(unit, defender)){
            return Integer.MIN_VALUE;
        }

        int value = 10020;
        CombatModel combatModel = unit.getGame().getCombatModel();

        if (getBestTreasureTrain(newTile) != null) {
            value += Math.min(getBestTreasureTrain(newTile).getTreasureAmount() / 10, 50);
        }
        if (defender.getType().getOffence() > 0 &&
            newTile.getSettlement() == null) {
            value += 200 - combatModel.getDefencePower(unit, defender) * 2 - turns * 50;
        }

        value += combatModel.getOffencePower(defender, unit) -
            combatModel.getDefencePower(defender, unit);
        value -= turns * 10;

        if (!defender.isNaval()) {
            if (defender.hasAbility("model.ability.expertSoldier")
                && !defender.isArmed()) {
                value += 10 - combatModel.getDefencePower(unit, defender) * 2 - turns * 25;
            }
            if (newTile.getSettlement() != null) {
                value += 300;
                Iterator<Unit> dp = newTile.getUnitIterator();
                while (dp.hasNext()) {
                    Unit u = dp.next();
                    if (u.isDefensiveUnit()) {
                        if (combatModel.getDefencePower(unit, u) > combatModel.getOffencePower(unit, u)) {
                            value -= 100 * (combatModel.getDefencePower(unit, u) - combatModel.getOffencePower(unit, u));
                        } else {
                            value -= combatModel.getDefencePower(unit, u);
                        }
                    }
                }
            }
        }
        return Math.max(0, value);
    }

    boolean isTargetValidForSeekAndDestroy(Unit attacker, Unit defender){    	
    	// Sanitation
    	if(defender == null){
            return false;
    	}
    	
    	// Needs to check if the unit is in a settlement -> attacker.getTile() == null
    	boolean attackerInLand = true;
    	if(attacker.getTile() != null)
            attackerInLand = attacker.getTile().isLand();
    	
    	boolean defenderInLand = true;
    	if(defender.getTile() != null)
            defenderInLand = defender.getTile().isLand();
    		
    	// a naval unit cannot target a unit on land and vice-versa
        if(attackerInLand != defenderInLand){
            return false;
        }

        // a naval unit cannot target a land unit and vice-versa
        if(attacker.isNaval() != defender.isNaval()){
            return false;
        }

    	Player attackerPlayer = attacker.getOwner();
    	Player defenderPlayer = defender.getOwner();

        // cannot target own units
        if(attackerPlayer == defenderPlayer){
            return false;
        }

        boolean notAtWar = attackerPlayer.getStance(defenderPlayer) != Stance.WAR;
        // if european, can only attack units whose owners are at war
        if(attackerPlayer.isEuropean() && notAtWar){
            return false;
        }

        // if indian, cannot attack if not at war or displeased
        if(attackerPlayer.isIndian()){
            boolean inFriendlyMood = attackerPlayer.getTension(defenderPlayer).getLevel().compareTo(Tension.Level.CONTENT) >= 0;
        	
            if(notAtWar && inFriendlyMood)
            	return false;
        }

        return true;
    }

    /**
     * Returns an iterator over all the <code>AIUnit</code>s owned by this
     * player.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<AIUnit> getAIUnitIterator() {
        if (aiUnits.size() == 0) {
            ArrayList<AIUnit> au = new ArrayList<AIUnit>();
            Iterator<Unit> unitsIterator = player.getUnitIterator();
            while (unitsIterator.hasNext()) {
                Unit theUnit = unitsIterator.next();
                AIUnit a = (AIUnit) getAIMain().getAIObject(theUnit.getId());
                if (a != null) {
                    au.add(a);
                } else {
                    logger.warning("Could not find the AIUnit for: " + theUnit + " (" + theUnit.getId() + ") - "
                                   + (getGame().getFreeColGameObject(theUnit.getId()) != null));
                }
            }
            aiUnits = au;
        }
        return aiUnits.iterator();
    }

    /**
     * Returns the <code>Player</code> this <code>AIPlayer</code> is
     * controlling.
     *
     * @return The <code>Player</code>.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the connection to the server.
     *
     * @return The connection that can be used when communication with the
     *         server.
     */
    public Connection getConnection() {
        if (debuggingConnection != null) {
            return debuggingConnection;
        } else {
            return ((DummyConnection) player.getConnection()).getOtherConnection();
        }
    }

    /**
     *
     * Sets the <code>Connection</code> to be used while communicating with
     * the server.
     *
     * This method is only used for debugging.
     *
     * @param debuggingConnection The connection to be used for debugging.
     */
    public void setDebuggingConnection(Connection debuggingConnection) {
        this.debuggingConnection = debuggingConnection;
    }

    /**
     * Returns the ID for this <code>AIPlayer</code>. This is the same as the
     * ID for the {@link Player} this <code>AIPlayer</code> controls.
     *
     * @return The ID.
     */
    @Override
    public String getId() {
        return player.getId();
    }

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("ID", getId());
        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading from the
     *             stream.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        player = (ServerPlayer) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "ID"));
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }


    /**
     * Called after another <code>Player</code> sends a <code>trade</code> message
     *
     *
     * @param goods The goods which we are going to offer
     */
    public void registerSellGoods(Goods goods) {
        String goldKey = "tradeGold#" + goods.getType().getIndex() + "#" + goods.getAmount()
            + "#" + goods.getLocation().getId();
        sessionRegister.put(goldKey, null);
    }

    /**
     * Called when another <code>Player</code> proposes a trade.
     *
     *
     * @param unit The foreign <code>Unit</code> trying to trade.
     * @param goods The goods the given <code>Unit</code> is trying to sell.
     * @param gold The suggested price.
     * @return The price this <code>AIPlayer</code> suggests or
     *         {@link NetworkConstants#NO_TRADE}.
     */
    public int buyProposition(Unit unit, Goods goods, int gold) {
        logger.finest("Entering method tradeProposition");
        IndianSettlement settlement = (IndianSettlement) goods.getLocation();
        String goldKey = "tradeGold#" + goods.getType().getIndex() + "#" + goods.getAmount()
            + "#" + settlement.getId();
        String hagglingKey = "tradeHaggling#" + unit.getId();

        Integer registered = sessionRegister.get(goldKey);
        if (registered == null) {
            int price = settlement.getPriceToSell(goods)
                + player.getTension(unit.getOwner()).getValue();
            sessionRegister.put(goldKey, new Integer(price));
            return price;
        } else {
            int price = registered.intValue();
            if (price < 0 || price == gold) {
                return price;
            } else if (gold < (price * 9) / 10) {
                logger.warning("Cheating attempt: sending a offer too low");
                sessionRegister.put(goldKey, new Integer(-1));
                return NetworkConstants.NO_TRADE;
            } else {
                int haggling = 1;
                if (sessionRegister.containsKey(hagglingKey)) {
                    haggling = sessionRegister.get(hagglingKey).intValue();
                }
                if (getRandom().nextInt(3 + haggling) <= 3) {
                    sessionRegister.put(goldKey, new Integer(gold));
                    sessionRegister.put(hagglingKey, new Integer(haggling + 1));
                    return gold;
                } else {
                    sessionRegister.put(goldKey, new Integer(-1));
                    return NetworkConstants.NO_TRADE;
                }
            }
        }
    }

    protected void clearAIUnits() {
        aiUnits.clear();
    }
}
