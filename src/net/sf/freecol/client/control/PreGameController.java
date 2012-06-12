/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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


import java.awt.Color;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ChipResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;

import org.w3c.dom.Element;



/**
 * The controller that will be used before the game starts.
 */
public final class PreGameController {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    /** The main client. */
    private FreeColClient freeColClient;

    /** The GUI to display on. */
    private GUI gui;


    /**
     * The constructor to use.
     *
     * @param freeColClient The main client.
     * @param gui The <code>GUI</code> to display on.
     */
    public PreGameController(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
    }


    /**
     * Sets this client to be (or not be) ready to start the game.
     *
     * @param ready Indicates whether or not this client is ready to
     *     start the game.
     */
    public void setReady(boolean ready) {
        freeColClient.getMyPlayer().setReady(ready);

        freeColClient.getClient().send(DOMMessage.createMessage("ready",
                "value", Boolean.toString(ready)));
    }

    /**
     * Sets this client's player's nation.
     *
     * @param nation Which <code>Nation</code> this player wishes to set.
     */
    public void setNation(Nation nation) {
        freeColClient.getMyPlayer().setNation(nation);

        freeColClient.getClient()
            .sendAndWait(DOMMessage.createMessage("setNation",
                    "value", nation.getId()));
    }

    /**
     * Sets this client's player's nation type.
     *
     * @param nationType Which nation type this player wishes to set.
     */
    public void setNationType(NationType nationType) {
        freeColClient.getMyPlayer().setNationType(nationType);

        freeColClient.getClient()
            .sendAndWait(DOMMessage.createMessage("setNationType",
                    "value", nationType.getId()));
    }

    /**
     * Sets a nation's state.
     *
     * @param nation The <code>Nation</code> to set.
     * @param state The <code>NationState</code> value to set.
     */
    public void setAvailable(Nation nation, NationState state) {
        freeColClient.getGame().getNationOptions()
            .getNations().put(nation, state);

        freeColClient.getClient()
            .sendAndWait(DOMMessage.createMessage("setAvailable",
                    "nation", nation.getId(),
                    "state", state.toString()));
    }

    /**
     * Requests the game to be started.  This will only be successful
     * if all players are ready to start the game.
     */
    public void requestLaunch() {
        if (freeColClient.getGame().isAllPlayersReadyToLaunch()) {
            gui.showStatusPanel(Messages.message("status.startingGame"));
            freeColClient.getClient()
                .send(DOMMessage.createMessage("requestLaunch"));
        } else {
            gui.errorMessage("server.notAllReady");
        }
    }

    /**
     * Sends a chat message.
     *
     * @param message The text of the message.
     */
    public void chat(String message) {
        freeColClient.getClient()
            .send(new ChatMessage(freeColClient.getMyPlayer(),
                    message, Boolean.FALSE).toXMLElement());
    }

    /**
     * Sends the {@link GameOptions} to the server.
     * This method should be called after updating that object.
     */
    public void sendGameOptions() {
        OptionGroup gameOptions = freeColClient.getGame().getSpecification()
            .getOptionGroup("gameOptions");

        Element up = DOMMessage.createMessage("updateGameOptions");
        up.appendChild(gameOptions.toXMLElement(up.getOwnerDocument()));
        freeColClient.getClient().send(up);
    }

    /**
     * Sends the {@link MapGeneratorOptions} to the server.
     * This method should be called after updating that object.
     */
    public void sendMapGeneratorOptions() {
        OptionGroup mapOptions = freeColClient.getGame()
            .getMapGeneratorOptions();

        Element up = DOMMessage.createMessage("updateMapGeneratorOptions");
        up.appendChild(mapOptions.toXMLElement(up.getOwnerDocument()));
        //freeColClient.getGame().setMapGeneratorOptions(mapGeneratorOptions);
        freeColClient.getClient().send(up);
    }

    /**
     * Add player-specific resources to the resource manager.
     *
     * @param nationId The player nation identifier.
     */
    private void addPlayerResources(String nationId) {
        Color color = ResourceManager.getColor(nationId + ".color");
        ResourceMapping gameMapping = new ResourceMapping();
        gameMapping.add(nationId + ".chip", ChipResource.colorChip(color));
        gameMapping.add(nationId + ".mission.chip",
                        ChipResource.missionChip(color, false));
        gameMapping.add(nationId + ".mission.expert.chip",
                        ChipResource.missionChip(color, true));
        ResourceManager.addGameMapping(gameMapping);
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        for (Player player : freeColClient.getGame().getPlayers()) {
            addPlayerResources(player.getNationID());
        }
        // Unknown nation is not in getPlayers() list.
        addPlayerResources(Nation.UNKNOWN_NATION_ID);

        Player myPlayer = freeColClient.getMyPlayer();
        if (!freeColClient.isHeadless()) {
            gui.closeMainPanel();
            gui.closeMenus();
            gui.closeStatusPanel();
            gui.playSound(null); // Stop the long introduction sound
            gui.playSound("sound.intro." + myPlayer.getNationID());
        }
        freeColClient.getClient()
            .setMessageHandler(freeColClient.getInGameInputHandler());

        if (!freeColClient.isHeadless()) {
            freeColClient.setInGame(true);
            gui.setupInGameMenuBar();
        }

        InGameController igc = freeColClient.getInGameController();
        gui.setSelectedTile((Tile) myPlayer.getEntryLocation(), false);
        if (freeColClient.currentPlayerIsMyPlayer()) {
            igc.nextActiveUnit();
        }

        gui.setUpMouseListenersForCanvas();

        if (FreeColDebugger.isInDebugMode() && FreeColDebugger.getDebugRunTurns() > 0) {
            freeColClient.skipTurns(FreeColDebugger.getDebugRunTurns());
        } else if (freeColClient.getGame().getTurn().getNumber() == 1) {
            ModelMessage message =
                new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                 "tutorial.startGame", myPlayer);
            String direction = myPlayer.getNation().startsOnEastCoast()
                ? "west" : "east";
            message.add("%direction%", direction);
            myPlayer.addModelMessage(message);
            // force view of tutorial message
            igc.nextModelMessage();
        }
    }
}
