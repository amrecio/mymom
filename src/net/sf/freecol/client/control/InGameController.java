/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.Canvas.EventType;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.FreeColActionUI;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.EndTurnDialog;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Event;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Limit;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRoute.Stop;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.BuyGoodsMessage;
import net.sf.freecol.common.networking.BuyMessage;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.CloseTransactionMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeclineMoundsMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipUnitMessage;
import net.sf.freecol.common.networking.GetNationSummaryMessage;
import net.sf.freecol.common.networking.GetTransactionMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadCargoMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToAmericaMessage;
import net.sf.freecol.common.networking.MoveToEuropeMessage;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SellGoodsMessage;
import net.sf.freecol.common.networking.SellMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SetTradeRoutesMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.StatisticsMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.UnloadCargoMessage;
import net.sf.freecol.common.networking.UpdateCurrentStopMessage;
import net.sf.freecol.common.networking.UpdateTradeRouteMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The controller that will be used while the game is played.
 */
public final class InGameController implements NetworkConstants {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    private final FreeColClient freeColClient;

    private final short UNIT_LAST_MOVE_DELAY = 300;

    // Selecting next unit depends on mode--- either from the active list,
    // from the going-to list, or flush going-to and end the turn.
    private final int MODE_NEXT_ACTIVE_UNIT = 0;
    private final int MODE_EXECUTE_GOTO_ORDERS = 1;
    private final int MODE_END_TURN = 2;

    private int moveMode = MODE_NEXT_ACTIVE_UNIT;
    private int turnsPlayed = 0;

    /** The most recently saved game file, or <b>null</b>. */
    private File lastSaveGameFile;

    private static FileFilter FSG_FILTER = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".fsg");
            }
        };

    /**
     * A hash map of messages to be ignored.
     */
    private HashMap<String, Integer> messagesToIgnore = new HashMap<String, Integer>();

    /**
     * The constructor to use.
     *
     * @param freeColClient The main controller.
     */
    public InGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
