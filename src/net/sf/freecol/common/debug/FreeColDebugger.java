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

package net.sf.freecol.common.debug;

import java.io.File;
import java.io.IOException;
import java.util.logging.LogRecord;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.server.FreeColServer;

/**
 * High-level debug handling.
 * Throughout the code, routines check getDebugLevel().  The level enables
 * various things:
 *   - OFF turns on nothing
 *   - LIMITED turns on lots of miscellaneous stuff
 *     - debug menu actions including turn skipping
 *     - extra commands
 *       - Set Goods in ColonyPanel
 *       - Take Ownership in TilePopup
 *       - DebugForeignColony in Canvas
 *     - extra displays
 *       - goods-in-market tooltip in MarketLabel
 *       - display of region and mission in MapViewer
 *     - verbose (non-i18n) server error message display
 *   - FULL turns on the prepopulated colony in new games
 *   - FULL_COMMS turns on trace printing of c-s communications
 */
public class FreeColDebugger {

    public static final int DEBUG_OFF = 0;

    public static final int DEBUG_LIMITED = 1;

    public static final int DEBUG_FULL = 2;

    public static final int DEBUG_FULL_COMMS = 3;

    private static int debugLevel = DEBUG_OFF;

    /**
     * The number of turns to run without stopping.
     */
    private static int debugRunTurns = -1;

    /**
     * The name of a file to save to at the end of a debug run.
     */
    private static String debugRunSave = null;


    /**
     * Configures the debug level.
     *
     * @param optionValue The command line option.
     */
    public static void configureDebugLevel(String optionValue) {
        try {
            debugLevel = Integer.parseInt(optionValue);
            debugLevel = Math.min(Math.max(getDebugLevel(), DEBUG_OFF),
                                  DEBUG_FULL_COMMS);
        } catch (NumberFormatException e) {
            FreeColDebugger.debugLevel = DEBUG_FULL;
        }
    }

    /**
     * Configures a debug run.
     *
     * @param option The command line option.
     */
    public static void configureDebugRun(String option) {
        int comma = option.indexOf(",");
        String turns = option.substring(0, (comma < 0) ? option.length() : comma);
        try {
            setDebugRunTurns(Integer.parseInt(turns));
        } catch (NumberFormatException e) {
            setDebugRunTurns(-1);
        }
        if (comma > 0) setDebugRunSave(option.substring(comma + 1));
    }

    /**
     * Gets the debug level.
     *
     * @return The debug level.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Checks if the program is in debug mode.
     *
     * @return True if the program is in debug mode.
     */
    public static boolean isInDebugMode() {
        return debugLevel > DEBUG_OFF;
    }

    /**
     * Sets the "debug mode" to be active or not.
     *
     * @param debug Should be <code>true</code> in order to active
     *      debug mode and <code>false</code> otherwise.
     */
    public static void setInDebugMode(boolean debug) {
        FreeColDebugger.debugLevel = (debug) ? DEBUG_FULL : DEBUG_OFF;
    }

    /**
     * Gets the turns to run in debug mode.
     *
     * @return The turns to run in debug mode.
     */
    public static int getDebugRunTurns() {
        return debugRunTurns;
    }

    /**
     * Sets the number of turns to run in debug mode.
     *
     * @param debugRunTurns The new number of debug turns.
     */
    public static void setDebugRunTurns(int debugRunTurns) {
        FreeColDebugger.debugRunTurns = debugRunTurns;
    }

    /**
     * Gets the debug save file name.
     *
     * @return The debug save file name.
     */
    public static String getDebugRunSave() {
        return debugRunSave;
    }

    /**
     * Sets the debug save file name.
     *
     * @param debugRunSave The new debug save file name.
     */
    public static void setDebugRunSave(String debugRunSave) {
        FreeColDebugger.debugRunSave = debugRunSave;
    }

    /**
     * Try to complete a debug run if one is happening.
     *
     * @param freeColClient The <code>FreeColClient</code> of the game.
     * @param force Force early completion of a run.
     * @return True if a debug run was completed.
     */
    public static boolean finishDebugRun(FreeColClient freeColClient,
                                         boolean force) {
        if (debugRunTurns < 0) return false; // Not a debug run
        if (debugRunTurns > 0 && !force) return false; // Still going
        // Zero => signalEndDebugRun was called
        debugRunTurns = -1;

        if (debugRunSave != null) {
            FreeColServer fcs = freeColClient.getFreeColServer();
            if (fcs != null) {
                try {
                    fcs.saveGame(new File(".", debugRunSave),
                                 freeColClient.getMyPlayer().getName(),
                                 freeColClient.getClientOptions());
                } catch (IOException e) {}
            }
            freeColClient.quit();
        }
        return true;
    }
    
    /**
     * Signal that a debug run should complete at the next suitable
     * opportunity.  Currently called from the server.
     */
    public static void signalEndDebugRun() {
        if (debugRunTurns > 0) debugRunTurns = 0;
    }

    /**
     * Handler for log records that include a crash.
     *
     * @param record The <code>LogRecord</code> with a crash.
     */
    public static void handleCrash(LogRecord record) {
        if (debugRunSave != null) signalEndDebugRun();
    }
}
