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

package net.sf.freecol.client.control;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelController;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;

/**
 * A client-side implementation of the <code>ModelController</code> interface.
 */
public class ClientModelController implements ModelController {
    private static final Logger logger = Logger.getLogger(ClientModelController.class.getName());




    private final FreeColClient freeColClient;


    /**
     * Creates a new <code>ClientModelController</code>.
     * 
     * @param freeColClient The main controller.
     */
    public ClientModelController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }

    /**
     * Returns a pseudorandom int, uniformly distributed between 0 (inclusive)
     * and the specified value (exclusive).
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param n The specified value.
     * @return The generated number.
     */
    public int getRandom(String taskID, int n) {
        Client client = freeColClient.getClient();

        Element getRandomElement = Message.createNewRootElement("getRandom");
        getRandomElement.setAttribute("taskID", taskID);
        getRandomElement.setAttribute("n", Integer.toString(n));

        logger.info("TaskID is " + taskID + " Waiting for the server to reply...");
        Element reply = client.ask(getRandomElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("getRandomConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        return Integer.parseInt(reply.getAttribute("result"));
    }

    /**
     * Creates a new unit.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param location The <code>Location</code> where the <code>Unit</code>
     *            will be created.
     * @param owner The <code>Player</code> owning the <code>Unit</code>.
     * @param type The type of unit (Unit.FREE_COLONIST...).
     * @return The created <code>Unit</code>.
     */
    public Unit createUnit(String taskID, Location location, Player owner, UnitType type) {

        Element createUnitElement = Message.createNewRootElement("createUnit");
        createUnitElement.setAttribute("taskID", taskID);
        createUnitElement.setAttribute("location", location.getId());
        createUnitElement.setAttribute("owner", owner.getId());
        createUnitElement.setAttribute("type", type.getId());

        logger.info("Waiting for the server to reply...");
        Element reply = freeColClient.getClient().ask(createUnitElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("createUnitConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Unit unit = new Unit(freeColClient.getGame(), (Element) reply.getElementsByTagName(Unit.getXMLElementTagName())
                .item(0));
        unit.setLocation(unit.getLocation());

        return unit;
    }

    /**
     * Creates a new building.
     * 
     * @param taskID The <code>taskID</code> should be a unique identifier.
     *            One method to make a unique <code>taskID</code>: <br>
     *            <br>
     *            getId() + "methodName:taskDescription" <br>
     *            <br>
     *            As long as the "taskDescription" is unique within the method
     *            ("methodName"), you get a unique identifier.
     * @param location The <code>Location</code> where the <code>Building</code>
     *            will be created.
     * @param owner The <code>Player</code> owning the <code>Building</code>.
     * @param type The type of building (Building.FREE_COLONIST...).
     * @return The created <code>Building</code>.
     */
    public Building createBuilding(String taskID, Colony colony, BuildingType type) {

        Element createBuildingElement = Message.createNewRootElement("createBuilding");
        createBuildingElement.setAttribute("taskID", taskID);
        createBuildingElement.setAttribute("colony", colony.getId());
        createBuildingElement.setAttribute("type", type.getId());

        logger.info("Waiting for the server to reply...");
        Element reply = freeColClient.getClient().ask(createBuildingElement);
        logger.info("Reply received from server.");

        if (!reply.getTagName().equals("createBuildingConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        Building building = new Building(freeColClient.getGame(),
                                         (Element) reply.getElementsByTagName(Building.getXMLElementTagName())
                                         .item(0));
        return building;
    }

    /**
     * Puts the specified <code>Unit</code> in America.
     * 
     * @param unit The <code>Unit</code>.
     * @return The <code>Location</code> where the <code>Unit</code>
     *         appears.
     */
    public Location setToVacantEntryLocation(Unit unit) {
        Element createUnitElement = Message.createNewRootElement("getVacantEntryLocation");
        createUnitElement.setAttribute("unit", unit.getId());

        Element reply = freeColClient.getClient().ask(createUnitElement);
        if (reply == null) {
            throw new IllegalStateException("No reply for getVacantEntryLocation!");
        } else if (!"getVacantEntryLocationConfirmed".equals(reply.getTagName())) {
            throw new IllegalStateException("Unexpected reply type for getVacantEntryLocation: " + reply.getTagName());
        }

        Location entryLocation = (Location) freeColClient.getGame()
                .getFreeColGameObject(reply.getAttribute("location"));
        unit.setLocation(entryLocation);

        return entryLocation;
    }

    /**
     * Updates stances.
     * 
     * @param first The first <code>Player</code>.
     * @param second The second <code>Player</code>.
     * @param stance The new stance.
     */
    public void setStance(Player first, Player second, int stance) {
        // Nothing to do.
    }

    /**
     * Explores the given tiles for the given player.
     * 
     * @param player The <code>Player</code> that should see more tiles.
     * @param tiles The tiles to explore.
     */
    public void exploreTiles(Player player, ArrayList<Tile> tiles) {
        // Nothing to do on the client side.
    }

    /**
     * Tells the <code>ModelController</code> that an internal change (that
     * is; not caused by the control) has occured in the model.
     * 
     * @param tile The <code>Tile</code> which will need an update.
     */
    public void update(Tile tile) {
        // Nothing to do on the client side.
    }

    /**
     * Get the pseudo-random number generator provided by the client.
     * 
     * @return random number generator.
     */
    public PseudoRandom getPseudoRandom() {
        return freeColClient.getPseudoRandom();
    }

    /**
     * Returns a new <code>TradeRoute</code> object.
     * 
     * @return a new <code>TradeRoute</code> object.
     */
    public TradeRoute getNewTradeRoute(Player player) {
        Game game = freeColClient.getGame();
        Client client = freeColClient.getClient();

        Element getNewTradeRouteElement = Message.createNewRootElement("getNewTradeRoute");
        Element reply = client.ask(getNewTradeRouteElement);

        if (!reply.getTagName().equals("getNewTradeRouteConfirmed")) {
            logger.warning("Wrong tag name.");
            throw new IllegalStateException();
        }

        TradeRoute tradeRoute = new TradeRoute(game, (Element) reply.getElementsByTagName(
                TradeRoute.getXMLElementTagName()).item(0));

        return tradeRoute;
    }

    /**
     * Check if game object should receive newTurn call.
     * 
     * @param freeColGameObject The game object.
     * @return true if owned by client player or not ownable.
     */
    public boolean shouldCallNewTurn(FreeColGameObject freeColGameObject) {
        if (freeColGameObject instanceof Ownable) {
            Ownable o = (Ownable) freeColGameObject;
            return o.getOwner() == freeColClient.getMyPlayer();
        }
        return true;
    }

}
