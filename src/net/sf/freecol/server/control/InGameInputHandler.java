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

package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.CombatModel.CombatResultType;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.DebugForeignColonyMessage;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GiveIndependenceMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.StatisticsMessage;
import net.sf.freecol.common.networking.UpdateCurrentStopMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles the network messages that arrives while
 * {@link FreeColServer#IN_GAME in game}.
 */
public final class InGameInputHandler extends InputHandler implements NetworkConstants {

    private static Logger logger = Logger.getLogger(InGameInputHandler.class.getName());

    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InGameInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
        // TODO: move and simplify methods later, for now just delegate
        register("createUnit", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return createUnit(connection, element);
            }
        });
        register("createBuilding", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return createBuilding(connection, element);
            }
        });
        register("getRandom", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getRandom(connection, element);
            }
        });
        register("getVacantEntryLocation", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getVacantEntryLocation(connection, element);
            }
        });
        register(SetDestinationMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return new SetDestinationMessage(getGame(), element).handle(freeColServer, connection);
            }
        });
        register(MoveMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new MoveMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("askSkill", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return askSkill(connection, element);
            }
        });
        register("attack", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return attack(connection, element);
            }
        });
        register(EmbarkMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new EmbarkMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("learnSkillAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return learnSkillAtSettlement(connection, element);
            }
        });
        register("scoutIndianSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return scoutIndianSettlement(connection, element);
            }
        });
        register("missionaryAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return missionaryAtSettlement(connection, element);
            }
        });
        register("inciteAtSettlement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return inciteAtSettlement(connection, element);
            }
        });
        register(DemandTributeMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new DemandTributeMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(DisembarkMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new DisembarkMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("loadCargo", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return loadCargo(connection, element);
            }
        });
        register("unloadCargo", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return unloadCargo(connection, element);
            }
        });
        register("buyGoods", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return buyGoods(connection, element);
            }
        });
        register("sellGoods", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return sellGoods(connection, element);
            }
        });
        register("moveToEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return moveToEurope(connection, element);
            }
        });
        register("moveToAmerica", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return moveToAmerica(connection, element);
            }
        });
        register(BuildColonyMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new BuildColonyMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(JoinColonyMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new JoinColonyMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("recruitUnitInEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return recruitUnitInEurope(connection, element);
            }
        });
        register(EmigrateUnitMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new EmigrateUnitMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("trainUnitInEurope", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return trainUnitInEurope(connection, element);
            }
        });
        register("equipUnit", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return equipUnit(connection, element);
            }
        });
        register("work", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return work(connection, element);
            }
        });
        register("changeWorkType", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return changeWorkType(connection, element);
            }
        });
        register("workImprovement", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return workImprovement(connection, element);
            }
        });
        register("setBuildQueue", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return setBuildQueue(connection, element);
            }
        });
        register("changeState", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return changeState(connection, element);
            }
        });
        register("putOutsideColony", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return putOutsideColony(connection, element);
            }
        });
        register("clearSpeciality", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return clearSpeciality(connection, element);
            }
        });
        register(NewLandNameMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new NewLandNameMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(NewRegionNameMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new NewRegionNameMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("endTurn", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return endTurn(connection, element);
            }
        });
        register("disbandUnit", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return disbandUnit(connection, element);
            }
        });
        register(CashInTreasureTrainMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new CashInTreasureTrainMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(GetTransactionMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new GetTransactionMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(CloseTransactionMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new CloseTransactionMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(GoodsForSaleMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new GoodsForSaleMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(BuyPropositionMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new BuyPropositionMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(SellPropositionMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new SellPropositionMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(BuyMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new BuyMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(SellMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new SellMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(DeliverGiftMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new DeliverGiftMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("indianDemand", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return indianDemand(connection, element);
            }
        });
        register(ClaimLandMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new ClaimLandMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("payForBuilding", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return payForBuilding(connection, element);
            }
        });
        register("payArrears", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return payArrears(connection, element);
            }
        });
        register("setGoodsLevels", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return setGoodsLevels(connection, element);
            }
        });
        register(DeclareIndependenceMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new DeclareIndependenceMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register(GiveIndependenceMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new GiveIndependenceMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("foreignAffairs", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return foreignAffairs(connection, element);
            }
        });
        register("highScores", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return highScores(connection, element);
            }
        });
        register("getREFUnits", new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return getREFUnits(connection, element);
            }
        });
        register(RenameMessage.getXMLElementTagName(), new CurrentPlayerNetworkRequestHandler() {
            @Override
            public Element handle(Player player, Connection connection, Element element) {
                return new RenameMessage(getGame(), element).handle(freeColServer, player, connection);
            }
        });
        register("getNewTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getNewTradeRoute(connection, element);
            }
        });
        register("updateTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateTradeRoute(connection, element);
            }
        });
        register("setTradeRoutes", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return setTradeRoutes(connection, element);
            }
        });
        register("assignTradeRoute", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return assignTradeRoute(connection, element);
            }
        });
        register(UpdateCurrentStopMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return new UpdateCurrentStopMessage(getGame(), element).handle(freeColServer, connection);
            }
        });
        register(DiplomacyMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return new DiplomacyMessage(getGame(), element).handle(freeColServer, connection);
            }
        });
        register(SpySettlementMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return new SpySettlementMessage(getGame(), element).handle(freeColServer, connection);
            }
        });
        register(DebugForeignColonyMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return new DebugForeignColonyMessage(getGame(), element).handle(freeColServer, connection);
            }
        });
        register("abandonColony", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return abandonColony(connection, element);
            }
        });
        register("continuePlaying", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return continuePlaying(connection, element);
            }
        });
        register("assignTeacher", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return assignTeacher(connection, element);
            }
        });
        register(StatisticsMessage.getXMLElementTagName(), new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return getServerStatistics(connection, element);
            }
        });
        register("retire", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return retire(connection, element);
            }
        });
    }


    // TODO: Remove these when their local users migrate out.
    // There are copies in InGameController.
    /**
     * Get a list of all server players, optionally excluding the supplied one.
     *
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     * @return A list of all connected server players except one.
     */
    private List<ServerPlayer> getOtherPlayers(ServerPlayer serverPlayer) {
        List<ServerPlayer> result = new ArrayList<ServerPlayer>();
        for (Player otherPlayer : getGame().getPlayers()) {
            ServerPlayer enemyPlayer = (ServerPlayer) otherPlayer;
            if (!enemyPlayer.equals(serverPlayer)
                && enemyPlayer.isConnected()) {
                result.add(enemyPlayer);
            }
        }
        return result;
    }

    /**
     * Tell all players to remove a unit, optionally excluding one.
     *
     * @param unit The <code>Unit</code> to remove.
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     */
    private void sendRemoveUnitToAll(Unit unit, ServerPlayer serverPlayer) {
        Element remove = Message.createNewRootElement("remove");
        unit.addToRemoveElement(remove);
        for (ServerPlayer enemyPlayer : getOtherPlayers(serverPlayer)) {
            if (unit.isVisibleTo(enemyPlayer)) {
                try {
                    enemyPlayer.getConnection().sendAndWait(remove);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    /**
     * Tell all players to update a tile, optionally excluding one.
     *
     * @param newTile The <code>Tile</code> to update.
     * @param serverPlayer A <code>ServerPlayer</code> to exclude (may be null).
     */
    private void sendUpdatedTileToAll(Tile newTile, ServerPlayer serverPlayer) {
        for (ServerPlayer enemyPlayer : getOtherPlayers(serverPlayer)) {
            if (enemyPlayer.canSee(newTile)) {
                Element update = Message.createNewRootElement("update");
                Document doc = update.getOwnerDocument();
                update.appendChild(newTile.toXMLElement(enemyPlayer, doc));
                try {
                    enemyPlayer.getConnection().sendAndWait(update);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }


    /**
     * Handles a "createUnit"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element createUnit(Connection connection, Element element) {
        logger.info("Receiving \"createUnit\"-request.");
        String taskID = element.getAttribute("taskID");
        Location location = (Location) getGame().getFreeColGameObject(element.getAttribute("location"));
        Player owner = (Player) getGame().getFreeColGameObject(element.getAttribute("owner"));
        UnitType type = FreeCol.getSpecification().getUnitType(element.getAttribute("type"));
        if (location == null) {
            throw new NullPointerException();
        }
        if (owner == null) {
            throw new NullPointerException();
        }
        Unit unit = getFreeColServer().getModelController()
                .createUnit(taskID, location, owner, type, false, connection);
        Element reply = Message.createNewRootElement("createUnitConfirmed");
        reply.appendChild(unit.toXMLElement(owner, reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "createBuilding"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element createBuilding(Connection connection, Element element) {
        logger.info("Receiving \"createBuilding\"-request.");
        String taskID = element.getAttribute("taskID");
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        BuildingType type = FreeCol.getSpecification().getBuildingType(element.getAttribute("type"));
        if (colony == null) {
            throw new NullPointerException();
        }
        Building building = getFreeColServer().getModelController()
                .createBuilding(taskID, colony, type, false, connection);
        Element reply = Message.createNewRootElement("createBuildingConfirmed");
        reply.appendChild(building.toXMLElement(colony.getOwner(), reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "getRandom"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getRandom(Connection connection, Element element) {
        //logger.info("Receiving \"getRandom\"-request.");
        String taskID = element.getAttribute("taskID");
        int n = Integer.parseInt(element.getAttribute("n"));
        int result = getFreeColServer().getModelController().getRandom(taskID, n);
        Element reply = Message.createNewRootElement("getRandomConfirmed");
        reply.setAttribute("result", Integer.toString(result));
        //logger.info("Result: " + result);
        return reply;
    }

    /**
     * Handles a "getVacantEntryLocation"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getVacantEntryLocation(Connection connection, Element element) {
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Player owner = unit.getOwner();
        ServerPlayer askingPlayer = getFreeColServer().getPlayer(connection);
        Location entryLocation = unit.getEntryLocation();
        if (owner != askingPlayer) {
            /**
             * WARNING: this is a gruesome hack to prevent a game
             * crash when the client tries to move AI units. As this
             * should never happen, we need to find out why it does.
             */
            if (entryLocation == null) {
                throw new IllegalStateException("Unit " + unit.getId() + " with owner " + owner
                                                + " not owned by " + askingPlayer
                                                + ", refusing to get vacant location!");
            } else {
                logger.warning("Unit " + unit.getId() + " with owner " + owner
                               + " not owned by " + askingPlayer
                               + ", entry location is " + entryLocation.getId());
            }
        } else {
            entryLocation = getFreeColServer().getModelController().setToVacantEntryLocation(unit);
        }
        Element reply = Message.createNewRootElement("getVacantEntryLocationConfirmed");
        reply.setAttribute("location", entryLocation.getId());
        return reply;
    }

    /**
     * Handles a "getNewTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element getNewTradeRoute(Connection connection, Element element) {
        Player player = getFreeColServer().getPlayer(connection);
        TradeRoute tradeRoute = getFreeColServer().getModelController().getNewTradeRoute(player);
        Element reply = Message.createNewRootElement("getNewTradeRouteConfirmed");
        reply.appendChild(tradeRoute.toXMLElement(player, reply.getOwnerDocument()));
        return reply;
    }

    /**
     * Handles a "setTradeRoutes"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element updateTradeRoute(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element childElement = (Element) element.getChildNodes().item(0);
        TradeRoute clientTradeRoute = new TradeRoute(null, childElement);
        TradeRoute serverTradeRoute = (TradeRoute) getGame().getFreeColGameObject(clientTradeRoute.getId());
        if (serverTradeRoute == null) {
            throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: "
                    + clientTradeRoute.getId());
        }
        if (serverTradeRoute.getOwner() != player) {
            throw new IllegalStateException("Not your trade route!");
        }
        serverTradeRoute.updateFrom(clientTradeRoute);
        return null;
    }

    /**
     * Handles a "updateTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element setTradeRoutes(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        ArrayList<TradeRoute> routes = new ArrayList<TradeRoute>();
        
        NodeList childElements = element.getChildNodes();
        for(int i = 0; i < childElements.getLength(); i++) {
            Element childElement = (Element) childElements.item(i);
            String id = childElement.getAttribute("id");
            TradeRoute serverTradeRoute = (TradeRoute) getGame().getFreeColGameObject(id);
            if (serverTradeRoute == null) {
                throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: " + id);
            }
            if (serverTradeRoute.getOwner() != player) {
                throw new IllegalStateException("Not your trade route!");
            }
            routes.add(serverTradeRoute);
        }
        player.setTradeRoutes(routes);
        return null;
    }

    /**
     * Handles a "assignTradeRoute"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element assignTradeRoute(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));

        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        } else if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        String tradeRouteString = element.getAttribute("tradeRoute");

        if (tradeRouteString == null || tradeRouteString == "") {
            unit.setTradeRoute(null);
        } else {
            TradeRoute tradeRoute = (TradeRoute) getGame().getFreeColGameObject(tradeRouteString);

            if (tradeRoute == null) {
                throw new IllegalArgumentException("Could not find 'TradeRoute' with specified ID: "
                                                   + element.getAttribute("tradeRoute"));
            }
            if (tradeRoute.getOwner() != player) {
                throw new IllegalStateException("Not your trade route!");
            }
            unit.setTradeRoute(tradeRoute);
        }
        return null;
    }

    /**
     * Handles an "abandonColony"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param abandonElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element abandonColony(Connection connection, Element abandonElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        Colony colony = (Colony) getGame().getFreeColGameObject(abandonElement.getAttribute("colony"));
        // Test the parameters:
        if (colony == null) {
            throw new IllegalArgumentException("Could not find 'Colony' with specified ID: "
                    + abandonElement.getAttribute("colony"));
        }
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
        }

        colony.getOwner().getHistory()
            .add(new HistoryEvent(colony.getGame().getTurn().getNumber(),
                                  HistoryEvent.Type.ABANDON_COLONY,
                                  "%colony%", colony.getName()));

        Tile tile = colony.getTile();
        // TODO: modify/abort trade routes?
        colony.dispose();
        sendUpdatedTileToAll(tile, player);
        return null;
    }

    /**
     * Handles an "askSkill"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element askSkill(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() == 0) {
            throw new IllegalArgumentException("Unit has no moves left.");
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();
        
        unit.setMovesLeft(0);
        Element reply = Message.createNewRootElement("provideSkill");
        if (settlement.getLearnableSkill() != null) {
            reply.setAttribute("skill", settlement.getLearnableSkill().getId());
        }
        // Set the Tile.PlayerExploredTile attribute.
        settlement.getTile().updateIndianSettlementSkill(player);
        return reply;
    }

    /**
     * Handles an "attack"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param attackElement The element containing the request.
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element attack(Connection connection, Element attackElement) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer player = freeColServer.getPlayer(connection);
        // Get parameters:
        String unitID = attackElement.getAttribute("unit");
        Unit unit = (Unit) getGame().getFreeColGameObject(unitID);
        Direction direction = Enum.valueOf(Direction.class, attackElement.getAttribute("direction"));
        // Test the parameters:
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: " + unitID);
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' is not on the map: " + unit.toString());
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile newTile = getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile in direction " + direction + " from unit with ID "
                    + unitID);
        }
        CombatResult result;
        int plunderGold = -1;
        Unit defender = newTile.getDefendingUnit(unit);
        Player defendingPlayer = null;
        if (defender == null) {
            if (newTile.getSettlement() != null) {
                defendingPlayer = newTile.getSettlement().getOwner();
                result = new CombatResult(CombatResultType.DONE_SETTLEMENT, 0);
            } else {
                throw new IllegalStateException("Nothing to attack in direction " + direction + " from unit with ID "
                        + unitID);
            }
        } else {
            defendingPlayer = defender.getOwner();
            result = unit.getGame().getCombatModel().generateAttackResult(unit, defender); 
        }
        if (result.type == CombatResultType.DONE_SETTLEMENT) {
            Settlement s = newTile.getSettlement();
            if (s instanceof Colony) {
                //colony: take amount proportional to colony-size/overall-colony-population
                plunderGold = (s.getOwner().getGold()*s.getUnitCount())/s.getOwner().getColoniesPopulation();
            } else {
                //indian settlement: 10% of their gold
                plunderGold = s.getOwner().getGold() / 10;
            }
        }
        
        // Gets repair location if necessary
        Location repairLocation = null;
        
        Player loserOwner = null;
        switch (result.type) {
        case WIN:
            if (defender.isNaval()) {
                loserOwner = defendingPlayer;
                repairLocation = loserOwner.getRepairLocation(defender);
            }
            break;
        case DONE_SETTLEMENT:
            for (Unit victim : newTile.getUnitList()) {
                if (victim.isNaval()) {
                    loserOwner = victim.getOwner();
                    repairLocation = loserOwner.getRepairLocation(victim);
                    break;
                }
            }
            break;
        case LOSS:
            if (unit.isNaval()) {
                loserOwner = player;
                repairLocation = loserOwner.getRepairLocation(unit);
            }
            break;
        case EVADES:
        case GREAT_LOSS:
        case GREAT_WIN:
            // Nothing special to do here.
            break;
        }
        
        // Inform the players (other then the player attacking) about
        // the attack:
        for (ServerPlayer enemyPlayer : getOtherPlayers(player)) {
            Element opponentAttackElement = Message.createNewRootElement("opponentAttack");
            if (unit.isVisibleTo(enemyPlayer) || defender.isVisibleTo(enemyPlayer)) {
                opponentAttackElement.setAttribute("direction", direction.toString());
                opponentAttackElement.setAttribute("result", result.type.toString());
                opponentAttackElement.setAttribute("damage", String.valueOf(result.damage));
                opponentAttackElement.setAttribute("plunderGold", Integer.toString(plunderGold));
                opponentAttackElement.setAttribute("unit", unit.getId());
                opponentAttackElement.setAttribute("defender", defender.getId());
                
                if (defender.getOwner() == enemyPlayer) {
                	// Naval battle, defender lost, needs repair location
                	if(repairLocation != null && loserOwner == defender.getOwner()){
                		opponentAttackElement.setAttribute("repairIn", repairLocation.getId());
                	}
                    // always update the attacker, defender needs its location
                	opponentAttackElement.setAttribute("update", "unit");
                	//Note: We should not send every info on the unit to the enemy player
                    opponentAttackElement.appendChild(unit.toXMLElement(enemyPlayer,
                            opponentAttackElement.getOwnerDocument(),false,false));
                } else if (!defender.isVisibleTo(enemyPlayer)) {
                    opponentAttackElement.setAttribute("update", "defender");
                    /*
                     * We need to send the ID of the Tile, since the unit
                     * may be inside a (hidden) ColonyTile:
                     */
                    opponentAttackElement.setAttribute("defenderTile", defender.getTile().getId());
                    if (!enemyPlayer.canSee(defender.getTile())) {
                        enemyPlayer.setExplored(defender.getTile());
                        opponentAttackElement.appendChild(defender.getTile()
                            .toXMLElement(enemyPlayer, opponentAttackElement.getOwnerDocument()));
                    }
                	//Note: We should not send every info on the unit to the player
                	// Ex: defender is in Colony Tile, player does not (and should not) have access to
                	// this info; with showAll=false, only the necessary info is sent
                    opponentAttackElement.appendChild(defender.toXMLElement(enemyPlayer,
                            opponentAttackElement.getOwnerDocument(),false,false));
                } else if (!unit.isVisibleTo(enemyPlayer)) {
                	//Note: We should not send every info on the unit to the player
                	// Ex: defender is in Colony Tile, player does not (and should not) have access to
                	// this info; with showAll=false, only the necessary info is sent
                    opponentAttackElement.setAttribute("update", "unit");
                    Element unitElm = unit.toXMLElement(enemyPlayer,opponentAttackElement.getOwnerDocument(),false,false);
                    opponentAttackElement.appendChild(unitElm);
                }
                try {
                    enemyPlayer.getConnection().sendAndWait(opponentAttackElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + enemyPlayer.getName()
                                   + " with connection " + enemyPlayer.getConnection());
                }
            }
        }
        // Create the reply for the attacking player:
        Element reply = Message.createNewRootElement("attackResult");
        reply.setAttribute("result", result.type.toString());
        reply.setAttribute("damage", String.valueOf(result.damage));
        reply.setAttribute("plunderGold", Integer.toString(plunderGold));
        
        // Naval battle, attacker lost, needs repair location
        if(repairLocation != null && player == loserOwner){
        	reply.setAttribute("repairIn", repairLocation.getId());
        }
        
        if (result.type == CombatResultType.DONE_SETTLEMENT && newTile.getColony() != null) {
            // If a colony will been won, send an updated tile:
            reply.appendChild(newTile.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
            reply.appendChild(defender.toXMLElement(newTile.getColony().getOwner(), reply.getOwnerDocument()));
        } else {
        	//Note: We should not send every info on the unit to the player
        	// Ex: defender is in Colony Tile, player does not (and should not) have access to
        	// this info; with showAll=false, only the necessary info is sent
            reply.appendChild(defender.toXMLElement(player, reply.getOwnerDocument(), false, false));
        }
        
        // Destroyed settlement was an indian capital, indians surrender
        // add capital burned flag
        boolean isIndianCapitalBurned=false;
        if(result.type == CombatResultType.DONE_SETTLEMENT && 
        		newTile.getSettlement() instanceof IndianSettlement &&
        		((IndianSettlement) newTile.getSettlement()).isCapital()) {
        	isIndianCapitalBurned = true;
        	reply.setAttribute("indianCapitalBurned", Boolean.toString(isIndianCapitalBurned));
        }
        
        int oldUnits = unit.getTile().getUnitCount();
        
        // update server info
        unit.getGame().getCombatModel().attack(unit, defender, result, plunderGold, repairLocation);
        if(isIndianCapitalBurned){
        	defendingPlayer.surrenderTo(player);
        }
        
        if (result.type.compareTo(CombatResultType.WIN) >= 0 
            && unit.getTile() != newTile
            && oldUnits < unit.getTile().getUnitCount()) {
            // If unit won, didn't move, there are more units,
            // then if the last one is not European it must be a convert
            // (not a combat captive), so send it
            Unit lastUnit = unit.getTile().getLastUnit();
            if (!lastUnit.getOwner().isEuropean()) {
                Element convertElement = reply.getOwnerDocument().createElement("convert");
                convertElement.appendChild(lastUnit.toXMLElement(unit.getOwner(), reply.getOwnerDocument()));
                reply.appendChild(convertElement);
            }
        }
        
        if (result.type.compareTo(CombatResultType.EVADES) >= 0 && unit.getTile().equals(newTile)) {
            // In other words, we moved...
            Element update = reply.getOwnerDocument().createElement("update");
            int lineOfSight = unit.getLineOfSight();
            if (result.type == CombatResultType.DONE_SETTLEMENT && newTile.getSettlement() != null) {
                lineOfSight = Math.max(lineOfSight, newTile.getSettlement().getLineOfSight());
            }
            List<Tile> surroundingTiles = getGame().getMap().getSurroundingTiles(unit.getTile(), lineOfSight);
            for (int i = 0; i < surroundingTiles.size(); i++) {
                Tile t = surroundingTiles.get(i);
                update.appendChild(t.toXMLElement(player, update.getOwnerDocument()));
            }
            update.appendChild(unit.getTile().toXMLElement(player, update.getOwnerDocument()));
            reply.appendChild(update);
        }
        return reply;
    }

    /**
     * Handles a "learnSkillAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element learnSkillAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        boolean cancelAction = false;
        if (element.getAttribute("action").equals("cancel")) {
            cancelAction = true;
        }
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("'Unit' not on map: ID: " + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        IndianSettlement settlement = (IndianSettlement) tile.getSettlement();
        if (settlement == null) {
            throw new IllegalStateException("No settlement to learn skill from.");
        }
        if (!unit.getType().canBeUpgraded(settlement.getLearnableSkill(), ChangeType.NATIVES)) {
            throw new IllegalStateException("Unit can't learn that skill from settlement!");
        }
        
        Element reply = Message.createNewRootElement("learnSkillResult");
        if (!cancelAction) {
            Tension tension = settlement.getAlarm(player);
            if (tension == null) {
                tension = new Tension(0);
            }
            switch (tension.getLevel()) {
            case HATEFUL:
                reply.setAttribute("result", "die");
                unit.dispose();
                break;
            case ANGRY:
                reply.setAttribute("result", "leave");
                break;
            default:
                unit.learnFromIndianSettlement(settlement);
                // Set the Tile.PlayerExploredTile attribute.
                settlement.getTile().updateIndianSettlementSkill(player);
                reply.setAttribute("result", "success");
            }
        } else {
            reply.setAttribute("result", "cancelled");
        }
        return reply;
    }

    /**
     * Handles a "scoutIndianSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element scoutIndianSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String action = element.getAttribute("action");
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile()).getSettlement();
        Element reply = Message.createNewRootElement("scoutIndianSettlementResult");
        if (action.equals("basic")) {
            unit.setMovesLeft(0);
            // Just return the skill and wanted goods.
            UnitType skill = settlement.getLearnableSkill();
            if (skill != null) {
                reply.setAttribute("skill", skill.getId());
            }
            settlement.updateWantedGoods();
            GoodsType[] wantedGoods = settlement.getWantedGoods();
            reply.setAttribute("highlyWantedGoods", wantedGoods[0].getId());
            reply.setAttribute("wantedGoods1", wantedGoods[1].getId());
            reply.setAttribute("wantedGoods2", wantedGoods[2].getId());
            reply.setAttribute("numberOfCamps", String.valueOf(settlement.getOwner().getSettlements().size()));
            for (Tile tile : getGame().getMap().getSurroundingTiles(settlement.getTile(), unit.getLineOfSight())) {
                reply.appendChild(tile.toXMLElement(player, reply.getOwnerDocument()));
            }
            // Set the Tile.PlayerExploredTile attribute.
            settlement.getTile().updateIndianSettlementInformation(player);
        } else if (action.equals("cancel")) {
            return null;
        } else if (action.equals("attack")) {
            // The movesLeft has been set to 0 when the scout
            // initiated its action.  If it wants to attack then it
            // can and it will need some moves to do it.
            unit.setMovesLeft(1);
            return null;
        } else if (settlement.getAlarm(player) != null &&
                   settlement.getAlarm(player).getLevel() == Tension.Level.HATEFUL) {
            reply.setAttribute("result", "die");
            unit.dispose();
        } else if (action.equals("speak")) {
            if (!settlement.hasBeenVisited()) {
                if (settlement.getLearnableSkill() != null
                    && settlement.getLearnableSkill().hasAbility("model.ability.expertScout")
                    && !unit.hasAbility("model.ability.expertScout")) {
                    unit.setType(settlement.getLearnableSkill());
                    reply.setAttribute("result", "expert");
                    Element update = reply.getOwnerDocument().createElement("update");
                    update.appendChild(unit.toXMLElement(player, update.getOwnerDocument(), false, false));
                    reply.appendChild(update);
                } else if (getPseudoRandom().nextInt(9) < 3) {
                    reply.setAttribute("result", "tales");
                    Element update = reply.getOwnerDocument().createElement("update");
                    Position center = new Position(settlement.getTile().getX(), settlement.getTile().getY());
                    Iterator<Position> circleIterator = map.getCircleIterator(center, true, 6);
                    while (circleIterator.hasNext()) {
                        Position position = circleIterator.next();
                        if ((!position.equals(center))
                                && (map.getTile(position).isLand() || map.getTile(position).isCoast())) {
                            Tile t = map.getTile(position);
                            player.setExplored(t);
                            update.appendChild(t.toXMLElement(player, update.getOwnerDocument(), false, false));
                        }
                    }
                    reply.appendChild(update);
                } else {
                    int beadsGold = (getPseudoRandom().nextInt(400) * settlement.getBonusMultiplier()) + 50;
                    if (unit.hasAbility("model.ability.expertScout")) {
                        beadsGold = (beadsGold * 11) / 10;
                    }
                    reply.setAttribute("result", "beads");
                    reply.setAttribute("amount", Integer.toString(beadsGold));
                    player.modifyGold(beadsGold);
                }
                settlement.setVisited(player);
            } else {
                reply.setAttribute("result", "nothing");
            }
        }
        return reply;
    }

    /**
     * Handles a "missionaryAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element missionaryAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        InGameController inGameController = freeColServer.getInGameController();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String action = element.getAttribute("action");
        IndianSettlement settlement = (IndianSettlement) map.getNeighbourOrNull(direction, unit.getTile())
                .getSettlement();
        unit.setMovesLeft(0);
        if (action.equals("cancel")) {
            return null;
        } else if (action.equals("establish")) {
            sendRemoveUnitToAll(unit, player);
            
            boolean success = inGameController.createMission(settlement,unit);
            
        	Element reply = Message.createNewRootElement("missionaryReply");
        	reply.setAttribute("success", String.valueOf(success));
        	reply.setAttribute("tension", settlement.getAlarm(unit.getOwner()).getLevel().toString());
            return reply;
        } else if (action.equals("heresy")) {
            Element reply = Message.createNewRootElement("missionaryReply");
            sendRemoveUnitToAll(unit, player);
            double random = Math.random() * settlement.getMissionary().getOwner().getImmigration() /
                (unit.getOwner().getImmigration() + 1);
            if (settlement.getMissionary().hasAbility("model.ability.expertMissionary")) {
                random += 0.2;
            }
            if (unit.hasAbility("model.ability.expertMissionary")) {
                random -= 0.2;
            }
            if (random < 0.5) {
            	boolean success = inGameController.createMission(settlement,unit);
            	reply.setAttribute("success", String.valueOf(success));
            	reply.setAttribute("tension", settlement.getAlarm(unit.getOwner()).getLevel().toString());    
            } else {
                reply.setAttribute("success", "false");
                unit.dispose();
            }
            return reply;
        } else if (action.equals("incite")) {
            Element reply = Message.createNewRootElement("missionaryReply");
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("incite"));
            reply.setAttribute("amount", String.valueOf(Game.getInciteAmount(player, enemy, settlement.getOwner())));
            // Move the unit into the settlement while we wait for the client's
            // response.
            unit.setLocation(settlement);
            return reply;
        } else {
            return null;
        }
    }

    /**
     * Handles a "inciteAtSettlement"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element inciteAtSettlement(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        Map map = getGame().getMap();
        ServerPlayer player = freeColServer.getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Direction direction = Enum.valueOf(Direction.class, element.getAttribute("direction"));
        String confirmed = element.getAttribute("confirmed");
        IndianSettlement settlement = (IndianSettlement) unit.getTile().getSettlement();
        // Move the unit back to its original Tile.
        unit.setLocation(map.getNeighbourOrNull(direction.getReverseDirection(), unit.getTile()));
        if (confirmed.equals("true")) {
            Player enemy = (Player) getGame().getFreeColGameObject(element.getAttribute("enemy"));
            int amount = Game.getInciteAmount(player, enemy, settlement.getOwner());
            if (player.getGold() < amount) {
                throw new IllegalStateException("Not enough gold to incite indians!");
            } else {
                player.modifyGold(-amount);
            }
            // Set the indian player at war with the european player (and vice
            // versa).
            settlement.getOwner().changeRelationWithPlayer(enemy, Stance.WAR);
            // Increase tension levels:
            settlement.modifyAlarm(enemy, 1000); // let propagation works
            enemy.modifyTension(settlement.getOwner(), 500);
            enemy.modifyTension(player, 250);
        }
        // else: no need to do anything: unit's moves are already zero.
        return null;
    }

    /**
     * Handles a "loadCargo"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param loadCargoElement The element containing the request.
     */
    private Element loadCargo(Connection connection, Element loadCargoElement) {
        Unit carrier = (Unit) getGame().getFreeColGameObject(loadCargoElement.getAttribute("carrier"));
        Goods goods = new Goods(getGame(), (Element) loadCargoElement.getChildNodes().item(0));
        goods.loadOnto(carrier);
        return null;
    }

    /**
     * Handles an "unloadCargo"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param unloadCargoElement The element containing the request.
     */
    private Element unloadCargo(Connection connection, Element unloadCargoElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Goods goods = new Goods(getGame(), (Element) unloadCargoElement.getChildNodes().item(0));
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getColony() != null) {
            goods.unload();
        } else {
            goods.setLocation(null);
        }
        return null;
    }

    /**
     * Handles a "buyGoods"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param buyGoodsElement The element containing the request.
     */
    private Element buyGoods(Connection connection, Element buyGoodsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit carrier = (Unit) getGame().getFreeColGameObject(buyGoodsElement.getAttribute("carrier"));
        GoodsType type = FreeCol.getSpecification().getGoodsType(buyGoodsElement.getAttribute("type"));
        int amount = Integer.parseInt(buyGoodsElement.getAttribute("amount"));
        if (carrier.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (carrier.getOwner() != player) {
            throw new IllegalStateException();
        }
        carrier.buyGoods(type, amount);
       
        Element marketElement = Message.createNewRootElement("marketElement");
        marketElement.setAttribute("type", type.getId());
        marketElement.setAttribute("amount", String.valueOf(-amount/4));
        getFreeColServer().getServer().sendToAll(marketElement, player.getConnection());
        return null;
    }

    /**
     * Handles a "sellGoods"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param sellGoodsElement The element containing the request.
     */
    private Element sellGoods(Connection connection, Element sellGoodsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Goods goods = new Goods(getGame(), (Element) sellGoodsElement.getChildNodes().item(0));
        if (goods.getLocation() instanceof Unit && ((Unit) goods.getLocation()).getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        player.getMarket().sell(goods, player);

        Element marketElement = Message.createNewRootElement("marketElement");
        marketElement.setAttribute("type", goods.getType().getId());
        marketElement.setAttribute("amount", String.valueOf(goods.getAmount()/4));
        getFreeColServer().getServer().sendToAll(marketElement, player.getConnection());
        return null;
    }

    /**
     * Handles a "moveToEurope"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param moveToEuropeElement The element containing the request.
     */
    private Element moveToEurope(Connection connection, Element moveToEuropeElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(moveToEuropeElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        // Inform other players the unit is moving off the map
        sendRemoveUnitToAll(unit, player);
        
        Tile oldTile = unit.getTile();
        unit.moveToEurope();
        return null;
    }

    /**
     * Handles a "moveToAmerica"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param moveToAmericaElement The element containing the request.
     */
    private Element moveToAmerica(Connection connection, Element moveToAmericaElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(moveToAmericaElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.moveToAmerica();
        return null;
    }

    /**
     * Handles a "recruitUnitInEurope"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param recruitUnitInEuropeElement The element containing the request.
     */
    private Element recruitUnitInEurope(Connection connection, Element recruitUnitInEuropeElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();
        int slot = Integer.parseInt(recruitUnitInEuropeElement.getAttribute("slot"));
        UnitType recruitable = europe.getRecruitable(slot);
        UnitType newRecruitable = player.generateRecruitable(player.getId() + "slot." + Integer.toString(slot));
        Unit unit = new Unit(getGame(), europe, player, recruitable, UnitState.ACTIVE, recruitable.getDefaultEquipment());
        Element reply = Message.createNewRootElement("recruitUnitInEuropeConfirmed");
        reply.setAttribute("newRecruitable", newRecruitable.getId());
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        europe.recruit(slot, unit, newRecruitable);
        return reply;
    }

    /**
     * Handles a "trainUnitInEurope"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param trainUnitInEuropeElement The element containing the request.
     */
    private Element trainUnitInEurope(Connection connection, Element trainUnitInEuropeElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Europe europe = player.getEurope();
        String unitId = trainUnitInEuropeElement.getAttribute("unitType");
        UnitType unitType = FreeCol.getSpecification().getUnitType(unitId);
        Unit unit = new Unit(getGame(), europe, player, unitType, UnitState.ACTIVE, unitType.getDefaultEquipment());
        Element reply = Message.createNewRootElement("trainUnitInEuropeConfirmed");
        reply.appendChild(unit.toXMLElement(player, reply.getOwnerDocument()));
        europe.train(unit);
        return reply;
    }

    /**
     * Handles a "equipUnit"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element equipUnit(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        String typeString = workElement.getAttribute("type");
        EquipmentType type = FreeCol.getSpecification().getEquipmentType(typeString);
        int amount = Integer.parseInt(workElement.getAttribute("amount"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        if (amount > 0) {
            unit.equipWith(type, amount);
        } else {
            unit.removeEquipment(type, -amount);
        }

        if (unit.getLocation() instanceof Tile) {
            sendUpdatedTileToAll(unit.getTile(), player);
        }
        return null;
    }

    /**
     * Handles a "work"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element work(Connection connection, Element workElement) {
        ServerPlayer serverPlayer = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        WorkLocation workLocation = (WorkLocation) getGame().getFreeColGameObject(workElement.getAttribute("workLocation"));
        if (unit.getOwner() != serverPlayer) {
            throw new IllegalStateException("Not your unit!");
        }
        if (workLocation == null) {
            throw new NullPointerException();
        }
        if (!workLocation.canAdd(unit)) {
            throw new IllegalStateException("Can not add " + unit.getName() + "(" + unit.getId()
                                            + ") to " + workLocation.toString() + "(" 
                                            + workLocation.getId() + ")");
        }
        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            Colony colony = workLocation.getColony();
            if (tile.getOwningSettlement() != colony) {
                // Claim known free land (because canAdd() succeeded).
                serverPlayer.claimLand(tile, colony, 0);
            }
        }

        Location oldLocation = unit.getLocation();
        unit.work(workLocation);
        // For updating the number of colonist:
        sendUpdatedTileToAll(unit.getTile(), serverPlayer);
        // oldLocation is empty now
        if (oldLocation instanceof ColonyTile) {
            sendUpdatedTileToAll(((ColonyTile) oldLocation).getWorkTile(), serverPlayer);
        }
        // workLocation is occupied now
        if (workLocation instanceof ColonyTile) {
            sendUpdatedTileToAll(((ColonyTile) workLocation).getWorkTile(), serverPlayer);
        }
        return null;
    }

    /**
     * Handles a "changeWorkType"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element changeWorkType(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }

        String workTypeString = workElement.getAttribute("workType");
        if (workTypeString != null) {
            GoodsType workType = FreeCol.getSpecification().getGoodsType(workTypeString);
            // No reason to send an update to other players: this is always hidden.
            unit.setWorkType(workType);

        }
        return null;

    }

    /**
     * Handles a "changeWorkType"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element workImprovement(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile tile = unit.getTile();

        String improvementTypeString = workElement.getAttribute("improvementType");
        if (improvementTypeString != null) {
            Element reply = Message.createNewRootElement("workImprovementConfirmed");

            if (tile.getTileItemContainer() == null) {
                tile.setTileItemContainer(new TileItemContainer(tile.getGame(), tile));
                reply.appendChild(tile.getTileItemContainer().toXMLElement(player, reply.getOwnerDocument()));
            }

            TileImprovementType type = FreeCol.getSpecification().getTileImprovementType(improvementTypeString);
            TileImprovement improvement = unit.getTile().findTileImprovementType(type);
            if (improvement == null) {
                // create new improvement
                improvement = new TileImprovement(getGame(), unit.getTile(), type);
                unit.getTile().add(improvement);
            }
            reply.appendChild(improvement.toXMLElement(player, reply.getOwnerDocument()));
            unit.work(improvement);
            return reply;
        } else {
            return null;
        }

    }

    /**
     * Handles a "assignTeacher"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param workElement The element containing the request.
     */
    private Element assignTeacher(Connection connection, Element workElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit student = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("student"));
        Unit teacher = (Unit) getGame().getFreeColGameObject(workElement.getAttribute("teacher"));

        if (!student.canBeStudent(teacher)) {
            throw new IllegalStateException("Unit can not be student!");
        }
        if (!teacher.getColony().canTrain(teacher)) {
            throw new IllegalStateException("Unit can not be teacher!");
        }
        if (student.getOwner() != player) {
            throw new IllegalStateException("Student is not your unit!");
        }
        if (teacher.getOwner() != player) {
            throw new IllegalStateException("Teacher is not your unit!");
        }
        if (student.getColony() != teacher.getColony()) {
            throw new IllegalStateException("Student and teacher are not in the same colony!");
        }
        if (!(student.getLocation() instanceof WorkLocation)) {
            throw new IllegalStateException("Student is not in a WorkLocation!");
        }
        // No reason to send an update to other players: this is always hidden.
        if (student.getTeacher() != null) {
            student.getTeacher().setStudent(null);
        }
        student.setTeacher(teacher);
        if (teacher.getStudent() != null) {
            teacher.getStudent().setTeacher(null);
        }
        teacher.setStudent(student);
        return null;
    }

    /**
     * Handles a "setBuildQueue"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param setBuildQueueElement The element containing the request.
     */
    private Element setBuildQueue(Connection connection, Element setBuildQueueElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(setBuildQueueElement.getAttribute("colony"));
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
        }
        List<BuildableType> buildQueue = new ArrayList<BuildableType>();
        int size = Integer.parseInt(setBuildQueueElement.getAttribute("size"));
        for (int x = 0; x < size; x++) {
            String typeId = setBuildQueueElement.getAttribute("x" + Integer.toString(x));
            buildQueue.add((BuildableType) Specification.getSpecification().getType(typeId));
        }

        colony.setBuildQueue(buildQueue);
        // TODO: what is the following line for?
        sendUpdatedTileToAll(colony.getTile(), player);
        return null;
    }

    /**
     * Handles a "changeState"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param changeStateElement The element containing the request.
     * @return null (always).
     * @exception IllegalArgumentException If the data format of the message is
     *                invalid.
     * @exception IllegalStateException If the request is not accepted by the
     *                model.
     */
    private Element changeState(Connection connection, Element changeStateElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObjectSafely(changeStateElement.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + changeStateElement.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        UnitState state = Enum.valueOf(UnitState.class, changeStateElement.getAttribute("state"));
        Tile oldTile = unit.getTile();
        if (unit.checkSetState(state)) {
            unit.setState(state);
        } else {
            logger.warning("Can't set state " + state + " for unit " + unit + " with current state " + unit.getState()
                    + " and " + unit.getMovesLeft() + " moves left belonging to " + player
                    + ". Possible cheating attempt (or bug)?");
        }
        // Send the updated tile anyway, we may have a synchronization issue
        sendUpdatedTileToAll(oldTile, player);
        return null;
    }

    /**
     * Handles a "putOutsideColony"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param putOutsideColonyElement The element containing the request.
     */
    private Element putOutsideColony(Connection connection, Element putOutsideColonyElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(putOutsideColonyElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Location oldLocation = unit.getLocation();
        unit.putOutsideColony();
        // Don't send updated tile! Other players can't see the unit.
        // sendUpdatedTileToAll(unit.getTile(), player);
        Element updateElement = Message.createNewRootElement("update");
        updateElement.appendChild(unit.getTile().toXMLElement(player, updateElement.getOwnerDocument()));
        if (oldLocation instanceof Building) {
            updateElement.appendChild(((Building) oldLocation)
                                      .toXMLElement(player, updateElement.getOwnerDocument()));
        } else if (oldLocation instanceof ColonyTile) {
            updateElement.appendChild(((ColonyTile) oldLocation)
                                      .toXMLElement(player, updateElement.getOwnerDocument()));
        }
        return updateElement;
    }

    /**
     * Handles a "payForBuilding"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param payForBuildingElement The element containing the request.
     */
    private Element payForBuilding(Connection connection, Element payForBuildingElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(payForBuildingElement.getAttribute("colony"));
        if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        colony.payForBuilding();
        return null;
    }

    /**
     * Handles a "payArrears"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param payArrearsElement The element containing the request.
     */
    private Element payArrears(Connection connection, Element payArrearsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        GoodsType goodsType = FreeCol.getSpecification().getGoodsType(payArrearsElement.getAttribute("goodsType"));
        int arrears = player.getArrears(goodsType);
        if (player.getGold() < arrears) {
            throw new IllegalStateException("Not enough gold to pay tax arrears!");
        } else {
            player.modifyGold(-arrears);
            player.resetArrears(goodsType);
        }
        return null;
    }

    /**
     * Handles a "setGoodsLevels"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param setGoodsLevelsElement The element containing the request.
     */
    private Element setGoodsLevels(Connection connection, Element setGoodsLevelsElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Colony colony = (Colony) getGame().getFreeColGameObject(setGoodsLevelsElement.getAttribute("colony"));
        if (colony == null) {
            throw new IllegalArgumentException("Found no colony with ID " + setGoodsLevelsElement.getAttribute("colony"));
        } else if (colony.getOwner() != player) {
            throw new IllegalStateException("Not your colony!");
            /**
             * we don't really care whether the colony has a custom house } else
             * if (!colony.getBuilding(Building.CUSTOM_HOUSE).isBuilt()) { throw
             * new IllegalStateException("Colony has no custom house!");
             */
        }
        ExportData exportData = new ExportData();
        exportData.readFromXMLElement((Element) setGoodsLevelsElement.getChildNodes().item(0));
        colony.setExportData(exportData);
        return null;
    }

    /**
     * Handles a "clearSpeciality"-request from a client.
     * 
     * @param connection The connection the message came from.
     * @param clearSpecialityElement The element containing the request.
     */
    private Element clearSpeciality(Connection connection, Element clearSpecialityElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(clearSpecialityElement.getAttribute("unit"));
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        unit.clearSpeciality();
        if (unit.getLocation() instanceof Tile) {
            sendUpdatedTileToAll(unit.getTile(), player);
        }
        return null;
    }

    /**
     * Handles an "endTurn" notification from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element endTurn(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        getFreeColServer().getInGameController().endTurn(player);
        return null;
    }

    /**
     * Handles a "disbandUnit"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element disbandUnit(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        Tile oldTile = unit.getTile();
        unit.dispose();
        sendUpdatedTileToAll(oldTile, player);
        return null;
    }

    /**
     * Handles a "foreignAffairs"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element foreignAffairs(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element reply = Message.createNewRootElement("foreignAffairsReport");
        Iterator<Player> enemyPlayerIterator = getGame().getPlayerIterator();
        while (enemyPlayerIterator.hasNext()) {
            ServerPlayer enemyPlayer = (ServerPlayer) enemyPlayerIterator.next();
            if (enemyPlayer.getConnection() == null || enemyPlayer.isIndian()
                || enemyPlayer.isDead()) {
                continue;
            }
            Element enemyElement = reply.getOwnerDocument().createElement("opponent");
            enemyElement.setAttribute("player", enemyPlayer.getId());
            int numberOfColonies = enemyPlayer.getSettlements().size();
            int numberOfUnits = 0;
            int militaryStrength = 0;
            int navalStrength = 0;
            Iterator<Unit> unitIterator = enemyPlayer.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();
                numberOfUnits++;
                if (unit.isNaval()) {
                    navalStrength += unit.getGame().getCombatModel().getOffencePower(unit, null);
                } else {
                    militaryStrength += unit.getGame().getCombatModel().getOffencePower(unit, null);
                }
            }
            Stance stance = enemyPlayer.getStance(player);
            if (stance == Stance.UNCONTACTED) {
                stance = Stance.PEACE;
            }
            enemyElement.setAttribute("numberOfColonies", String.valueOf(numberOfColonies));
            enemyElement.setAttribute("numberOfUnits", String.valueOf(numberOfUnits));
            enemyElement.setAttribute("militaryStrength", String.valueOf(militaryStrength));
            enemyElement.setAttribute("navalStrength", String.valueOf(navalStrength));
            enemyElement.setAttribute("stance", String.valueOf(stance));
            enemyElement.setAttribute("gold", String.valueOf(enemyPlayer.getGold()));
            if (player.equals(enemyPlayer) ||
                player.hasAbility("model.ability.betterForeignAffairsReport")) {
                enemyElement.setAttribute("SoL", String.valueOf(enemyPlayer.getSoL()));
                enemyElement.setAttribute("foundingFathers", String.valueOf(enemyPlayer.getFatherCount()));
                enemyElement.setAttribute("tax", String.valueOf(enemyPlayer.getTax()));
            }
            reply.appendChild(enemyElement);
        }
        return reply;
    }


    /**
     * Handles a "highScores"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element highScores(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element reply = Message.createNewRootElement("highScoresReport");
        for (HighScore score : getFreeColServer().getHighScores()) {
            reply.appendChild(score.toXMLElement(player, reply.getOwnerDocument()));
        }
        return reply;
    }


    /**
     * Handles a "retire"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element retire(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Element reply = Message.createNewRootElement("confirmRetire");
        boolean highScore = getFreeColServer().newHighScore(player);
        if (highScore) {
            try {
                getFreeColServer().saveHighScores();
                reply.setAttribute("highScore", "true");
            } catch (Exception e) {
                logger.warning(e.toString());
                reply.setAttribute("highScore", "false");
            }
        } else {
            reply.setAttribute("highScore", "false");
        }
        return reply;
    }


    /**
     * Handles a "getREFUnits"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element getREFUnits(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        UnitType defaultType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
        if (player.getMonarch() == null) {
            ServerPlayer enemyPlayer = (ServerPlayer) player.getREFPlayer();
            java.util.Map<UnitType, EnumMap<Role, Integer>> unitHash =
                new HashMap<UnitType, EnumMap<Role, Integer>>();
            for (Unit unit : enemyPlayer.getUnits()) {
                if (unit.isOffensiveUnit()) {
                    UnitType unitType = defaultType;
                    if (unit.getType().getOffence() > 0 ||
                        unit.hasAbility("model.ability.expertSoldier")) {
                        unitType = unit.getType();
                    }
                    EnumMap<Role, Integer> roleMap = unitHash.get(unitType);
                    if (roleMap == null) {
                        roleMap = new EnumMap<Role, Integer>(Role.class);
                    }
                    Role role = unit.getRole();
                    Integer count = roleMap.get(role);
                    if (count == null) {
                        roleMap.put(role, new Integer(1));
                    } else {
                        roleMap.put(role, new Integer(count.intValue() + 1));
                    }
                    unitHash.put(unitType, roleMap);
                }
            }
            for (java.util.Map.Entry<UnitType, EnumMap<Role, Integer>> typeEntry : unitHash.entrySet()) {
                for (java.util.Map.Entry<Role, Integer> roleEntry : typeEntry.getValue().entrySet()) {
                    units.add(new AbstractUnit(typeEntry.getKey(), roleEntry.getKey(), roleEntry.getValue()));
                }
            }
        } else {
            units = player.getMonarch().getREF();
        }

        Element reply = Message.createNewRootElement("REFUnits");
        for (AbstractUnit unit : units) {
            reply.appendChild(unit.toXMLElement(player,reply.getOwnerDocument()));
        }
        return reply;
    }

    /**
     * Handles an "indianDemand"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element indianDemand(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        Unit unit = (Unit) getGame().getFreeColGameObject(element.getAttribute("unit"));
        Colony colony = (Colony) getGame().getFreeColGameObject(element.getAttribute("colony"));
        if (unit == null) {
            throw new IllegalArgumentException("Could not find 'Unit' with specified ID: "
                    + element.getAttribute("unit"));
        }
        if (unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        if (colony == null) {
            throw new IllegalArgumentException("Could not find 'Colony' with specified ID: "
                    + element.getAttribute("colony"));
        }
        if (unit.getOwner() != player) {
            throw new IllegalStateException("Not your unit!");
        }
        if (unit.getTile().getDistanceTo(colony.getTile()) > 1) {
            throw new IllegalStateException("Not adjacent to colony!");
        }
        ServerPlayer receiver = (ServerPlayer) colony.getOwner();
        if (receiver.isConnected()) {
            int gold = 0;
            Goods goods = null;
            Element goodsElement = Message.getChildElement(element, Goods.getXMLElementTagName());
            if (goodsElement == null) {
                gold = Integer.parseInt(element.getAttribute("gold"));
            } else {
                goods = new Goods(getGame(), goodsElement);
            }
            try {
                Element reply = receiver.getConnection().ask(element);
                boolean accepted = Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                if (accepted) {
                    if (goods == null) {
                        receiver.modifyGold(-gold);
                    } else {
                        colony.getGoodsContainer().removeGoods(goods);
                    }
                }
                return reply;
            } catch (IOException e) {
                logger.warning("Could not send \"demand\"-message!");
            }
        }
        return null;
    }

    /**
     * Handles an "continuePlaying"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The element containing the request.
     */
    private Element continuePlaying(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!getFreeColServer().isSingleplayer()) {
            throw new IllegalStateException("Can't continue playing in multiplayer!");
        }
        if (player != getFreeColServer().getInGameController().checkForWinner()) {
            throw new IllegalStateException("Can't continue playing! Player "
                    + player.getName() + " hasn't won the game");
        }
        GameOptions go = getGame().getGameOptions();
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_REF)).setValue(false);
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_EUROPEANS)).setValue(false);
        ((BooleanOption) go.getObject(GameOptions.VICTORY_DEFEAT_HUMANS)).setValue(false);
        
        // victory panel is shown after end turn, end turn again to start turn of next player
        final ServerPlayer currentPlayer = (ServerPlayer) getFreeColServer().getGame().getCurrentPlayer();
        getFreeColServer().getInGameController().endTurn(currentPlayer);
        return null;
    }

    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param logoutElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     * @return The reply.
     */
    @Override
    protected Element logout(Connection connection, Element logoutElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        logger.info("Logout by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));
        if (player == null) {
            return null;
        }
        // TODO
        // Remove the player's units/colonies from the map and send map updates
        // to the
        // players that can see such units or colonies.
        // SHOULDN'T THIS WAIT UNTIL THE CURRENT PLAYER HAS FINISHED HIS TURN?
        /*
         * player.setDead(true); Element setDeadElement =
         * Message.createNewRootElement("setDead");
         * setDeadElement.setAttribute("player", player.getId());
         * freeColServer.getServer().sendToAll(setDeadElement, connection);
         */
        /*
         * TODO: Setting the player dead directly should be a server option, but
         * for now - allow the player to reconnect:
         */
        player.setConnected(false);
        if (getFreeColServer().getGame().getCurrentPlayer() == player
                && !getFreeColServer().isSingleplayer()) {
            getFreeColServer().getInGameController().endTurn(player);
        }
        try {
            getFreeColServer().updateMetaServer();
        } catch (NoRouteToServerException e) {}
        
        return null;
    }

    /*
     * Method not used, keep in comments. private void sendErrorToAll(String
     * message, Player player) { Game game = getFreeColServer().getGame();
     * Iterator enemyPlayerIterator = getGame().getPlayerIterator(); while
     * (enemyPlayerIterator.hasNext()) { ServerPlayer enemyPlayer =
     * (ServerPlayer) enemyPlayerIterator.next(); if ((player != null) &&
     * (player.equals(enemyPlayer)) || enemyPlayer.getConnection() == null) {
     * continue; } try { Element errorElement = createErrorReply(message);
     * enemyPlayer.getConnection().send(errorElement); } catch (IOException e) {
     * logger.warning("Could not send message to: " + enemyPlayer.getName() + "
     * with connection " + enemyPlayer.getConnection()); } } }
     */

    private Element getServerStatistics(Connection connection, Element request) {
        StatisticsMessage m = new StatisticsMessage(getGame(), getFreeColServer().getAIMain());
        Element reply = m.toXMLElement();
        return reply;
    }
}
