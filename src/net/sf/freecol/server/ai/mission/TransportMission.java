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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Transportable;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;

import org.w3c.dom.Element;

/**
 * Mission for transporting units and goods on a carrier.
 * 
 * @see net.sf.freecol.common.model.Unit Unit
 */
public class TransportMission extends Mission {
    private static final Logger logger = Logger.getLogger(TransportMission.class.getName());




    private static final String ELEMENT_TRANSPORTABLE = "transportable";

    private static final int MINIMUM_GOLD_TO_STAY_IN_EUROPE = 600;

    private ArrayList<Transportable> transportList = new ArrayList<Transportable>();


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     */
    public TransportMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        if (!getUnit().isCarrier()) {
            logger.warning("Only carriers can transport unit/goods.");
            throw new IllegalArgumentException("Only carriers can transport unit/goods.");
        }
    }

    /**
     * Loads a <code>TransportMission</code> from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public TransportMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>TransportMission</code> and reads the given
     * element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public TransportMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Adds every <code>Goods</code> and <code>Unit</code> onboard the
     * carrier to the transport list.
     * 
     * @see Goods
     * @see Unit
     */
    private void updateTransportList() {
        Unit carrier = getUnit();

        Iterator<Unit> ui = carrier.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            AIUnit aiUnit = (AIUnit) getAIMain().getAIObject(u);
            addToTransportList(aiUnit);
        }
        
        // Remove items that are no longer on the transport list:
        List<Transportable> ts = new LinkedList<Transportable>();
        for (Transportable t : new LinkedList<Transportable>(transportList)) {
            if (ts.contains(t) || isCarrying(t)) {
                if (t.getTransportDestination() == null) {
                    removeFromTransportList(t);
                }
            } else {
                if (t.getTransportSource() == null) {
                    removeFromTransportList(t);
                }
            }
            ts.add(t);
        }
    }

    /**
     * Checks if the carrier using this mission is carrying the given
     * <code>Transportable</code>.
     * 
     * @param t The <code>Transportable</code>.
     * @return <code>true</code> if the given <code>Transportable</code> is
     *         {@link Unit#getLocation located} in the carrier.
     */
    private boolean isCarrying(Transportable t) {
        // TODO: Proper code for checking if the goods is onboard the carrier.
        return t.getTransportLocatable().getLocation() == getUnit();
    }

    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
    	// a new list must be created as the first one may be changed
    	//elsewhere in between loop calls
    	List<Transportable> cargoList = new ArrayList<Transportable>();
    	List<Transportable> scheduledCargoList = new ArrayList<Transportable>();
    	
    	Iterator<Transportable> ti = transportList.iterator();
        while (ti.hasNext()) {
            Transportable t = ti.next();
            // the cargo is on board, add to list to be disposed of
            if (isCarrying(t)) {
            	cargoList.add(t);
            } else {
            	// the cargo was scheduled to be transported
            	// cancel order
            	scheduledCargoList.add(t);
            }
        }
        
        for (Transportable t : cargoList)
        	((AIObject) t).dispose();
        
        for (Transportable t : scheduledCargoList)
        	t.setTransport(null);
        
        super.dispose();
    }

    /**
     * Checks if the given <code>Transportable</code> is on the transport
     * list.
     * 
     * @param newTransportable The <code>Transportable</code> to be checked
     * @return <code>true</code> if the given <code>Transportable</code> was
     *         on the transport list, and <code>false</code> otherwise.
     */
    public boolean isOnTransportList(Transportable newTransportable) {
        for (int i = 0; i < transportList.size(); i++) {
            if (transportList.get(i) == newTransportable) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the given <code>Transportable</code> from the transport list.
     * This method calls {@link Transportable#setTransport(AIUnit)}.
     * 
     * @param transportable The <code>Transportable</code>.
     */
    public void removeFromTransportList(Transportable transportable) {
        Iterator<Transportable> ti = transportList.iterator();
        while (ti.hasNext()) {
            Transportable t = ti.next();
            if (t == transportable) {
                ti.remove();
                if (transportable.getTransport() == getAIUnit()) {
                    transportable.setTransport(null);
                }
            }
        }

    }

    /**
     * Adds the given <code>Transportable</code> to the transport list. The
     * method returns immediately if the {@link Transportable} has already be
     * added.
     * 
     * <br>
     * <br>
     * 
     * Both the source and destination {@link Location} for the
     * <code>Transportable</code> is entered into the transport list if the
     * <code>Transportable</code> is not already loaded onto the transport. If
     * the <code>Transportable</code> is onboard the transport, then only the
     * destination is put on the transport list.
     * 
     * @param newTransportable The <code>Transportable</code>.
     */
    public void addToTransportList(Transportable newTransportable) {
        Unit carrier = getUnit();
        if (newTransportable.getTransportLocatable() instanceof Unit
                && ((Unit) newTransportable.getTransportLocatable()).isCarrier()) {
            throw new IllegalArgumentException("You cannot add a carrier to the transport list.");
        }
        Location newSource = newTransportable.getTransportSource();
        Location newDestination = newTransportable.getTransportDestination();

        if (newDestination == null) {
            if (newTransportable instanceof AIGoods) {
                logger.warning("No destination for goods: " + newTransportable.getTransportLocatable().toString());
                return;
            } else {
                logger.warning("No destination for: " + newTransportable.getTransportLocatable().toString());
                return;
            }
        }

        if (newSource == null && !isCarrying(newTransportable)) {
            logger.warning("No source for: " + newTransportable.getTransportLocatable().toString());
            return;
        }

        if (isOnTransportList(newTransportable)) {
            return;
        }

        int bestSourceIndex = -1;
        if (!isCarrying(newTransportable)) {
            
            int distToSource;
            if (carrier.getLocation().getTile() == newSource.getTile()) {
                distToSource = 0;
            } else {
                distToSource = getDistanceTo(newTransportable, ((carrier.getTile() != null) ? carrier.getTile()
                        : carrier.getEntryLocation().getTile()), true);
                // Sanitation
                // Carrier cant reach source
                if(distToSource == Map.COST_INFINITY){
                    return;
                }
            }
            bestSourceIndex = 0;
            int bestSourceDistance = distToSource;
            for (int i = 1; i < transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = transportList.get(i - 1);
                if (t1.getTransportSource() != null && t1.getTransportSource().getTile() == newSource.getTile()
                        || t1.getTransportDestination() != null
                        && t1.getTransportDestination().getTile() == newSource.getTile()) {
                    bestSourceIndex = i;
                    bestSourceDistance = 0;
                }

            }
            
            for (int i = 1; i < transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = transportList.get(i - 1);
                 
                if (isCarrying(t1)){
                    int distToDestination = getDistanceTo(newTransportable, t1.getTransportDestination(), true);
                    if(distToDestination == Map.COST_INFINITY){
                        continue;
                    }
                    if(distToDestination <= bestSourceDistance) {
                         bestSourceIndex = i;
                         bestSourceDistance = distToDestination;
                    }
                } else{
                    distToSource = getDistanceTo(newTransportable, t1.getTransportSource(), true);
                    if(distToSource == Map.COST_INFINITY){
                        continue;
                    }   
                    if (distToSource <= bestSourceDistance) {
                        bestSourceIndex = i;
                        bestSourceDistance = distToSource;
                    }
                }
            }
            transportList.add(bestSourceIndex, newTransportable);
        }

        int bestDestinationIndex = bestSourceIndex + 1;
        int bestDestinationDistance = Integer.MAX_VALUE;
        if (bestSourceIndex == -1) {
            bestDestinationIndex = 0;
            if (carrier.getTile() == newSource.getTile()) {
                bestDestinationDistance = 0;
            } else {
                int distToCarrier = getDistanceTo(newTransportable, carrier.getTile(), false);
                if(distToCarrier != Map.COST_INFINITY){
                    bestDestinationDistance = distToCarrier;
                }
            }
        }
        for (int i = Math.max(bestSourceIndex, 1); i < transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = transportList.get(i - 1);
            if (t1.getTransportSource().getTile() == newDestination.getTile()
                    || t1.getTransportDestination().getTile() == newDestination.getTile()) {
                bestDestinationIndex = i;
                bestDestinationDistance = 0;
            }
        }
        for (int i = Math.max(bestSourceIndex, 1); i < transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = transportList.get(i - 1);
            if (isCarrying(t1)){
                int distToDestination = getDistanceTo(newTransportable, t1.getTransportDestination(), false);
                if(distToDestination == Map.COST_INFINITY){
                    continue;
                }
                if(distToDestination <= bestDestinationDistance) {
                    bestDestinationIndex = i;
                    bestDestinationDistance = distToDestination;
                }
            } else{
                int distToSource = getDistanceTo(newTransportable, t1.getTransportSource(), false); 
                if(distToSource == Map.COST_INFINITY){
                    continue;
                }
                if(distToSource <= bestDestinationDistance) {
                    bestDestinationIndex = i;
                    bestDestinationDistance =  distToSource;
                }
            }
        }
        transportList.add(bestDestinationIndex, newTransportable);

        if (newTransportable.getTransport() != getAIUnit()) {
            newTransportable.setTransport(getAIUnit());
        }
    }

    /**
     * Gets the distance to the given <code>Transportable</code>.
     * 
     * @param start The <code>Location</code> to check the distance from.
     *            <code>Europe</code> is used instead of this location if
     *            <code>start.getTile() == null</code>.
     * @param source Sets wether the <code>Transportable</code>'s
     *            {@link Transportable#getTransportSource source} or
     *            {@link Transportable#getTransportDestination destination}
     *            should be used.
     * @return The distance from the given <code>Location</code> to the source
     *         or destination of the given <code>Transportable</code>.
     */
    private int getDistanceTo(Transportable t, Location start, boolean source) {
        // TODO: This takes to much resources - find another method:
        PathNode path = getPath(t, start, source);
        
        if(path == null){
            return Map.COST_INFINITY;
        }
        
        return path.getTotalTurns();
    }

    /**
     * Performs the mission.
     * 
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        if (transportList == null || transportList.size() <= 0) {
            updateTransportList();
        }

        Unit carrier = getUnit();
        if (carrier.getLocation() instanceof Europe) {
            // Coming to/from Europe, do nothing
            if (carrier.getState() == UnitState.TO_EUROPE || carrier.getState() == UnitState.TO_AMERICA) {
                return;
            }
            // Actually in Europe
            inEurope(connection);
            return;
            
        }

        restockCargoAtDestination(connection);

        boolean transportListChanged = false;
        boolean moreWork = true;
        for (int i = 0; i < transportList.size() && moreWork || i == 0; i++) {
            moreWork = false;

            if (transportListChanged) {
                i = 0;
                transportListChanged = false;
            }

            boolean moveToEurope = false;

            // Determine the path to the next target:
            PathNode path = null;
            if (i == 0 && transportList.size() == 0) {
                // Send to Europe if the transport list is empty:
                path = findPathToEurope(carrier.getTile());
                moveToEurope = true;
            } else {
                Transportable transportable = transportList.get(i);
                try {
                    path = getPath(transportable);
                    moveToEurope = isCarrying(transportable) ? (transportable.getTransportDestination() instanceof Europe)
                            : (transportable.getTransportLocatable().getLocation() instanceof Europe);
                } catch (IllegalArgumentException e) {
                    transportListChanged = restockCargoAtDestination(connection);
                    continue;
                }
            }

            // Move towards the next target:
            if (path != null) {
                // Tile oldTile = carrier.getTile();

                Direction r = moveTowards(connection, path);
                if (r != null && carrier.getMoveType(r).isProgress()) {
                    // Tile target = getGame().getMap().getNeighbourOrNull(r,
                    // carrier.getTile());
                    if (carrier.getMoveType(r) == MoveType.MOVE_HIGH_SEAS && moveToEurope) {
                        Element moveToEuropeElement = Message.createNewRootElement("moveToEurope");
                        moveToEuropeElement.setAttribute("unit", carrier.getId());
                        try {
                            connection.sendAndWait(moveToEuropeElement);
                        } catch (IOException e) {
                            logger.warning("Could not send \"moveToEuropeElement\"-message!");
                        }
                    } else {
                        move(connection, r);
                    }

                    if (!(carrier.getLocation() instanceof Europe)) {
                        moreWork = true;
                    }
                }

                transportListChanged = restockCargoAtDestination(connection);
            } else if (moveToEurope && carrier.canMoveToEurope()) {
                Element moveToEuropeElement = Message.createNewRootElement("moveToEurope");
                moveToEuropeElement.setAttribute("unit", carrier.getId());
                try {
                    connection.sendAndWait(moveToEuropeElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"moveToEuropeElement\"-message (2)!");
                }
            }
        }
    }

    /**
     * Buys cargo (units and goods) when the carrier is in <code>Europe</code>.
     * 
     * <br>
     * <br>
     * 
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     * 
     * @param connection The <code>Connection</code> to the server.
     */
    private void buyCargo(Connection connection) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId());

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        /*
         * Quick fix for forcing the AI to build more colonies. This fix should
         * be removed after a proper implementation has been created.
         */
        if (aiPlayer.hasFewColonies()) {
            // since we are in Europe, use the carrier entry point to search for a good settlement spot.
            Unit carrier = getUnit();
            Tile colonyTile = BuildColonyMission.findColonyLocation(carrier);
            int space = getAvailableSpace();
            while (colonyTile!=null && space > 0) {
                AIUnit newUnit = getCheapestUnitInEurope(connection);
                if (newUnit != null) {
                    if (newUnit.getUnit().isColonist() && !newUnit.getUnit().isArmed()
                        && !newUnit.getUnit().isMounted() && newUnit.getUnit().getRole() != Role.PIONEER) {
                        // send the colonist to build the new colony
                        int colonyValue = aiPlayer.getPlayer().getColonyValue(colonyTile);
                        newUnit.setMission(new BuildColonyMission(getAIMain(), newUnit, colonyTile, colonyValue));
                    }
                    addToTransportList(newUnit);
                    space--;
                } else {
                    return;
                }
            }
        }

        /*
         * Add colonies containing wishes with the same destination as an item
         * in the transport list to the "aiColonies"-list:
         */
        ArrayList<AIColony> aiColonies = new ArrayList<AIColony>();
        for (int i = 0; i < transportList.size(); i++) {
            Transportable t = transportList.get(i);
            if (t.getTransportDestination() != null && t.getTransportDestination().getTile() != null
                    && t.getTransportDestination().getTile().getColony() != null
                    && t.getTransportDestination().getTile().getColony().getOwner() == getUnit().getOwner()) {
                AIColony ac = (AIColony) getAIMain().getAIObject(
                        t.getTransportDestination().getTile().getColony().getId());
                aiColonies.add(ac);
            }
        }

        /*
         * Add the colony containing the wish with the highest value to the
         * "aiColonies"-list:
         */
        Iterator<Wish> highValueWishIterator = ((AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId()))
                .getWishIterator();
        while (highValueWishIterator.hasNext()) {
            Wish w = highValueWishIterator.next();
            if (w.getTransportable() != null) {
                continue;
            }
            if (w instanceof WorkerWish && w.getDestination() instanceof Colony) {
                WorkerWish ww = (WorkerWish) w;
                Colony c = (Colony) ww.getDestination();
                AIColony ac = (AIColony) getAIMain().getAIObject(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else if (w instanceof GoodsWish && w.getDestination() instanceof Colony) {
                GoodsWish gw = (GoodsWish) w;
                Colony c = (Colony) gw.getDestination();
                AIColony ac = (AIColony) getAIMain().getAIObject(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else {
                logger.warning("Unknown type of wish: " + w);
            }
        }
        for (int i = 0; i < aiColonies.size(); i++) {
            AIColony ac = aiColonies.get(i);
            // Assuming that all colonists which can be bought in Europe take
            // the same space: TODO: fix this
            int space = getAvailableSpace(getUnit().getType(), getUnit().getOwner().getEurope(), ac.getColony());
            Iterator<Wish> wishIterator = ac.getWishIterator();
            while (space > 0 && wishIterator.hasNext()) {
                Wish w = wishIterator.next();
                if (w.getTransportable() != null) {
                    continue;
                }
                if (w instanceof WorkerWish) {
                    WorkerWish ww = (WorkerWish) w;
                    AIUnit newUnit = getUnitInEurope(connection, ww.getUnitType());
                    if (newUnit != null) {
                        newUnit.setMission(new WishRealizationMission(getAIMain(), newUnit, ww));
                        ww.setTransportable(newUnit);
                        addToTransportList(newUnit);
                        space--;
                    }
                } else if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    AIGoods ag = buyGoodsInEurope(connection, gw.getGoodsType(), 100, gw.getDestination());
                    if (ag != null) {
                        gw.setTransportable(ag);
                        addToTransportList(ag);
                        space--;
                    }
                } else {
                    logger.warning("Unknown type of wish: " + w);
                }
            }
        }

        // Fill the transport with cheap colonists:
        int space = getAvailableSpace();
        while (space > 0) {
            AIUnit newUnit = getCheapestUnitInEurope(connection);
            if (newUnit != null) {
                addToTransportList(newUnit);
                space--;
            } else {
                break;
            }
        }
    }

    /**
     * Buys the given cargo.
     * 
     * <br>
     * <br>
     * 
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     * 
     * @param connection The <code>Connection</code> to use when communicating
     *            with the server.
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param destination The <code>Location</code> to which the goods should
     *            be transported.
     * @return The goods.
     */
    public AIGoods buyGoodsInEurope(Connection connection, GoodsType type, int amount, Location destination) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId());
        Player player = aiPlayer.getPlayer();
        Market market = player.getMarket();

        if (player.getGold() >= market.getBidPrice(type, amount)) {
            Element buyGoodsElement = Message.createNewRootElement("buyGoods");
            buyGoodsElement.setAttribute("carrier", getUnit().getId());
            buyGoodsElement.setAttribute("type", type.getId());
            buyGoodsElement.setAttribute("amount", Integer.toString(amount));
            try {
                connection.sendAndWait(buyGoodsElement);
            } catch (IOException e) {
                logger.warning("Could not send \"buyGoods\"-message to the server.");
                return null;
            }
            AIGoods ag = new AIGoods(getAIMain(), getUnit(), type, amount, destination);
            return ag;
        } else {
            return null;
        }
    }

    /**
     * Returns the given type of <code>Unit</code>.
     * 
     * <br>
     * <br>
     * 
     * <b>Warning:</b> This method can only be called when the carrier is
     * located in {@link Europe}.
     * 
     * <br>
     * <br>
     * 
     * This sequence is used when trying to get the unit: <br>
     * <br>
     * <ol>
     * <li>Getting the unit from the docks.
     * <li>Recruiting the unit.
     * <li>Training the unit.
     * </ol>
     * 
     * @param connection The <code>Connection</code> to the server.
     * @param unitType The type of {@link Unit} to be found/recruited/trained.
     * @return The <code>AIUnit</code>.
     */
    private AIUnit getUnitInEurope(Connection connection, UnitType unitType) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        // Check if the given type of unit appear on the docks:
        Iterator<Unit> ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (unitType == null || unitType == u.getType()) {
                return (AIUnit) getAIMain().getAIObject(u.getId());
            }
        }

        // Try recruiting the unit:
        // TODO: Check if it will be cheaper to train the unit instead.
        if (player.getGold() >= player.getRecruitPrice()) {
            for (int i = 0; i < 3; i++) {
                // Note, used to be 1-3 but the method expects 0-2
                if (europe.getRecruitable(i) == unitType) {
                    return aiPlayer.recruitAIUnitInEurope(i);
                }
            }
        }

        // Try training the unit:
        if (unitType.hasPrice() && europe.getUnitPrice(unitType) >= 0 &&
                player.getGold() >= europe.getUnitPrice(unitType)) {
            return aiPlayer.trainAIUnitInEurope(unitType);
        }

        return null;
    }

    /**
     * Returns the cheapest unit which can be bought in <code>Europe</code>.
     * 
     * @param connection The connection to use when communicating with the
     *            server.
     * @return The <code>AIUnit</code>.
     */
    private AIUnit getCheapestUnitInEurope(Connection connection) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }
        if (!player.canRecruitUnits()) {
            return null;
        }

        // Check if there are any units on the docks:
        Iterator<Unit> ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (!u.isCarrier() && ((AIUnit) getAIMain().getAIObject(u)).getTransport() == null) {
                return (AIUnit) getAIMain().getAIObject(u.getId());
            }
        }

        int priceTrained = 0;
        UnitType cheapestTrained = null;
        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypesTrainedInEurope();
        for (UnitType unitType : unitTypes) {
            int price = europe.getUnitPrice(unitType);
            if (cheapestTrained == null || price < priceTrained) {
            	cheapestTrained = unitType;
            	priceTrained = price;
            }
        }
        // Try recruiting the unit:
        if (player.getGold() >= player.getRecruitPrice() && cheapestTrained != null
                && player.getRecruitPrice() < priceTrained) {
            // TODO: Take the best unit (Seasoned scout, pioneer, soldier etc)
            return aiPlayer.recruitAIUnitInEurope(1);
        }

        // Try training the unit:
        if (cheapestTrained != null && player.getGold() >= priceTrained) {
            return aiPlayer.trainAIUnitInEurope(cheapestTrained);
        }

        return null;
    }

    /**
     * Returns the path the carrier should use to get/drop the given
     * <code>Transportable</code>.
     * 
     * @param transportable The <code>Transportable</code>.
     * @return The path.
     */
    public PathNode getPath(Transportable transportable) {
        return getPath(transportable, getUnit().getTile(), !isCarrying(transportable));
    }

    /**
     * Returns the path the carrier should use to get/drop the given
     * <code>Transportable</code>.
     * 
     * @param transportable The <code>Transportable</code>.
     * @param start The <code>Tile</code> to search from. If
     *            <code>start == null</code> or
     *            <code>start.getTile() == null</code> then the carrier's
     *            {@link Unit#getEntryLocation entry location} is used instead.
     * @param source
     * @return The path.
     */
    private PathNode getPath(Transportable transportable, Location start, boolean source) {
        Unit carrier = getUnit();

        if (isCarrying(transportable) && source) {
            throw new IllegalStateException(
                    "Cannot find the path to the source while the transportable is on the carrier.");
        }

        PathNode path;
        Locatable locatable = transportable.getTransportLocatable();

        if (start == null || start.getTile() == null) {
            start = getUnit().getEntryLocation();
        }

        Location destination;
        if (source) {
            destination = locatable.getLocation();
        } else {
            destination = transportable.getTransportDestination();
        }

        if (destination == null) {
            return null;
        }

        if (destination instanceof Europe) {
            path = findPathToEurope(start.getTile());
        } else if (locatable instanceof Unit && isCarrying(transportable)) {
            path = getGame().getMap().findPath((Unit) locatable, start.getTile(), destination.getTile(), carrier);
            if (path == null || path.getTransportDropNode().previous == null) {
                path = null;
            } else {
                path.getTransportDropNode().previous.next = null;
            }
        } else {
            path = getGame().getMap().findPath(carrier, start.getTile(), destination.getTile());
        }

        return path;
    }

    /**
     * Returns the available space for the given <code>Transportable</code>.
     * 
     * @param t The <code>Transportable</code>
     * @return The space available for <code>Transportable</code>s with the
     *         same source and
     *         {@link Transportable#getTransportDestination destination}.
     */
    public int getAvailableSpace(Transportable t) {
        if (t.getTransportLocatable() instanceof Unit) {
            Unit u = (Unit) t.getTransportLocatable();
            return getAvailableSpace(u.getType(), t.getTransportSource(), t.getTransportDestination());
        } else {
            return getAvailableSpace(null, t.getTransportSource(), t.getTransportDestination());
        }
    }

    /**
     * Returns the available space for the given type of <code>Unit</code> at
     * the given <code>Location</code>.
     * 
     * @param unitType The type of {@link Unit} or <code>null</code> for
     *            {@link Goods}
     * @param source The source for the unit. This is where the unit is
     *            presently located.
     * @param destination The destination for the unit.
     * @return The space available
     */
    public int getAvailableSpace(UnitType unitType, Location source, Location destination) {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }

    /**
     * Returns the available space for any type of unit going to any type of
     * location.
     * 
     * @return The space available
     */
    public int getAvailableSpace() {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }

    /**
     * Loads and unloads any <code>Transportable</code>.
     * 
     * @param connection The <code>Connection</code> to the server.
     * @return <code>true</code> if something has been loaded/unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean restockCargoAtDestination(Connection connection) {
        return unloadCargoAtDestination(connection) | loadCargoAtDestination(connection);
    }

    /**
     * Unloads any <code>Transportable</code>s which have reached their
     * destination.
     * 
     * @param connection The <code>Connection</code> to the server.
     * @return <code>true</code> if something has been unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean unloadCargoAtDestination(Connection connection) {
        Unit carrier = getUnit();

        boolean transportListChanged = false;

        // Make a copy for iteration, the main list may change inside the loop
        for (Transportable t : new ArrayList<Transportable>(transportList)) {
            if (!isCarrying(t)) {
                continue;
            }
            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                Mission mission = au.getMission();
                if (mission != null && mission.isValid()) {
                    if (au.getTransportDestination() != null
                            && au.getTransportDestination().getTile() == carrier.getTile()
                            && carrier.getState() != UnitState.TO_EUROPE && carrier.getState() != UnitState.TO_AMERICA) {
                        if (carrier.getLocation() instanceof Europe || u.getColony() != null) {
                            try {
                                connection.sendAndWait(new DisembarkMessage(u).toXMLElement());
                            } catch (IOException e) {
                                logger.warning("Could not send \"disembark\"-message!");
                            }
                        }
                        mission.doMission(connection);
                        if (u.getLocation() != getUnit()) {
                            removeFromTransportList(au);
                            transportListChanged = true;
                        }
                    } else if (!(carrier.getLocation() instanceof Europe) && au.getTransportDestination() != null
                            && au.getTransportDestination().getTile() != null) {
                        PathNode p = getGame().getMap().findPath(u, carrier.getTile(),
                                au.getTransportDestination().getTile(), carrier);
                        if (p != null) {
                            final PathNode dropNode = p.getTransportDropNode();
                            int distToCarrier = dropNode.getTile().getDistanceTo(carrier.getTile());
                            if (dropNode != null &&
                                    distToCarrier != Map.COST_INFINITY &&
                                    distToCarrier <= 1) {
                                mission.doMission(connection);
                                if (u.getLocation() != getUnit()) {
                                    removeFromTransportList(au);
                                    transportListChanged = true;
                                }    
                            }
                        }
                        /*
                        boolean atTarget = (au.getTransportDestination().getTile() == carrier.getTile());
                        for (Tile c : getGame().getMap().getSurroundingTiles(carrier.getTile(), 1)) {
                            if (c == au.getTransportDestination().getTile()) {
                                atTarget = true;
                            }
                        }
                        if (atTarget) {
                            mission.doMission(connection);
                            if (u.getLocation() != getUnit()) {
                                removeFromTransportList(au);
                                transportListChanged = true;
                            }
                        }
                        */
                        /*
                        PathNode p = getGame().getMap().findPath(u, carrier.getTile(),
                                au.getTransportDestination().getTile());
                        if (p != null && p.getTransportDropNode().getTurns() <= 0) {
                            mission.doMission(connection);
                            if (u.getLocation() != getUnit()) {
                                removeFromTransportList(au);
                                transportListChanged = true;
                            }
                        }
                        */
                    }
                }
            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                if (ag.getTransportDestination() != null
                        && ag.getTransportDestination().getTile() == carrier.getLocation().getTile()
                        && carrier.getState() != UnitState.TO_EUROPE && carrier.getState() != UnitState.TO_AMERICA) {
                    if (carrier.getLocation() instanceof Europe) {
                        // TODO-AI-CHEATING: REMOVE WHEN THE AI IS GOOD ENOUGH:
                        Player p = carrier.getOwner();
                        if (p.isAI() && getAIMain().getFreeColServer().isSingleplayer()) {
                            // Double the income by adding this bonus:
                            p.modifyGold(p.getMarket().getSalePrice(ag.getGoods()));
                        }
                        // END: TODO-AI-CHEATING.
                        Element sellGoodsElement = Message.createNewRootElement("sellGoods");
                        sellGoodsElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(),
                                sellGoodsElement.getOwnerDocument()));
                        try {
                            connection.sendAndWait(sellGoodsElement);
                            removeFromTransportList(ag);
                            ag.dispose();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"sellGoodsElement\"-message!");
                        }
                    } else {
                        Element unloadCargoElement = Message.createNewRootElement("unloadCargo");
                        unloadCargoElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(),
                                unloadCargoElement.getOwnerDocument()));
                        try {
                            connection.sendAndWait(unloadCargoElement);
                            removeFromTransportList(ag);
                            ag.dispose();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"unloadCargoElement\"-message!");
                        }
                    }
                }
            } else {
                logger.warning("Unknown Transportable.");
            }
        }

        return transportListChanged;
    }

    /**
     * Loads any <code>Transportable</code>s being in range of the carrier.
     * 
     * @param connection The <code>Connection</code> to the server.
     * @return <code>true</code> if something has been unloaded and
     *         <code>false</code>otherwise.
     */
    private boolean loadCargoAtDestination(Connection connection) {
        Unit carrier = getUnit();

        // TODO: Add code for rendez-vous.

        boolean transportListChanged = false;

        Iterator<Transportable> tli = transportList.iterator();
        while (tli.hasNext()) {
            Transportable t = tli.next();
            if (isCarrying(t)) {
                continue;
            }
            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                if (u.getTile() == carrier.getTile() && carrier.getState() != UnitState.TO_EUROPE
                        && carrier.getState() != UnitState.TO_AMERICA) {
                    Element boardShipElement = Message.createNewRootElement("boardShip");
                    boardShipElement.setAttribute("unit", u.getId());
                    boardShipElement.setAttribute("carrier", carrier.getId());
                    try {
                        connection.sendAndWait(boardShipElement);
                        tli.remove();
                        transportListChanged = true;
                    } catch (IOException e) {
                        logger.warning("Could not send \"boardShipElement\"-message!");
                    }
                }
            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                if (ag.getGoods().getTile() == carrier.getTile() && carrier.getState() != UnitState.TO_EUROPE
                        && carrier.getState() != UnitState.TO_AMERICA) {
                    if (carrier.getLocation() instanceof Europe) {
                        Element buyGoodsElement = Message.createNewRootElement("buyGoods");
                        buyGoodsElement.setAttribute("carrier", carrier.getId());
                        buyGoodsElement.setAttribute("type", Integer.toString(ag.getGoods().getType().getIndex()));
                        buyGoodsElement.setAttribute("amount", Integer.toString(ag.getGoods().getAmount()));
                        try {
                            connection.sendAndWait(buyGoodsElement);
                            tli.remove();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"buyGoodsElement\"-message!");
                        }
                        ag.setGoods(new Goods(getGame(), carrier, ag.getGoods().getType(), ag.getGoods().getAmount()));
                    } else {
                        Element loadCargoElement = Message.createNewRootElement("loadCargo");
                        loadCargoElement.setAttribute("carrier", carrier.getId());
                        loadCargoElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(),
                                loadCargoElement.getOwnerDocument()));

                        try {
                            connection.sendAndWait(loadCargoElement);
                            tli.remove();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"loadCargoElement\"-message!");
                        }
                        ag.setGoods(new Goods(getGame(), carrier, ag.getGoods().getType(), ag.getGoods().getAmount()));
                    }
                }
            } else {
                logger.warning("Unknown Transportable.");
            }
        }

        return transportListChanged;
    }

    /**
     * Checks if this mission is still valid to perform.
     * 
     * @return <code>true</code>
     */
    public boolean isValid() {
        Unit unit = this.getAIUnit().getUnit();
        if (unit.isNaval() && unit.isUnderRepair())
            return false;
        else
            return true;
    }

    /**
     * Returns the destination of a required transport.
     * 
     * @return <code>null</code>
     */
    public Tile getTransportDestination() {
        return null;
    }

    /**
     * Returns the priority of getting the unit to the transport destination.
     * 
     * @return o
     */
    public int getTransportPriority() {
        return 0;
    }

    /**
     * unit is in Europe, unload cargo on board, buy required goods and board unit 
     * @param connection  The <code>Connection</code> to the server.
     */
    private void inEurope(Connection connection){
        restockCargoAtDestination(connection);
        buyCargo(connection);
        restockCargoAtDestination(connection);

        // Move back to America:
        Unit carrier = getUnit();
        if (carrier.getOwner().getGold() < MINIMUM_GOLD_TO_STAY_IN_EUROPE || transportList.size() > 0) {
            Element moveToAmericaElement = Message.createNewRootElement("moveToAmerica");
            moveToAmericaElement.setAttribute("unit", carrier.getId());
            try {
                connection.sendAndWait(moveToAmericaElement);
            } catch (IOException e) {
                logger.warning("Could not send \"moveToAmericaElement\"-message!");
            }
        }        
    }
    
    /**
     * Finds the best path to <code>Europe</code>.
     * 
     * @param start The starting <code>Tile</code>.
     * @return The path to the target or <code>null</code> if no target can be
     *         found.
     * @see Europe
     */
    protected PathNode findPathToEurope(Tile start) {
        return getGame().getMap().findPathToEurope(getUnit(), start);
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

        Iterator<Transportable> tli = transportList.iterator();
        while (tli.hasNext()) {
            Transportable t = tli.next();
            out.writeStartElement(ELEMENT_TRANSPORTABLE);
            out.writeAttribute("ID", ((AIObject) t).getId());
            out.writeEndElement();
        }
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

        transportList.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(ELEMENT_TRANSPORTABLE)) {
                String tid = in.getAttributeValue(null, "ID");
                AIObject ao = getAIMain().getAIObject(tid);
                if (ao == null) {
                    if (tid.startsWith(Unit.getXMLElementTagName())) {
                        ao = new AIUnit(getAIMain(), tid);
                    } else {
                        ao = new AIGoods(getAIMain(), tid);
                    }
                }
                if (!(ao instanceof Transportable)) {
                    logger.warning("AIObject not Transportable, ID: " + in.getAttributeValue(null, "ID"));
                } else {
                    transportList.add((Transportable) ao);
                }
                in.nextTag();
            } else {
                logger.warning("Unknown tag.");
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "transportMission".
     */
    public static String getXMLElementTagName() {
        return "transportMission";
    }
    
    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     * 
     * @return The <code>String</code>: "(x, y) z" or "(x, y) z!" where
     *         <code>x</code> and <code>y</code> is the coordinates of the
     *         target tile for this mission, and <code>z</code> is the value
     *         of building the colony. The exclamation mark is added if the unit
     *         should continue searching for a colony site if the targeted site
     *         is lost.
     */
    public String getDebuggingInfo() {
        Unit carrier = getUnit();
        return this.toString();
    }

    /**
     * Creates a <code>String</code> representation of this mission to be used
     * for debugging purposes.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("Transport list:\n");
        List<Transportable> ts = new LinkedList<Transportable>();
        for(Transportable t : transportList) {
            Locatable l = t.getTransportLocatable();
            sb.append(l.toString());
            sb.append(" (");
            Location target; 
            if (ts.contains(t) || isCarrying(t)) {
                sb.append("to ");
                target = t.getTransportDestination();
            } else {
                sb.append("from ");
                target = t.getTransportSource();
            }
            if (target instanceof Europe) {
                sb.append("Europe");
            } else if (target == null) {
                sb.append("null");
            } else {
                sb.append(target.getTile().getPosition());
            }
            sb.append(")");
            sb.append("\n");
            ts.add(t);
        }
        return sb.toString();
    }
}