// TODO: fetch value of lastSaveGameFile from a persistent client value
//        lastSaveGameFile = new File(freeColClient.getClientOptions().getString(null));
    }

    private Specification getSpecification() {
        return freeColClient.getGame().getSpecification();
    }

    /**
     * Returns a string representation of the given turn suitable for
     * savegame files.
     *
     * @param turn a <code>Turn</code> value
     * @return A string with the format: "<i>[season] year</i>".
     *         Examples: "1602_1_Spring", "1503"...
     */
    private String getSaveGameString(Turn turn) {
        int year = turn.getYear();
        switch (turn.getSeason()) {
        case SPRING:
            return Integer.toString(year) + "_1_" + Messages.message("spring");
        case AUTUMN:
            return Integer.toString(year) + "_2_" + Messages.message("autumn");
        case YEAR:
        default:
            return Integer.toString(year);
        }
    }


    /** Returns the most recently saved game file, or <b>null</b>.
     *  (This may be either from a recent arbitrary user operation or an
     *  autosave function.)
     *
     *  @return File recent save game file
     */
    public File getLastSaveGameFile () {
        File lastSave = null;
        for (File directory : new File[] { FreeCol.getSaveDirectory(), FreeCol.getAutosaveDirectory() }) {
            for (File savegame : directory.listFiles(FSG_FILTER)) {
                if (lastSave == null
                    || savegame.lastModified() > lastSave.lastModified()) {
                    lastSave = savegame;
                }
            }
        }

       return lastSave;
    }

    /**
     * Saves the game to a fix-named file in the autosave directory, which may
     * be used for quick-reload.
     *
     * @return boolean <b>true</b> if and only if the game was saved
     */
    public boolean quicksaveGame () {
       Game game = freeColClient.getGame();
       if (game != null) {
          String gid = Integer.toHexString(game.getUUID().hashCode());
          String filename = "quicksave-" + gid + ".fsg";
          File file = new File(FreeCol.getAutosaveDirectory(), filename);
          return saveGame(file);
       }
       return false;
    }

    /** Reloads a game state which was previously saved via <code>quicksaveGame</code>.
     *
     * @return boolean <b>true</b> if and only if a game was loaded
     */
    public boolean quickReload () {
       Canvas canvas = freeColClient.getCanvas();
       Game game = freeColClient.getGame();
       if (game != null) {
          String gid = Integer.toHexString(game.getUUID().hashCode());
          String filename = "quicksave-" + gid + ".fsg";
          File file = new File(FreeCol.getAutosaveDirectory(), filename);
          if (file.isFile()) {
             // ask user to confirm reload action
             boolean ok = true; // canvas.showConfirmDialog(gid, gid, filename);

             // perform loading game state if answer == ok
             if (ok) {
                freeColClient.getConnectController().quitGame(true);
                canvas.removeInGameComponents();
                freeColClient.getConnectController().loadGame(file);
                return true;
             }
          }
       }
       return false;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * saves the game.
     *
     * @return True if the game was saved.
     */
    public boolean saveGame() {
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        Game game = freeColClient.getGame();
        String gid = Integer.toHexString(game.getUUID().hashCode());
        String fileName = //player.getName() + "_"

                gid + "_"
                + Messages.message(player.getNationName()) + "_"
                + getSaveGameString(game.getTurn());
        fileName = fileName.replaceAll(" ", "_");

        if (freeColClient.canSaveCurrentGame()) {
            final File file = canvas.showSaveDialog(FreeCol.getSaveDirectory(), fileName);
            if (file != null) {
                FreeCol.setSaveDirectory(file.getParentFile());
                return saveGame(file);
            }
        }
        return false;
    }

    /**
     * Saves the game to the given file.
     *
     * @param file The <code>File</code>.
     * @return True if the game was saved.
     */
    public boolean saveGame(final File file) {
        Canvas canvas = freeColClient.getCanvas();
        FreeColServer server = freeColClient.getFreeColServer();
        boolean result = false;
        canvas.showStatusPanel(Messages.message("status.savingGame"));
        try {
            server.setActiveUnit(freeColClient.getGUI().getActiveUnit());
            server.saveGame(file, freeColClient.getMyPlayer().getName(),
                          freeColClient.getClientOptions());
            lastSaveGameFile = file;
            canvas.closeStatusPanel();
            result = true;
        } catch (IOException e) {
            canvas.errorMessage("couldNotSaveGame");
        }
        canvas.requestFocusInWindow();
        return result;
    }

    /**
     * Opens a dialog where the user should specify the filename and
     * loads the game.
     */
    public void loadGame() {
        Canvas canvas = freeColClient.getCanvas();
        File file = canvas.showLoadDialog(FreeCol.getSaveDirectory());
        if (file == null) {
            return;
        }
        if (!file.isFile()) {
            canvas.errorMessage("fileNotFound");
            return;
        }
        if (!canvas.showConfirmDialog("stopCurrentGame.text",
                                      "stopCurrentGame.yes",
                                      "stopCurrentGame.no")) {
            return;
        }

        freeColClient.getConnectController().quitGame(true);
        canvas.removeInGameComponents();
        freeColClient.getConnectController().loadGame(file);
    }

    /**
     * Send a trivial message (tag only) to the server.
     *
     * @param tag The tag for the message.
     * @return True if the server replied.
     */
    private boolean askTrivial(String tag) {
        Client client = freeColClient.getClient();
        Element element = Message.createNewRootElement(tag);
        element = askExpecting(client, element, null);
        if (element == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, element);
        return true;
    }


    /**
     * Sets the "debug mode" to be active or not. Calls
     * {@link FreeCol#setInDebugMode(boolean)} and reinitialize the
     * <code>FreeColMenuBar</code>.
     *
     * @param debug Set to <code>true</code> to enable debug mode.
     */
    public void setInDebugMode(boolean debug) {
        FreeCol.setInDebugMode(debug);
        logger.info("Debug mode set to " + debug);
        freeColClient.updateMenuBar();
    }

    /** Informs this controller that a game has been newly loaded. */
    public void setGameConnected () {
       turnsPlayed = 0;
    }

    /**
     * Require that it is this client's player's turn.
     * Put up the notYourTurn message if not.
     *
     * @return True if it is our turn.
     */
    private boolean requireOurTurn() {
        if (freeColClient.getGame().getCurrentPlayer()
            != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().showInformationMessage("notYourTurn");
            return false;
        }
        return true;
    }


    /**
     * Sends the specified message to the server and returns the reply,
     * if it has the specified tag.
     * Handle "error" replies if they have a messageID or when in debug mode.
     * This routine allows code simplification in much of the following
     * client-server communication.
     *
     * In following routines we follow the convention that server I/O
     * is confined to the ask<foo>() routine, which typically returns
     * true if the server interaction succeeded, which does *not*
     * necessarily imply that the actual substance of the request was
     * allowed (e.g. a move may result in the death of a unit rather
     * than actually moving).
     *
     * @param client a <code>Client</code> value
     * @param element The <code>Element</code> (root element in a
     *        DOM-parsed XML tree) that holds all the information
     * @param tag The expected tag
     * @return The answer from the server if it has the specified tag,
     *         otherwise <code>null</code>.
     */
    private Element askExpecting(Client client, Element element, String tag) {
        // Send the element, return null on failure or null return.
        Element reply = null;
        try {
            reply = client.ask(element);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not send " + element, e);
            return null;
        }
        if (reply == null) {
            logger.warning("Received null reply to " + element);
            return null;
        }

        // Process explicit errors.
        if ("error".equals(reply.getTagName())) {
            String messageId = reply.getAttribute("messageID");
            String message = reply.getAttribute("message");
            if (messageId != null && message != null
                && FreeCol.isInDebugMode()) {
                // If debugging suppress the bland but i18n compliant
                // failure message in favour of the higher detail
                // non-i18n text.
                reply.removeAttribute("messageID");
            }
            if (messageId == null && message == null) {
                logger.warning("Received null error response");
            } else {
                logger.warning("Received error response: "
                               + ((messageId != null) ? messageId : "")
                               + "/" + ((message != null) ? message : ""));
                Connection conn = client.getConnection();
                freeColClient.getInGameInputHandler().handle(conn, reply);
            }
            return null;
        }

        // Success!  Do the standard processing.
        if (tag == null || tag.equals(reply.getTagName())) {
            String sound = reply.getAttribute("sound");
            if (sound != null && !sound.isEmpty()) {
                freeColClient.playSound(sound);
            }
            return reply;
        }

        // Unexpected reply.  Whine and fail.
        String complaint = "Received reply with tag " + reply.getTagName()
            + " which should have been " + tag
            + " to message " + element;
        logger.warning(complaint);
        if (FreeCol.isInDebugMode()) {
            freeColClient.getCanvas().errorMessage(null, complaint);
        }
        return null;
    }

    // Simple helper container to remember a colony state prior to some
    // change, and fire off any consequent property changes.
    private class ColonyWas {
        private Colony colony;
        private int population;
        private int productionBonus;
        private List<BuildableType> buildQueue;

        public ColonyWas(Colony colony) {
            this.colony = colony;
            this.population = colony.getUnitCount();
            this.productionBonus = colony.getProductionBonus();
            this.buildQueue
                = new ArrayList<BuildableType>(colony.getBuildQueue());
        }

        /**
         * Fire any property changes resulting from actions within a
         * colony.
         */
        public void fireChanges() {
            int newPopulation = colony.getUnitCount();
            if (newPopulation != population) {
                String pc = ColonyChangeEvent.POPULATION_CHANGE.toString();
                colony.firePropertyChange(pc, population, newPopulation);
            }
            int newProductionBonus = colony.getProductionBonus();
            if (newProductionBonus != productionBonus) {
                String pc = ColonyChangeEvent.BONUS_CHANGE.toString();
                colony.firePropertyChange(pc, productionBonus, newProductionBonus);
            }
            List<BuildableType> newBuildQueue = colony.getBuildQueue();
            if (!newBuildQueue.equals(buildQueue)) {
                String pc = ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString();
                colony.firePropertyChange(pc, buildQueue, newBuildQueue);
            }
            colony.getGoodsContainer().fireChanges();
        }
    }

    // Simple helper container to remember the Europe state prior to
    // some change, and fire off any consequent property changes.
    private class EuropeWas {
        private Europe europe;
        private int unitCount;

        public EuropeWas(Europe europe) {
            this.europe = europe;
            this.unitCount = europe.getUnitCount();
        }

        /**
         * Fire any property changes resulting from actions in Europe.
         */
        public void fireChanges() {
            int newUnitCount = europe.getUnitCount();

            if (newUnitCount != unitCount) {
                String pc = Europe.UNIT_CHANGE.toString();
                europe.firePropertyChange(pc, unitCount, newUnitCount);
            }
        }
    }

    // Simple helper container to remember a unit state prior to some
    // change, and fire off any consequent property changes.
    private class UnitWas {
        private Unit unit;
        private UnitType type;
        private Unit.Role role;
        private Location loc;
        private GoodsType work;
        private int amount;
        private Colony colony;

        public UnitWas(Unit unit) {
            this.unit = unit;
            this.type = unit.getType();
            this.role = unit.getRole();
            this.loc = unit.getLocation();
            this.work = unit.getWorkType();
            this.amount = getAmount(loc, work);
            this.colony = unit.getColony();
        }

        // TODO: fix this non-OO nastiness
        private int getAmount(Location location, GoodsType goodsType) {
            if (goodsType != null) {
                if (location instanceof Building) {
                    Building building = (Building) location;
                    ProductionInfo info = building.getProductionInfo();
                    return (info == null || info.getProduction() == null
                            || info.getProduction().size() == 0) ? 0
                        : info.getProduction().get(0).getAmount();
                } else if (location instanceof ColonyTile) {
                    return ((ColonyTile)location).getProductionOf(goodsType);
                }
            }
            return 0;
        }

        // TODO: fix this non-OO nastiness
        private String change(FreeColGameObject fcgo) {
            return (fcgo instanceof Tile) ? Tile.UNIT_CHANGE
                : (fcgo instanceof Europe) ? Europe.UNIT_CHANGE
                : (fcgo instanceof ColonyTile) ? ColonyTile.UNIT_CHANGE
                : (fcgo instanceof Building) ? Building.UNIT_CHANGE
                : (fcgo instanceof Unit) ? Unit.CARGO_CHANGE
                : null;
        }

        /**
         * Fire any property changes resulting from actions of a unit.
         */
        public void fireChanges() {
            UnitType newType = null;
            Unit.Role newRole = null;
            Location newLoc = null;
            GoodsType newWork = null;
            int newAmount = 0;
            if (!unit.isDisposed()) {
                newLoc = unit.getLocation();
                if (colony != null) {
                    newType = unit.getType();
                    newRole = unit.getRole();
                    newWork = unit.getWorkType();
                    newAmount = (newWork == null) ? 0
                        : getAmount(newLoc, newWork);
                }
            }

            if (loc != newLoc) {
                FreeColGameObject oldFcgo = (FreeColGameObject) loc;
                oldFcgo.firePropertyChange(change(oldFcgo), unit, null);
                if (newLoc != null) {
                    FreeColGameObject newFcgo = (FreeColGameObject) newLoc;
                    newFcgo.firePropertyChange(change(newFcgo), null, unit);
                }
            }
            if (colony != null) {
                if (type != newType && newType != null) {
                    String pc = ColonyChangeEvent.UNIT_TYPE_CHANGE.toString();
                    colony.firePropertyChange(pc, type, newType);
                } else if (role != newRole && newRole != null) {
                    String pc = Tile.UNIT_CHANGE.toString();
                    colony.firePropertyChange(pc, role.toString(),
                                              newRole.toString());
                }
                if (work == newWork) {
                    if (work != null && amount != newAmount) {
                        colony.firePropertyChange(work.getId(),
                                                  amount, newAmount);
                    }
                } else {
                    if (work != null) {
                        colony.firePropertyChange(work.getId(), amount, 0);
                    }
                    if (newWork != null) {
                        colony.firePropertyChange(newWork.getId(), 0, newAmount);
                    }
                }
            }
            if (unit.getGoodsContainer() != null) {
                unit.getGoodsContainer().fireChanges();
            }
        }
    }

    /**
     * Creates at least one autosave game file of the currently played
     * game in the autosave directory. Does nothing if there is no
     * game running.
     *
     */
    private void autosave_game () {
        Game game = freeColClient.getGame();
        if (game == null) {
           return;
        }
        Player player = game.getCurrentPlayer();

        // unconditional save per round (fix file "last-turn")
        String autosave_text = Messages.message("clientOptions.savegames.autosave.fileprefix");
        String filename = autosave_text + "-" + Messages.message(
                "clientOptions.savegames.autosave.lastturn") + ".fsg";
        String beforeFilename = autosave_text + "-" + Messages.message(
                "clientOptions.savegames.autosave.beforelastturn") + ".fsg";
        File autosaveDir = FreeCol.getAutosaveDirectory();
        File saveGameFile = new File(autosaveDir, filename);
        File beforeSaveFile = new File(autosaveDir, beforeFilename);

        // if "last-turn" file exists, shift it to "before-last-turn" file
        if (saveGameFile.exists()) {
           beforeSaveFile.delete();
           saveGameFile.renameTo(beforeSaveFile);
        }
        saveGame(saveGameFile);

        // conditional save after user-set period
        ClientOptions options = freeColClient.getClientOptions();
        int savegamePeriod = options.getInteger(ClientOptions.AUTOSAVE_PERIOD);
        int turnNumber = game.getTurn().getNumber();
        if (savegamePeriod <= 1
            || (savegamePeriod != 0 && turnNumber % savegamePeriod == 0)) {
            String playernation = player == null ? "" :
                                  Messages.message(player.getNation().getNameKey());
            String gid = Integer.toHexString(game.getUUID().hashCode());
            filename = Messages.message("clientOptions.savegames.autosave.fileprefix")
                + '-' + gid  + "_" + playernation  + "_" + getSaveGameString(game.getTurn()) + ".fsg";
            saveGameFile = new File(autosaveDir, filename);
            saveGame(saveGameFile);
        }
    }

    /**
     * Set a player to be the new current player.
     *
     * @param player The <code>Player</code> to be the new current player.
     */
    public void setCurrentPlayer(Player player) {
        logger.finest("Entering client setCurrentPlayer: " + player.getName());
        Game game = freeColClient.getGame();
        game.setCurrentPlayer(player);

        if (freeColClient.getMyPlayer().equals(player)
            && freeColClient.getFreeColServer() != null) {

           // auto-save the game (if it isn't newly loaded)
           if ( turnsPlayed > 0 ) {
              autosave_game();
           }

           player.invalidateCanSeeTiles();

           // Check for emigration.
           if (player.checkEmigrate()) {
                if (player.hasAbility("model.ability.selectRecruit")
                    && player.getEurope().recruitablesDiffer()) {
                    Canvas canvas = freeColClient.getCanvas();
                    int index = canvas.showEmigrationPanel(false);
                    emigrate(player, index + 1);
                } else {
                    emigrate(player, 0);
                }
           }

           // GUI management.
           if (!freeColClient.isSingleplayer()) {
               freeColClient.playSound("sound.anthem." + player.getNationID());
           }
           displayModelMessages(true);
        }
        logger.finest("Exiting client setCurrentPlayer: " + player.getName());
    }

    /**
     * Operate a trade route, doing load/unload actions and updating the
     * destination.
     *
     * @param unit The <code>Unit</code> on the route.
     * @param messages A list of messages to update.
     * @return A destination to move to, or null if none available or the
     *     unit can not move further.
     */
    private Location operateTradeRoute(Unit unit, List<ModelMessage> messages) {
        Canvas canvas = freeColClient.getCanvas();

        // Complain and return if the stop is no longer valid.
        Stop stop = unit.getStop();
        if (!TradeRoute.isStopValid(unit, stop)) {
            String name = unit.getTradeRoute().getName();
            canvas.showInformationMessage(unit, StringTemplate.template("traderoute.broken")
                                          .addName("%name%", name));
            clearOrders(unit);
            logger.warning("Trade unit " + unit.getId()
                           + " in route " + name
                           + " cannot continue: stop invalid.");
            return null;
        }

        // Defend against a bug where a unit with a trade route ends
        // up with a null destination.  Make sure we reset the
        // destination to the next stop, which will avoid the unit
        // being considered an "active" unit.
        if (unit.getDestination() == null) {
            setDestination(unit, stop.getLocation());
        }

        if ((stop.getLocation() instanceof Europe && unit.isInEurope())
            || (!(stop.getLocation() instanceof Europe)
                && unit.getTile() == stop.getLocation().getTile())) {
            // The unit has arrived at its stop.

            if (unit.getInitialMovesLeft() == unit.getMovesLeft()) {
                // The unit was already at the stop at the beginning
                // of the turn, so load, and aim for the next stop.
                loadUnitAtStop(unit, messages);
                if (!askUpdateCurrentStop(unit)) return null;
            } else {
                // The unit has arrived this turn, try to unload.
                if (!unloadUnitAtStop(unit, messages)) {
                    // Nothing unloaded, try to load.
                    if (!loadUnitAtStop(unit, messages)) {
                        // Nothing loaded, try to move on.
                        if (!askUpdateCurrentStop(unit)) return null;
                    }
                }
            }
        }

        return (unit.getMovesLeft() > 0) ? unit.getDestination() : null;
    }

    /**
     * Handle server query-response for updating the current stop.
     *
     * @param unit The <code>Unit</code> whose stop is to be updated.
     * @return True if the query-response succeeds.
     */
    private boolean askUpdateCurrentStop(Unit unit) {
        Client client = freeColClient.getClient();
        UpdateCurrentStopMessage message = new UpdateCurrentStopMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Work out what goods to load onto a unit at a stop, and load them.
     *
     * @param unit The <code>Unit</code> to load.
     * @param messages A list of messages to update.
     * @return True if goods were loaded.
     */
    private boolean loadUnitAtStop(Unit unit, List<ModelMessage> messages) {
        // Copy the list of goods types to load at this stop.
        Stop stop = unit.getStop();
        List<GoodsType> goodsTypesToLoad
            = new ArrayList<GoodsType>(stop.getCargo());
        boolean ret = false;

        // First handle partial loads.
        // For each cargo the unit is already carrying, and which is
        // not to be unloaded at this stop, check if the cargo is
        // completely full and if not, try to fill to capacity.
        Colony colony = unit.getColony();
        Location loc = (unit.isInEurope()) ? unit.getOwner().getEurope()
            : colony;
        Game game = freeColClient.getGame();
        for (Goods goods : unit.getGoodsList()) {
            GoodsType type = goods.getType();
            int index, toLoad;
            if ((toLoad = GoodsContainer.CARGO_SIZE - goods.getAmount()) > 0
                && (index = goodsTypesToLoad.indexOf(type)) >= 0) {
                int present, atStop;
                if (unit.isInEurope()) {
                    present = atStop = Integer.MAX_VALUE;
                } else {
                    present = colony.getGoodsContainer().getGoodsCount(type);
                    atStop = colony.getExportAmount(type);
                }
                if (atStop > 0) {
                    Goods cargo = new Goods(game, loc, type,
                                            Math.min(toLoad, atStop));
                    if (loadGoods(cargo, unit)) {
                        messages.add(getLoadGoodsMessage(unit, type,
                            cargo.getAmount(), present, atStop, toLoad));
                        ret = true;
                    }
                } else if (present > 0) {
                    messages.add(getLoadGoodsMessage(unit, type,
                        0, present, 0, toLoad));
                }
                // Do not try to load this goods type again.  Either
                // it has already succeeded, or it can not ever
                // succeed because there is nothing available.
                goodsTypesToLoad.remove(index);
            }
        }

        // Then fill any remaining empty cargo slots.
        for (GoodsType type : goodsTypesToLoad) {
            if (unit.getSpaceLeft() <= 0) break; // Full
            int toLoad = GoodsContainer.CARGO_SIZE;
            int present, atStop;
            if (unit.isInEurope()) {
                present = atStop = Integer.MAX_VALUE;
            } else {
                present = colony.getGoodsContainer().getGoodsCount(type);
                atStop = colony.getExportAmount(type);
            }
            if (atStop > 0) {
                Goods cargo = new Goods(game, loc, type,
                                        Math.min(toLoad, atStop));
                if (loadGoods(cargo, unit)) {
                    messages.add(getLoadGoodsMessage(unit, type,
                        cargo.getAmount(), present, atStop, toLoad));
                    ret = true;
                }
            } else if (present > 0) {
                messages.add(getLoadGoodsMessage(unit, type,
                    0, present, 0, toLoad));
            }
        }

        return ret;
    }

    /**
     * Gets a message describing a goods loading.
     *
     * @param unit The <code>Unit</code> that is loading.
     * @param type The <code>GoodsType</code> the type of goods being loaded.
     * @param amount The amount of goods loaded.
     * @param present The amount of goods already at the location.
     * @param atStop The amount of goods available to load.
     * @param toLoad The amount of goods the unit could load.
     * @return A model message describing the load.
     */
    private ModelMessage getLoadGoodsMessage(Unit unit, GoodsType type,
                                             int amount, int present,
                                             int atStop, int toLoad) {
        Player player = unit.getOwner();
        Location loc = unit.getLocation();
        String route = unit.getTradeRoute().getName();
        ModelMessage m;

        if (toLoad < atStop) {
            m = new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                 "traderoute.loadImportLimited", unit)
                .addName("%route%", route)
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addStringTemplate("%location%", loc.getLocationNameFor(player))
                .addName("%amount%", Integer.toString(amount))
                .add("%goods%", type.getNameKey())
                .addName("%more%", Integer.toString(atStop - toLoad));
        } else if (present > atStop && toLoad > atStop) {
            m = new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                 "traderoute.loadExportLimited", unit)
                .addName("%route%", route)
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addStringTemplate("%location%", loc.getLocationNameFor(player))
                .addName("%amount%", Integer.toString(amount))
                .add("%goods%", type.getNameKey())
                .addName("%more%", Integer.toString(present - atStop));
        } else {
            m = new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT,
                                 "traderoute.load", unit)
                .addName("%route%", route)
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addStringTemplate("%location%", loc.getLocationNameFor(player))
                .addName("%amount%", Integer.toString(amount))
                .add("%goods%", type.getNameKey());
        }
        return m;
    }

    /**
     * Load some goods onto a carrier.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the load succeeded.
     */
    private boolean loadGoods(Goods goods, Unit carrier) {
        if (carrier.isInEurope() && goods.getLocation() instanceof Europe) {
            if (!carrier.getOwner().canTrade(goods)) return false;
            return buyGoods(goods.getType(), goods.getAmount(), carrier);
        }
        GoodsType type = goods.getType();
        GoodsContainer container = carrier.getGoodsContainer();
        int oldAmount = container.getGoodsCount(type);
        UnitWas unitWas = new UnitWas(carrier);
        Colony colony = carrier.getColony();
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        if (askLoadCargo(goods, carrier)
            && container.getGoodsCount(type) != oldAmount) {
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }

    /**
     * Handle server query-response for loading cargo.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     * @return True if the query-response succeeds.
     */
    private boolean askLoadCargo(Goods goods, Unit carrier) {
        Client client = freeColClient.getClient();
        LoadCargoMessage message = new LoadCargoMessage(goods, carrier);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Work out what goods to unload from a unit at a stop, and unload them.
     *
     * @param unit The <code>Unit</code> to unload.
     * @param messages A list of messages to update.
     * @return True if something was unloaded.
     */
    private boolean unloadUnitAtStop(Unit unit, List<ModelMessage> messages) {
        Colony colony = unit.getColony();
        Stop stop = unit.getStop();
        final List<GoodsType> goodsTypesToLoad = stop.getCargo();
        boolean ret = false;

        // Unload everything that is on the carrier but not listed to
        // be loaded at this stop.
        Game game = freeColClient.getGame();
        for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
            GoodsType type = goods.getType();
            if (goodsTypesToLoad.contains(type)) continue; // Keep this cargo.

            int atStop = (colony == null) ? Integer.MAX_VALUE // Europe
                : colony.getImportAmount(type);
            int toUnload = goods.getAmount();
            if (toUnload > atStop) {
                // Unloading here will overflow the colony warehouse
                // (can not be Europe!).  Decide whether to unload the
                // whole cargo or not.
                Canvas canvas = freeColClient.getCanvas();
                String locName = colony.getName();
                String overflow = Integer.toString(toUnload - atStop);
                int option = freeColClient.getClientOptions()
                    .getInteger(ClientOptions.UNLOAD_OVERFLOW_RESPONSE);
                switch (option) {
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ASK:
                    StringTemplate template =
                        StringTemplate.template("traderoute.warehouseCapacity")
                        .addStringTemplate("%unit%", Messages.getLabel(unit))
                        .addName("%colony%", locName)
                        .addName("%amount%", overflow)
                        .add("%goods%", goods.getNameKey());
                    if (!canvas.showConfirmDialog(colony.getTile(), template,
                                                  "yes", "no")) {
                        toUnload = atStop;
                    }
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_NEVER:
                    toUnload = atStop;
                    break;
                case ClientOptions.UNLOAD_OVERFLOW_RESPONSE_ALWAYS:
                    break;
                default:
                    logger.warning("Illegal UNLOAD_OVERFLOW_RESPONSE: "
                                   + Integer.toString(option));
                    break;
                }
            }

            // Try to unload.
            Goods cargo = (goods.getAmount() == toUnload) ? goods
                : new Goods(game, unit, type, toUnload);
            if (unloadGoods(cargo, unit, colony)) {
                messages.add(getUnloadGoodsMessage(unit, type,
                    cargo.getAmount(), atStop, goods.getAmount(), toUnload));
                ret = true;
            }
        }

        return ret;
    }

    /**
     * Gets a message describing a goods unloading.
     *
     * @param unit The <code>Unit</code> that is unloading.
     * @param type The <code>GoodsType</code> the type of goods being unloaded.
     * @param amount The amount of goods unloaded.
     * @param present The amount of goods already carried by the unit.
     * @param atStop The amount of goods available to unload.
     * @param toUnload The amount of goods actually unloaded.
     * @return A model message describing the unload.
     */
    private ModelMessage getUnloadGoodsMessage(Unit unit, GoodsType type,
                                               int amount, int atStop,
                                               int present, int toUnload) {
        String key = null;
        int overflow = 0;

        if (present == toUnload) {
            key = "traderoute.unload";
        } else if (toUnload > atStop) {
            key = "traderoute.overflow";
            overflow = toUnload - atStop;
        } else {
            key = "traderoute.nounload";
            overflow = present - atStop;
        }

        return new ModelMessage(ModelMessage.MessageType.GOODS_MOVEMENT, key, unit)
            .addName("%route%", unit.getTradeRoute().getName())
            .addStringTemplate("%unit%", Messages.getLabel(unit))
            .addStringTemplate("%location%", unit.getLocation().getLocationNameFor(unit.getOwner()))
            .addAmount("%amount%", amount)
            .addAmount("%overflow%", overflow)
            .add("%goods%", type.getNameKey());
    }

    /**
     * Unload some goods from a carrier.
     *
     * @param goods The <code>Goods</code> to unload.
     * @param carrier The <code>Unit</code> carrying the goods.
     * @param colony The <code>Colony</code> to unload to,
     *               or null if unloading in Europe.
     * @return True if the unload succeeded.
     */
    private boolean unloadGoods(Goods goods, Unit carrier, Colony colony) {
        if (colony == null && carrier.isInEurope()) {
            return (!carrier.getOwner().canTrade(goods)) ? false
                : sellGoods(goods);
        }
        GoodsType type = goods.getType();
        GoodsContainer container = carrier.getGoodsContainer();
        int oldAmount = container.getGoodsCount(type);
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(carrier);
        if (askUnloadCargo(goods)
            && container.getGoodsCount(type) != oldAmount) {
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }

    /**
     * Handle server query-response for unloading cargo.
     *
     * @param goods The <code>Goods</code> to unload.
     * @return True if the query-response succeeds.
     */
    private boolean askUnloadCargo(Goods goods) {
        Client client = freeColClient.getClient();
        UnloadCargoMessage message = new UnloadCargoMessage(goods);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Moves the given unit towards its destinations if possible.
     *
     * @param unit The <code>Unit</code> to move.
     */
    public void moveToDestination(Unit unit) {
        if (!requireOurTurn()) return;
        Player player = freeColClient.getMyPlayer();
        List<ModelMessage> messages = new ArrayList<ModelMessage>();
        GUI gui = freeColClient.getGUI();
        gui.setActiveUnit(unit);

        Location destination;
        while (unit.getMovesLeft() > 0) {
            // Look for valid destinations
            if (unit.getTradeRoute() == null) {
                if ((destination = unit.getDestination()) == null) {
                    break; // No destination
                } else if (destination instanceof Europe) {
                    if (unit.isInEurope()) break; // Arrived in Europe
                } else if (destination.getTile() == null) {
                    break; // Not on the map
                } else if (unit.getTile() == destination.getTile()) {
                    break; // Arrived at on-map destination
                }
            } else {
                destination = operateTradeRoute(unit, messages);
                if (destination == null
                    || unit.getTile() == destination.getTile()) break;
            }

            // Find a path to the destination.
            PathNode path = (destination instanceof Europe)
                ? unit.findPathToEurope()
                : unit.findPath(destination.getTile());

            // No path, give up.
            if (path == null) {
                StringTemplate dest = destination.getLocationNameFor(player);
                freeColClient.getCanvas().showInformationMessage(unit,
                    StringTemplate.template("selectDestination.failed")
                    .addStringTemplate("%destination%", dest));
                break;
            }

            // Try to follow the path.
            if (!movePath(unit, path)) break;
        }

        // Clear ordinary destinations, leave trade routes but display their
        // messages if required.
        if (unit.getTradeRoute() == null) {
            Location location = unit.getDestination();
            if (location != null) {
                if (unit.getTile() == null) {
                    if (unit.getLocation() == location) {
                        clearGotoOrders(unit);
                    }
                } else {
                    if (unit.getTile() == location.getTile()) {
                        clearGotoOrders(unit);
                    }
                }
            }
            checkCashInTreasureTrain(unit);
            if (unit.getMovesLeft() > 0 && unit.getTile() != null
                && freeColClient.getClientOptions()
                .getBoolean(ClientOptions.ALWAYS_CENTER)) {
                freeColClient.getGUI().setSelectedTile(unit.getTile(), false);
            }
        } else {
            if (freeColClient.getClientOptions()
                .getBoolean(ClientOptions.SHOW_GOODS_MOVEMENT)) {
                for (ModelMessage m : messages) {
                    unit.getOwner().addModelMessage(m);
                }
            }
        }
    }

    /**
     * Moves the active unit in a specified direction. This may result in an
     * attack, move... action.
     *
     * @param direction The direction in which to move the active unit.
     */
    public void moveActiveUnit(Direction direction) {
        if (!requireOurTurn()) return;

        Unit unit = freeColClient.getGUI().getActiveUnit();
        if (unit != null) {
            clearGotoOrders(unit);
            move(unit, direction);
        } // else: nothing: There is no active unit that can be moved.
    }

    /**
     * Selects a destination for this unit. Europe and the player's
     * colonies are valid destinations.
     *
     * @param unit The unit for which to select a destination.
     */
    public void selectDestination(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        Location destination = canvas.showSelectDestinationDialog(unit);
        if (destination == null) return; // user aborted

        if (setDestination(unit, destination)
            && freeColClient.getGame().getCurrentPlayer()
                == freeColClient.getMyPlayer()) {
            if (destination instanceof Europe && unit.getTile() != null
                && (unit.getTile().canMoveToEurope()
                    || unit.getTile().isAdjacentToMapEdge())) {
                moveToEurope(unit);
            } else {
                moveToDestination(unit);
            }
        }
    }


    // Public user actions that may require interactive confirmation
    // before requesting an update from the server.

    /**
     * Declares independence for the home country.
     *
     * TODO: Move magic 50% number to the spec.
     */
    public void declareIndependence() {
        if (!requireOurTurn()) return;
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();

        // Check for adequate support.
        Event event = getSpecification()
            .getEvent("model.event.declareIndependence");
        for (Limit limit : event.getLimits()) {
            if (!limit.evaluate(player)) {
                canvas.showInformationMessage(StringTemplate.template(limit.getDescriptionKey())
                                              .addAmount("%limit%", limit.getRightHandSide().getValue()));
                return;
            }
        }
        if (player.getNewLandName() == null) {
            // Can only happen in debug mode.
            return;
        }

        // Confirm intention, and collect nation+country names.
        List<String> names = canvas.showConfirmDeclarationDialog();
        if (names == null
            || names.get(0) == null || names.get(0).length() == 0
            || names.get(1) == null || names.get(1).length() == 0) {
            // Empty name => user cancelled.
            return;
        }

        // Ask server.
        String nationName = names.get(0);
        String countryName = names.get(1);
        if (askDeclare(nationName, countryName)
            && player.getPlayerType() == PlayerType.REBEL) {
            canvas.showDeclarationDialog();
            freeColClient.getActionManager().update();
            nextModelMessage();
        }
    }

    /**
     * Handle server query-response for declaring independence.
     *
     * @param nation The name for the new nation.
     * @param country The name for the new country.
     * @return True if the server interaction succeeded.
     */
    private boolean askDeclare(String nation, String country) {
        Client client = freeColClient.getClient();
        DeclareIndependenceMessage message
            = new DeclareIndependenceMessage(nation, country);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sends a public chat message.
     *
     * @param message The text of the message.
     */
    public void sendChat(String message) {
        ChatMessage chatMessage
            = new ChatMessage(freeColClient.getMyPlayer(), message, false);
        freeColClient.getClient().sendAndWait(chatMessage.toXMLElement());
    }


    /**
     * Renames a <code>Nameable</code>.
     * Apparently this can be done while it is not your turn.
     *
     * @param object The object to rename.
     */
    public void rename(Nameable object) {
        Player player = freeColClient.getMyPlayer();
        if (!(object instanceof Ownable)
            || ((Ownable) object).getOwner() != player) {
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        String name = null;
        if (object instanceof Colony) {
            Colony colony = (Colony) object;
            name = canvas.showInputDialog(colony.getTile(),
                                          StringTemplate.key("renameColony.text"),
                                          colony.getName(),
                                          "renameColony.yes", "renameColony.no",
                                          true);
            if (name == null) {
                // User cancelled, 0-length invalid.
                return;
            } else if (colony.getName().equals(name)) {
                // No change
                return;
            } else if (player.getSettlement(name) != null) {
                // Colony name must be unique.
                canvas.showInformationMessage((Colony) object,
                                              StringTemplate.template("nameColony.notUnique")
                                              .addName("%name%", name));
                return;
            }
        } else if (object instanceof Unit) {
            Unit unit = (Unit) object;
            name = canvas.showInputDialog(unit.getTile(),
                                          StringTemplate.key("renameUnit.text"),
                                          unit.getName(),
                                          "renameUnit.yes", "renameUnit.no",
                                          false);
            if (name == null) return; // User cancelled, 0-length clears name.
        } else {
            logger.warning("Tried to rename an unsupported Nameable: "
                           + object.toString());
            return;
        }

        askRename((FreeColGameObject) object, name);
    }

    /**
     * Server query-response for renaming an object.
     *
     * @param object A <code>FreeColGameObject</code> to rename.
     * @param name The name to apply.
     * @return True if the renaming succeeded.
     */
    private boolean askRename(FreeColGameObject object, String name) {
        RenameMessage message = new RenameMessage(object, name);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Use the active unit to build a colony.
     */
    public void buildColony() {
        if (!requireOurTurn()) return;
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();

        // Check unit can build, and is on the map.
        // Show the colony warnings if required.
        GUI gui = freeColClient.getGUI();
        Unit unit = gui.getActiveUnit();
        if (unit == null) {
            return;
        } else if (!unit.canBuildColony()) {
            canvas.showInformationMessage(unit, StringTemplate.template("buildColony.badUnit")
                                          .addName("%unit%", unit.getName()));
            return;
        }
        Tile tile = unit.getTile();
        if (tile.getColony() != null) {
            askJoinColony(unit, tile.getColony());
            return;
        } else if (!player.canAcquireToFoundSettlement(tile)) {
            canvas.showInformationMessage("buildColony.badTile");
            return;
        }

        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_COLONY_WARNINGS)
            && !showColonyWarnings(tile, unit)) {
            return;
        }

        if (tile.getOwner() != null && tile.getOwner() != player) {
            // Claim tile from other owners before founding a settlement.
            if (!claimTile(player, tile, null, player.getLandPrice(tile), 0))
                return;
            // One more check that founding can now proceed.
            if (!player.canClaimToFoundSettlement(tile)) return;
        }

        // Get and check the name.
        String name = player.getSettlementName();
        if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
            player.installSettlementNames(Messages.getSettlementNames(player),
                                          null);
            name = player.getSettlementName();
        }
        name = canvas.showInputDialog(tile, StringTemplate.key("nameColony.text"),
                                      name, "nameColony.yes", "nameColony.no",
                                      true);
        if (name == null) return; // User cancelled, 0-length invalid.

        if (player.getSettlement(name) != null) {
            // Colony name must be unique.
            canvas.showInformationMessage(tile, StringTemplate.template("nameColony.notUnique")
                                          .addName("%name%", name));
            return;
        }

        if (askBuildColony(name, unit) && tile.getSettlement() != null) {
            player.invalidateCanSeeTiles();
            freeColClient.playSound("sound.event.buildingComplete");
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile, false);

            // Check units present for treasure cash-in as they are now
            // suddenly in-colony.
            ArrayList<Unit> units = new ArrayList<Unit>(tile.getUnitList());
            for (Unit unitInTile : units) {
                checkCashInTreasureTrain(unitInTile);
            }
        }
    }

    /**
     * A colony is proposed to be built.  Show warnings if this has
     * disadvantages.
     *
     * @param tile The <code>Tile</code> on which the colony is to be built.
     * @param unit The <code>Unit</code> which is to build the colony.
     */
    private boolean showColonyWarnings(Tile tile, Unit unit) {
        boolean landLocked = true;
        boolean ownedByEuropeans = false;
        boolean ownedBySelf = false;
        boolean ownedByIndians = false;

        java.util.Map<GoodsType, Integer> goodsMap = new HashMap<GoodsType, Integer>();
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isFoodType()) {
                int potential = 0;
                if (tile.getType().isPrimaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            } else if (goodsType.isBuildingMaterial()) {
                while (goodsType.isRefined()) {
                    goodsType = goodsType.getRawMaterial();
                }
                int potential = 0;
                if (tile.getType().isSecondaryGoodsType(goodsType)) {
                    potential = tile.potential(goodsType, null);
                }
                goodsMap.put(goodsType, new Integer(potential));
            }
        }

        for (Tile newTile: tile.getSurroundingTiles(1)) {
            if (!newTile.isLand()) {
                landLocked = false;
            }
            for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
                entry.setValue(entry.getValue().intValue() +
                               newTile.potential(entry.getKey(), null));
            }
            Player tileOwner = newTile.getOwner();
            if (tileOwner == unit.getOwner()) {
                if (newTile.getOwningSettlement() != null) {
                    // we are using newTile
                    ownedBySelf = true;
                } else {
                    for (Tile ownTile: newTile.getSurroundingTiles(1)) {
                        Colony colony = ownTile.getColony();
                        if (colony != null && colony.getOwner() == unit.getOwner()) {
                            // newTile can be used from an own colony
                            ownedBySelf = true;
                            break;
                        }
                    }
                }
            } else if (tileOwner != null && tileOwner.isEuropean()) {
                ownedByEuropeans = true;
            } else if (tileOwner != null) {
                ownedByIndians = true;
            }
        }

        int food = 0;
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (entry.getKey().isFoodType()) {
                food += entry.getValue().intValue();
            }
        }

        ArrayList<ModelMessage> messages = new ArrayList<ModelMessage>();
        if (landLocked) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                          "buildColony.landLocked", unit,
                                          getSpecification().getGoodsType("model.goods.fish")));
        }
        if (food < 8) {
            messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                          "buildColony.noFood", unit,
                                          getSpecification().getPrimaryFoodType()));
        }
        for (Entry<GoodsType, Integer> entry : goodsMap.entrySet()) {
            if (!entry.getKey().isFoodType() && entry.getValue().intValue() < 4) {
                messages.add(new ModelMessage(ModelMessage.MessageType.MISSING_GOODS,
                                              "buildColony.noBuildingMaterials",
                                              unit, entry.getKey())
                             .add("%goods%", entry.getKey().getNameKey()));
            }
        }

        if (ownedBySelf) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.ownLand", unit));
        }
        if (ownedByEuropeans) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.EuropeanLand", unit));
        }
        if (ownedByIndians) {
            messages.add(new ModelMessage(ModelMessage.MessageType.WARNING,
                                          "buildColony.IndianLand", unit));
        }

        if (messages.isEmpty()) return true;
        ModelMessage[] modelMessages = messages.toArray(new ModelMessage[messages.size()]);
        return freeColClient.getCanvas().showConfirmDialog(unit.getTile(),
                                                           modelMessages,
                                                           "buildColony.yes",
                                                           "buildColony.no");
    }

    /**
     * Server query-response for building a colony.
     *
     * @param name The name for the colony.
     * @param unit The <code>Unit</code> that will build.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuildColony(String name, Unit unit) {
        Client client = freeColClient.getClient();
        BuildColonyMessage message = new BuildColonyMessage(name, unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for joining a colony.
     *
     * @param unit The <code>Unit</code> that will join.
     * @param colony The <code>Colony</code> to join.
     * @return True if the server interaction succeeded.
     */
    private boolean askJoinColony(Unit unit, Colony colony) {
        Client client = freeColClient.getClient();
        JoinColonyMessage message = new JoinColonyMessage(colony, unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Abandon a colony with no units.
     *
     * @param colony The <code>Colony</code> to be abandoned.
     */
    public void abandonColony(Colony colony) {
        if (!requireOurTurn()) return;
        Player player = freeColClient.getMyPlayer();

        // Sanity check
        if (colony == null || colony.getOwner() != player
            || colony.getUnitCount() > 0) {
            throw new IllegalStateException("Abandon bogus colony");
        }

        // Proceed to abandon
        Tile tile = colony.getTile();
        if (askAbandonColony(colony) && tile.getSettlement() == null) {
            player.invalidateCanSeeTiles();
            GUI gui = freeColClient.getGUI();
            gui.setActiveUnit(null);
            gui.setSelectedTile(tile, false);
        }
    }

    /**
     * Server query-response to abandon a colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     * @return True if the server interaction succeeded.
     */
    private boolean askAbandonColony(Colony colony) {
        AbandonColonyMessage message = new AbandonColonyMessage(colony);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Clears the goto orders of the given unit by setting its destination
     * to null.
     *
     * @param unit The <code>Unit</code> to clear the destination for.
     */
    public void clearGotoOrders(Unit unit) {
        if (unit != null && unit.getDestination() != null) {
            setDestination(unit, null);
        }
    }

    /**
     * Set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the destination was set.
     * @see Unit#setDestination(Location)
     */
    public boolean setDestination(Unit unit, Location destination) {
        if (unit.getTradeRoute() != null) {
            Canvas canvas = freeColClient.getCanvas();
            StringTemplate template = StringTemplate.template("traderoute.reassignRoute")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .add("%route%", unit.getTradeRoute().getName());
            if (!canvas.showConfirmDialog(unit.getTile(), template,
                                          "yes", "no")) return false;
        }
        return askSetDestination(unit, destination)
            && unit.getDestination() == destination;
    }

    /**
     * Server query-response to set the destination of the given unit.
     *
     * @param unit The <code>Unit</code> to direct.
     * @param destination The destination <code>Location</code>.
     * @return True if the server interaction succeeded.
     * @see Unit#setDestination(Location)
     */
    private boolean askSetDestination(Unit unit, Location destination) {
        SetDestinationMessage message
            = new SetDestinationMessage(unit, destination);
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Emigrate a unit from Europe.
     *
     * @param player The <code>Player</code> that owns the unit.
     * @param slot The slot to emigrate from.
     */
    private void emigrate(Player player, int slot) {
        Europe europe = player.getEurope();
        EuropeWas europeWas = new EuropeWas(europe);
        if (askEmigrate(slot)) {
            europeWas.fireChanges();
            freeColClient.getCanvas().updateGoldLabel();
        }
    }

    /**
     * Recruit a unit from a specified index in Europe.
     *
     * @param index The index in Europe to recruit from ([0..2]).
     */
    public void recruitUnitInEurope(int index) {
        if (!requireOurTurn()) return;

        Player player = freeColClient.getMyPlayer();
        if (!player.checkGold(player.getRecruitPrice())) {
            freeColClient.getCanvas().errorMessage("notEnoughGold");
            return;
        }

        emigrate(player, index + 1);
    }

    /**
     * Handle server query-response for emigration.
     *
     * @param slot The slot from which the unit migrates, 1-3 selects
     *             a specific one, otherwise the server will choose one.
     * @return True if the client-server interaction succeeded.
     */
    private boolean askEmigrate(int slot) {
        Client client = freeColClient.getClient();
        EmigrateUnitMessage message = new EmigrateUnitMessage(slot);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Moves the specified unit in a specified direction. This may
     * result in many different types of action.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the unit.
     */
    public void move(Unit unit, Direction direction) {
        if (!requireOurTurn()) return;

        moveDirection(unit, direction, true);

        // TODO: check if this is necessary for all actions?
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    freeColClient.getActionManager().update();
                    freeColClient.updateMenuBar();
                }
            });
    }

    /**
     * Move a unit along a path.
     *
     * @param unit The <code>Unit</code> to move.
     * @param path The path to follow.
     * @return True if the unit has completed the path and can move further.
     */
    private boolean movePath(Unit unit, PathNode path) {
        // Traverse the path to the destination.
        for (; path != null; path = path.next) {

            // Special case for the map edges on maps not
            // surrounded by high seas.
            if (unit.getDestination() instanceof Europe
                && unit.getTile() != null
                && unit.getTile().isAdjacentToMapEdge()) {
                moveToEurope(unit);
                return false;
            }

            if (!moveDirection(unit, path.getDirection(), false)) return false;
        }
        return true;
    }

    /**
     * Convenience function to find an adjacent settlement.  Intended
     * to be called in contexts where we are expecting a settlement to
     * be there, such as when handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return A settlement on the adjacent tile if any.
     */
    private Settlement getSettlementAt(Tile tile, Direction direction) {
        return tile.getNeighbourOrNull(direction).getSettlement();
    }

    /**
     * Convenience function to find the nation controlling an adjacent
     * settlement.  Intended to be called in contexts where we are
     * expecting a settlement or unit to be there, such as when
     * handling a particular move type.
     *
     * @param tile The <code>Tile</code> to start at.
     * @param direction The <code>Direction</code> to step.
     * @return The name of the nation controlling a settlement on the
     *         adjacent tile if any.
     */
    private StringTemplate getNationAt(Tile tile, Direction direction) {
        Tile newTile = tile.getNeighbourOrNull(direction);
        Player player = null;
        if (newTile.getSettlement() != null) {
            player = newTile.getSettlement().getOwner();
        } else if (newTile.getFirstUnit() != null) {
            player = newTile.getFirstUnit().getOwner();
        } else { // should not happen
            player = freeColClient.getGame().getUnknownEnemy();
        }
        return player.getNationName();
    }

    /**
     * Move a unit in a given direction.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The <code>Direction</code> to move in.
     * @param interactive Interactive mode: play sounds and emit errors.
     * @return True if the unit can possibly move further.
     */
    private boolean moveDirection(Unit unit, Direction direction,
                                  boolean interactive) {
        Canvas canvas = freeColClient.getCanvas();

        // Consider all the move types
        switch (unit.getMoveType(direction)) {
        case MOVE:
            moveMove(unit, direction);
            return unit.getMovesLeft() > 0;
        case MOVE_HIGH_SEAS:
            if (!interactive && unit.getDestination() instanceof Europe) {
                moveToEurope(unit);
                return false;
            }
            return moveHighSeas(unit, direction);
        case EXPLORE_LOST_CITY_RUMOUR:
            moveExplore(unit, direction);
            return false;
        case ATTACK:
            moveAttack(unit, direction);
            return false;
        case EMBARK:
            moveEmbark(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
            moveLearnSkill(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
            moveScoutIndianSettlement(unit, direction);
            return false;
        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
            moveUseMissionary(unit, direction);
            return false;
        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
            moveScoutColony(unit, direction);
            return false;
        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
            moveTrade(unit, direction);
            return false;

        case MOVE_NO_ACCESS_BEACHED:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessBeached")
                        .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_CONTACT:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessContact")
                        .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_LAND:
            if (!moveDisembark(unit, direction)) {
                if (interactive) {
                    freeColClient.playSound("sound.event.illegalMove");
                }
            }
            return false;
        case MOVE_NO_ACCESS_SETTLEMENT:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessSettlement")
                       .addStringTemplate("%unit%", Messages.getLabel(unit))
                       .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_SKILL:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessSkill")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_ACCESS_TRADE:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessTrade")
                        .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_WAR:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                StringTemplate nation = getNationAt(unit.getTile(), direction);
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessWar")
                        .addStringTemplate("%nation%", nation));
            }
            return false;
        case MOVE_NO_ACCESS_WATER:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAccessWater")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_ATTACK_MARINE:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
                canvas.showInformationMessage(unit,
                    StringTemplate.template("move.noAttackWater")
                    .addStringTemplate("%unit%", Messages.getLabel(unit)));
            }
            return false;
        case MOVE_NO_MOVES:
            if (!interactive) {
                // The unit may have some moves left, but not enough
                // to move to the next node.  Clear its remaining
                // moves on the client side only, to avoid it being
                // reselected.
                unit.setMovesLeft(0);
            }
            return false;
        default:
            if (interactive) {
                freeColClient.playSound("sound.event.illegalMove");
            }
            return false;
        }
    }

    /**
     * Actually move a unit in a specified direction, following a move
     * of MoveType.MOVE.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move the Unit.
     */
    private void moveMove(Unit unit, Direction direction) {
        // If we are in a colony, or Europe, load sentries.
        if (unit.canCarryUnits() && unit.getSpaceLeft() > 0
            && (unit.getColony() != null || unit.isInEurope())) {
            for (Unit sentry : new ArrayList<Unit>(unit.getLocation().getUnitList())) {
                if (sentry.getState() == UnitState.SENTRY) {
                    if (sentry.getSpaceTaken() <= unit.getSpaceLeft()) {
                        boardShip(sentry, unit);
                        logger.finest("Unit " + unit.toString()
                                      + " loaded sentry " + sentry.toString());
                    } else {
                        logger.finest("Unit " + sentry.toString()
                                      + " is too big to board " + unit.toString());
                    }
                }
            }
        }

        // Ask the server
        UnitWas unitWas = new UnitWas(unit);
        Element reply = askMove(unit, direction);
        if (reply == null) return;
        unitWas.fireChanges();

        // Handle special cases
        Game game = freeColClient.getGame();
        final Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        final Tile tile = unit.getTile();
        if (reply.hasAttribute("slowedBy")) { // ship slowed
            Unit slowedBy = (Unit) game.getFreeColGameObject(reply.getAttribute("slowedBy"));
            StringTemplate enemy = slowedBy.getOwner().getNationName();
            canvas.showInformationMessage(slowedBy,
                StringTemplate.template("model.unit.slowed")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .addStringTemplate("%enemyUnit%", Messages.getLabel(slowedBy))
                .addStringTemplate("%enemyNation%", enemy));
        }

        ModelMessage m = null;
        if (reply.hasAttribute("nameNewLand")) {
            String defaultName = reply.getAttribute("nameNewLand");
            String newLandName = canvas.showInputDialog(tile,
                                                        StringTemplate.key("newLand.text"),
                                                        defaultName,
                                                        "newLand.yes", null,
                                                        true);
            // Default out on null, 0-length invalid.
            if (newLandName == null) newLandName = defaultName;

            // Check for special welcome on landing.
            Player welcomer = null;
            boolean accept = false;
            if (reply.hasAttribute("welcome")) {
                String who = reply.getAttribute("welcome");
                if (game.getFreeColGameObjectSafely(who) instanceof Player) {
                    welcomer = (Player) game.getFreeColGameObjectSafely(who);
                    String messageId = (tile.getOwner() == welcomer)
                        ? "welcomeOffer.text" : "welcomeSimple.text";
                    String camps = reply.getAttribute("camps");
                    String type = ((IndianNationType) welcomer.getNationType())
                        .getSettlementTypeKey(true);
                    accept = canvas.showConfirmDialog(tile,
                                                      StringTemplate.template(messageId)
                                                      .addStringTemplate("%nation%", welcomer.getNationName())
                                                      .addName("%camps%", camps)
                                                      .add("%settlementType%", type),
                                                      "welcome.yes",
                                                      "welcome.no");
                }
            }

            Canvas.EventType event = null;
            if (askNewLandName(newLandName, welcomer, accept)
                && newLandName.equals(player.getNewLandName())) {
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            canvas.showEventPanel(tile, EventType.FIRST_LANDING);
                        }
                    });

                String key = FreeColActionUI.getHumanKeyStrokeText(freeColClient.getActionManager()
                                                                   .getFreeColAction("buildColonyAction").getAccelerator());
                m = new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                     "tutorial.buildColony", player)
                    .addName("%build_colony_key%", key)
                    .add("%build_colony_menu_item%", "buildColonyAction.name")
                    .add("%orders_menu_item%", "menuBar.orders");
                player.addModelMessage(m);
            }
        }

        if (reply.hasAttribute("discoverPacific")) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        canvas.showEventPanel(tile, EventType.DISCOVER_PACIFIC);
                    }
                });
        }
        if (reply.hasAttribute("discoverRegion")
            && reply.hasAttribute("regionType")) {
            String newRegionType = reply.getAttribute("regionType");
            String defaultName = reply.getAttribute("discoverRegion");
            String newRegionName = canvas.showInputDialog(unit.getTile(),
                                                          StringTemplate.template("nameRegion.text")
                                                          .addName("%name%", newRegionType),
                                                          defaultName, "ok", null, true);
            if (newRegionName == null || "".equals(newRegionName)) {
                newRegionName = defaultName;
            }
            askNewRegionName(newRegionName, unit);
        }

        if (reply.hasAttribute("fountainOfYouth")) {
            // Without Brewster, the migrants have already been selected
            // and were updated to the European docks by the server.
            final int migrants = Integer.parseInt(reply.getAttribute("fountainOfYouth"));
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < migrants; i++) {
                            int index = canvas.showEmigrationPanel(true);
                            askEmigrate(index + 1);
                        }
                    }
                });
        }

        // Perform a short pause on an active unit's last move if
        // the option is enabled.
        ClientOptions options = freeColClient.getClientOptions();
        if (unit.getMovesLeft() <= 0
            && options.getBoolean(ClientOptions.UNIT_LAST_MOVE_DELAY)) {
            canvas.paintImmediately(canvas.getBounds());
            try {
                Thread.sleep(UNIT_LAST_MOVE_DELAY);
            } catch (InterruptedException e) {} // Ignore
        }

        // Update the active unit and GUI.
        if (unit.isDisposed() || checkCashInTreasureTrain(unit)) {
            nextActiveUnit(tile);
        } else {
            if (tile.getColony() != null
                && unit.isCarrier()
                && unit.getTradeRoute() == null
                && (unit.getDestination() == null
                    || unit.getDestination().getTile() == tile.getTile())) {
                canvas.showColonyPanel(tile.getColony());
            }
            if (unit.getMovesLeft() == 0) {
                nextActiveUnit();
            } else {
                displayModelMessages(false);
            }
        }
    }

    /**
     * Server query-response for moving a unit.
     *
     * @param unit The <code>Unit</code> to move.
     * @param direction The direction to move in.
     * @return An <code>Element</code> containing the response, which
     *         may have special attributes set indicating further action,
     *         or null on failure.
     */
    private Element askMove(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        MoveMessage message = new MoveMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return null;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return reply;
    }

    /**
     * Server query-response for naming a new land.
     *
     * @param name The new land name.
     * @param welcomer A welcoming native player with whom to make a treaty.
     * @param accept True if the treaty was accepted.
     * @return True if the server interaction succeeded.
     */
    private boolean askNewLandName(String name, Player welcomer,
                                   boolean accept) {
        Client client = freeColClient.getClient();
        NewLandNameMessage message = new NewLandNameMessage(name,
                                                            welcomer, accept);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for naming a new region.
     *
     * @param name The new region name.
     * @param unit The <code>Unit</code> that discovered the region.
     * @return True if the server interaction succeeded.
     */
    private boolean askNewRegionName(String name, Unit unit) {
        Client client = freeColClient.getClient();
        NewRegionNameMessage message
            = new NewRegionNameMessage(name, unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Moves a unit onto the "high seas" in a specified direction following
     * a move of MoveType.MOVE_HIGH_SEAS.
     * This may result in a move to Europe, no move, or an ordinary move.
     *
     * @param unit The <code>Unit</code> to be moved.
     * @param direction The direction in which to move.
     */
    private boolean moveHighSeas(Unit unit, Direction direction) {
        // Confirm moving to Europe if told to move to a null tile
        // (TODO: can this still happen?), or if crossing the boundary
        // between coastal and high sea.  Otherwise just move.
        Tile oldTile = unit.getTile();
        Tile newTile = oldTile.getNeighbourOrNull(direction);
        Canvas canvas = freeColClient.getCanvas();
        if ((newTile == null
             || (!oldTile.canMoveToEurope() && newTile.canMoveToEurope()))
            && canvas.showConfirmDialog(oldTile, StringTemplate.template("highseas.text")
                                        .addAmount("%number%", getSpecification()
                                                   .getInteger("model.option.turnsToSail")),
                                        "highseas.yes", "highseas.no")) {
            moveToEurope(unit);
            nextActiveUnit();
            return false;
        }
        moveMove(unit, direction);
        return true;
    }

    /**
     * Moves the specified unit to America.
     *
     * @param unit The <code>Unit</code> to be moved to America.
     */
    public void moveToAmerica(Unit unit) {
        if (!requireOurTurn()) return;

        if (!(unit.getLocation() instanceof Europe)) {
            freeColClient.playSound("sound.event.illegalMove");
            return;
        }

        // Ask for autoload emigrants
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS)) {
            int spaceLeft = unit.getSpaceLeft();
            for (Unit u : new ArrayList<Unit>(unit.getLocation().getUnitList())) {
                if (!u.isNaval()) {
                    if (u.getType().getSpaceTaken() > spaceLeft) break;
                    boardShip(u, unit);
                    spaceLeft -= u.getType().getSpaceTaken();
                }
            }
        }

        if (askMoveToAmerica(unit)) {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for moving to America.
     *
     * @param unit The <code>Unit</code> to move.
     * @return True if the server interaction succeeded.
     */
    private boolean askMoveToAmerica(Unit unit) {
        Client client = freeColClient.getClient();
        MoveToAmericaMessage message = new MoveToAmericaMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Moves the specified unit to Europe.
     *
     * @param unit The <code>Unit</code> to be moved to Europe.
     */
    public void moveToEurope(Unit unit) {
        if (!requireOurTurn()) return;

        if (!unit.canMoveToEurope()) {
            freeColClient.playSound("sound.event.illegalMove");
            return;
        }

        if (askMoveToEurope(unit)) {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for moving to Europe.
     *
     * @param unit The <code>Unit</code> to move.
     * @return True if the server interaction succeeded.
     */
    private boolean askMoveToEurope(Unit unit) {
        Client client = freeColClient.getClient();
        MoveToEuropeMessage message = new MoveToEuropeMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Confirm exploration of a lost city rumour, following a move of
     * MoveType.EXPLORE_LOST_CITY_RUMOUR.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The direction of a rumour.
     */
    private void moveExplore(Unit unit, Direction direction) {
        // Confirm exploration.
        Canvas canvas = freeColClient.getCanvas();
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (canvas.showConfirmDialog(unit.getTile(),
                                     StringTemplate.key("exploreLostCityRumour.text"),
                                     "exploreLostCityRumour.yes",
                                     "exploreLostCityRumour.no")) {
            if (tile.getLostCityRumour().getType()
                == LostCityRumour.RumourType.MOUNDS
                && !canvas.showConfirmDialog(unit.getTile(),
                                             StringTemplate.key("exploreMoundsRumour.text"),
                                             "exploreLostCityRumour.yes",
                                             "exploreLostCityRumour.no")) {
                askDeclineMounds(unit, direction);
            }
            moveMove(unit, direction);
        }
    }

    /**
     * Server query-response for the special case of deciding to
     * explore a rumour but then declining not to investigate the
     * strange mounds.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The <code>Direction</code> to move.
     * @return True if the server interaction succeeded.
     */
    private boolean askDeclineMounds(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        DeclineMoundsMessage message = new DeclineMoundsMessage(unit,
                                                                direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Confirm attack or demand a tribute from a native settlement, following
     * a move of MoveType.ATTACK.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveAttack(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        clearGotoOrders(unit);

        // Extra option with native settlement
        Tile tile = unit.getTile();
        Tile target = tile.getNeighbourOrNull(direction);
        IndianSettlement is = target.getIndianSettlement();
        if (is != null && unit.isArmed()) {
            switch (canvas.showArmedUnitIndianSettlementDialog(is)) {
            case CANCEL:
                return;
            case INDIAN_SETTLEMENT_ATTACK:
                break; // Go on to usual attack confirmation.
            case INDIAN_SETTLEMENT_TRIBUTE:
                moveTribute(unit, direction);
                return;
            default:
                logger.warning("showArmedUnitIndianSettlementDialog failure.");
                return;
            }
        }

        // Normal attack confirmation.
        if (confirmHostileAction(unit, target)
            && confirmPreCombat(unit, target)) {
            attack(unit, direction);
        }
    }

    /**
     * Demand a tribute.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void moveTribute(Unit unit, Direction direction) {
        if (askDemandTribute(unit, direction)) {
            // Assume tribute paid
            freeColClient.getCanvas().updateGoldLabel();
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for demanding a tribute from a native
     * settlement.
     *
     * @param unit The <code>Unit</code> that demands.
     * @param direction The direction to demand in.
     * @return True if the server interaction succeeded.
     */
    private boolean askDemandTribute(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        DemandTributeMessage message
            = new DemandTributeMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param target The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmHostileAction(Unit attacker, Tile target) {
        if (attacker.hasAbility("model.ability.piracy")) {
            // Privateers can attack and remain at peace
            return true;
        }

        Player enemy;
        if (target.getSettlement() != null) {
            enemy = target.getSettlement().getOwner();
        } else if (target == attacker.getTile()) {
            // Fortify on tile owned by another nation
            enemy = target.getOwner();
            if (enemy == null) return true;
        } else {
            Unit defender = target.getDefendingUnit(attacker);
            if (defender == null) {
                logger.warning("Attacking, but no defender - will try!");
                return true;
            }
            if (defender.hasAbility("model.ability.piracy")) {
                // Privateers can be attacked and remain at peace
                return true;
            }
            enemy = defender.getOwner();
        }

        // Confirm attack given current stance
        Canvas canvas = freeColClient.getCanvas();
        String messageID = null;
        switch (attacker.getOwner().getStance(enemy)) {
        case WAR:
            logger.finest("Player at war, no confirmation needed");
            return true;
        case UNCONTACTED: case PEACE:
            messageID = "model.diplomacy.attack.peace";
            break;
        case CEASE_FIRE:
            messageID = "model.diplomacy.attack.ceaseFire";
            break;
        case ALLIANCE:
            messageID = "model.diplomacy.attack.alliance";
            break;
        }
        return canvas.showConfirmDialog(attacker.getTile(),
                                        StringTemplate.template(messageID)
                                        .addStringTemplate("%nation%", enemy.getNationName()),
                                        "model.diplomacy.attack.confirm",
                                        "cancel");
    }

    /**
     * If the client options include a pre-combat dialog, allow the
     * user to view the odds and possibly cancel the attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    private boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlement = tile.getSettlement();
            // Don't tell the player how a settlement is defended!
            FreeColGameObject defender = (settlement != null) ? settlement
                : tile.getDefendingUnit(attacker);
            Canvas canvas = freeColClient.getCanvas();
            return canvas.showPreCombatDialog(attacker, defender, tile);
        }
        return true;
    }

    /**
     * Attack.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private void attack(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Unit defender = unit.getTile().getNeighbourOrNull(direction)
            .getDefendingUnit(unit);
        String loot = askAttack(unit, direction);
        if ("true".equals(loot)) {
            nextModelMessage(); // See the combat message first
            List<Goods> goods = askLoot(unit, defender);
            if (goods != null) {
                goods = canvas.showCaptureGoodsDialog(unit, goods);
                askLoot(unit, defender, goods);
            }
        }
        canvas.refresh();
        nextActiveUnit();
    }

    /**
     * Server query-response for attacking.
     *
     * @param unit The <code>Unit</code> to perform the attack.
     * @param direction The direction in which to attack.
     */
    private String askAttack(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        AttackMessage message = new AttackMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return null;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return reply.getAttribute("loot");
    }

    /**
     * Server query-response for looting (initial query).
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param loser The <code>Unit</code> that is looted.
     * @return The <code>Goods</code> to loot.
     */
    private List<Goods> askLoot(Unit winner, Unit loser) {
        Client client = freeColClient.getClient();
        LootCargoMessage message = new LootCargoMessage(winner, loser.getId(),
                                                        null);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     "lootCargo");
        return (reply == null) ? null
            : new LootCargoMessage(freeColClient.getGame(), reply).getGoods();
    }

    /**
     * Server query-response for looting (request to loot).
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param loser The <code>Unit</code> that is looted.
     */
    private void askLoot(Unit winner, Unit loser, List<Goods> goods) {
        Client client = freeColClient.getClient();
        LootCargoMessage message = new LootCargoMessage(winner, loser.getId(),
                                                        goods);
        Element reply = askExpecting(client, message.toXMLElement(), null);

        if (reply == null) return;
        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
    }

    /**
     * Embarks the specified unit onto a carrier in a specified direction
     * following a move of MoveType.EMBARK.
     *
     * @param unit The <code>Unit</code> that wishes to embark.
     * @param direction The direction in which to embark.
     */
    private void moveEmbark(Unit unit, Direction direction) {
        clearGotoOrders(unit);

        // Choose which carrier to embark upon.
        Canvas canvas = freeColClient.getCanvas();
        Tile sourceTile = unit.getTile();
        Tile destinationTile = sourceTile.getNeighbourOrNull(direction);
        Unit carrier = null;
        List<ChoiceItem<Unit>> choices = new ArrayList<ChoiceItem<Unit>>();
        for (Unit u : destinationTile.getUnitList()) {
            if (u.getSpaceLeft() >= unit.getType().getSpaceTaken()) {
                String m = Messages.message(Messages.getLabel(u));
                choices.add(new ChoiceItem<Unit>(m, u));
                carrier = u; // Save a default
            }
        }
        if (choices.size() == 0) {
            throw new RuntimeException("Unit " + unit.getId()
                                       + " found no carrier to embark upon.");
        } else if (choices.size() == 1) {
            // Use the default
        } else {
            carrier = canvas.showChoiceDialog(unit.getTile(),
                                              Messages.message("embark.text"),
                                              Messages.message("embark.cancel"),
                                              choices);
            if (carrier == null) return; // User cancelled
        }

        // Proceed to embark
        if (askEmbark(unit, carrier, direction)
            && unit.getLocation() == carrier) {
            if (carrier.getMovesLeft() > 0) {
                freeColClient.getGUI().setActiveUnit(carrier);
            } else {
                nextActiveUnit();
            }
        }
        clearGotoOrders(unit);
    }

    /**
     * Check the carrier for passengers to disembark, possibly
     * snatching a useful result from the jaws of a
     * MOVE_NO_ACCESS_LAND failure.
     *
     * @param unit The carrier containing the unit to disembark.
     * @param direction The direction in which to disembark the unit.
     * @return True if the disembark "succeeds" (which deliberately includes
     *         declined disembarks).
     */
    private boolean moveDisembark(Unit unit, Direction direction) {
        Tile tile = unit.getTile().getNeighbourOrNull(direction);
        if (tile.getFirstUnit() != null
            && tile.getFirstUnit().getOwner() != unit.getOwner()) {
            return false; // Can not disembark onto other nation units.
        }

        // Disembark selected units able to move.
        List<Unit> disembarkable = new ArrayList<Unit>();
        unit.setStateToAllChildren(UnitState.ACTIVE);
        for (Unit u : unit.getUnitList()) {
            if (u.getMoveType(tile).isProgress()) {
                disembarkable.add(u);
            }
        }
        if (disembarkable.size() == 0) {
            // Did not find any unit that could disembark, fail.
            return false;
        }

        // Pick units the user wants to disembark.
        Canvas canvas = freeColClient.getCanvas();
        while (disembarkable.size() > 0) {
            if (disembarkable.size() == 1) {
                if (canvas.showConfirmDialog("disembark.text", "yes", "no")) {
                    move(disembarkable.get(0), direction);
                }
                break;
            }
            List<ChoiceItem<Unit>> choices = new ArrayList<ChoiceItem<Unit>>();
            for (Unit dUnit : disembarkable) {
                choices.add(new ChoiceItem<Unit>(Messages.message(Messages.getLabel(dUnit)), dUnit));
            }
            if (disembarkable.size() > 1) {
                choices.add(new ChoiceItem<Unit>(Messages.message("all"), unit));
            }
            Unit u = canvas.showChoiceDialog(unit.getTile(),
                                             Messages.message("disembark.text"),
                                             Messages.message("disembark.cancel"),
                                             choices);
            if (u == null) break; // Done
            // Call move() as while the destination tile is known to
            // be clear of settlements or other player units, it *may*
            // have a rumour.
            if (u == unit) {
                for (Unit dUnit : disembarkable) move(dUnit, direction);
                disembarkable.clear();
            } else {
                move(u, direction);
                disembarkable.remove(u);
            }
        }
        return true;
    }

    /**
     * Move a free colonist to a native settlement to learn a skill following
     * a move of MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST.
     * The colonist does not physically get into the village, it will
     * just stay where it is and gain the skill.
     *
     * @param unit The <code>Unit</code> to learn the skill.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveLearnSkill(Unit unit, Direction direction) {
        clearGotoOrders(unit);
        // Refresh knowledge of settlement skill.  It may have been
        // learned by another player.
        if (!askSkill(unit, direction)) {
            return;
        }

        Canvas canvas = freeColClient.getCanvas();
        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            canvas.showInformationMessage(settlement,
                                          "indianSettlement.noMoreSkill");
        } else if (!unit.getType().canBeUpgraded(skill, ChangeType.NATIVES)) {
            canvas.showInformationMessage(settlement,
                StringTemplate.template("indianSettlement.cantLearnSkill")
                .addStringTemplate("%unit%", Messages.getLabel(unit))
                .add("%skill%", skill.getNameKey()));
        } else if (canvas.showConfirmDialog(unit.getTile(),
                                            StringTemplate.template("learnSkill.text")
                                            .add("%skill%", skill.getNameKey()),
                                            "learnSkill.yes", "learnSkill.no")) {
            if (askLearnSkill(unit, direction)) {
                if (unit.isDisposed()) {
                    canvas.showInformationMessage(settlement, "learnSkill.die");
                    nextActiveUnit(unit.getTile());
                    return;
                }
                if (unit.getType() != skill) {
                    canvas.showInformationMessage(settlement, "learnSkill.leave");
                }
            }
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for finding out the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    private boolean askSkill(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        AskSkillMessage message = new AskSkillMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for learning the skill taught at a settlement.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The direction to a settlement to ask.
     * @return True if the server interaction succeeded.
     */
    private boolean askLearnSkill(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        LearnSkillMessage message = new LearnSkillMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Move a scout into an Indian settlement to speak with the chief,
     * or demand a tribute following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT.
     * The scout does not physically get into the village, it will
     * just stay where it is.
     *
     * @param unit The <code>Unit</code> that is scouting.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveScoutIndianSettlement(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Tile unitTile = unit.getTile();
        Tile tile = unitTile.getNeighbourOrNull(direction);
        IndianSettlement settlement = tile.getIndianSettlement();
        clearGotoOrders(unit);

        // Offer the choices.
        NationSummary ns = getNationSummary(settlement.getOwner());
        String number = (ns == null) ? "1" : ns.getNumberOfSettlements();
        switch (canvas.showScoutIndianSettlementDialog(settlement, number)) {
        case CANCEL:
            return;
        case INDIAN_SETTLEMENT_ATTACK:
            if (confirmPreCombat(unit, tile)) {
                attack(unit, direction);
            }
            return;
        case INDIAN_SETTLEMENT_SPEAK:
            Player player = unit.getOwner();
            final int oldGold = player.getGold();
            String result = askScoutSpeak(unit, direction);
            if (result == null) {
                logger.warning("Null result from askScoutSpeak");
            } else if ("die".equals(result)) {
                canvas.showInformationMessage(settlement,
                                              "scoutSettlement.speakDie");
                nextActiveUnit(unitTile);
                return;
            } else if ("expert".equals(result)) {
                canvas.showInformationMessage(settlement,
                    StringTemplate.template("scoutSettlement.expertScout")
                    .add("%unit%", unit.getType().getNameKey()));
            } else if ("tales".equals(result)) {
                canvas.showInformationMessage(settlement,
                                              "scoutSettlement.speakTales");
            } else if ("beads".equals(result)) {
                canvas.updateGoldLabel();
                canvas.showInformationMessage(settlement,
                                              StringTemplate.template("scoutSettlement.speakBeads")
                                              .addAmount("%amount%", player.getGold() - oldGold));
            } else if ("nothing".equals(result)) {
                canvas.showInformationMessage(settlement,
                                              "scoutSettlement.speakNothing");
            } else {
                logger.warning("Invalid result from askScoutSpeak: " + result);
            }
            nextActiveUnit();
            break;
        case INDIAN_SETTLEMENT_TRIBUTE:
            moveTribute(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutIndianSettlementDialog fail");
        }
    }

    /**
     * Server query-response for speaking with a native chief.
     *
     * @param unit The <code>Unit</code> that is speaking.
     * @param direction The direction to a settlement to ask.
     * @return A string containing the value of the "result" attribute on the
     *         reply from the server, or null on failure.
     */
    private String askScoutSpeak(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        ScoutIndianSettlementMessage message
            = new ScoutIndianSettlementMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return null;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return reply.getAttribute("result");
    }

    /**
     * Move a missionary into a native settlement, following a move of
     * MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY.
     *
     * @param unit The <code>Unit</code> that will enter the settlement.
     * @param direction The direction in which the Indian settlement lies.
     */
    private void moveUseMissionary(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        IndianSettlement settlement
            = (IndianSettlement) getSettlementAt(unit.getTile(), direction);
        Unit missionary = settlement.getMissionary();
        boolean canEstablish = missionary == null;
        boolean canDenounce = missionary != null
            && missionary.getOwner() != unit.getOwner();
        clearGotoOrders(unit);

        // Offer the choices.
        switch (canvas.showUseMissionaryDialog(unit, settlement,
                                               canEstablish, canDenounce)) {
        case CANCEL:
            return;
        case ESTABLISH_MISSION:
            if (askMissionary(unit, direction, false)) {
                if (settlement.getMissionary() == unit) {
                    freeColClient.playSound("sound.event.missionEstablished");
                }
                nextActiveUnit();
            }
            break;
        case DENOUNCE_HERESY:
            if (askMissionary(unit, direction, true)) {
                if (settlement.getMissionary() == unit) {
                    freeColClient.playSound("sound.event.missionEstablished");
                }
                nextModelMessage();
                nextActiveUnit();
            }
            break;
        case INCITE_INDIANS:
            List<Player> enemies
                = new ArrayList<Player>(freeColClient.getGame().getLiveEuropeanPlayers());
            Player player = freeColClient.getMyPlayer();
            enemies.remove(player);
            Player enemy = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                         "missionarySettlement.inciteQuestion",
                                                         "missionarySettlement.cancel",
                                                         enemies);
            if (enemy == null) return;
            int gold = askIncite(unit, direction, enemy, -1);
            if (gold < 0) {
                // protocol fail
            } else if (!player.checkGold(gold)) {
                canvas.showInformationMessage(settlement,
                                              StringTemplate.template("missionarySettlement.inciteGoldFail")
                                              .add("%player%", enemy.getName())
                                              .addAmount("%amount%", gold));
            } else {
                if (canvas.showConfirmDialog(unit.getTile(),
                                             StringTemplate.template("missionarySettlement.inciteConfirm")
                                             .add("%player%", enemy.getName())
                                             .addAmount("%amount%", gold),
                                             "yes", "no")) {
                    if (askIncite(unit, direction, enemy, gold) > 0) {
                        canvas.updateGoldLabel();
                    }
                }
                nextActiveUnit();
            }
            break;
        default:
            logger.warning("showUseMissionaryDialog fail");
            break;
        }
    }

    /**
     * Server query-response for establishing/denouncing a mission.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to establish with.
     * @param denounce True if this is a denouncement.
     * @return True if the server interaction succeeded.
     */
    private boolean askMissionary(Unit unit, Direction direction,
                                  boolean denounce) {
        Client client = freeColClient.getClient();
        MissionaryMessage message
            = new MissionaryMessage(unit, direction, denounce);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Server query-response for inciting the natives.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The direction to a settlement to speak to.
     * @param enemy An enemy <code>Player</code>.
     * @param gold The amount of bribe, negative to enquire.
     * @return An amount of gold needed or paid, or negative if the
     *         server interaction failed.
     */
    private int askIncite(Unit unit, Direction direction, Player enemy,
                          int gold) {
        Client client = freeColClient.getClient();
        InciteMessage message
            = new InciteMessage(unit, direction, enemy, gold);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null || reply.getAttribute("gold") == null) return -1;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        try {
            return Integer.parseInt(reply.getAttribute("gold"));
        } catch (NumberFormatException e) {}
        return -1;
    }

    /**
     * Move to a foreign colony and either attack, negotiate with the
     * foreign power or spy on them.  Follows a move of
     * MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT.
     * TODO: Unify trade and negotiation.
     *
     * @param unit The unit that will spy, negotiate or attack.
     * @param direction The direction in which the foreign colony lies.
     */
    private void moveScoutColony(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Colony colony = (Colony) getSettlementAt(unit.getTile(), direction);
        boolean canNeg = colony.getOwner() != unit.getOwner().getREFPlayer();
        clearGotoOrders(unit);

        switch (canvas.showScoutForeignColonyDialog(colony, unit, canNeg)) {
        case CANCEL:
            break;
        case FOREIGN_COLONY_ATTACK:
            moveAttack(unit, direction);
            break;
        case FOREIGN_COLONY_NEGOTIATE:
            moveTradeColony(unit, direction);
            break;
        case FOREIGN_COLONY_SPY:
            moveSpy(unit, direction);
            break;
        default:
            throw new IllegalArgumentException("showScoutForeignColonyDialog fail");
        }
    }

    /**
     * Initiates a negotiation with a foreign power. The player
     * creates a DiplomaticTrade with the NegotiationDialog. The
     * DiplomaticTrade is sent to the other player. If the other
     * player accepts the offer, the trade is concluded.  If not, this
     * method returns, since the next offer must come from the other
     * player.
     *
     * @param unit The <code>Unit</code> negotiating.
     * @param direction The direction of a settlement to negotiate with.
     */
    private void moveTradeColony(Unit unit, Direction direction) {
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement == null) return;

        // Can not negotiate with the REF.
        if (settlement.getOwner() == unit.getOwner().getREFPlayer()) {
            throw new IllegalStateException("Unit tried to negotiate with REF");
        }

        String nation = Messages.message(settlement.getOwner().getNationName());
        Player player = freeColClient.getMyPlayer();
        Client client = freeColClient.getClient();
        Canvas canvas = freeColClient.getCanvas();
        DiplomaticTrade ourAgreement = null;
        DiplomaticTrade theirAgreement = null;
        Boolean done = false;
        while (!done) {
            ourAgreement = canvas.showNegotiationDialog(unit, settlement,
                                                        theirAgreement);
            if (ourAgreement == null) {
                if (theirAgreement != null) {
                    // Inform of rejection of the old agreement
                    theirAgreement.setStatus(TradeStatus.REJECT_TRADE);
                    client.sendAndWait(new DiplomacyMessage(unit,
                            settlement, theirAgreement).toXMLElement());
                }
                break;
            }

            // Send this acceptance or proposal to the other player
            theirAgreement = askDiplomacy(unit, settlement, ourAgreement);

            // What did they say?
            TradeStatus status
                = (theirAgreement == null) ? TradeStatus.REJECT_TRADE
                : theirAgreement.getStatus();
            switch (status) {
            case ACCEPT_TRADE:
                canvas.showInformationMessage(settlement,
                                              StringTemplate.template("negotiationDialog.offerAccepted")
                                              .addName("%nation%", nation));
                // Colony and unit ownership could change!
                player.invalidateCanSeeTiles();
                done = true;
                break;
            case REJECT_TRADE:
                canvas.showInformationMessage(settlement,
                                              StringTemplate.template("negotiationDialog.offerRejected")
                                              .add("%nation%", nation));
                done = true;
                break;
            case PROPOSE_TRADE:
                break; // Loop with this proposal
            default:
                logger.warning("Bogus trade status");
                done = true;
                break;
            }
        }
        nextActiveUnit();
    }

    /**
     * Handler server query-response for diplomatic messages.
     *
     * @param unit The <code>Unit</code> conducting the diplomacy.
     * @param settlement The <code>Settlement</code> to negotiate with.
     * @param agreement The <code>DiplomaticTrade</code> agreement to propose.
     * @return The agreement returned from the other party, or null.
     */
    private DiplomaticTrade askDiplomacy(Unit unit, Settlement settlement,
                                         DiplomaticTrade agreement) {
        Client client = freeColClient.getClient();
        Game game = freeColClient.getGame();
        DiplomacyMessage message = new DiplomacyMessage(unit, settlement,
                                                        agreement);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return null;

        // The reply should contain diplomacy somewhere.  Hoick it out
        // and return its agreement for interactive handling rather
        // than processing it in the input handler.
        Element diplomacy;
        if (reply.getTagName().equals("diplomacy")) {
            diplomacy = reply;
            reply = null;
        } else {
            diplomacy = Message.getChildElement(reply, "diplomacy");
            if (diplomacy != null) {
                reply.removeChild(diplomacy);
            }
        }
        // Process any residual updates.
        if (reply != null) {
            Connection conn = client.getConnection();
            freeColClient.getInGameInputHandler().handle(conn, reply);
        }
        return (diplomacy == null) ? null
            : new DiplomacyMessage(game, (Element) diplomacy).getAgreement();
    }

    /**
     * Spy on a foreign colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     */
    private void moveSpy(Unit unit, Direction direction) {
        if (askSpy(unit, direction)) {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for spying on a colony.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> of a colony to spy on.
     * @return True if the client/server interaction succeeded.
     */
    private boolean askSpy(Unit unit, Direction direction) {
        Client client = freeColClient.getClient();
        SpySettlementMessage message
            = new SpySettlementMessage(unit, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Arrive at a settlement with a laden carrier following a move of
     * MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS.
     *
     * @param unit The carrier.
     * @param direction The direction to the settlement.
     */
    private void moveTrade(Unit unit, Direction direction) {
        clearGotoOrders(unit);

        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        if (settlement instanceof Colony) {
            moveTradeColony(unit, direction);
        } else if (settlement instanceof IndianSettlement) {
            moveTradeIndianSettlement(unit, direction);
        } else {
            logger.warning("Bogus settlement: " + settlement.getId());
        }
    }

    /**
     * Trading with the natives, including buying, selling and
     * delivering gifts.  (Deliberate use of Settlement rather than
     * IndianSettlement throughout these routines as some unification
     * with colony trading is anticipated, and the native AI already
     * uses the same DeliverGiftMessage to deliver gifts to Colonies).
     *
     * @param unit The <code>Unit</code> that is a carrier containing goods.
     * @param direction The direction the unit could move in order to enter a
     *            <code>Settlement</code>.
     * @exception IllegalArgumentException if the unit is not a carrier, or if
     *                there is no <code>Settlement</code> in the given
     *                direction.
     * @see Settlement
     */
    private void moveTradeIndianSettlement(Unit unit, Direction direction) {
        Canvas canvas = freeColClient.getCanvas();
        Settlement settlement = getSettlementAt(unit.getTile(), direction);
        java.util.Map<String, Boolean> session;
        boolean done = false;

        while (!done) {
            session = askOpenTransactionSession(unit, settlement);
            if (session == null) break;
            // The session tracks buy/sell/gift events and disables
            // canFoo when one happens.  So only offer such options if
            // the session allows it and the carrier is in good shape.
            boolean buy = session.get("canBuy")  && (unit.getSpaceLeft() > 0);
            boolean sel = session.get("canSell") && (unit.getGoodsCount() > 0);
            boolean gif = session.get("canGift") && (unit.getGoodsCount() > 0);
            if (!buy && !sel && !gif) break;

            switch (canvas.showIndianSettlementTradeDialog(settlement,
                                                           buy, sel, gif)) {
            case CANCEL:
                done = true;
                break;
            case BUY:
                attemptBuyFromSettlement(unit, settlement);
                break;
            case SELL:
                attemptSellToSettlement(unit, settlement);
                break;
            case GIFT:
                attemptGiftToSettlement(unit, settlement);
                break;
            default:
                throw new IllegalArgumentException("showIndianSettlementTradeDialog fail");
            }
        }

        askCloseTransactionSession(unit, settlement);
        if (unit.getMovesLeft() > 0) { // May have been restored if no trade
            freeColClient.getGUI().setActiveUnit(unit);
        } else {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response to get the transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return A transaction session or null on failure.
     */
    private java.util.Map<String,Boolean> askOpenTransactionSession(Unit unit, Settlement settlement) {
        Client client = freeColClient.getClient();
        GetTransactionMessage message = new GetTransactionMessage(unit, settlement);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return null;

        java.util.Map<String,Boolean> session = new HashMap<String,Boolean>();
        session.put("canBuy",  Boolean.parseBoolean(reply.getAttribute("canBuy")));
        session.put("canSell", Boolean.parseBoolean(reply.getAttribute("canSell")));
        session.put("canGift", Boolean.parseBoolean(reply.getAttribute("canGift")));
        return session;
    }

    /**
     * Server query-response to close a transaction session for a trade.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return True if the server interaction succeeded.
     */
    private boolean askCloseTransactionSession(Unit unit, Settlement settlement) {
        Client client = freeColClient.getClient();
        CloseTransactionMessage message
            = new CloseTransactionMessage(unit, settlement);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for buying from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptBuyFromSettlement(Unit unit, Settlement settlement) {
        // Get list of goods for sale
        List<Goods> forSale = askGoodsForSaleInSettlement(unit, settlement);

        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        Goods goods = null;

        for (;;) {
            if (forSale.isEmpty()) {
                // There is nothing to sell to the player
                canvas.showInformationMessage(settlement, "trade.nothingToSell");
                return;
            }

            // Choose goods to buy
            goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                  "buyProposition.text",
                                                  "buyProposition.nothing",
                                                  forSale);
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askBuyPriceFromSettlement(unit, settlement, goods, gold);
                if (gold == NO_TRADE) { // Proposal was refused
                    canvas.showInformationMessage(settlement, "trade.noTrade");
                    return;
                } else if (gold < NO_TRADE) { // failure
                    return;
                }

                // Show dialog for buy proposal
                boolean canBuy = player.checkGold(gold);
                switch (canvas.showBuyDialog(unit, settlement, goods, gold,
                                             canBuy)) {
                case CANCEL: // User cancelled
                    return;
                case BUY: // Accept price, make purchase
                    if (askBuyFromSettlement(unit, settlement, goods, gold)) {
                        canvas.updateGoldLabel(); // Assume success
                    }
                    return;
                case HAGGLE: // Try to negotiate a lower price
                    gold = gold * 9 / 10;
                    break;
                default:
                    throw new IllegalStateException("showBuyDialog fail");
                }
            }
        }
    }

    /**
     * Server query-response to get a list of goods for sale from a settlement.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @return The list of goods for sale, or null on failure.
     */
    private List<Goods> askGoodsForSaleInSettlement(Unit unit,
                                                    Settlement settlement) {
        Client client = freeColClient.getClient();
        GoodsForSaleMessage message
            = new GoodsForSaleMessage(unit, settlement, null);
        Element reply = askExpecting(client, message.toXMLElement(),
                GoodsForSaleMessage.getXMLElementTagName());
        if (reply == null) return null;

        Game game = freeColClient.getGame();
        ArrayList<Goods> goodsOffered = new ArrayList<Goods>();
        NodeList childNodes = reply.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            goodsOffered.add(new Goods(game, (Element) childNodes.item(i)));
        }
        return goodsOffered;
    }

    /**
     * Server query-response to ask the natives if a purchase is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The asking price,
     *         or NO_TRADE if the trade is outright refused,
     *         or NO_TRADE-1 on error.
     */
    private int askBuyPriceFromSettlement(Unit unit, Settlement settlement,
                                          Goods goods, int gold) {
        Client client = freeColClient.getClient();
        BuyPropositionMessage message
            = new BuyPropositionMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return NO_TRADE - 1; // signal failure

        try {
            return Integer.parseInt(reply.getAttribute("gold"));
        } catch (NumberFormatException e) {
            return NO_TRADE - 1;
        }
    }

    /**
     * Server query-response to buy the given goods from the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuyFromSettlement(Unit unit, Settlement settlement,
                                         Goods goods, int gold) {
        Client client = freeColClient.getClient();
        BuyMessage message = new BuyMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for selling to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptSellToSettlement(Unit unit, Settlement settlement) {
        Canvas canvas = freeColClient.getCanvas();
        Goods goods = null;
        for (;;) {
            // Choose goods to sell
            goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                  "sellProposition.text",
                                                  "sellProposition.nothing",
                                                  unit.getGoodsList());
            if (goods == null) break; // Trade aborted by the player

            int gold = -1; // Initially ask for a price
            for (;;) {
                gold = askSellPriceToSettlement(unit, settlement, goods, gold);
                if (gold == NO_NEED_FOR_THE_GOODS) {
                    canvas.showInformationMessage(settlement,
                        StringTemplate.template("trade.noNeedForTheGoods")
                        .add("%goods%", goods.getNameKey()));
                    return;
                } else if (gold == NO_TRADE) {
                    canvas.showInformationMessage(settlement, "trade.noTrade");
                    return;
                } else if (gold < NO_TRADE) { // error
                    return;
                }

                // Show dialog for sale proposal
                switch (canvas.showSellDialog(unit, settlement, goods, gold)) {
                case CANCEL:
                    return;
                case SELL: // Accepted price, make the sale
                    if (askSellToSettlement(unit, settlement, goods, gold)) {
                        canvas.updateGoldLabel(); // Assume success
                    }
                    return;
                case HAGGLE: // Ask for more money
                    gold = (gold * 11) / 10;
                    break;
                case GIFT: // Decide to make a gift of the goods
                    askDeliverGiftToSettlement(unit, settlement, goods);
                    return;
                default:
                    throw new IllegalStateException("showSellDialog fail");
                }
            }
        }
    }

    /**
     * Server query-response to ask the natives if a sale is acceptable.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to trade.
     * @param gold The proposed price (including query on negative).
     * @return The asking price, or NO_NEED_FOR_GOODS if they do not want them,
     *         NO_TRADE if the trade is outright refused,
     *         or NO_TRADE-1 on error.
     */
    private int askSellPriceToSettlement(Unit unit, Settlement settlement,
                                         Goods goods, int gold) {
        Client client = freeColClient.getClient();
        SellPropositionMessage message
            = new SellPropositionMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return NO_TRADE - 1; // Signal failure

        try {
            return Integer.parseInt(reply.getAttribute("gold"));
        } catch (NumberFormatException e) {
            return NO_TRADE - 1;
        }
    }

    /**
     * Server query-response to sell the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The agreed price.
     * @return True if the server interaction succeeded.
     */
    private boolean askSellToSettlement(Unit unit, Settlement settlement,
                                        Goods goods, int gold) {
        Client client = freeColClient.getClient();
        SellMessage message = new SellMessage(unit, settlement, goods, gold);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * User interaction for delivering a gift to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    private void attemptGiftToSettlement(Unit unit, Settlement settlement) {
        Canvas canvas = freeColClient.getCanvas();
        Goods goods = canvas.showSimpleChoiceDialog(unit.getTile(),
                                                    "gift.text", "cancel",
                                                    unit.getGoodsList());
        if (goods != null) {
            askDeliverGiftToSettlement(unit, settlement, goods);
        }
    }

    /**
     * Server query-response to give the given goods to the natives.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to give.
     * @return True if the server interaction succeeded.
     */
    private boolean askDeliverGiftToSettlement(Unit unit, Settlement settlement,
                                               Goods goods) {
        Client client = freeColClient.getClient();
        DeliverGiftMessage message
            = new DeliverGiftMessage(unit, settlement, goods);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    // End of move-consequents


    /**
     * Claim a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param colony An optional <code>Colony</code> to own the tile.
     * @param offer An offer to pay.
     * @return True if the claim succeeded.
     */
    public boolean claimLand(Tile tile, Colony colony, int offer) {
        if (!requireOurTurn()) return false;

        Player player = freeColClient.getMyPlayer();
        int price = ((colony != null) ? player.canClaimForSettlement(tile)
                     : player.canClaimForImprovement(tile)) ? 0
            : player.getLandPrice(tile);
        return claimTile(player, tile, colony, price, offer);
    }

    /**
     * Claim a tile.
     *
     * @param player The <code>Player</code> that is claiming.
     * @param tile The <code>Tile</code> to claim.
     * @param colony An optional <code>Colony</code> to own the tile.
     * @param price The price required.
     * @param offer An offer to pay.
     * @return True if the claim succeeded.
     */
    private boolean claimTile(Player player, Tile tile, Colony colony,
                              int price, int offer) {
        Canvas canvas = freeColClient.getCanvas();
        Player owner = tile.getOwner();
        if (price < 0) return false; // not for sale
        if (price > 0) { // for sale by natives
            if (offer >= price) { // offered more than enough
                price = offer;
            } else if (offer < 0) { // plan to steal
                price = NetworkConstants.STEAL_LAND;
            } else {
                boolean canAccept = player.checkGold(price);
                switch (canvas.showClaimDialog(tile, player, price,
                                               owner, canAccept)) {
                case CANCEL:
                    return false;
                case ACCEPT: // accepted price
                    break;
                case STEAL:
                    price = NetworkConstants.STEAL_LAND;
                    break;
                default:
                    throw new IllegalStateException("showClaimDialog fail");
                }
            }
        } // else price == 0 and we can just proceed

        // Ask the server
        if (askClaimLand(tile, colony, price) && tile.getOwner() == player) {
            canvas.updateGoldLabel();
            return true;
        }
        return false;
    }

    /**
     * Server query-response to claim a piece of land.
     *
     * @param tile The land to claim.
     * @param colony An optional <code>Colony</code> to own the land.
     * @param price The amount to pay.
     * @return True if the server interaction succeeded.
     */
    private boolean askClaimLand(Tile tile, Colony colony, int price) {
        Client client = freeColClient.getClient();
        ClaimLandMessage message = new ClaimLandMessage(tile, colony, price);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Check if a unit is a treasure train, and if it should be cashed in.
     * Transfers the gold carried by this unit to the {@link Player owner}.
     *
     * @param unit The <code>Unit</code> to be checked.
     * @return True if the unit was cashed in (and disposed).
     */
    public boolean checkCashInTreasureTrain(Unit unit) {
        if (!unit.canCarryTreasure() || !unit.canCashInTreasureTrain()
            || !requireOurTurn()) {
            return false; // Fail quickly if just not a candidate.
        }

        // Cash in or not?
        Canvas canvas = freeColClient.getCanvas();
        boolean cash;
        Tile tile = unit.getTile();
        Europe europe = unit.getOwner().getEurope();
        if (europe == null || unit.getLocation() == europe) {
            cash = true; // No need to check for transport.
        } else {
            int fee = getSpecification()
                .getInteger("model.option.treasureTransportFee");
            StringTemplate template = (fee == 0)
                ? StringTemplate.template("cashInTreasureTrain.free")
                : StringTemplate.template("cashInTreasureTrain.pay")
                    .addName("%fee%", Integer.toString(fee));
            cash = canvas.showConfirmDialog(unit.getTile(), template,
                                            "cashInTreasureTrain.yes",
                                            "cashInTreasureTrain.no");
        }

        // Update if cash in succeeds.
        UnitWas unitWas = new UnitWas(unit);
        if (cash && askCashInTreasureTrain(unit) && unit.isDisposed()) {
            freeColClient.playSound("sound.event.cashInTreasureTrain");
            unitWas.fireChanges();
            canvas.updateGoldLabel();
            nextActiveUnit(tile);
            return true;
        }
        return false;
    }

    /**
     * Server query-response to cash in a treasure train.
     *
     * @param unit The treasure train <code>Unit</code> to cash in.
     * @return True if the server interaction succeeded.
     */
    private boolean askCashInTreasureTrain(Unit unit) {
        Client client = freeColClient.getClient();
        CashInTreasureTrainMessage message
            = new CashInTreasureTrainMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Boards a specified unit onto a carrier.
     * The carrier must be at the same location as the boarding unit.
     *
     * @param unit The <code>Unit</code> which is to board the carrier.
     * @param carrier The carrier to board.
     * @return True if the unit boards the carrier.
     */
    public boolean boardShip(Unit unit, Unit carrier) {
        if (!requireOurTurn()) return false;

        // Sanity checks.
        if (unit == null) {
            logger.warning("unit == null");
            return false;
        }
        if (carrier == null) {
            logger.warning("Trying to load onto a non-existent carrier.");
            return false;
        }
        if (unit.isNaval()) {
            logger.warning("Trying to load a ship onto another carrier.");
            return false;
        }
        if (unit.isInEurope() != carrier.isInEurope()
            || unit.getTile() != carrier.getTile()) {
            logger.warning("Unit and carrier are not co-located.");
            return false;
        }

        // Proceed to board
        UnitWas unitWas = new UnitWas(unit);
        if (askEmbark(unit, carrier, null) && unit.getLocation() == carrier) {
            freeColClient.playSound("sound.event.loadCargo");
            unitWas.fireChanges();
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for boarding a carrier.
     *
     * @param unit The <code>Unit</code> that is boarding.
     * @param carrier The carrier <code>Unit</code>.
     * @param direction An optional direction if the unit is boarding from
     *        an adjacent tile, or null if from the same tile.
     * @return True if the server interaction succeeded.
     */
    private boolean askEmbark(Unit unit, Unit carrier, Direction direction) {
        Client client = freeColClient.getClient();
        EmbarkMessage message = new EmbarkMessage(unit, carrier, direction);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Leave a ship.  The ship must be in harbour.
     *
     * @param unit The <code>Unit</code> which is to leave the ship.
     * @return boolean
     */
    public boolean leaveShip(Unit unit) {
        if (!requireOurTurn()) {
           return false;
        }

        // Sanity check, and find our carrier before we get off.
        if (!(unit.getLocation() instanceof Unit)) {
            logger.warning("Unit " + unit.getId() + " is not on a carrier.");
            return false;
        }
        Unit carrier = (Unit) unit.getLocation();

        // Ask the server
        UnitWas unitWas = new UnitWas(unit);
        if (askDisembark(unit) && unit.getLocation() != carrier) {
            checkCashInTreasureTrain(unit);
            unitWas.fireChanges();
            nextActiveUnit();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for disembarking from a carrier.
     *
     * @param unit The <code>Unit</code> that is disembarking.
     * @return True if the server interaction succeeded.
     */
    private boolean askDisembark(Unit unit) {
        Client client = freeColClient.getClient();
        DisembarkMessage message = new DisembarkMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Loads a cargo onto a carrier.
     *
     * @param goods The <code>Goods</code> which are going aboard the carrier.
     * @param carrier The <code>Unit</code> acting as carrier.
     */
    public void loadCargo(Goods goods, Unit carrier) {
        if (!requireOurTurn()) return;

        // Sanity checks.
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        } else if (carrier == null) {
            throw new IllegalArgumentException("Null carrier.");
        } else if (carrier.isInEurope()) {
            // empty
        } else if (carrier.getColony() == null) {
            throw new IllegalArgumentException("Carrier not at colony or Europe.");
        }

        // Try to load.
        if (loadGoods(goods, carrier)) {
            freeColClient.playSound("sound.event.loadCargo");
        }
    }

    /**
     * Unload cargo. If the unit carrying the cargo is not in a
     * harbour, or if the given boolean is true, the goods will be
     * dumped.
     *
     * @param goods The <code>Goods<code> to unload.
     * @param dump If true, dump the goods.
     */
    public void unloadCargo(Goods goods, boolean dump) {
        if (!requireOurTurn()) return;

        // Sanity tests.
        if (goods == null) {
            throw new IllegalArgumentException("Null goods.");
        } else if (goods.getAmount() <= 0) {
            throw new IllegalArgumentException("Empty goods.");
        }
        Unit carrier = null;
        if (!(goods.getLocation() instanceof Unit)) {
            throw new IllegalArgumentException("Unload from non-unit.");
        }
        carrier = (Unit) goods.getLocation();
        Colony colony = null;
        if (!carrier.isInEurope()) {
            if (carrier.getTile() == null) {
                throw new IllegalArgumentException("Carrier with null location.");
            }
            colony = carrier.getColony();
            if (!dump && colony == null) {
                throw new IllegalArgumentException("Unload is really a dump.");
            }
        }

        // Try to unload.  TODO: should there be a sound for this?
        unloadGoods(goods, carrier, colony);
    }

    /**
     * Unload, including dumping cargo.
     *
     * @param unit The <code>Unit<code> that is dumping.
     */
    public void unload(Unit unit) {
        if (!requireOurTurn()) return;

        // Sanity tests.
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        } else if (!unit.isCarrier()) {
            throw new IllegalArgumentException("Unit is not a carrier.");
        }

        Player player = freeColClient.getMyPlayer();
        boolean inEurope = unit.isInEurope();
        if (unit.getColony() != null) {
            // In colony, unload units and goods.
            for (Unit u : new ArrayList<Unit>(unit.getUnitList())) {
                leaveShip(u);
            }
            for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
                unloadCargo(goods, false);
            }
        } else {
            if (inEurope) { // In Europe, unload non-boycotted goods
                for (Goods goods : new ArrayList<Goods>(unit.getGoodsList())) {
                    if (player.canTrade(goods)) unloadCargo(goods, false);
                }
            }
            // Goods left here must be dumped.
            if (unit.getGoodsCount() > 0) {
                List<Goods> goodsList
                    = freeColClient.getCanvas().showDumpCargoDialog(unit);
                if (goodsList != null) {
                    for (Goods goods : goodsList) {
                        unloadCargo(goods, true);
                    }
                }
            }
        }
    }

    /**
     * Buy goods in Europe.
     * The amount of goods is adjusted to the space in the carrier.
     *
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The <code>Unit</code> acting as carrier.
     * @return True if the purchase succeeds.
     */
    public boolean buyGoods(GoodsType type, int amount, Unit carrier) {
        if (!requireOurTurn()) return false;

        // Sanity checks.  Should not happen!
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        if (type == null) {
            throw new NullPointerException("Goods type must not be null.");
        } else if (carrier == null) {
            throw new NullPointerException("Carrier must not be null.");
        } else if (carrier.getOwner() != player) {
            throw new IllegalArgumentException("Carrier owned by someone else.");
        } else if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        } else if (!player.canTrade(type)) {
            throw new IllegalArgumentException("Goods are boycotted.");
        }

        // Size check, if there are spare holds they can be filled, but...
        int toBuy = GoodsContainer.CARGO_SIZE;
        if (carrier.getSpaceLeft() <= 0) {
            // ...if there are no spare holds, we can only fill a hold
            // already partially filled with this type, otherwise fail.
            int partial = carrier.getGoodsContainer().getGoodsCount(type)
                % GoodsContainer.CARGO_SIZE;
            if (partial == 0) return false;
            toBuy -= partial;
        }
        if (amount < toBuy) toBuy = amount;

        // Check that the purchase is funded.
        Market market = player.getMarket();
        if (!player.checkGold(market.getBidPrice(type, toBuy))) {
            canvas.errorMessage("notEnoughGold");
            return false;
        }

        // Try to purchase.
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        int price = market.getCostToBuy(type);
        UnitWas unitWas = new UnitWas(carrier);
        if (askBuyGoods(carrier, type, toBuy)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            freeColClient.playSound("sound.event.loadCargo");
            unitWas.fireChanges();
            for (TransactionListener listener : market.getTransactionListener()) {
                listener.logPurchase(type, toBuy, price);
            }
            canvas.updateGoldLabel();
            return true;
        }

        // Purchase failed for some reason.
        return false;
    }

    /**
     * Server query-response for buying goods in Europe.
     *
     * @param carrier The <code>Unit</code> to load with the goods.
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     * @return True if the server interaction succeeded.
     */
    private boolean askBuyGoods(Unit carrier, GoodsType type, int amount) {
        Client client = freeColClient.getClient();
        BuyGoodsMessage message = new BuyGoodsMessage(carrier, type, amount);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sells goods in Europe.
     *
     * @param goods The goods to be sold.
     * @return True if the sale succeeds.
     */
    public boolean sellGoods(Goods goods) {
        if (!requireOurTurn()) return false;

        // Sanity checks.
        Player player = freeColClient.getMyPlayer();
        if (goods == null) {
            throw new NullPointerException("Goods must not be null.");
        }
        Unit carrier = null;
        if (goods.getLocation() instanceof Unit) {
            carrier = (Unit) goods.getLocation();
        }
        if (carrier == null) {
            throw new IllegalStateException("Goods not on carrier.");
        } else if (!carrier.isInEurope()) {
            throw new IllegalStateException("Goods not on carrier in Europe.");
        } else if (!player.canTrade(goods)) {
            throw new IllegalStateException("Goods are boycotted.");
        }

        // Try to sell.  Remember a bunch of stuff first so the transaction
        // can be logged.
        Market market = player.getMarket();
        GoodsType type = goods.getType();
        int amount = goods.getAmount();
        int price = market.getPaidForSale(type);
        int tax = player.getTax();
        int oldAmount = carrier.getGoodsContainer().getGoodsCount(type);
        UnitWas unitWas = new UnitWas(carrier);
        if (askSellGoods(goods, carrier)
            && carrier.getGoodsContainer().getGoodsCount(type) != oldAmount) {
            freeColClient.playSound("sound.event.sellCargo");
            unitWas.fireChanges();
            for (TransactionListener listener : market.getTransactionListener()) {
                listener.logSale(type, amount, price, tax);
            }
            freeColClient.getCanvas().updateGoldLabel();
            return true;
        }

        // Sale failed for some reason.
        return false;
    }

    /**
     * Server query-response for selling goods in Europe.
     *
     * @param goods The <code>Goods</code> to sell.
     * @param carrier The <code>Unit</code> in Europe with the goods.
     * @return True if the server interaction succeeded.
     */
    private boolean askSellGoods(Goods goods, Unit carrier) {
        Client client = freeColClient.getClient();
        SellGoodsMessage message = new SellGoodsMessage(goods, carrier);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Clear the speciality of a Unit, making it a Free Colonist.
     *
     * @param unit The <code>Unit</code> to clear the speciality of.
     */
    public void clearSpeciality(Unit unit) {
        if (!requireOurTurn()) return;

        // Check this makes sense and confirm.
        Canvas canvas = freeColClient.getCanvas();
        UnitType oldType = unit.getType();
        UnitType newType = oldType.getTargetType(ChangeType.CLEAR_SKILL,
                                                 unit.getOwner());
        if (newType == null) {
            canvas.showInformationMessage(unit,
                StringTemplate.template("clearSpeciality.impossible")
                .addStringTemplate("%unit%", Messages.getLabel(unit)));
            return;
        }

        Tile tile = (canvas.isShowingSubPanel()) ? null : unit.getTile();
        if (!canvas.showConfirmDialog(tile,
                StringTemplate.template("clearSpeciality.areYouSure")
                    .addStringTemplate("%oldUnit%", Messages.getLabel(unit))
                    .add("%unit%", newType.getNameKey()),
                "yes", "no")) {
            return;
        }

        // Try to clear.
        if (askClearSpeciality(unit) && unit.getType() == newType) {
            // Would expect to need to do:
            //    unit.firePropertyChange(Unit.UNIT_TYPE_CHANGE,
            //                            oldType, newType);
            // but this routine is only called out of UnitLabel, where the
            // unit icon is always updated anyway.
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for clearing a unit speciality.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    private boolean askClearSpeciality(Unit unit) {
        Client client = freeColClient.getClient();
        ClearSpecialityMessage message = new ClearSpecialityMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Disbands the active unit.
     */
    public void disbandActiveUnit() {
        if (!requireOurTurn()) return;

        GUI gui = freeColClient.getGUI();
        Unit unit = gui.getActiveUnit();
        if (unit == null) return;
        Canvas canvas = freeColClient.getCanvas();
        Tile tile = (canvas.isShowingSubPanel()) ? null : unit.getTile();
        if (!canvas.showConfirmDialog(tile,
                                      StringTemplate.key("disbandUnit.text"),
                                      "disbandUnit.yes", "disbandUnit.no")) {
            return;
        }

        // Try to disband
        if (askDisbandUnit(unit)) {
            nextActiveUnit();
        }
    }

    /**
     * Server query-response for disbanding a unit.
     *
     * @param unit The <code>Unit</code> to operate on.
     * @return True if the server interaction succeeded.
     */
    private boolean askDisbandUnit(Unit unit) {
        Client client = freeColClient.getClient();
        DisbandUnitMessage message = new DisbandUnitMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Sets the export settings of the custom house.
     *
     * @param colony The colony with the custom house.
     * @param goodsType The goods for which to set the settings.
     */
    public void setGoodsLevels(Colony colony, GoodsType goodsType) {
        askSetGoodsLevels(colony, colony.getExportData(goodsType));
    }

    /**
     * Server query-response for setting goods levels.
     *
     * @param colony The <code>Colony</code> where the levels are set.
     * @param data The <code>ExportData</code> setting.
     * @return True if the server interaction succeeded.
     */
    private boolean askSetGoodsLevels(Colony colony, ExportData data) {
        Client client = freeColClient.getClient();
        SetGoodsLevelsMessage message = new SetGoodsLevelsMessage(colony, data);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Change the amount of equipment a unit has.
     *
     * @param unit The <code>Unit</code>.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount How to change the amount of equipment the unit has.
     */
    public void equipUnit(Unit unit, EquipmentType type, int amount) {
        if (!requireOurTurn() || amount == 0) return;

        Player player = freeColClient.getMyPlayer();
        List<AbstractGoods> requiredGoods = type.getGoodsRequired();
        Colony colony = null;
        if (unit.isInEurope()) {
            for (AbstractGoods goods : requiredGoods) {
                GoodsType goodsType = goods.getType();
                if (!player.canTrade(goodsType) && !payArrears(goodsType)) {
                    return; // payment failed for some reason
                }
            }
        } else {
            colony = unit.getColony();
            if (colony == null) {
                throw new IllegalStateException("Equip unit not in settlement/Europe");
            }
        }

        int oldAmount = unit.getEquipmentCount(type);
        int newAmount;
        ColonyWas colonyWas = (colony == null) ? null : new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askEquipUnit(unit, type, amount)
            && (newAmount = unit.getEquipmentCount(type)) != oldAmount) {
            unit.firePropertyChange(Unit.EQUIPMENT_CHANGE,
                                    oldAmount, newAmount);
            if (colonyWas != null) colonyWas.fireChanges();
            unitWas.fireChanges();
            freeColClient.getCanvas().updateGoldLabel();
        }
    }

    /**
     * Server query-response for equipping a unit.
     *
     * @param unit The <code>Unit</code> to equip on.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount The amount of equipment.
     * @return True if the server interaction succeeded.
     */
    private boolean askEquipUnit(Unit unit, EquipmentType type, int amount) {
        Client client = freeColClient.getClient();
        EquipUnitMessage message = new EquipUnitMessage(unit, type, amount);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Moves a <code>Unit</code> to a <code>WorkLocation</code>.
     *
     * @param unit The <code>Unit</code>.
     * @param workLocation The <code>WorkLocation</code>.
     */
    public void work(Unit unit, WorkLocation workLocation) {
        if (!requireOurTurn()) return;

        Colony colony = workLocation.getColony();
        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            if (tile.hasLostCityRumour()) {
                freeColClient.getCanvas()
                    .showInformationMessage("tileHasRumour");
                return;
            }
            if (tile.getOwner() != unit.getOwner()) {
                if (!claimLand(tile, colony, 0)) return;
            }
        }

        // Try to change the work location.
        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askWork(unit, workLocation) && unit.getLocation() == workLocation) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
        }
    }

    /**
     * Server query-response for changing a work location.
     *
     * @param unit The <code>Unit</code> to change the workLocation of.
     * @param workLocation The <code>WorkLocation</code> to change to.
     * @return True if the server interaction succeeded.
     */
    private boolean askWork(Unit unit, WorkLocation workLocation) {
        Client client = freeColClient.getClient();
        WorkMessage message = new WorkMessage(unit, workLocation);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Puts the specified unit outside the colony.
     *
     * @param unit The <code>Unit</code>
     * @return <i>true</i> if the unit was successfully put outside the colony.
     */
    public boolean putOutsideColony(Unit unit) {
        if (!requireOurTurn()) return false;

        Colony colony = unit.getColony();
        if (colony == null) {
            throw new IllegalStateException("Unit is not in colony.");
        } else if (!colony.canReducePopulation()) {
            return false;
        }

        ColonyWas colonyWas = new ColonyWas(colony);
        UnitWas unitWas = new UnitWas(unit);
        if (askPutOutsideColony(unit)) {
            colonyWas.fireChanges();
            unitWas.fireChanges();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for putting a unit outside a colony.
     *
     * @param unit The <code>Unit</code> to put out.
     * @return True if the server interaction succeeded.
     */
    private boolean askPutOutsideColony(Unit unit) {
        Client client = freeColClient.getClient();
        PutOutsideColonyMessage message = new PutOutsideColonyMessage(unit);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param workType The new <code>GoodsType</code> to produce.
     */
    public void changeWorkType(Unit unit, GoodsType workType) {
        if (!requireOurTurn()) return;

        UnitWas unitWas = new UnitWas(unit);
        if (askChangeWorkType(unit, workType)) {
            unitWas.fireChanges();
        }
    }

    /**
     * Server query-response for changing work type.
     *
     * @param unit The <code>Unit</code> to change the work type of.
     * @param workType The new <code>GoodsType</code> to produce.
     * @return True if the server interaction succeeded.
     */
    private boolean askChangeWorkType(Unit unit, GoodsType workType) {
        Client client = freeColClient.getClient();
        ChangeWorkTypeMessage message = new ChangeWorkTypeMessage(unit,
                                                                  workType);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Changes the work type of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param improvementType a <code>TileImprovementType</code> value
     */
    public void changeWorkImprovementType(Unit unit,
                                          TileImprovementType improvementType) {
        if (!requireOurTurn()) return;

        if (!unit.checkSetState(UnitState.IMPROVING)
            || improvementType.isNatural()) {
            return; // Don't bother (and don't log, this is not exceptional)
        }

        Player player = freeColClient.getMyPlayer();
        Tile tile = unit.getTile();
        if (player != tile.getOwner()) {
            if (!claimTile(player, tile, null, player.getLandPrice(tile), 0)
                || player != tile.getOwner()) return;
        }

        if (askChangeWorkImprovementType(unit, improvementType)) {
            // Redisplay should work
        }
        nextActiveUnit();
    }

    /**
     * Server query-response for changing work improvement type.
     *
     * @param unit The <code>Unit</code> to change the work type of.
     * @param type The new <code>TileImprovementType</code> to work on.
     * @return True if the server interaction succeeded.
     */
    private boolean askChangeWorkImprovementType(Unit unit,
                                                 TileImprovementType type) {
        Client client = freeColClient.getClient();
        ChangeWorkImprovementTypeMessage message
            = new ChangeWorkImprovementTypeMessage(unit, type);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Changes the state of this <code>Unit</code>.
     *
     * @param unit The <code>Unit</code>
     * @param state The state of the unit.
     */
    public void changeState(Unit unit, UnitState state) {
        if (!requireOurTurn()) return;

        if (!unit.checkSetState(state)) {
            return; // Don't bother (and don't log, this is not exceptional)
        }

        // Check if this is a hostile fortification, and give the player
        // a chance to confirm.
        Player player = freeColClient.getMyPlayer();
        if (state == UnitState.FORTIFYING && unit.isOffensiveUnit()
            && !unit.hasAbility("model.ability.piracy")) {
            Tile tile = unit.getTile();
            if (tile != null && tile.getOwningSettlement() != null) {
                Player enemy = tile.getOwningSettlement().getOwner();
                if (player != enemy
                    && player.getStance(enemy) != Stance.ALLIANCE) {
                    if (!confirmHostileAction(unit, tile)) return; // Aborted
                }
            }
        }

        Canvas canvas = freeColClient.getCanvas();
        if (askChangeState(unit, state)) {
            if (!canvas.isShowingSubPanel()
                && (unit.getMovesLeft() == 0
                    || unit.getState() == UnitState.SENTRY
                    || unit.getState() == UnitState.SKIPPED)) {
                nextActiveUnit();
            } else {
                canvas.refresh();
            }
        }
    }

    /**
     * Clears the orders of the given unit.
     * Make the unit active and set a null destination and trade route.
     *
     * @param unit The <code>Unit</code> to clear the orders of
     * @return boolean <b>true</b> if the orders were cleared
     */
    public boolean clearOrders(Unit unit) {
        if (!requireOurTurn() || unit == null
            || !unit.checkSetState(UnitState.ACTIVE)) return false;

        // Ask the user for confirmation, as this is a classic mistake.
        // Cancelling a pioneer terrain improvement is a waste of many turns.
        Canvas canvas = freeColClient.getCanvas();
        if (unit.getState() == UnitState.IMPROVING
            && !canvas.showConfirmDialog(unit.getTile(),
                                         StringTemplate.template("model.unit.confirmCancelWork")
                                         .addAmount("%turns%", unit.getWorkTurnsLeft()),
                                         "yes", "no")) {
            return false;
        }

        assignTradeRoute(unit, null);
        clearGotoOrders(unit);
        return askChangeState(unit, UnitState.ACTIVE);
    }

    /**
     * Server query-response for changing unit state.
     *
     * @param unit The <code>Unit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return boolean <b>true</b> if the server interaction succeeded.
     */
    private boolean askChangeState(Unit unit, UnitState state) {
        Client client = freeColClient.getClient();
        ChangeStateMessage message = new ChangeStateMessage(unit, state);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Assigns a unit to a teacher <code>Unit</code>.
     *
     * @param student an <code>Unit</code> value
     * @param teacher an <code>Unit</code> value
     */
    public void assignTeacher(Unit student, Unit teacher) {
        Player player = freeColClient.getMyPlayer();
        if (!requireOurTurn()
            || student == null
            || student.getOwner() != player
            || student.getColony() == null
            || !(student.getLocation() instanceof WorkLocation)
            || teacher == null
            || teacher.getOwner() != player
            || !student.canBeStudent(teacher)
            || teacher.getColony() == null
            || student.getColony() != teacher.getColony()
            || !teacher.getColony().canTrain(teacher)) {
            return;
        }

        askAssignTeacher(student, teacher);
    }

    /**
     * Server query-response for assigning a teacher.
     *
     * @param student The student <code>Unit</code>.
     * @param teacher The teacher <code>Unit</code>.
     * @return True if the server interaction succeeded.
     */
    private boolean askAssignTeacher(Unit student, Unit teacher) {
        Client client = freeColClient.getClient();
        AssignTeacherMessage message
            = new AssignTeacherMessage(student, teacher);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Changes the current construction project of a <code>Colony</code>.
     *
     * @param colony The <code>Colony</code>
     * @param buildQueue List of <code>BuildableType</code>
     */
    public void setBuildQueue(Colony colony, List<BuildableType> buildQueue) {
        if (!requireOurTurn()) return;

        ColonyWas colonyWas = new ColonyWas(colony);
        if (askSetBuildQueue(colony, buildQueue)) {
            colonyWas.fireChanges();
        }
    }

    /**
     * Server query-response for changing a build queue.
     *
     * @param colony the Colony
     * @param buildQueue the new values for the build queue
     * @return True if the server interaction succeeded.
     */
    private boolean askSetBuildQueue(Colony colony,
                                     List<BuildableType> buildQueue) {
        Client client = freeColClient.getClient();
        SetBuildQueueMessage message = new SetBuildQueueMessage(colony,
                                                                buildQueue);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Trains a unit of a specified type in Europe.
     *
     * @param unitType The type of unit to be trained.
     */
    public void trainUnitInEurope(UnitType unitType) {
        if (!requireOurTurn()) return;

        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        Europe europe = player.getEurope();
        if (!player.checkGold(europe.getUnitPrice(unitType))) {
            canvas.errorMessage("notEnoughGold");
            return;
        }

        EuropeWas europeWas = new EuropeWas(europe);
        if (askTrainUnitInEurope(unitType)) {
            canvas.updateGoldLabel();
            europeWas.fireChanges();
        }
    }

    /**
     * Server query-response for training a unit in Europe.
     *
     * @param type The <code>UnitType</code> to train.
     * @return True if the server interaction succeeded.
     */
    private boolean askTrainUnitInEurope(UnitType type) {
        Client client = freeColClient.getClient();
        TrainUnitInEuropeMessage message = new TrainUnitInEuropeMessage(type);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Buys the remaining hammers and tools for the {@link Building} currently
     * being built in the given <code>Colony</code>.
     *
     * @param colony The {@link Colony} where the building should be bought.
     */
    public void payForBuilding(Colony colony) {
        if (!requireOurTurn()) return;

        Canvas canvas = freeColClient.getCanvas();
        if (!colony.canPayToFinishBuilding()) {
            canvas.errorMessage("notEnoughGold");
            return;
        }
        int price = colony.getPriceForBuilding();
        if (!canvas.showConfirmDialog(null,
                                      StringTemplate.template("payForBuilding.text")
                                      .addAmount("%replace%", price),
                                      "payForBuilding.yes", "payForBuilding.no")) {
            return;
        }

        ColonyWas colonyWas = new ColonyWas(colony);
        if (askPayForBuilding(colony) && colony.getPriceForBuilding() == 0) {
            colonyWas.fireChanges();
            canvas.updateGoldLabel();
        }
    }

    /**
     * Server query-response for paying for a building.
     *
     * @param colony The <code>Colony</code> that is building.
     * @return True if the server interaction succeeded.
     */
    private boolean askPayForBuilding(Colony colony) {
        Client client = freeColClient.getClient();
        PayForBuildingMessage message = new PayForBuildingMessage(colony);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Pays the tax arrears on this type of goods.
     *
     * @param goods The goods for which to pay arrears.
     * @return True if the arrears were paid.
     */
    public boolean payArrears(Goods goods) {
        return payArrears(goods.getType());
    }

    /**
     * Pays the tax arrears on this type of goods.
     *
     * @param type The type of goods for which to pay arrears.
     * @return True if the arrears were paid.
     */
    public boolean payArrears(GoodsType type) {
        if (!requireOurTurn()) return false;

        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        int arrears = player.getArrears(type);
        if (arrears <= 0) return false;
        if (!player.checkGold(arrears)) {
            canvas.showInformationMessage(StringTemplate.template("model.europe.cantPayArrears")
                                          .addAmount("%amount%", arrears));
            return false;
        }
        if (canvas.showConfirmDialog(null,
                                     StringTemplate.template("model.europe.payArrears")
                                     .addAmount("%replace%", arrears),
                                     "ok", "cancel")
            && askPayArrears(type) && player.canTrade(type)) {
            canvas.updateGoldLabel();
            return true;
        }
        return false;
    }

    /**
     * Server query-response for tax paying arrears.
     *
     * @param type The <code>GoodsType</code> to pay the arrears for.
     * @return True if the server interaction succeeded.
     */
    private boolean askPayArrears(GoodsType type) {
        Client client = freeColClient.getClient();
        PayArrearsMessage message = new PayArrearsMessage(type);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }


    /**
     * Gathers information about the REF.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractUnit> getREFUnits() {
        if (!requireOurTurn()) return Collections.emptyList();

        return askGetREFUnits();
    }

    /**
     * Server query-response for asking about a players REF.
     *
     * @return A list of REF units for the player.
     */
    private List<AbstractUnit> askGetREFUnits() {
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, Message.createNewRootElement("getREFUnits"), null);
        if (reply == null) return Collections.emptyList();

        List<AbstractUnit> result = new ArrayList<AbstractUnit>();
        NodeList childElements = reply.getChildNodes();
        for (int index = 0; index < childElements.getLength(); index++) {
            AbstractUnit unit = new AbstractUnit();
            unit.readFromXMLElement((Element) childElements.item(index));
            result.add(unit);
        }
        return result;
    }


    /**
     * Retrieves high scores from server.
     *
     * @return The list of high scores.
     */
    public List<HighScore> getHighScores() {
        return askGetHighScores();
    }

    /**
     * Server query-response for asking for the high scores list.
     *
     * @return The list of high scores.
     */
    private List<HighScore> askGetHighScores() {
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client,
                Message.createNewRootElement("getHighScores"), null);
        if (reply == null) return Collections.emptyList();

        List<HighScore> result = new ArrayList<HighScore>();
        NodeList childElements = reply.getChildNodes();
        for (int i = 0; i < childElements.getLength(); i++) {
            try {
                HighScore score = new HighScore((Element)childElements.item(i));
                result.add(score);
            } catch (XMLStreamException e) {
                logger.warning("Unable to read score element: "
                               + e.getMessage());
            }
        }
        return result;
    }


    /**
     * Get the nation summary for a player.
     *
     * @param player The <code>Player</code> to summarize.
     * @return A summary of that nation, or null on error.
     */
    public NationSummary getNationSummary(Player player) {
        return askNationSummary(player);
    }

    /**
     * Server query-response for asking for the nation summary of a player.
     *
     * @param player The <code>Player</code> to summarize.
     * @return A summary of that nation, or null on error.
     */
    private NationSummary askNationSummary(Player player) {
        Client client = freeColClient.getClient();
        GetNationSummaryMessage message = new GetNationSummaryMessage(player);
        Element reply = askExpecting(client, message.toXMLElement(),
                                     GetNationSummaryMessage.getXMLElementTagName());
        if (reply == null) return null;

        Game game = freeColClient.getGame();
        return new GetNationSummaryMessage(game, reply).getNationSummary();
    }


    /**
     * Retrieves server statistics
     *
     * @return The server statistics.
     */
    public StatisticsMessage getServerStatistics() {
        return askStatistics();
    }

    /**
     * Server query-response for asking for the server statistics.
     *
     * @return The server statistics.
     */
    private StatisticsMessage askStatistics() {
        Client client = freeColClient.getClient();
        Element reply = askExpecting(client, Message
            .createNewRootElement(StatisticsMessage.getXMLElementTagName()),
                                     null);
        if (reply == null) return null;

        return new StatisticsMessage(reply);
    }


    /**
     * Assigns a trade route to a unit using the trade route dialog.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     */
    public void assignTradeRoute(Unit unit) {
        Canvas canvas = freeColClient.getCanvas();
        TradeRoute oldRoute = unit.getTradeRoute();
        TradeRoute route = canvas.showTradeRouteDialog(unit);
        if (route == null) return; // Cancelled
        // Delete or deassign of trade route removes the route from the unit
        route = unit.getTradeRoute();
        if (oldRoute != route) assignTradeRoute(unit, route);
    }

    /**
     * Assigns a trade route to a unit.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     */
    public void assignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        if (askAssignTradeRoute(unit, tradeRoute)) {
            if ((tradeRoute = unit.getTradeRoute()) != null
                && freeColClient.getGame().getCurrentPlayer()
                == freeColClient.getMyPlayer()) {
                moveToDestination(unit);
            }
        }
    }

    /**
     * Server query-response for assigning a trade route to a unit.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     * @return True if the server interaction succeeded.
     */
    private boolean askAssignTradeRoute(Unit unit, TradeRoute tradeRoute) {
        Client client = freeColClient.getClient();
        AssignTradeRouteMessage message
            = new AssignTradeRouteMessage(unit, tradeRoute);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Gets a new trade route for a player.
     *
     * @param player The <code>Player</code> to get a new trade route for.
     * @return A new <code>TradeRoute</code>.
     */
    public TradeRoute getNewTradeRoute(Player player) {
        int n = player.getTradeRoutes().size();
        if (askGetNewTradeRoute()
            && player.getTradeRoutes().size() == n + 1) {
            return player.getTradeRoutes().get(n);
        }
        return null;
    }

    /**
     * Server query-response for creating a new trade route.
     *
     * @return True if the server interaction succeeded.
     */
    private boolean askGetNewTradeRoute() {
        Client client = freeColClient.getClient();
        Element element = Message.createNewRootElement("getNewTradeRoute");
        Element reply = askExpecting(client, element, null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Sets the trade routes for this player
     *
     * @param routes The trade routes to set.
     */
    public void setTradeRoutes(List<TradeRoute> routes) {

        askSetTradeRoutes(routes);
    }

    /**
     * Server query-response for setting the trade routes.
     *
     * @param routes A list of trade routes to update.
     * @return True if the server interaction succeeded.
     */
    private boolean askSetTradeRoutes(List<TradeRoute> routes) {
        Client client = freeColClient.getClient();
        SetTradeRoutesMessage message = new SetTradeRoutesMessage(routes);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        if (reply == null) return false;

        Connection conn = client.getConnection();
        freeColClient.getInGameInputHandler().handle(conn, reply);
        return true;
    }

    /**
     * Updates a trade route.
     *
     * @param route The trade route to update.
     */
    public void updateTradeRoute(TradeRoute route) {
        askUpdateTradeRoute(route);
    }

    /**
     * Server query-response for asking for updating the trade route.
     *
     * @param route The trade route to update.
     * @return True if the server interaction succeeded.
     */
    private boolean askUpdateTradeRoute(TradeRoute route) {
        Client client = freeColClient.getClient();
        UpdateTradeRouteMessage message = new UpdateTradeRouteMessage(route);
        Element reply = askExpecting(client, message.toXMLElement(), null);
        return reply == null;
    }



    /**
     * End the turn command.
     */
    public void endTurn() {
        if (!requireOurTurn()) return;

        List<Unit> units = new ArrayList<Unit>();
        for (Unit unit : freeColClient.getMyPlayer().getUnits()) {
            if (unit.couldMove()) {
                units.add(unit);
            }
        }
        if (units.size() > 0) {
            Canvas canvas = freeColClient.getCanvas();
            if (!canvas.showFreeColDialog(new EndTurnDialog(canvas, units))) {
                return;
            }
        }
        // Make sure all goto orders are complete before ending turn.
        moveMode = MODE_END_TURN;
        if (!doExecuteGotoOrders()) return;
        doEndTurn();
    }

    /**
     * Actually do the end turn operation.
     */
    private void doEndTurn() {
        // Ensure end-turn mode sticks.
        if (moveMode < MODE_END_TURN) {
            moveMode = MODE_END_TURN;
        }

        // Clear active unit if any
        GUI gui = freeColClient.getGUI();
        gui.setActiveUnit(null);

        // Inform the server of end of turn
        askTrivial("endTurn");

        // Restart the selection cycle
        moveMode = MODE_NEXT_ACTIVE_UNIT;
        turnsPlayed++;
    }

    /**
     * Execute goto orders command.
     */
    public void executeGotoOrders() {
        doExecuteGotoOrders();
    }

    /**
     * Actually do the goto orders operation.
     *
     * @return True if all goto orders have been performed.
     */
    private boolean doExecuteGotoOrders() {
        // Ensure the goto mode sticks.
        if (moveMode < MODE_EXECUTE_GOTO_ORDERS) {
            moveMode = MODE_EXECUTE_GOTO_ORDERS;
        }

        // Process all units.
        Canvas canvas = freeColClient.getCanvas();
        Player player = freeColClient.getMyPlayer();
        GUI gui = freeColClient.getGUI();
        while (player.hasNextGoingToUnit()) {
            // Give the player a chance to deal with any problems
            // shown in a popup before pressing on with more moves.
            if (canvas.isShowingSubPanel()) {
                canvas.getShowingSubPanel().requestFocus();
                return false;
            }

            // Move the unit as much as possible
            Unit unit = player.getNextGoingToUnit();
            gui.setActiveUnit(unit);
            moveToDestination(unit);
            unit.setMovesLeft(0); // Fake change, client side only
            nextModelMessage();
        }
        return true;
    }

    /**
     * Tell a unit to wait.
     */
    public void waitActiveUnit() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = canvas.getGUI();
        gui.setActiveUnit(null);
        nextActiveUnit();
    }

    /**
     * Skip a unit.
     */
    public void skipActiveUnit() {
        changeState(freeColClient.getGUI().getActiveUnit(), UnitState.SKIPPED);
    }

    /**
     * Makes a new unit active.
     */
    public void nextActiveUnit() {
        nextActiveUnit(null);
    }

    /**
     * Makes a new unit active if any, or focus on a tile (useful if the
     * current unit just died).
     * Displays any new <code>ModelMessage</code>s with
     * {@link #nextModelMessage}.
     *
     * @param tile The <code>Tile</code> to select if no new unit can
     *             be made active.
     */
    public void nextActiveUnit(Tile tile) {
        if (!requireOurTurn()) return;

        // Always flush outstanding messages first.
        Canvas canvas = freeColClient.getCanvas();
        nextModelMessage();
        //if (canvas.isShowingSubPanel()) {
        //    canvas.getShowingSubPanel().requestFocus();
        //    return;
        //}

        // Flush any outstanding orders once the mode is raised.
        if (moveMode >= MODE_EXECUTE_GOTO_ORDERS
            && !doExecuteGotoOrders()) {
            return;
        }

        // Look for active units.
        Player player = freeColClient.getMyPlayer();
        GUI gui = canvas.getGUI();
        Unit unit = gui.getActiveUnit();
        if (unit != null && !unit.isDisposed() && unit.getMovesLeft() > 0
            && unit.getState() != UnitState.SKIPPED) {
            return; // Current active unit has more moves to do.
        }
        if (player.hasNextActiveUnit()) {
            gui.setActiveUnit(player.getNextActiveUnit());
            return; // Successfully found a unit to display
        }

        // No active units left.  Do the goto orders.
        if (!doExecuteGotoOrders()) return;

        // If not already ending the turn, use the fallback tile if
        // supplied, then check for automatic end of turn, otherwise
        // just select nothing and wait.
        gui.setActiveUnit(null);
        ClientOptions options = freeColClient.getClientOptions();
        if (moveMode >= MODE_END_TURN) {
            doEndTurn();
        } else if (tile != null) {
            gui.setSelectedTile(tile, false);
        } else if (options.getBoolean(ClientOptions.AUTO_END_TURN)) {
            doEndTurn();
        }
    }


    /**
     * Ignore this ModelMessage from now on until it is not generated in a turn.
     *
     * @param message a <code>ModelMessage</code> value
     * @param flag whether to ignore the ModelMessage or not
     */
    public synchronized void ignoreMessage(ModelMessage message, boolean flag) {
        String key = message.getSourceId();
        if (message.getTemplateType() == StringTemplate.TemplateType.TEMPLATE) {
            for (String otherkey : message.getKeys()) {
                if ("%goods%".equals(otherkey)) {
                    key += otherkey;
                }
                break;
            }
        }
        if (flag) {
            startIgnoringMessage(key, freeColClient.getGame().getTurn().getNumber());
        } else {
            stopIgnoringMessage(key);
        }
    }

    /**
     * Displays the next <code>ModelMessage</code>.
     *
     * @see net.sf.freecol.common.model.ModelMessage ModelMessage
     */
    public void nextModelMessage() {
        displayModelMessages(false);
    }

    public void displayModelMessages(final boolean allMessages) {

        int thisTurn = freeColClient.getGame().getTurn().getNumber();

        final ArrayList<ModelMessage> messageList = new ArrayList<ModelMessage>();
        List<ModelMessage> inputList;
        if (allMessages) {
            inputList = freeColClient.getMyPlayer().getModelMessages();
        } else {
            inputList = freeColClient.getMyPlayer().getNewModelMessages();
        }

        for (ModelMessage message : inputList) {
            if (shouldAllowMessage(message)) {
                if (message.getMessageType() == ModelMessage.MessageType.WAREHOUSE_CAPACITY) {
                    String key = message.getSourceId();
                    if (message.getTemplateType() == StringTemplate.TemplateType.TEMPLATE) {
                        for (String otherkey : message.getKeys()) {
                            if ("%goods%".equals(otherkey)) {
                                key += otherkey;
                                break;
                            }
                        }
                    }

                    Integer turn = getTurnForMessageIgnored(key);
                    if (turn != null && turn.intValue() == thisTurn - 1) {
                        startIgnoringMessage(key, thisTurn);
                        message.setBeenDisplayed(true);
                        continue;
                    }
                } else if (message.getMessageType() == ModelMessage.MessageType.BUILDING_COMPLETED) {
                    freeColClient.playSound("sound.event.buildingComplete");
                }
                messageList.add(message);
            }

            // flag all messages delivered as "beenDisplayed".
            message.setBeenDisplayed(true);
        }

        purgeOldMessagesFromMessagesToIgnore(thisTurn);
        final ModelMessage[] messages = messageList.toArray(new ModelMessage[0]);

        Runnable uiTask = new Runnable() {
                public void run() {
                    Canvas canvas = freeColClient.getCanvas();
                    if (messageList.size() > 0) {
                        if (allMessages || messageList.size() > 5) {
                            canvas.showReportTurnPanel(messages);
                        } else {
                            canvas.showModelMessages(messages);
                        }
                    }
                    freeColClient.getActionManager().update();
                }
            };
        if (SwingUtilities.isEventDispatchThread()) {
            uiTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(uiTask);
            } catch (InterruptedException e) {
                // Ignore
            } catch (InvocationTargetException e) {
                // Ignore
            }
        }
    }

    private synchronized Integer getTurnForMessageIgnored(String key) {
        return messagesToIgnore.get(key);
    }

    private synchronized void startIgnoringMessage(String key, int turn) {
        logger.finer("Ignoring model message with key " + key);
        messagesToIgnore.put(key, new Integer(turn));
    }

    private synchronized void stopIgnoringMessage(String key) {
        logger.finer("Removing model message with key " + key + " from ignored messages.");
        messagesToIgnore.remove(key);
    }

    private synchronized void purgeOldMessagesFromMessagesToIgnore(int thisTurn) {
        List<String> keysToRemove = new ArrayList<String>();
        for (Entry<String, Integer> entry : messagesToIgnore.entrySet()) {
            if (entry.getValue().intValue() < thisTurn - 1) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Removing old model message with key " + entry.getKey() + " from ignored messages.");
                }
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            stopIgnoringMessage(key);
        }
    }

    /**
     * Provides an opportunity to filter the messages delivered to the canvas.
     *
     * @param message the message that is candidate for delivery to the canvas
     * @return true if the message should be delivered
     */
    private boolean shouldAllowMessage(ModelMessage message) {
        BooleanOption option = freeColClient.getClientOptions().getBooleanOption(message);
        if (option == null) {
            return true;
        } else {
            return option.getValue();
        }
    }
}
