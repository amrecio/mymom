
package net.sf.freecol.server.control;

import java.awt.Color;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.Iterator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
* Handles the network messages that arrives while 
* {@link FreeColServer#IN_GAME in game}.
*/
public final class InGameInputHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    private FreeColServer freeColServer;



    /**
    * The constructor to use.
    * @param freeColServer The main control object.
    */
    public InGameInputHandler(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }





    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        if (element != null) {
            if (freeColServer.getGame().getCurrentPlayer().equals(freeColServer.getPlayer(connection))) {
                if (type.equals("move")) {
                    reply = move(connection, element);
                } else if (type.equals("embark")) {
                    reply = embark(connection, element);
                } else if (type.equals("boardShip")) {
                    reply = boardShip(connection, element);
                } else if (type.equals("leaveShip")) {
                    reply = leaveShip(connection, element);
                } else if (type.equals("moveToEurope")) {
                    reply = moveToEurope(connection, element);
                } else if (type.equals("moveToAmerica")) {
                    reply = moveToAmerica(connection, element);
                } else if (type.equals("buildColony")) {
                    reply = buildColony(connection, element);
                } else if (type.equals("recruitUnitInEurope")) {
                    reply = recruitUnitInEurope(connection, element);
                } else if (type.equals("trainUnitInEurope")) {
                    reply = trainUnitInEurope(connection, element);
                } else if (type.equals("work")) {
                    reply = work(connection, element);
                } else if (type.equals("putOutsideColony")) {
                    reply = putOutsideColony(connection, element);
                } else if (type.equals("endTurn")) {
                    reply = endTurn(connection, element);
                } else {
                    logger.warning("Unknown request from client " + element.getTagName());
                }
            } else {
                reply = Message.createNewRootElement("error");
                reply.setAttribute("message", "Not your turn.");
           }
        }

        return reply;
    }


    /**
    * Handles a "move"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    * @exception IllegalStateException If the request is not accepted by the model.
    *
    */
    private Element move(Connection connection, Element moveElement) {
        Game game = freeColServer.getGame();

        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveElement.getAttribute("unit"));
        int direction = Integer.parseInt(moveElement.getAttribute("direction"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + moveElement.getAttribute("unit"));
        }

        Tile oldTile = unit.getTile();

        unit.move(direction);

        Tile newTile = unit.getTile();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            Element opponentMoveElement = Message.createNewRootElement("opponentMove");
            opponentMoveElement.setAttribute("direction", Integer.toString(direction));

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    opponentMoveElement.setAttribute("unit", unit.getID());
                    enemyPlayer.getConnection().send(opponentMoveElement);
                } else if (enemyPlayer.canSee(newTile)) {
                    opponentMoveElement.setAttribute("tile", unit.getTile().getID());
                    opponentMoveElement.appendChild(unit.toXMLElement(enemyPlayer, opponentMoveElement.getOwnerDocument()));
                    enemyPlayer.getConnection().send(opponentMoveElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }


        Element reply = Message.createNewRootElement("update");
        Vector surroundingTiles = game.getMap().getSurroundingTiles(unit.getTile(), unit.getLineOfSight());

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);
            player.setExplored(t);
            reply.appendChild(t.toXMLElement(player, reply.getOwnerDocument()));
        }

        return reply;
    }


    /**
    * Handles an "embark"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param embarkElement The element containing the request.
    * @exception IllegalArgumentException If the data format of the message is invalid.
    */
    private Element embark(Connection connection, Element embarkElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(embarkElement.getAttribute("unit"));
        int direction = Integer.parseInt(embarkElement.getAttribute("direction"));
        Unit destinationUnit = (Unit) game.getFreeColGameObject(embarkElement.getAttribute("embarkOnto"));

        if (unit == null || destinationUnit == null || game.getMap().getNeighbourOrNull(direction, unit.getTile()) != destinationUnit.getTile()) {
            throw new IllegalArgumentException("Invalid data format in client message.");
        }

        Tile oldTile = unit.getTile();

        unit.embark(destinationUnit);

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    Element removeElement = Message.createNewRootElement("remove");
                    
                    Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                    removeUnit.setAttribute("ID", unit.getID());
                    removeElement.appendChild(removeUnit);

                    enemyPlayer.getConnection().send(removeElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        return null;
    }


    /**
    * Handles an "boardShip"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param boardShipElement The element containing the request.
    */
    private Element boardShip(Connection connection, Element boardShipElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(boardShipElement.getAttribute("unit"));
        Unit carrier = (Unit) game.getFreeColGameObject(boardShipElement.getAttribute("carrier"));

        Tile oldTile = unit.getTile();

        unit.boardShip(carrier);

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    Element removeElement = Message.createNewRootElement("remove");

                    Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                    removeUnit.setAttribute("ID", unit.getID());
                    removeElement.appendChild(removeUnit);

                    enemyPlayer.getConnection().send(removeElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        return null;
    }

    
    /**
    * Handles a "leaveShip"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param leaveShipElement The element containing the request.
    */
    private Element leaveShip(Connection connection, Element leaveShipElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(leaveShipElement.getAttribute("unit"));

        unit.leaveShip();
        Tile newTile = unit.getTile();

        sendUpdatedTileToAll(unit.getTile(), player);

        return null;
    }


    /**
    * Handles a "moveToEurope"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param moveToEuropeElement The element containing the request.
    */
    private Element moveToEurope(Connection connection, Element moveToEuropeElement) {
        Game game = freeColServer.getGame();
        ServerPlayer player = freeColServer.getPlayer(connection);

        Unit unit = (Unit) game.getFreeColGameObject(moveToEuropeElement.getAttribute("unit"));
        
        Tile oldTile = unit.getTile();
        unit.moveToEurope();
        
        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(oldTile)) {
                    Element removeElement = Message.createNewRootElement("remove");

                    Element removeUnit = removeElement.getOwnerDocument().createElement("removeObject");
                    removeUnit.setAttribute("ID", unit.getID());
                    removeElement.appendChild(removeUnit);

                    enemyPlayer.getConnection().send(removeElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }

        return null;
    }


    /**
    * Handles a "moveToAmerica"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element moveToAmerica(Connection connection, Element moveToAmericaElement) {
        Game game = freeColServer.getGame();

        Unit unit = (Unit) game.getFreeColGameObject(moveToAmericaElement.getAttribute("unit"));
        unit.moveToAmerica();

        return null;
    }


    /**
    * Handles a "buildColony"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element buildColony(Connection connection, Element buildColonyElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);

        String name = buildColonyElement.getAttribute("name");
        Unit unit = (Unit) freeColServer.getGame().getFreeColGameObject(buildColonyElement.getAttribute("unit"));

        if (unit.canBuildColony()) {
            Colony colony = new Colony(game, player, name, unit.getTile());

            Element reply = Message.createNewRootElement("buildColonyConfirmed");
            reply.appendChild(colony.toXMLElement(player, reply.getOwnerDocument()));

            unit.buildColony(colony);

            sendUpdatedTileToAll(unit.getTile(), player);

            return reply;
        } else {
            logger.warning("A client is requesting to build a colony, but the operation is not permitted! (unsynchronized?)");
            return null;
        }
    }


    /**
    * Handles a "recruitUnitInEurope"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element recruitUnitInEurope(Connection connection, Element recruitUnitInEuropeElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);
        Europe europe = player.getEurope();

        int slot = Integer.parseInt(recruitUnitInEuropeElement.getAttribute("slot"));
        int recruitable = europe.getRecruitable(slot);
        int newRecruitable = Unit.generateRecruitable();

        Unit unit = new Unit(game, player, recruitable);

        Element reply = Message.createNewRootElement("recruitUnitInEuropeConfirmed");
        reply.setAttribute("newRecruitable", Integer.toString(newRecruitable));
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.recruit(slot, unit, newRecruitable);

        return reply;
    }


    /**
    * Handles a "trainUnitInEurope"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element trainUnitInEurope(Connection connection, Element trainUnitInEuropeElement) {
        Game game = freeColServer.getGame();
        Player player = freeColServer.getPlayer(connection);
        Europe europe = player.getEurope();

        int unitType = Integer.parseInt(trainUnitInEuropeElement.getAttribute("unitType"));

        if (player.getGold() < Unit.getPrice(unitType)) {
            logger.warning("Received a request from a client to train a unit in europe, but the player does not have enough money!");
            return null;
        }

        Unit unit = new Unit(game, player, unitType);

        Element reply = Message.createNewRootElement("trainUnitInEuropeConfirmed");
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));

        europe.train(unit);

        return reply;
    }

    
    /**
    * Handles a "work"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param workElement The element containing the request.
    */
    private Element work(Connection connection, Element workElement) {
        Game game = freeColServer.getGame();

        Unit unit = (Unit) game.getFreeColGameObject(workElement.getAttribute("unit"));
        WorkLocation workLocation = (WorkLocation) game.getFreeColGameObject(workElement.getAttribute("workLocation"));
        
        unit.work(workLocation);

        return null;
    }


    /**
    * Handles a "putOutsideColony"-request from a client.
    *
    * @param connection The connection the message came from.
    * @param putOutsideColonyElement The element containing the request.
    */
    private Element putOutsideColony(Connection connection, Element putOutsideColonyElement) {
        Game game = freeColServer.getGame();

        Unit unit = (Unit) game.getFreeColGameObject(putOutsideColonyElement.getAttribute("unit"));
        unit.putOutsideColony();

        return null;
    }
    

    /**
    * Handles an "endTurn" notification from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element endTurn(Connection connection, Element moveElement) {
        Game game = freeColServer.getGame();

        Player nextPlayer = game.getNextPlayer();

        if (nextPlayer.equals(game.getFirstPlayer())) {
            game.newTurn();

            Element newTurnElement = Message.createNewRootElement("newTurn");
            freeColServer.getServer().sendToAll(newTurnElement, null);
        }
        
        game.setCurrentPlayer(nextPlayer);

        Element setCurrentPlayerElement = Message.createNewRootElement("setCurrentPlayer");
        setCurrentPlayerElement.setAttribute("player", nextPlayer.getID());

        freeColServer.getServer().sendToAll(setCurrentPlayerElement, null);

        return null;
    }


    private void sendUpdatedTileToAll(Tile newTile, Player player) {
        Game game = freeColServer.getGame();

        Iterator enemyPlayerIterator = game.getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();

            if (player.equals(enemyPlayer)) {
                continue;
            }

            try {
                if (enemyPlayer.canSee(newTile)) {
                    Element updateElement = Message.createNewRootElement("update");
                    updateElement.appendChild(newTile.toXMLElement(enemyPlayer, updateElement.getOwnerDocument()));

                    enemyPlayer.getConnection().send(updateElement);
                }
            } catch (IOException e) {
                logger.warning("Could not send message to: " + enemyPlayer.getName() + " with connection " + enemyPlayer.getConnection());
            }
        }
    }
}
