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

package net.sf.freecol.client.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.UIManager;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileItem;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.resources.ResourceManager;



/**
* This class is responsible for drawing the map/background on the <code>Canvas</code>.
* In addition, the graphical state of the map (focus, active unit..) is also a responsibility
* of this class.
*/
public final class GUI {

    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    private final FreeColClient freeColClient;
    private Dimension size;
    private ImageLibrary lib;
    private TerrainCursor cursor;
    private ViewMode viewMode;

    /** Indicates if the game has started, has nothing to do with whether or not the
        client is logged in. */
    private boolean inGame;

    private final Vector<GUIMessage> messages;

    private Map.Position selectedTile;
    private Map.Position focus = null;
    private Unit activeUnit;

    /** A path to be displayed on the map. */
    private PathNode currentPath;
    private PathNode gotoPath = null;
    private boolean gotoStarted = false;

    // Helper variables for displaying the map.
    private int tileHeight,
    tileWidth,
    topSpace,
    topRows,
    //bottomSpace,
    bottomRows,
    leftSpace,
    rightSpace;

    // The y-coordinate of the Tiles that will be drawn at the bottom
    private int bottomRow = -1;
    // The y-coordinate of the Tiles that will be drawn at the top
    private int topRow;
    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the bottom
    private int bottomRowY;
    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the top
    private int topRowY;

    // The x-coordinate of the Tiles that will be drawn at the left side
    private int leftColumn;
    // The x-coordinate of the Tiles that will be drawn at the right side
    private int rightColumn;
    // The x-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the left (can be less than 0)
    private int leftColumnX;

    // The height offset to paint a Unit at (in pixels).
    public static final int UNIT_OFFSET = 20,
    TEXT_OFFSET_X = 2, // Relative to the state indicator.
    TEXT_OFFSET_Y = 13, // Relative to the state indicator.
    STATE_OFFSET_X = 25,
    STATE_OFFSET_Y = 10,
    ALARM_OFFSET_X = 37,
    ALARM_OFFSET_Y = 10,
    RUMOUR_OFFSET_X = 40,
    RUMOUR_OFFSET_Y = 5,
    MISSION_OFFSET_X = 49,
    MISSION_OFFSET_Y = 10,
    OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
    OTHER_UNITS_OFFSET_Y = 1,
    OTHER_UNITS_WIDTH = 3,
    MAX_OTHER_UNITS = 10,
    MESSAGE_COUNT = 3,
    MESSAGE_AGE = 30000; // The amount of time before a message gets deleted (in milliseconds).

    public static final int OVERLAY_INDEX = 100;
    public static final int FOREST_INDEX = 200;


    private boolean displayBorders = false;
    private boolean displayGrid = false;
    private int displayTileText = 0;
    private GeneralPath gridPath = null;

    // Debug variables:
    public boolean displayCoordinates = false;
    public boolean displayColonyValue = false;
    public Player displayColonyValuePlayer = null;

    public boolean debugShowMission = false;
    public boolean debugShowMissionInfo = false;
    
    private volatile boolean blinkingMarqueeEnabled;
    
    private Image cursorImage;
    
    private JLabel coatOfArms;
    private JLabel greyLayer;
    private JLabel waitingForMsg;
    
    private java.util.Map<Unit, Integer> unitsOutForAnimation;
    private java.util.Map<Unit, JLabel> unitsOutForAnimationLabels;

    /**
    * The constructor to use.
    *
    * @param freeColClient The main control class.
    * @param size The size of the GUI (= the entire screen if the app is displayed in full-screen).
    * @param lib The library of images needed to display certain things visually.
    */
    public GUI(FreeColClient freeColClient, Dimension size, ImageLibrary lib) {
        this.freeColClient = freeColClient;
        this.size = size;

        setImageLibrary(lib);
        
        unitsOutForAnimation = new HashMap<Unit, Integer>();
        unitsOutForAnimationLabels = new HashMap<Unit, JLabel>();

        inGame = false;
        logger.info("GUI created.");
        messages = new Vector<GUIMessage>(MESSAGE_COUNT);
        viewMode = new ViewMode(this);
        logger.info("Starting in Move Units View Mode");
        displayBorders = freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_BORDERS);
        displayGrid = freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_GRID);
        displayTileText = freeColClient.getClientOptions().getDisplayTileText();
        blinkingMarqueeEnabled = true;

        cursor = new net.sf.freecol.client.gui.TerrainCursor();
    }
    
    public void setImageLibrary(ImageLibrary lib) {
        this.lib = lib;
        cursorImage = lib.getMiscImage(ImageLibrary.UNIT_SELECT);
        TileType tileType = FreeCol.getSpecification().getTileTypeList().get(0);
        tileHeight = lib.getTerrainImageHeight(tileType);
        tileWidth = lib.getTerrainImageWidth(tileType);
        updateMapDisplayVariables();
    }
    
    private boolean isOutForAnimation(final Unit unit) {
        return unitsOutForAnimation.containsKey(unit);
    }
    
    /**
     * Run some code with the given unit made invisible.
     * You can nest several of these method calls in order
     * to hide multiple units. There are no problems
     * related to nested calls with the same unit.
     * 
     * @param unit The unit to be hidden.
     * @param r The code to be executed.
     */
    public void executeWithUnitOutForAnimation(final Unit unit, final OutForAnimationCallback r) {
        final JLabel unitLabel = enterUnitOutForAnimation(unit);
        try {
            r.executeWithUnitOutForAnimation(unitLabel);
        } finally {
            releaseUnitOutForAnimation(unit);
        }
    }
    
    private JLabel enterUnitOutForAnimation(final Unit unit) {
        Integer i = unitsOutForAnimation.get(unit);
        if (i == null) {
            i = 1;
            final JLabel unitLabel = getUnitLabel(unit);
            unitsOutForAnimationLabels.put(unit, unitLabel);
            final Integer UNIT_LABEL_LAYER = JLayeredPane.DEFAULT_LAYER;
            freeColClient.getCanvas().add(unitLabel, UNIT_LABEL_LAYER, false);
        } else {
            i++;
        }
        unitsOutForAnimation.put(unit, i);
        return unitsOutForAnimationLabels.get(unit);
    }
    
    private void releaseUnitOutForAnimation(final Unit unit) {
        Integer i = unitsOutForAnimation.get(unit);
        if (i == null) {
            throw new IllegalStateException("Tried to release unit that was not out for animation"); 
        }
        if (i == 1) {
            unitsOutForAnimation.remove(unit);
            freeColClient.getCanvas().remove(unitsOutForAnimationLabels.remove(unit), false);
        } else {
            i--;
            unitsOutForAnimation.put(unit, i); 
        }
    }

    private void updateMapDisplayVariables() {
        // Calculate the amount of rows that will be drawn above the central Tile
        topSpace = (size.height - tileHeight) / 2;
        if ((topSpace % (tileHeight / 2)) != 0) {
            topRows = topSpace / (tileHeight / 2) + 2;
        } else {
            topRows = topSpace / (tileHeight / 2) + 1;
        }
        bottomRows = topRows;
        leftSpace = (size.width - tileWidth) / 2;
        rightSpace = leftSpace;
    }
    
    /**
     *  Get the <code>View Mode</code> object
     * @return the current view mode.
     */
    public ViewMode getViewMode(){
        return viewMode;
    }
    
    /**
     * Starts the unit-selection-cursor blinking animation.
     */
    public void startCursorBlinking() {
        
        final FreeColClient theFreeColClient = freeColClient; 
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (!blinkingMarqueeEnabled) return;
                if (getActiveUnit() != null && getActiveUnit().getTile() != null) {
                    //freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
                    theFreeColClient.getCanvas().refreshTile(getActiveUnit().getTile());            
                }               
            }
        };
        
        cursor.addActionListener(taskPerformer);
        
        cursor.startBlinking();
    }
    
    public TerrainCursor getCursor(){
        return cursor;
    }
    
    public void setSize(Dimension size) {
        this.size = size;
        updateMapDisplayVariables();
    }
    
    public void moveTileCursor(Direction direction){
        Tile selectedTile = freeColClient.getGame().getMap().getTile(getSelectedTile());
        if(selectedTile != null){   
            Tile newTile = freeColClient.getGame().getMap().getNeighbourOrNull(direction, selectedTile);
            if(newTile != null)
                setSelectedTile(newTile.getPosition());
        }
        else{
            logger.warning("selectedTile is null");
        }
    }

    /**
    * Selects the tile at the specified position, without clearing
    * the orders of the first unit contained.
    *
    * @param selectedTile The <code>Position</code> of the tile
    *                     to be selected.
    * @see #setSelectedTile(Map.Position, boolean)
    */
    public void setSelectedTile(Position selectedTile) {
        setSelectedTile(selectedTile, false);
    }

    /**
    * Selects the tile at the specified position. There are three
    * possible cases:
    *
    * <ol>
    *   <li>If there is a {@link Colony} on the {@link Tile} the
    *       {@link Canvas#showColonyPanel} will be invoked.
    *   <li>If the tile contains a unit that can become active, then
    *       that unit will be set as the active unit, and clear their
    *       goto orders if clearGoToOrders is <code>true</code>
    *   <li>If the two conditions above do not match, then the
    *       <code>selectedTile</code> will become the map focus.
    * </ol>
    *
    * If a unit is active and is located on the selected tile,
    * then nothing (except perhaps a map reposition) will happen.
    *
    * @param selectedTile The <code>Position</code> of the tile
    *                     to be selected.
    * @param clearGoToOrders Use <code>true</code> to clear goto orders
    *                        of the unit which is activated.
    * @see #getSelectedTile
    * @see #setActiveUnit
    * @see #setFocus(Map.Position)
    */
    public void setSelectedTile(Position selectedTile, boolean clearGoToOrders) {
        Game gameData = freeColClient.getGame();

        if (selectedTile != null && !gameData.getMap().isValid(selectedTile)) {
            return;
        }

        Position oldPosition = this.selectedTile;

        this.selectedTile = selectedTile;

        if (viewMode.getView() == ViewMode.MOVE_UNITS_MODE) {
            if (activeUnit == null ||
                (activeUnit.getTile() != null &&
                 !activeUnit.getTile().getPosition().equals(selectedTile))) {
                Tile t = gameData.getMap().getTile(selectedTile);
                if (t != null && t.getSettlement() != null) {
                    Settlement s = t.getSettlement();
                    if (s instanceof Colony && s.getOwner().equals(freeColClient.getMyPlayer())) {
                        // show my colony
                        setFocus(selectedTile);
                        freeColClient.getCanvas().showColonyPanel((Colony) s);
                        return;
                    } else if (s instanceof IndianSettlement) {
                        // show the Indian camp
                        setFocus(selectedTile);
                        Canvas canvas =freeColClient.getCanvas();
                        canvas.showPanel(new IndianSettlementPanel(canvas, (IndianSettlement) s));
                        return;
                    }
                }

                // else, just select a unit on the selected tile
                Unit unitInFront = getUnitInFront(gameData.getMap().getTile(selectedTile));
                if (unitInFront != null) {
                    setActiveUnit(unitInFront);
                    updateGotoPathForActiveUnit();
                } else {
                    setFocus(selectedTile);
                }
            } else if (activeUnit.getTile() != null &&
                    activeUnit.getTile().getPosition().equals(selectedTile)) {
                // Clear goto order when unit is already active
                if (clearGoToOrders && activeUnit.getDestination() != null) {
                    freeColClient.getInGameController().clearGotoOrders(activeUnit);
                    updateGotoPathForActiveUnit();
                }
            }
        }
        
        freeColClient.getActionManager().update();
        freeColClient.getCanvas().updateJMenuBar();

        int x = 0, y = 0;
        MapControls mapControls = freeColClient.getCanvas().getMapControls();
        if (mapControls != null) {
            x = getWidth() - mapControls.getInfoPanelWidth();
            y = getHeight() - mapControls.getInfoPanelHeight();
        }
        freeColClient.getCanvas().repaint(x, y, getWidth(), getHeight());

        // Check if the gui needs to reposition:
        if ((!onScreen(selectedTile) &&
             freeColClient.getClientOptions().getBoolean(ClientOptions.JUMP_TO_ACTIVE_UNIT)) ||
            freeColClient.getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER)) {
                setFocus(selectedTile);
        } else {
            if (oldPosition != null) {
                freeColClient.getCanvas().refreshTile(oldPosition);
            }

            if (selectedTile != null) {
                freeColClient.getCanvas().refreshTile(selectedTile);
            }
        }
    }

    public void showColonyPanel(Position selectedTile) {
        Game gameData = freeColClient.getGame();

        if (selectedTile != null && !gameData.getMap().isValid(selectedTile)) {
            return;
        }

        if (viewMode.getView() == ViewMode.MOVE_UNITS_MODE) {
            Tile t = gameData.getMap().getTile(selectedTile);
            if (t != null && t.getSettlement() != null && t.getSettlement() instanceof Colony
                && t.getSettlement().getOwner().equals(freeColClient.getMyPlayer())) {
                
                setFocus(selectedTile);
                stopBlinking();
                freeColClient.getCanvas().showColonyPanel((Colony) t.getSettlement());
                return;
            }
        }
    }
    
    public void restartBlinking() {
        blinkingMarqueeEnabled = true;
    }
    
    public void stopBlinking() {
        blinkingMarqueeEnabled = false;
    }

    /**
    * Gets the unit that should be displayed on the given tile.
    *
    * @param unitTile The <code>Tile</code>.
    * @return The <code>Unit</code> or <i>null</i> if no unit applies.
    */
    public Unit getUnitInFront(Tile unitTile) {
        if (unitTile == null || unitTile.getUnitCount() <= 0) {
            return null;
        }

        if (activeUnit != null && activeUnit.getTile() == unitTile) {
            return activeUnit;
        } else {
            if (unitTile.getSettlement() == null) {
                Unit bestDefendingUnit = null;
                if (activeUnit != null) {
                    bestDefendingUnit = unitTile.getDefendingUnit(activeUnit);
                    if (bestDefendingUnit != null) {
                        return bestDefendingUnit;
                    }
                }
                
                Unit movableUnit = unitTile.getMovableUnit();
                if (movableUnit != null && movableUnit.getLocation() == movableUnit.getTile()) {
                    return movableUnit;
                } else {
                    Unit bestPick = null;
                    Iterator<Unit> unitIterator = unitTile.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = unitIterator.next();
                        if (bestPick == null || bestPick.getMovesLeft() < u.getMovesLeft()) {
                            bestPick = u;
                        }
                    }
                    
                    return bestPick;
                }
            } else {
                return null;
            }
        }
    }

    
    /**
    * Gets the selected tile.
    *
    * @return The <code>Position</code> of that tile.
    * @see #setSelectedTile(Map.Position)
    */
    public Position getSelectedTile() {
        return selectedTile;
    }


    /**
    * Gets the active unit.
    *
    * @return The <code>Unit</code>.
    * @see #setActiveUnit
    */
    public Unit getActiveUnit() {
        return activeUnit;
    }


    /**
    * Sets the active unit. Invokes {@link #setSelectedTile(Map.Position)} if the
    * selected tile is another tile than where the <code>activeUnit</code>
    * is located.
    *
    * @param activeUnit The new active unit.
    * @see #setSelectedTile(Map.Position)
    */
    public void setActiveUnit(Unit activeUnit) {
        // Don't select a unit with zero moves left. -sjm
        // The user might what to check the status of a unit - SG
        /*if ((activeUnit != null) && (activeUnit.getMovesLeft() == 0)) {
            freeColClient.getInGameController().nextActiveUnit();
            return;
        }*/
        
        if (activeUnit != null && activeUnit.getOwner() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
            return;
        }

        if (activeUnit != null && activeUnit.getTile() == null) {
            activeUnit = null;
        }

        this.activeUnit = activeUnit;

        if (activeUnit != null) {
            if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                if (activeUnit.getState() != UnitState.ACTIVE) {
                    freeColClient.getInGameController().clearOrders(activeUnit);
                }
            } else {
                freeColClient.getInGameController().clearGotoOrders(activeUnit);
            }
        }
        updateGotoPathForActiveUnit();

        // The user activated a unit
        if(viewMode.getView() == ViewMode.VIEW_TERRAIN_MODE && activeUnit != null)
            viewMode.changeViewMode(ViewMode.MOVE_UNITS_MODE);

        //if (activeUnit != null && !activeUnit.getTile().getPosition().equals(selectedTile)) {
        if (activeUnit != null) {
            setSelectedTile(activeUnit.getTile().getPosition());
        } else {
            freeColClient.getActionManager().update();
            freeColClient.getCanvas().updateJMenuBar();

            int x = 0, y = 0;
            MapControls mapControls = freeColClient.getCanvas().getMapControls();
            if (mapControls != null) {
                x = getWidth() - mapControls.getInfoPanelWidth();
                y = getHeight() - mapControls.getInfoPanelHeight();
            }
            freeColClient.getCanvas().repaint(x, y, getWidth(), getHeight());
        }
    }


    /**
    * Gets the focus of the map. That is the center tile of the displayed
    * map.
    *
    * @return The <code>Position</code> of the center tile of the
    *         displayed map
    * @see #setFocus(Map.Position)
    */
    public Position getFocus() {
        return focus;
    }


    /**
    * Sets the focus of the map.
    *
    * @param focus The <code>Position</code> of the center tile of the
    *             displayed map.
    * @see #getFocus
    */
    public void setFocus(Position focus) {
        this.focus = focus;

        forceReposition();
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
    }

    /**
    * Sets the focus of the map and repaints the screen immediately.
    *
    * @param focus The <code>Position</code> of the center tile of the
    *             displayed map.
    * @see #getFocus
    */
    public void setFocusImmediately(Position focus) {
        this.focus = focus;

        forceReposition();
        freeColClient.getCanvas().paintImmediately(0, 0, getWidth(), getHeight());
    }

    /**
    * Sets the focus of the map.
    *
    * @param x The x-coordinate of the center tile of the
    *         displayed map.
    * @param y The x-coordinate of the center tile of the
    *         displayed map.
    * @see #getFocus
    */
    public void setFocus(int x, int y) {
        setFocus(new Map.Position(x,y));
    }


    /**
     * Gets the amount of message that are currently being displayed on this GUI.
     * @return The amount of message that are currently being displayed on this GUI.
     */
    public int getMessageCount() {
        return messages.size();
    }


    /**
     * Gets the message at position 'index'. The message at position 0 is the oldest
     * message and is most likely to be removed during the next call of removeOldMessages().
     * The higher the index of a message, the more recently it was added.
     *
     * @param index The index of the message to return.
     * @return The message at position 'index'.
     */
    public GUIMessage getMessage(int index) {
        return messages.get(index);
    }


    /**
    * Adds a message to the list of messages that need to be displayed on the GUI.
    * @param message The message to add.
    */
    public synchronized void addMessage(GUIMessage message) {
        if (getMessageCount() == MESSAGE_COUNT) {
            messages.remove(0);
        }
        messages.add(message);

        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
    }


    /**
     * Removes all the message that are older than MESSAGE_AGE.
     * @return 'true' if at least one message has been removed, 'false' otherwise.
     * This can be useful to see if it is necessary to refresh the screen.
     */
    public synchronized boolean removeOldMessages() {
        long currentTime = new Date().getTime();
        boolean result = false;

        int i = 0;
        while (i < getMessageCount()) {
            long messageCreationTime = getMessage(i).getCreationTime().getTime();
            if ((currentTime - messageCreationTime) >= MESSAGE_AGE) {
                result = true;
                messages.remove(i);
            } else {
                i++;
            }
        }

        return result;
    }


    /**
     * Returns the width of this GUI.
     * @return The width of this GUI.
     */
    public int getWidth() {
        return size.width;
    }


    /**
     * Returns the height of this GUI.
     * @return The height of this GUI.
     */
    public int getHeight() {
        return size.height;
    }


    /**
     * Displays this GUI onto the given Graphics2D.
     * @param g The Graphics2D on which to display this GUI.
     */
    public void display(Graphics2D g) {
        if ((freeColClient.getGame() != null)
                && (freeColClient.getGame().getMap() != null)
                && (focus != null)
                && inGame) {
            removeOldMessages();
            displayMap(g);
        } else {
            if (freeColClient.isMapEditor()) {
                g.setColor(Color.black);
                g.fillRect(0, 0, size.width, size.height);                
            } else {
                Image bgImage = ResourceManager.getImage("CanvasBackgroundImage", size);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, freeColClient.getCanvas());
                    
                    // Show version on initial screen
                    String versionStr = "v. " + FreeCol.getVersion();
                    Font oldFont = g.getFont();
                    Color oldColor = g.getColor();
                    Font newFont = oldFont.deriveFont(Font.BOLD);
                    TextLayout layout = new TextLayout(versionStr, newFont, g.getFontRenderContext());
                   
                    Rectangle2D bounds = layout.getBounds();
                    float x = getWidth() - (float) bounds.getWidth() - 5;
                    float y = getHeight() - (float) bounds.getHeight();
                    g.setColor(Color.white);
                    layout.draw(g, x, y);
                    
                    // restore old values
                    g.setFont(oldFont);
                    g.setColor(oldColor);
                    
                } else {
                    g.setColor(Color.black);
                    g.fillRect(0, 0, size.width, size.height);
                }
            }
        }
    }


    /**
     * Returns the amount of columns that are to the left of the Tile
     * that is displayed in the center of the Map.
     * @return The amount of columns that are to the left of the Tile
     * that is displayed in the center of the Map.
     */
    private int getLeftColumns() {
        return getLeftColumns(focus.getY());
    }


    /**
     * Returns the amount of columns that are to the left of the Tile
     * with the given y-coordinate.
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the left of the Tile
     * with the given y-coordinate.
     */
    private int getLeftColumns(int y) {
        int leftColumns;

        if ((y % 2) == 0) {
            leftColumns = leftSpace / tileWidth + 1;
            if ((leftSpace % tileWidth) > 32) {
                leftColumns++;
            }
        } else {
            leftColumns = leftSpace / tileWidth + 1;
            if ((leftSpace % tileWidth) == 0) {
                leftColumns--;
            }
        }

        return leftColumns;
    }


    /**
     * Returns the amount of columns that are to the right of the Tile
     * that is displayed in the center of the Map.
     * @return The amount of columns that are to the right of the Tile
     * that is displayed in the center of the Map.
     */
    private int getRightColumns() {
        return getRightColumns(focus.getY());
    }


    /**
     * Returns the amount of columns that are to the right of the Tile
     * with the given y-coordinate.
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the right of the Tile
     * with the given y-coordinate.
     */
    private int getRightColumns(int y) {
        int rightColumns;

        if ((y % 2) == 0) {
            rightColumns = rightSpace / tileWidth + 1;
            if ((rightSpace % tileWidth) == 0) {
                rightColumns--;
            }
        } else {
            rightColumns = rightSpace / tileWidth + 1;
            if ((rightSpace % tileWidth) > 32) {
                rightColumns++;
            }
        }

        return rightColumns;
    }


    /**
     * Position the map so that the Tile at the focus location is
     * displayed at the center.
     */
    private void positionMap() {
        Game gameData = freeColClient.getGame();

        if (focus == null) {
            return;
        }
        
        int x = focus.getX(),
            y = focus.getY();
        int leftColumns = getLeftColumns(),
            rightColumns = getRightColumns();

        /*
        PART 1
        ======
        Calculate: bottomRow, topRow, bottomRowY, topRowY
        This will tell us which rows need to be drawn on the screen (from
        bottomRow until and including topRow).
        bottomRowY will tell us at which height the bottom row needs to be
        drawn.
        */

        if (y < topRows) {
            // We are at the top of the map
            bottomRow = (size.height / (tileHeight / 2)) - 1;
            if ((size.height % (tileHeight / 2)) != 0) {
                bottomRow++;
            }
            topRow = 0;
            bottomRowY = bottomRow * (tileHeight / 2);
            topRowY = 0;
        } else if (y >= (gameData.getMap().getHeight() - bottomRows)) {
            // We are at the bottom of the map
            bottomRow = gameData.getMap().getHeight() - 1;

            topRow = size.height / (tileHeight / 2);
            if ((size.height % (tileHeight / 2)) > 0) {
                topRow++;
            }
            topRow = gameData.getMap().getHeight() - topRow;

            bottomRowY = size.height - tileHeight;
            topRowY = bottomRowY - (bottomRow - topRow) * (tileHeight / 2);
        } else {
            // We are not at the top of the map and not at the bottom
            bottomRow = y + bottomRows;
            topRow = y - topRows;
            bottomRowY = topSpace + (tileHeight / 2) * bottomRows;
            topRowY = topSpace - topRows * (tileHeight / 2);
        }

        /*
        PART 2
        ======
        Calculate: leftColumn, rightColumn, leftColumnX
        This will tell us which columns need to be drawn on the screen (from
        leftColumn until and including rightColumn).
        leftColumnX will tell us at which x-coordinate the left column needs
        to be drawn (this is for the Tiles where y%2==0; the others should be
        tileWidth / 2 more to the right).
        */

        if (x < leftColumns) {
            // We are at the left side of the map
            leftColumn = 0;

            rightColumn = size.width / tileWidth - 1;
            if ((size.width % tileWidth) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
        } else if (x >= (gameData.getMap().getWidth() - rightColumns)) {
            // We are at the right side of the map
            rightColumn = gameData.getMap().getWidth() - 1;

            leftColumn = size.width / tileWidth;
            if ((size.width % tileWidth) > 0) {
                leftColumn++;
            }

            leftColumnX = size.width - tileWidth - tileWidth / 2 -
                leftColumn * tileWidth;
            leftColumn = rightColumn - leftColumn;
        } else {
            // We are not at the left side of the map and not at the right side
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;
            leftColumnX = (size.width - tileWidth) / 2 - leftColumns * tileWidth;
        }
    }

    
    private void displayGotoPath(Graphics2D g, PathNode gotoPath) {
        if (gotoPath != null) {
            PathNode temp = gotoPath;
            while (temp != null) {
                Point p = getTilePosition(temp.getTile());
                if (p != null) {
                    Tile tile = temp.getTile();
                    Image image;
                    final Color textColor; 
                    if (temp.getTurns() == 0) {
                        g.setColor(Color.GREEN);                        
                        image = getPathImage(activeUnit);
                        if (activeUnit != null 
                                && tile.isExplored()
                                && activeUnit.isNaval()
                                && tile.isLand() 
                                && (tile.getColony() == null || tile.getColony().getOwner() != activeUnit.getOwner())) {
                            image = getPathImage(activeUnit.getFirstUnit());
                        }
                        textColor = Color.BLACK;
                    } else {
                        g.setColor(Color.RED);
                        image = getPathNextTurnImage(activeUnit);
                        if (activeUnit != null
                                && tile.isExplored()
                                && activeUnit.isNaval()
                                && tile.isLand() 
                                && (tile.getColony() == null || tile.getColony().getOwner() != activeUnit.getOwner())) {
                            image = getPathNextTurnImage(activeUnit.getFirstUnit());
                        }
                        textColor = Color.WHITE;
                    }                
                    if (image != null) {
                        g.drawImage(image, p.x + (tileWidth - image.getWidth(null))/2, p.y + (tileHeight - image.getHeight(null))/2, null);
                    } else {
                        g.fillOval(p.x + tileWidth/2, p.y + tileHeight/2, 10, 10);
                        g.setColor(Color.BLACK);
                        g.drawOval(p.x + tileWidth/2, p.y + tileHeight/2, 10, 10);
                    }                
                    if (temp.getTurns() > 0) {
                        BufferedImage stringImage = createStringImage(g, Integer.toString(temp.getTurns()), textColor, tileWidth, 12);
                        g.drawImage(stringImage, p.x + (tileWidth - stringImage.getWidth(null))/2, p.y + (tileHeight - stringImage.getHeight()) / 2, null);
                    }
                }                    
                temp = temp.next;
            }
        }
    }

    private int getXOffset(int clipLeftX, int tileY) {
        int xx = clipLeftX;
        if ((tileY % 2) != 0) {
            xx += tileWidth / 2;
        }
        return xx;
    }



    /**
     * Displays the Map onto the given Graphics2D object. The Tile at
     * location (x, y) is displayed in the center.
     * @param g The Graphics2D object on which to draw the Map.
     */
    private void displayMap(Graphics2D g) {
        Rectangle clipBounds = g.getClipBounds();
        Map map = freeColClient.getGame().getMap();

        /*
        PART 1
        ======
        Position the map if it is not positioned yet.
        */

        if (bottomRow < 0) {
            positionMap();
        }

        /*
        PART 1a
        =======
        Determine which tiles need to be redrawn.
        */
        int clipTopRow = (clipBounds.y - topRowY) / (tileHeight / 2) - 1;
        int clipTopY = topRowY + clipTopRow * (tileHeight / 2);
        clipTopRow = topRow + clipTopRow;

        int clipLeftCol = (clipBounds.x - leftColumnX) / tileWidth - 1;
        int clipLeftX = leftColumnX + clipLeftCol * tileWidth;
        clipLeftCol = leftColumn + clipLeftCol;

        int clipBottomRow = (clipBounds.y + clipBounds.height - topRowY) / (tileHeight / 2);
        clipBottomRow = topRow + clipBottomRow;

        int clipRightCol = (clipBounds.x + clipBounds.width - leftColumnX) / tileWidth;
        clipRightCol = leftColumn + clipRightCol;

        /*
        PART 1b
        =======
        Create a GeneralPath to draw the grid with, if needed.
        */
        if (displayGrid) {
            gridPath = new GeneralPath();
            gridPath.moveTo(0, 0);
            int nextX = tileWidth / 2;
            int nextY = - (tileHeight / 2);

            for (int i = 0; i <= ((clipRightCol - clipLeftCol) * 2 + 1); i++) {
                gridPath.lineTo(nextX, nextY);
                nextX += tileWidth / 2;
                if (nextY == - (tileHeight / 2)) {
                    nextY = 0;
                }
                else {
                    nextY = - (tileHeight / 2);
                }
            }
        }

        /*
        PART 2
        ======
        Display the Tiles and the Units.
        */

        g.setColor(Color.black);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        int xx;

        /*
        PART 2a
        =======
        Display the base Tiles
        */
        
        int yy = clipTopY;

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(4));

        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = getXOffset(clipLeftX, tileY);

            // Column per column; start at the left side to display the tiles.
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                Tile tile = map.getTile(tileX, tileY);
                displayBaseTile(g, map, tile, xx, yy, true);
                paintBorders(g, tile, xx, yy);
                xx += tileWidth;
            }

            yy += tileHeight / 2;
        }
        g.setStroke(oldStroke);

        /*
        PART 2b
        =======
        Display the Tile overlays and Units
        */

        List<Unit> darkUnits = new ArrayList<Unit>();
        List<Integer> darkUnitsX = new ArrayList<Integer>();
        List<Integer> darkUnitsY = new ArrayList<Integer>();
        
        yy = clipTopY;

        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = getXOffset(clipLeftX, tileY);

            if (displayGrid) {
                // Display the grid.
                g.translate(xx, yy + (tileHeight / 2));
                g.setColor(Color.BLACK);
                g.draw(gridPath);
                g.translate(- xx, - (yy + (tileHeight / 2)));
            }

            // Column per column; start at the left side to display the tiles.
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                Tile tile = map.getTile(tileX, tileY);
                    
                // Display the Tile overlays:
                displayTileOverlays(g, map, tile, xx, yy, true, true);
                    
                if (viewMode.displayTileCursor(tile,xx,yy)) {
                    drawCursor(g, xx, yy);
                }
                xx += tileWidth;
            }

            xx = getXOffset(clipLeftX, tileY);

            // Again, column per column starting at the left side. Now display the units
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                // Display any units on that Tile:

                Unit unitInFront = getUnitInFront(map.getTile(tileX, tileY));
                if (unitInFront != null && !isOutForAnimation(unitInFront)) {
                    displayUnit(g, unitInFront, xx, yy);
                        
                    if (unitInFront.isUndead()) {
                        darkUnits.add(unitInFront);
                        darkUnitsX.add(xx);
                        darkUnitsY.add(yy);
                    }
                }
                xx += tileWidth;
            }

            yy += tileHeight / 2;
        }

        /*
        PART 2c
        =======
        Display darkness (revenge mode)
        */
        if (darkUnits.size() > 0) {
            g.setColor(Color.BLACK);
            final Image im = getImageLibrary().getMiscImage(ImageLibrary.DARKNESS);
            for (int index=0; index<darkUnits.size(); index++) {
                final Unit u = darkUnits.get(index);
                final int x = darkUnitsX.get(index);
                final int y = darkUnitsY.get(index);            
                g.drawImage(im, x  + tileWidth/2 - im.getWidth(null)/2, y + tileHeight/2 - im.getHeight(null)/2, null);
                displayUnit(g, u, x, y);
            }
        }

        /*
        PART 3
        ======
        Display the colony names.
        */
        //xx = 0;
        yy = clipTopY;
        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = getXOffset(clipLeftX, tileY);

            // Column per column; start at the left side
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                Tile tile = map.getTile(tileX, tileY);
                if (tile != null && tile.getSettlement() != null
                    && tile.getSettlement().getName() != null) {
                    Settlement settlement = tile.getSettlement();
                    BufferedImage stringImage =
                        createSettlementNameImage(g, settlement,
                                                  lib.getTerrainImageWidth(tile.getType()) * 4/3, 16);
                    g.drawImage(stringImage, 
                                xx + (lib.getTerrainImageWidth(tile.getType()) - 
                                      stringImage.getWidth())/2 + 1,
                                yy + (lib.getSettlementImage(settlement).getHeight(null) + 1), null);
                }
                xx += tileWidth;
            }
            yy += tileHeight / 2;
        }

        /*
        PART 4
        ======
        Display goto path
        */

        displayGotoPath(g, currentPath);
        displayGotoPath(g, gotoPath);
        
        /*
        PART 5
        ======
        Grey out the map if it is not my turn (and a multiplayer game).
         */
        Canvas canvas = freeColClient.getCanvas();
        // Remove the last coat of arms and waiting for message if it was set
        if (coatOfArms != null) {
            canvas.remove(coatOfArms, false);
            coatOfArms = null;
        }
        if (waitingForMsg != null) {
            canvas.remove(waitingForMsg, false);
            waitingForMsg = null;
        }
        
        if (!freeColClient.isMapEditor()
                && freeColClient.getGame() != null
                && freeColClient.getMyPlayer() != freeColClient.getGame().getCurrentPlayer()) {
            
            if (greyLayer == null) {
                greyLayer = buildGreyLayer();
            } else if (!greyLayer.getSize().equals(size)) {
                canvas.remove(greyLayer, false);
                greyLayer = buildGreyLayer();
            }
            if (greyLayer.getParent() == null) { // Not added to the canvas yet.
                canvas.add(greyLayer, JLayeredPane.DEFAULT_LAYER, false);
                canvas.moveToFront(greyLayer);
            }
            
            final Color currentPlayerColor = freeColClient.getGame().getCurrentPlayer().getColor();
            final String nation = freeColClient.getGame().getCurrentPlayer().getNationAsString();
            Image im = createStringImage(g, 
                    Messages.message("waitingFor", "%nation%", nation),
                    currentPlayerColor,
                    640,
                    18);
                        
            final ImageIcon coatOfArmsIcon = lib.getCoatOfArmsImageIcon(freeColClient.getGame().getCurrentPlayer().getNation());
            final int cHeight = (coatOfArmsIcon != null) ? coatOfArmsIcon.getIconHeight() : 0;
            final int cWidth = (coatOfArmsIcon != null) ? coatOfArmsIcon.getIconWidth() : 0;
            
            waitingForMsg = new JLabel(new ImageIcon(im));
            waitingForMsg.setBounds((size.width - im.getWidth(null)) / 2, (size.height - im.getHeight(null) - cHeight) / 2, im.getWidth(null), im.getHeight(null));
            canvas.add(waitingForMsg, JLayeredPane.DEFAULT_LAYER, false);
            canvas.moveToFront(waitingForMsg);
            
            if (coatOfArmsIcon != null) {
                coatOfArms = new JLabel(coatOfArmsIcon);
                coatOfArms.setBounds((size.width - cWidth) / 2, (size.height - cHeight) / 2 + im.getHeight(null), cWidth, cHeight);
                canvas.add(coatOfArms, JLayeredPane.DEFAULT_LAYER, false);
                canvas.moveToFront(coatOfArms);
            }
        }
        else {
            if (greyLayer != null && greyLayer.getParent() != null) {
                canvas.remove(greyLayer, false);
            }
        }

        /*
        PART 6
        ======
        Display the messages.
        */

        // Don't edit the list of messages while I'm drawing them.
        synchronized (this) {
            BufferedImage si = createStringImage(g, "getSizes", Color.WHITE, size.width, 12);

            yy = size.height - 300 - getMessageCount() * si.getHeight();// 200 ;
            xx = 40;

            for (int i = 0; i < getMessageCount(); i++) {
                GUIMessage message = getMessage(i);
                g.drawImage(createStringImage(g, message.getMessage(), message.getColor(), size.width, 12), xx, yy, null);
                yy += si.getHeight();
            }
        }
    }
    
    /**
     * Builds a JLabel object which is a image of a black rectangle with 60% opacity.
     * The size will be the GUI's size and coordinates 0,0
     * @return The JLabel containing the grey layer
     */
    private JLabel buildGreyLayer() {
        BufferedImage greyLayerImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D greyG = greyLayerImg.createGraphics();
        greyG.setColor(Color.BLACK);
        Composite oldComposite = greyG.getComposite();
        greyG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        greyG.fill(new Rectangle(0, 0, size.width, size.height));
        greyG.setComposite(oldComposite);
        JLabel toReturn = new JLabel(new ImageIcon(greyLayerImg));
        toReturn.setBounds(0, 0, size.width, size.height);
        
        return toReturn;
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + u.getPathTypeImage() + ".image");
        }
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     *
    private Image getPathIllegalImage(Unit u) {
        if (u == null || u.isNaval()) {
            return (Image) UIManager.get("path.naval.illegal.image");
        } else if (u.isMounted()) {
            return (Image) UIManager.get("path.horse.illegal.image");
        } else if (u.getType() == Unit.WAGON_TRAIN || u.getType() == Unit.TREASURE_TRAIN || u.getType() == Unit.ARTILLERY || u.getType() == Unit.DAMAGED_ARTILLERY) {
            return (Image) UIManager.get("path.wagon.illegal.image");
        } else {
            return (Image) UIManager.get("path.foot.illegal.image");
        }
    }
    */
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathNextTurnImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + u.getPathTypeImage() + ".nextTurn.image");
        }
    }

    /**
    * Creates an image with a string of a given color and with 
    * a black border around the glyphs.
    *
    * @param g A <code>Graphics</code>-object for getting a
    *       <code>Font</code>.
    * @param nameString The <code>String</code> to make an image of.
    * @param color The <code>Color</code> to use when displaying 
    *       the <code>nameString</code>.
    * @param maxWidth The maximum width of the image. The size of 
    *       the <code>Font</code> will be adjusted if the image gets 
    *       larger than this value.
    * @param preferredFontSize The preferred font size.
    * @return The image that was created.
    */
    public BufferedImage createStringImage(Graphics2D g, String nameString, Color color, int maxWidth, int preferredFontSize) {
        return createStringImage(null, g, nameString, color, maxWidth, preferredFontSize);
    }
    
    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param c A <code>JComponent</code>-object for getting a
     *       <code>Font</code>.
     * @param nameString The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>nameString</code>.
     * @param maxWidth The maximum width of the image. The size of 
     *       the <code>Font</code> will be adjusted if the image gets 
     *       larger than this value.
     * @param preferredFontSize The preferred font size.
     * @return The image that was created.
     */
    public BufferedImage createStringImage(JComponent c, String nameString, Color color, int maxWidth, int preferredFontSize) {
        return createStringImage(c, null, nameString, color, maxWidth, preferredFontSize);
    }
    
    /**
     * For performance reason, string images are rendered once and cached for reuse.
     * The class StringImageKey provide an identifier for looking up images
     */
    private class StringImageKey {
        public Color color;
        public String name;
        public StringImageKey(Color c, String s) {
            this.color = c;
            this.name = s;
        }
        public int hashCode() {
            return name.hashCode();
        }
        public boolean equals(Object o) {
            if (o==null || !(o instanceof StringImageKey))
                return false;
            StringImageKey other = (StringImageKey) o;
            return (other.color.equals(this.color)) && (other.name.equals(this.name));
        }
    }
    private HashMap<StringImageKey, BufferedImage> stringImageCache = new HashMap<StringImageKey, BufferedImage>();
    
    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param c A <code>JComponent</code>-object for getting a
     *       <code>Font</code>.
     * @param g A <code>Graphics</code>-object for getting a
     *       <code>Font</code>.
     * @param nameString The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>nameString</code>.
     * @param maxWidth The maximum width of the image. The size of 
     *       the <code>Font</code> will be adjusted if the image gets 
     *       larger than this value.
     * @param preferredFontSize The preferred font size.
     * @return The image that was created.
     */
    private BufferedImage createStringImage(JComponent c, Graphics g, String nameString, Color color, int maxWidth, int preferredFontSize) {
        if (color == null) {
            logger.warning("createStringImage called with color null");
            color = Color.WHITE;
        }
        BufferedImage bi = null;
        // Lookup in the cache if the image has been generated already
        StringImageKey key = new StringImageKey(color, nameString);
        bi = stringImageCache.get(key);
        if (bi!=null) {
            return bi;
        }
        
        Font nameFont = (c != null) ? c.getFont() : g.getFont();
        FontMetrics nameFontMetrics = (c != null) ? c.getFontMetrics(nameFont) : g.getFontMetrics(nameFont);

        // create an image of the appropriate size
        int fontSize = preferredFontSize;
        do {
            nameFont = nameFont.deriveFont(Font.BOLD, fontSize);
            nameFontMetrics = (c != null) ? c.getFontMetrics(nameFont) : g.getFontMetrics(nameFont);
            bi = new BufferedImage(nameFontMetrics.stringWidth(nameString) + 4, 
                                   nameFontMetrics.getMaxAscent() + nameFontMetrics.getMaxDescent(), BufferedImage.TYPE_INT_ARGB);
            fontSize -= 2;
        } while (bi.getWidth() > maxWidth);

        // draw the string with selected color
        Graphics2D big = bi.createGraphics();
        big.setColor(color);
        big.setFont(nameFont);
        big.drawString(nameString, 2, nameFontMetrics.getMaxAscent());

        // draw the border around letters
        int playerColor = color.getRGB();
        int borderColor = getStringBorderColor(color).getRGB();
        for (int biX=0; biX<bi.getWidth(); biX++) {
            for (int biY=0; biY<bi.getHeight(); biY++) {
                int r = bi.getRGB(biX, biY);

                if (r == playerColor) {
                    continue;
                }

                for (int cX=-1; cX <=1; cX++) {
                    for (int cY=-1; cY <=1; cY++) {
                        if (biX+cX >= 0 && biY+cY >= 0 && biX+cX < bi.getWidth() && biY+cY < bi.getHeight() && bi.getRGB(biX + cX, biY + cY) == playerColor) {
                            bi.setRGB(biX, biY, borderColor);
                            continue;
                        }
                    }
                }
            }
        }
        this.stringImageCache.put(key, bi);
        return bi;
    }

    private BufferedImage createSettlementNameImage(Graphics g, Settlement settlement,
                                                int maxWidth, int preferredFontSize) {        
        return createStringImage((Graphics2D) g,
                settlement.getName(),
                settlement.getOwner().getColor(),
                maxWidth,
                preferredFontSize);
    }

    /**
    * Draws a road, between the given points, on the provided <code>Graphics</code>.
    * When you provide the same <code>seed</code> you will get the same road.
    *
    * @param g The <code>Graphics</code> to draw the road upon.
    * @param seed The seed of the random generator that is creating the road.
    * @param x1 The x-component of the first coordinate.
    * @param y1 The y-component of the first coordinate.
    * @param x2 The x-component of the second coordinate.
    * @param y2 The y-component of the second coordinate.
    */
    public void drawRoad(Graphics2D g, long seed, int x1, int y1, int x2, int y2) {
        final int MAX_CORR = 4;
        Color oldColor = g.getColor();
        Random roadRandom = new Random(seed);

        int i = Math.max(Math.abs(x2-x1), Math.abs(y2-y1));
        int baseX = x1;
        int baseY = y1;
        double addX = (x2-x1)/((double) i);
        double addY = (y2-y1)/((double) i);
        int corr = 0;
        int xCorr = 0;
        int yCorr = 0;
        int lastDiff = 1;

        g.setColor(new Color(128, 64, 0));
        g.drawLine(baseX, baseY, baseX, baseY);

        for (int j=1; j<=i; j++) {
            int oldCorr = corr;
            //if (roadRandom.nextInt(3) == 0) {
                corr = corr + roadRandom.nextInt(3)-1;
                if (oldCorr != corr) {
                    lastDiff = oldCorr - corr;
                }
            //}

            if (Math.abs(corr) > MAX_CORR || Math.abs(corr) >= i-j) {
                if (corr > 0) {
                    corr--;
                } else {
                    corr++;
                }
            }

            if (corr != oldCorr) {
                g.setColor(new Color(128, 128, 0));
                g.drawLine(baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr, baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr);
            } else {
                int oldXCorr = 0;
                int oldYCorr = 0;

                if (x2-x1 == 0) {
                    oldXCorr = corr+lastDiff;
                    oldYCorr = 0;
                } else if (y2-y1 == 0) {
                    oldXCorr = 0;
                    oldYCorr = corr+lastDiff;
                } else {
                    if (corr > 0) {
                        oldXCorr = corr+lastDiff;
                    } else {
                        oldYCorr = corr+lastDiff;
                    }
                }

                g.setColor(new Color(128, 128, 0));
                g.drawLine(baseX+(int) (j*addX)+oldXCorr, baseY+(int) (j*addY)+oldYCorr, baseX+(int) (j*addX)+oldXCorr, baseY+(int) (j*addY)+oldYCorr);
            }

            if (x2-x1 == 0) {
                xCorr = corr;
                yCorr = 0;
            } else if (y2-y1 == 0) {
                xCorr = 0;
                yCorr = corr;
            } else {
                if (corr > 0) {
                    xCorr = corr;
                } else {
                    yCorr = corr;
                }
            }

            g.setColor(new Color(128, 64, 0));
            g.drawLine(baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr, baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr);
        }
        g.setColor(oldColor);
    }


    /**
     * Displays the given <code>Tile</code> onto the given 
     * <code>Graphics2D</code> object at the location specified 
     * by the coordinates. The visualization of the <code>Tile</code>
     * also includes information from the corresponding
     * <code>ColonyTile</code> from the given <code>Colony</code>.
     * 
     * @param g The <code>Graphics2D</code> object on which to draw 
     *      the <code>Tile</code>.
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     *      (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     *      (in pixels).
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> for. This object is also used to
     *      get the <code>ColonyTile</code> for the given <code>Tile</code>.
     */
    public void displayColonyTile(Graphics2D g, Map map, Tile tile, int x, int y, Colony colony) {
        displayBaseTile(g, map, tile, x, y, false);        

        Unit occupyingUnit = null;
        int price = 0;
        if (colony != null) {
            occupyingUnit = colony.getColonyTile(tile).getOccupyingUnit();
            Settlement settlement = tile.getOwningSettlement();
            price = colony.getOwner().getLandPrice(tile);
            if ((settlement != null &&
                 (settlement instanceof Colony && settlement != colony) ||
                 (settlement instanceof IndianSettlement && price > 0)) ||
                occupyingUnit != null) {
                // tile is used by another Settlement
                g.drawImage(lib.getMiscImage(ImageLibrary.TILE_TAKEN), x, y, null);
            }
        }
        displayTileOverlays(g, map, tile, x, y, false, false);
        
        if (price > 0 && tile.getSettlement() == null) {
            // tile is owned by an IndianSettlement
            Image image = lib.getMiscImage(ImageLibrary.TILE_OWNED_BY_INDIANS);
            g.drawImage(image, x+tileWidth/2-image.getWidth(null)/2, y+tileHeight/2-image.getHeight(null)/2, null);
        }
        
        if (occupyingUnit != null) {
            ImageIcon image = lib.getScaledImageIcon(lib.getUnitImageIcon(occupyingUnit), 0.5f);
            g.drawImage(image.getImage(), (x + tileWidth / 4) - image.getIconWidth() / 2,
                    (y + tileHeight / 2) - image.getIconHeight() / 2, null);
            // Draw an occupation and nation indicator.
            displayOccupationIndicator(g, occupyingUnit, x + (int) (STATE_OFFSET_X * lib.getScalingFactor()), y);
        }
    }


    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Draws the terrain and
     * improvements. Doesn't draw settlements, lost city rumours, fog
     * of war, optional values neither units.
     *
     * <br><br>The same as calling <code>displayTile(g, map, tile, x, y, true);</code>.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    public void displayTerrain(Graphics2D g, Map map, Tile tile, int x, int y) {
        displayBaseTile(g, map, tile, x, y, true);
        displayTileItems(g, map, tile, x, y);
        //displayUnexploredBorders(g, map, tile, x, y);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     *
     * <br><br>The same as calling <code>displayTile(g, map, tile, x, y, true);</code>.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, int x, int y) {
        displayTile(g, map, tile, x, y, true);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, int x, int y, boolean drawUnexploredBorders) {
        displayBaseTile(g, map, tile, x, y, drawUnexploredBorders);
        displayTileOverlays(g, map, tile, x, y, drawUnexploredBorders, true);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Only base terrain will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    private void displayBaseTile(Graphics2D g, Map map, Tile tile, int x, int y, boolean drawUnexploredBorders) {
        if (tile == null) {
            return;
        }
        g.drawImage(lib.getTerrainImage(tile.getType(), tile.getX(), tile.getY()), x, y, null);

        Map.Position pos = new Map.Position(tile.getX(), tile.getY());

        for (Direction direction : Direction.values()) {
            Tile borderingTile = map.getAdjacentTile(pos, direction);
            if (borderingTile!=null) {

                if (!drawUnexploredBorders && !borderingTile.isExplored() &&
                    (direction == Direction.SE || direction == Direction.S ||
                     direction == Direction.SW)) {
                    continue;
                }

                if (tile.getType() == borderingTile.getType()) {
                    // Equal tiles, no need to draw border
                    continue;
                }
                else if (tile.isLand() && !borderingTile.isLand()) {
                    // The beach borders are drawn on the side of water tiles only
                    continue;
                }
                else if (!tile.isLand() && borderingTile.isLand() && borderingTile.isExplored()) {
                    // If there is a Coast image (eg. beach) defined, use it, otherwise skip
                    if (borderingTile.getType().getArtCoast() != null) {
                        g.drawImage(lib.getCoastImage(borderingTile.getType(), direction,
                                                        tile.getX(), tile.getY()),
                                                        x, y, null);
                    }
                    // Draw the grass from the neighboring tile, spilling over on the side of this tile
                    g.drawImage(lib.getBorderImage(borderingTile.getType(), direction,
                                                    tile.getX(), tile.getY()),
                                                    x, y, null);
                } else if (tile.isExplored() && borderingTile.isExplored()) {
                    if (tile.getType().getArtBasic().equals(borderingTile.getType().getArtBasic())) {
                        // Do not draw limit between tile that share same graphics (ocean & great river)
                        continue;
                    }
                    else if (borderingTile.getType().getIndex() < tile.getType().getIndex()) {
                        // Draw land terrain with bordering land type, or ocean/high seas limit
                        g.drawImage(lib.getBorderImage(borderingTile.getType(), direction,
                                                        tile.getX(), tile.getY()),
                                                        x, y, null);
                    }
                }
            }
        }
    }    


    public void paintBorders(Graphics2D g, Tile tile, int x, int y) {
        if (tile == null) {
            return;
        }
        Player owner = tile.getOwner();
        if (displayBorders && owner != null) {
            Map map = tile.getMap();
            Tile otherTile = map.getNeighbourOrNull(Map.Direction.NE, tile);
            int dx = lib.getTerrainImageWidth(tile.getType());
            int dy = lib.getTerrainImageHeight(tile.getType());
            Color oldColor = g.getColor();
            g.setColor(new Color(owner.getColor().getRed(),
                                 owner.getColor().getGreen(),
                                 owner.getColor().getBlue()));
            GeneralPath line = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            if (otherTile.getOwner() != owner) {
                line.moveTo(x + dx/2, y + 2);
                line.lineTo(x + dx - 2, y + dy/2);
            }
            otherTile = map.getNeighbourOrNull(Map.Direction.SE, tile);
            if (otherTile.getOwner() != owner) {
                line.moveTo(x + dx - 2, y + dy/2);
                line.lineTo(x + dx/2, y + dy - 2);
            }
            otherTile = map.getNeighbourOrNull(Map.Direction.SW, tile);
            if (otherTile.getOwner() != owner) {
                line.moveTo(x + dx/2, y + dy - 2);
                line.lineTo(x + 2, y + dy/2);
            }
            otherTile = map.getNeighbourOrNull(Map.Direction.NW, tile);
            if (otherTile.getOwner() != owner) {
                line.moveTo(x + 2, y + dy/2);
                line.lineTo(x + dx/2, y + 2);
            }
            g.draw(line);
            g.setColor(oldColor);
        }
    }



    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     * @param withNumber indicates if the number of inhabitants should be drawn too.
     */
    private void displayTileOverlays(Graphics2D g, Map map, Tile tile, int x, int y,
                                     boolean drawUnexploredBorders, boolean withNumber) {
        if (tile != null) {
            if (drawUnexploredBorders) {
                displayUnexploredBorders(g, map, tile, x, y);
            }
            displayTileItems(g, map, tile, x, y);
            displaySettlement(g, map, tile, x, y, withNumber);
            displayFogOfWar(g, map, tile, x, y);
            displayOptionalValues(g, map, tile, x, y);
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Addtions and improvements to
     * Tile will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    private void displayTileItems(Graphics2D g, Map map, Tile tile, int x, int y) {  

        if (!tile.isExplored()) {
            g.drawImage(lib.getTerrainImage(null, tile.getX(), tile.getY()), x, y, null);
        } else {
            // layer additions and improvements according to zIndex
            List<TileItem> tileItems = new ArrayList<TileItem>();
            if (tile.getTileItemContainer() != null) {
                tileItems = tile.getTileItemContainer().getTileItems();
            }
            int startIndex = 0;
            for (int index = startIndex; index < tileItems.size(); index++) {
                if (tileItems.get(index).getZIndex() < OVERLAY_INDEX) {
                    drawItem(g, tile, tileItems.get(index), x, y);
                    startIndex = index + 1;
                } else {
                    startIndex = index;
                    break;
                }
            }
            // Tile Overlays (eg. hills and mountains)
            if (tile.getType().getArtOverlay() != null) {
                g.drawImage(lib.getOverlayImage(tile.getType(), tile.getX(), tile.getY()), x,
                            y - (int) (32 * lib.getScalingFactor()), null);
            }
            for (int index = startIndex; index < tileItems.size(); index++) {
                if (tileItems.get(index).getZIndex() < FOREST_INDEX) {
                    drawItem(g, tile, tileItems.get(index), x, y);
                    startIndex = index + 1;
                } else {
                    startIndex = index;
                    break;
                }
            }
            // Forest
            if (tile.isForested()) {
                g.drawImage(lib.getForestImage(tile.getType()), x, y, null);
            }

            // draw all remaining items
            for (int index = startIndex; index < tileItems.size(); index++) {
                drawItem(g, tile, tileItems.get(index), x, y);
            }
        }
    }

    private void drawItem(Graphics2D g, Tile tile, TileItem item, int x, int y) {

        if (item instanceof Resource) {
            Image bonusImage = lib.getBonusImage(((Resource) item).getType());
            if (bonusImage != null) {
                g.drawImage(bonusImage, x + tileWidth/2 - bonusImage.getWidth(null)/2,
                            y + tileHeight/2 - bonusImage.getHeight(null)/2, null);
            }
        } else if (item instanceof LostCityRumour) {
            g.drawImage(lib.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR),
                        x + (int) (RUMOUR_OFFSET_X * lib.getScalingFactor()),
                        y + (int) (RUMOUR_OFFSET_Y * lib.getScalingFactor()), null);
        } else if (item instanceof TileImprovement) {
            TileImprovement improvement = (TileImprovement) item;
            if (!improvement.isComplete()) {
                return;
            } else if (improvement.getType().getArtOverlay() != null) {
                // Has its own Overlay Image in Misc, use it
                g.drawImage(ResourceManager.getImage(improvement.getType().getArtOverlay()), x, y, null);
            } else if (improvement.isRiver() && improvement.getMagnitude() < TileImprovement.FJORD_RIVER) {
                g.drawImage(lib.getRiverImage(improvement.getStyle()), x, y, null);
            } else if (improvement.isRoad()) {
                long seed = Long.parseLong(Integer.toString(tile.getX()) + Integer.toString(tile.getY()));
                boolean connectedRoad = false;
                for (Direction direction : Direction.values()) {
                    Tile borderingTile = tile.getMap().getAdjacentTile(tile.getPosition(), direction);
                    if (borderingTile!=null) {
                        if (borderingTile.hasRoad()) {
                            connectedRoad =  true;
                            int nx = x + tileWidth/2;
                            int ny = y + tileHeight/2;

                            switch (direction) {
                            case N : nx = x + tileWidth/2; ny = y; break;
                            case NE: nx = x + (tileWidth*3)/4; ny = y + tileHeight/4; break;
                            case E : nx = x + tileWidth; ny = y + tileHeight/2; break;
                            case SE: nx = x + (tileWidth*3)/4; ny = y + (tileHeight*3)/4; break;
                            case S : nx = x + tileWidth/2; ny = y + tileHeight; break;
                            case SW: nx = x + tileWidth/4; ny = y + (tileHeight*3)/4; break;
                            case W : nx = x; ny = y + tileHeight/2; break;
                            case NW: nx = x + tileWidth/4; ny = y + tileHeight/4; break;
                            }

                            drawRoad(g, seed, x + tileWidth/2, y + tileHeight/2, nx, ny);
                        }
                    }
                }

                if (!connectedRoad) {
                    drawRoad(g, seed, x + tileWidth/2 - 10, y + tileHeight/2,
                             x + tileWidth/2 + 10, y + tileHeight/2);
                    drawRoad(g, seed, x + tileWidth/2, y + tileHeight/2 - 10,
                             x + tileWidth/2, y + tileHeight/2 + 10);
                }
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Settlements and Lost City
     * Rumours will be shown.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    private void displaySettlement(Graphics2D g, Map map, Tile tile, int x, int y, boolean withNumber) {  
        if (tile.isExplored()) {
            Settlement settlement = tile.getSettlement();

            if (settlement != null) {
                if (settlement instanceof Colony) {
                    Image colonyImage = lib.getSettlementImage(settlement);
                    // Draw image of colony in center of the tile.
                    g.drawImage(colonyImage,
                                x + (lib.getTerrainImageWidth(tile.getType()) -
                                     colonyImage.getWidth(null)) / 2,
                                y + (lib.getTerrainImageHeight(tile.getType()) -
                                     colonyImage.getHeight(null)) / 2, null);

                    if (withNumber) {
                        String populationString = Integer.toString(((Colony)settlement).getUnitCount());
                        Color theColor = null;

                        int bonus = ((Colony)settlement).getProductionBonus();
                        switch (bonus) {
                        case 2:
                            theColor = Color.BLUE;
                            break;
                        case 1:
                            theColor = Color.GREEN;
                            break;
                        case -1:
                            theColor = Color.ORANGE;
                            break;
                        case -2:
                            theColor = Color.RED;
                            break;
                        default:
                            theColor = Color.WHITE;
                        break;
                        }

                        BufferedImage stringImage = createStringImage(g, populationString, theColor, lib.getTerrainImageWidth(tile.getType()), 12);
                        g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, y + ((lib.getTerrainImageHeight(tile.getType()) - stringImage.getHeight()) / 2) + 1, null);
                    }
                    g.setColor(Color.BLACK);
                } else if (settlement instanceof IndianSettlement) {
                    IndianSettlement indianSettlement = (IndianSettlement) settlement;
                    Image settlementImage = lib.getSettlementImage(settlement);

                    // Draw image of indian settlement in center of the tile.
                    g.drawImage(settlementImage,
                                x + (lib.getTerrainImageWidth(tile.getType()) -
                                     settlementImage.getWidth(null)) / 2,
                                y + (lib.getTerrainImageHeight(tile.getType()) - 
                                     settlementImage.getHeight(null)) / 2, null);

                    // Draw the color chip for the settlement.
                    g.drawImage(lib.getColorChip(indianSettlement.getOwner().getColor()),
                                x + (int) (STATE_OFFSET_X * lib.getScalingFactor()),
                                y + (int) (STATE_OFFSET_Y * lib.getScalingFactor()), null);

                    // Draw the mission chip if needed.
                    Unit missionary = indianSettlement.getMissionary();
                    if (missionary != null) {
                        boolean expert = missionary.hasAbility("model.ability.expertMissionary");
                        g.drawImage(lib.getMissionChip(missionary.getOwner().getColor(), expert),
                                    x + (int) (STATE_OFFSET_X * lib.getScalingFactor()) +
                                    (MISSION_OFFSET_X - STATE_OFFSET_X),
                                    y + (int) (MISSION_OFFSET_Y * lib.getScalingFactor()), null);
                    }

                    // Draw the alarm chip if needed.
                    if (freeColClient.getMyPlayer() != null) {
                        Tension alarm = indianSettlement.getAlarm(freeColClient.getMyPlayer());
                        if (alarm != null) {
                            // TODO: make it work
                            final boolean visited = indianSettlement.hasBeenVisited(freeColClient.getMyPlayer());
                            g.drawImage(lib.getAlarmChip(alarm.getLevel(), visited),
                                        x + (int) (STATE_OFFSET_X * lib.getScalingFactor()) +
                                        (ALARM_OFFSET_X - STATE_OFFSET_X),
                                        y + (int) (ALARM_OFFSET_Y  * lib.getScalingFactor()), null);
                        }
                    }

                    g.setColor(Color.BLACK);
                    if (indianSettlement.isCapital()) {
                        // TODO: make this look nicer
                        g.drawString("*",
                                     x + (STATE_OFFSET_X * lib.getScalingFactor()) + TEXT_OFFSET_X + 1,
                                     y + (int) (STATE_OFFSET_Y * lib.getScalingFactor()) + TEXT_OFFSET_Y + 2);
                    } else {
                        g.drawString("-", 
                                     x + (int) (STATE_OFFSET_X * lib.getScalingFactor()) + TEXT_OFFSET_X,
                                     y + (int) (STATE_OFFSET_Y * lib.getScalingFactor()) + TEXT_OFFSET_Y);
                    }
                } else {
                    logger.warning("Requested to draw unknown settlement type.");
                }
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Fog of war will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    private void displayFogOfWar(Graphics2D g, Map map, Tile tile, int x, int y) {  
        if (tile.isExplored()) {
            final boolean displayFogOfWar = freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_FOG_OF_WAR);
            if (displayFogOfWar
                    && freeColClient.getMyPlayer() != null
                    && !freeColClient.getMyPlayer().canSee(tile)) {
                g.setColor(Color.BLACK);
                Composite oldComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                Polygon pol = new Polygon(new int[] {x + tileWidth/2, x + tileWidth, x + tileWidth/2, x},
                                          new int[] {y, y + tileHeight/2, y + tileHeight, y + tileHeight/2},
                                          4);
                g.fill(pol);
                g.setComposite(oldComposite);
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Borders next to unexplored
     * tiles will be drawn differently.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    private void displayUnexploredBorders(Graphics2D g, Map map, Tile tile, int x, int y) {  
        if (tile.isExplored()) {
            Map.Position pos = new Map.Position(tile.getX(), tile.getY());

            for (Direction direction : Direction.values()) {
                Tile borderingTile = map.getAdjacentTile(pos, direction);
                if (borderingTile!=null) {

                    if (borderingTile.isExplored()){
                        continue;
                    }

                    g.drawImage(lib.getBorderImage(null, direction, tile.getX(), tile.getY()), x, y, null);
                }
            }
        }
    }


    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Show tile names, coordinates
     * and colony values.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    private void displayOptionalValues(Graphics2D g, Map map, Tile tile, int x, int y) {
        switch (displayTileText) {
        case ClientOptions.DISPLAY_TILE_TEXT_EMPTY:
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_NAMES:
            if (tile.getName() != null) {
                String tileName = tile.getName();
                g.setColor(Color.BLACK);
                int b = getBreakingPoint(tileName);
                if (b == -1) {
                    g.drawString(tileName, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName))/2, y + (lib.getTerrainImageHeight(tile.getType())/2));
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileName, Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - stringImage.getHeight()/2, null);
                    */
                } else {
                    g.drawString(tileName.substring(0, b), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName.substring(0, b)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 - (g.getFontMetrics().getAscent()*2)/3);
                    g.drawString(tileName.substring(b+1), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName.substring(b+1)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 + (g.getFontMetrics().getAscent()*2)/3);
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileName.substring(0, b), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - (stringImage.getHeight()) - 5, null);
                       stringImage = createStringImage(g, tileName.substring(b+1), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - 5, null);
                    */
                }
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_OWNERS:
            if (tile.getOwner() != null) {
                String tileOwner = tile.getOwner().getNationAsString();
                g.setColor(Color.BLACK);
                int b = getBreakingPoint(tileOwner);
                if (b == -1) {
                    g.drawString(tileOwner, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileOwner))/2, y + (lib.getTerrainImageHeight(tile.getType())/2));
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileOwner, Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - stringImage.getHeight()/2, null);
                    */
                } else {
                    g.drawString(tileOwner.substring(0, b), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileOwner.substring(0, b)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 - (g.getFontMetrics().getAscent()*2)/3);
                    g.drawString(tileOwner.substring(b+1), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileOwner.substring(b+1)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 + (g.getFontMetrics().getAscent()*2)/3);
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileOwner.substring(0, b), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - (stringImage.getHeight()) - 5, null);
                       stringImage = createStringImage(g, tileOwner.substring(b+1), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - 5, null);
                    */
                }
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_REGIONS:
            if (tile.getRegion() != null) {
                String regionString = tile.getRegion().getDisplayName();
                g.setColor(Color.BLACK);
                int b = getBreakingPoint(regionString);
                if (b == -1) {
                    g.drawString(regionString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(regionString))/2, y + (lib.getTerrainImageHeight(tile.getType())/2));
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileOwner, Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - stringImage.getHeight()/2, null);
                    */
                } else {
                    g.drawString(regionString.substring(0, b), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(regionString.substring(0, b)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 - (g.getFontMetrics().getAscent()*2)/3);
                    g.drawString(regionString.substring(b+1), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(regionString.substring(b+1)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 + (g.getFontMetrics().getAscent()*2)/3);
                    /* Takes to much resources:
                       BufferedImage stringImage = createStringImage(g, tileOwner.substring(0, b), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - (stringImage.getHeight()) - 5, null);
                       stringImage = createStringImage(g, tileOwner.substring(b+1), Color.BLACK, lib.getTerrainImageWidth(tile.getType().getIndex()), 10);
                       g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType().getIndex()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType().getIndex())/2 - 5, null);
                    */
                }
            }
            break;
        default:
            logger.warning("displayTileText out of range");
            break;
        }
        

        /*
        if (tile.getPosition().equals(selectedTile)) {
            g.drawImage(lib.getMiscImage(ImageLibrary.UNIT_SELECT), x, y, null);
        }*/

        g.setColor(Color.BLACK);

        if (displayCoordinates) {
            String posString = tile.getX() + ", " + tile.getY();
            g.drawString(posString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(posString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - g.getFontMetrics().getAscent())/2);
        }
        if (displayColonyValue && tile.isExplored() && tile.isLand()) {
            String valueString;
            if (displayColonyValuePlayer == null) {
                valueString = Integer.toString(tile.getColonyValue());
            } else {
                valueString = Integer.toString(displayColonyValuePlayer.getColonyValue(tile));
            }
            g.drawString(valueString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(valueString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - g.getFontMetrics().getAscent())/2);
        }
    }


    /**
    * Stops any ongoing goto operation on the mapboard.
    */
    public void stopGoto() {
        freeColClient.getCanvas().setCursor(null);
        setGotoPath(null);
        updateGotoPathForActiveUnit();
        gotoStarted = false;
    }


    /**
    * Starts a goto operation on the mapboard.
    */
    public void startGoto() {
        gotoStarted = true;
        freeColClient.getCanvas().setCursor((java.awt.Cursor) UIManager.get("cursor.go"));
        setGotoPath(null);
    }


    /**
     * Checks if there is currently a goto operation on the mapboard.
     * @return <code>true</code> if a goto operation is in progress.
     */
    public boolean isGotoStarted() {
        return gotoStarted;
    }


    /**
    * Sets the path of the active unit to display it.
    */
    public void updateGotoPathForActiveUnit() {
        if (activeUnit == null || activeUnit.getDestination() == null) {
            currentPath = null;
        } else {
            if (activeUnit.getDestination() instanceof Europe) {
                currentPath = freeColClient.getGame().getMap().findPathToEurope(activeUnit, activeUnit.getTile());
            } else if (activeUnit.getDestination().getTile() == activeUnit.getTile()) {
                // No need to do anything as the unit has arrived, there is no path to be shown.
                currentPath = null;
            } else {
                currentPath = activeUnit.findPath(activeUnit.getDestination().getTile());
            }
        }
    }

    /**
    * Sets the path to be drawn on the map.
    * @param gotoPath The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public void setGotoPath(PathNode gotoPath) {
        this.gotoPath = gotoPath;

        freeColClient.getCanvas().refresh();
    }


    /**
    * Gets the path to be drawn on the map.
    * @return The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public PathNode getGotoPath() {
        return gotoPath;
    }


    /**
    * If set to <i>true</i> then a grid is drawn on the map.
    * @param displayGrid <code>true</code> if the grid should be drawn
    *       on the map and <code>false</code> otherwise.
    */
    public void setDisplayGrid(boolean displayGrid) {
        this.displayGrid = displayGrid;
    }


    /**
     * If set to <i>true</i> then a borders is drawn on the map.
     * @param displayBorders <code>true</code> if the borders should be drawn
     *       on the map and <code>false</code> otherwise.
     */
    public void setDisplayBorders(boolean displayBorders) {
        this.displayBorders = displayBorders;
    }


    /**
     * Set the type of text drawn in the tiles.
     *
     * @param tileTextType One of the <code>ClientOptions.DISPLAY_TILE_TEXT</code> values
     */
    public void setDisplayTileText(int tileTextType) {
        this.displayTileText = tileTextType;
    }


    /**
    * Breaks a line between two words. The breaking point
    * is as close to the center as possible.
    *
    * @param string The line for which we should determine a
    *               breaking point.
    * @return The best breaking point or <code>-1</code> if there
    *         are none.
    */
    public int getBreakingPoint(String string) {
        int center = string.length() / 2;
        int bestIndex = string.indexOf(' ');

        int index = 0;
        while (index != -1 && index != bestIndex) {
            if (Math.abs(center-index) < Math.abs(center-bestIndex)) {
                bestIndex = index;
            }

            index = string.indexOf(' ', bestIndex);
        }

        if (bestIndex == 0 || bestIndex == string.length()) {
            return -1;
        } else {
            return bestIndex;
        }
    }
    
    /**
     * For performance reason, indicator images are rendered once and cached for reuse.
     * The class IndicatorImageKey provide an identifier for looking up images
     */
    private class IndicatorImageKey {
        public Color bgColor;
        public Color fgColor;
        public String name;
        public IndicatorImageKey(Color bg, Color fg, String s) {
            this.bgColor = bg;
            this.fgColor = fg;
            this.name = s;
        }
        public int hashCode() {
            return name.hashCode();
        }
        public boolean equals(Object o) {
            if (o==null || !(o instanceof IndicatorImageKey))
                return false;
            IndicatorImageKey other = (IndicatorImageKey) o;
            return (other.bgColor.equals(this.bgColor)) && 
                   (other.fgColor.equals(this.fgColor)) && 
                   (other.name.equals(this.name));
        }
    }
    private HashMap<IndicatorImageKey, BufferedImage> indicatorImageCache = new HashMap<IndicatorImageKey, BufferedImage>();

    private Image getOccupationIndicatorImage(Unit unit) {
        Color backgroundColor = unit.getOwner().getColor();
        Color foregroundColor = getForegroundColor(unit.getOwner().getColor());
        String occupationString;
        if (unit.getOwner() != freeColClient.getMyPlayer()
                && unit.isNaval()) {
            occupationString = Integer.toString(unit.getVisibleGoodsCount());
        } else {
            occupationString = unit.getOccupationIndicator();
            if (unit.getState() == UnitState.FORTIFIED)
                foregroundColor = Color.GRAY;
        }
        // Lookup in the cache if the image has been generated already
        IndicatorImageKey key = new IndicatorImageKey(backgroundColor, foregroundColor, occupationString);
        BufferedImage img = indicatorImageCache.get(key);
        if (img!=null)
            return img;
        // Draw it and put it in the cache
        Image chip = lib.getColorChip(backgroundColor);
        img = new BufferedImage(chip.getWidth(null), chip.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.drawImage(chip, 0, 0, null);
        g.setColor(foregroundColor);
        g.drawString(occupationString, TEXT_OFFSET_X, TEXT_OFFSET_Y);
        indicatorImageCache.put(key, img);
        return img;
    }


    private Color getForegroundColor(Color background) {
        /*
         * Our eyes have different sensitivity towards
         * red, green and blue. We want a foreground
         * color with the inverse brightness.
         */
        if (background.getRed() * 0.3
                + background.getGreen() * 0.59
                + background.getBlue() * 0.11 < 126) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
    
    private Color getStringBorderColor(Color color) {
        /*
         * I think string border colors should be black
         * unless the color of the string is
         * really dark.
         */
        if (color.getRed() * 0.3
                + color.getGreen() * 0.59
                + color.getBlue() * 0.11 < 10) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
    
    public void displayOccupationIndicator(Graphics g, Unit unit, int x, int y) {
        g.drawImage(getOccupationIndicatorImage(unit), x, y, null);
    }

    /**
     * Displays the given Unit onto the given Graphics2D object at the
     * location specified by the coordinates.
     * @param g The Graphics2D object on which to draw the Unit.
     * @param unit The Unit to draw.
     * @param x The x-coordinate of the location where to draw the Unit
     * (in pixels). These are the coordinates of the Tile on which
     * the Unit is located.
     * @param y The y-coordinate of the location where to draw the Unit
     * (in pixels). These are the coordinates of the Tile on which
     * the Unit is located.
     */
    private void displayUnit(Graphics2D g, Unit unit, int x, int y) {
        try {
            // Draw the 'selected unit' image if needed.
            //if ((unit == getActiveUnit()) && cursor) {
            if (viewMode.displayUnitCursor(unit,x,y)) {
                drawCursor(g,x,y);
            }

            // Draw the unit.
            // If unit is sentry, draw in grayscale
            Image image = lib.getUnitImageIcon(unit, unit.getState() == UnitState.SENTRY).getImage();
            Point p = getUnitImagePositionInTile(image, x, y);
            g.drawImage(image, p.x, p.y, null);

            // Draw an occupation and nation indicator.
            displayOccupationIndicator(g, unit, x + (int) (STATE_OFFSET_X * lib.getScalingFactor()), y);

            // Draw one small line for each additional unit (like in civ3).
            int unitsOnTile = 0;
            if (unit.getTile() != null) {
                // When a unit is moving from tile to tile, it is removed from the source tile
                // So the unit stack indicator cannot be drawn during the movement
                // see UnitMoveAnimation.animate() for details
                unitsOnTile = unit.getTile().getTotalUnitCount();
            }
            if (unitsOnTile > 1) {
                g.setColor(Color.WHITE);
                int unitLinesY = y + OTHER_UNITS_OFFSET_Y;
                for (int i = 0; (i < unitsOnTile) && (i < MAX_OTHER_UNITS); i++) {
                    g.drawLine(x + (int) ((STATE_OFFSET_X + OTHER_UNITS_OFFSET_X) * lib.getScalingFactor()), unitLinesY, x + (int) ((STATE_OFFSET_X + OTHER_UNITS_OFFSET_X + OTHER_UNITS_WIDTH) * lib.getScalingFactor()), unitLinesY);
                    unitLinesY += 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // FOR DEBUGGING:
        if (debugShowMission && freeColClient.getFreeColServer() != null) {
            net.sf.freecol.server.ai.AIUnit au = (net.sf.freecol.server.ai.AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(unit);
            if (au != null) {
                g.setColor(Color.WHITE);
                String text = (unit.getOwner().isAI()) ? "" : "(";
                String debuggingInfo = "";
                if (au.getMission() != null) {
                    String missionName = au.getMission().getClass().toString();
                    missionName = missionName.substring(missionName.lastIndexOf('.') + 1);
                    text += missionName;
                    if (debugShowMissionInfo) {
                        debuggingInfo = au.getMission().getDebuggingInfo();
                    }
                } else {
                    text += "No mission";
                }                
                text += (unit.getOwner().isAI()) ? "" : ")";
                g.drawString(text, x , y);
                g.drawString(debuggingInfo, x , y+25);
            }
        }
    }
    
    /**
     * Gets the coordinates to draw a unit in a given tile.
     * @param unitImage The unit's image
     * @param tileX The X coordinate of the tile
     * @param tileY The Y coordinate of the tile
     * @return The coordinates where the unit should be drawn onscreen
     */
    private Point getUnitImagePositionInTile(Image unitImage, int tileX, int tileY) {
        return getUnitImagePositionInTile(unitImage.getWidth(null), unitImage.getHeight(null), tileX, tileY);
    }
    
    /**
     * Gets the coordinates to draw a unit in a given tile.
     * @param unitImageWidth The unit image's width
     * @param unitImageHeight The unit image's height
     * @param tileX The X coordinate of the tile
     * @param tileY The Y coordinate of the tile
     * @return The coordinates where the unit should be drawn onscreen
     */
    private Point getUnitImagePositionInTile(int unitImageWidth, int unitImageHeight, int tileX, int tileY) {
        int unitX = ((tileX + getTileWidth() / 2) - unitImageWidth / 2);
        int unitY = (tileY + getTileHeight() / 2) - unitImageHeight / 2 -
                    (int) (UNIT_OFFSET * lib.getScalingFactor());
        
        return new Point(unitX, unitY);
    }
    
    /**
     * Gets the position where a unitLabel located at tile should be drawn. 
     * @param unitLabel The unit label with the unit's image and occupation indicator drawn.
     * @param tile The tile where the unitLabel will be drawn over
     * @return The position where to put the label, null if the Tile is offscreen.
     */
    public Point getUnitLabelPositionInTile(JLabel unitLabel, Tile tile) {
        return getUnitLabelPositionInTile(unitLabel, getTilePosition(tile));
    }
    
    /**
     * Gets the position where a unitLabel located at tile should be drawn. 
     * @param unitLabel The unit label with the unit's image and occupation indicator drawn.
     * @param tileP The position of the Tile on the screen.
     * @return The position where to put the label, null if tileP is null.
     */
    public Point getUnitLabelPositionInTile(JLabel unitLabel, Point tileP) {
        if (tileP != null) {
            int labelX = tileP.x + getTileWidth() / 2 - unitLabel.getWidth() / 2;
            int labelY = tileP.y + getTileHeight() / 2 - unitLabel.getHeight() / 2 -
                        (int) (UNIT_OFFSET * lib.getScalingFactor());
            
            return new Point(labelX, labelY);
        } else {
            return null;
        }
    }
    
    /**
     * Draw the unit's image and occupation indicator in one JLabel object.
     * @param unit The unit to be drawn
     * @return A JLabel object with the unit's image.
     */
    private JLabel getUnitLabel(Unit unit) {
        final Image unitImg = lib.getUnitImageIcon(unit).getImage();
        //final Image chipImg = getOccupationIndicatorImage(unit);
        
        final int width = tileWidth/2 + unitImg.getWidth(null)/2;
        final int height = unitImg.getHeight(null);
        
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        
        final int unitX = (width - unitImg.getWidth(null)) / 2;
        g.drawImage(unitImg, unitX, 0, null);
        
        //final int chipX = ((int) (STATE_OFFSET_X * lib.getScalingFactor()));
        //final int chipY = (int) (((height / 2 + UNIT_OFFSET*lib.getScalingFactor())) - tileHeight / 2);
        //g.drawImage(chipImg, chipX, chipY, null);
        
        final JLabel label = new JLabel(new ImageIcon(img));
        label.setSize(width, height);
        if (unit.getTile() != null && getTilePosition(unit.getTile()) != null) {
            label.setLocation(getUnitLabelPositionInTile(label, getTilePosition(unit.getTile())));
        } else {
            label.setVisible(false);
        }
        return label;
    }
    
    private void drawCursor(Graphics2D g, int x, int y) {
        g.drawImage(cursorImage, x, y, null);
    }


    /**
    * Notifies this GUI that the game has started or ended.
    * @param inGame Indicates whether or not the game has started.
    */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }


    /**
    * Checks if the game has started.
    * @return <i>true</i> if the game has started.
    * @see #setInGame
    */
    public boolean isInGame() {
        return inGame;
    }


    /**
     * Checks if the Tile/Units at the given coordinates are displayed
     * on the screen (or, if the map is already displayed and the focus
     * has been changed, whether they will be displayed on the screen
     * the next time it'll be redrawn).
     * @param x The x-coordinate of the Tile in question.
     * @param y The y-coordinate of the Tile in question.
     * @return 'true' if the Tile will be drawn on the screen, 'false'
     * otherwise.
     */
    public boolean onScreen(int x, int y) {
        if (bottomRow < 0) {
            positionMap();
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 2 < rightColumn;
        } else {
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 2 < rightColumn;
        }
    }


    /**
     * Checks if the Tile/Units at the given coordinates are displayed
     * on the screen (or, if the map is already displayed and the focus
     * has been changed, whether they will be displayed on the screen
     * the next time it'll be redrawn).
     *
     * @param position The position of the Tile in question.
     * @return <i>true</i> if the Tile will be drawn on the screen, <i>false</i>
     * otherwise.
     */
    public boolean onScreen(Position position) {
        return onScreen(position.getX(), position.getY());
    }


    /**
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The map coordinates of the Tile that is located at
     * the given position on the screen.
     */
    public Map.Position convertToMapCoordinates(int x, int y) {
        Game gameData = freeColClient.getGame();
        if ((gameData == null) || (gameData.getMap() == null)) {
            return null;
        }

        int leftOffset;
        if (focus.getX() < getLeftColumns()) {
            // we are at the left side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = tileWidth * focus.getX() + tileWidth / 2;
            } else {
                leftOffset = tileWidth * (focus.getX() + 1);
            }
        } else if (focus.getX() >= (gameData.getMap().getWidth() - getRightColumns())) {
            // we are at the right side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = size.width - (gameData.getMap().getWidth() - focus.getX()) * tileWidth;
            } else {
                leftOffset = size.width - (gameData.getMap().getWidth() - focus.getX() - 1) * tileWidth - tileWidth / 2;
            }
        } else {
            if ((focus.getY() % 2) == 0) {
                leftOffset = (size.width / 2);
            } else {
                leftOffset = (size.width / 2) + tileWidth / 2;
            }
        }

        int topOffset;
        if (focus.getY() < topRows) {
            // we are at the top of the map
            topOffset = (focus.getY() + 1) * (tileHeight / 2);
        } else if (focus.getY() >= (gameData.getMap().getHeight() - bottomRows)) {
            // we are at the bottom of the map
            topOffset = size.height - (gameData.getMap().getHeight() - focus.getY()) * (tileHeight / 2);
        } else {
            topOffset = (size.height / 2);
        }

        // At this point (leftOffset, topOffset) is the center pixel of the Tile
        // that was on focus (= the Tile that should have been drawn at the center
        // of the screen if possible).

        // The difference in rows/columns between the selected
        // tile (x, y) and the current center Tile.
        // These values are positive if (x, y) is located NW
        // of the current center Tile.
        int diffUp = (topOffset - y) / (tileHeight / 4),
            diffLeft = (leftOffset - x) / (tileWidth / 4);

        // The following values are used when the user clicked somewhere
        // near the crosspoint of 4 Tiles.
        int orDiffUp = diffUp,
            orDiffLeft = diffLeft,
            remainderUp = (topOffset - y) % (tileHeight / 4),
            remainderLeft = (leftOffset - x) % (tileWidth / 4);

        if ((diffUp % 2) == 0) {
            diffUp = diffUp / 2;
        } else {
            if (diffUp < 0) {
                diffUp = (diffUp / 2) - 1;
            } else {
                diffUp = (diffUp / 2) + 1;
            }
        }

        if ((diffLeft % 2) == 0) {
            diffLeft = diffLeft / 2;
        } else {
            if (diffLeft < 0) {
                diffLeft = (diffLeft / 2) - 1;
            } else {
                diffLeft = (diffLeft / 2) + 1;
            }
        }

        boolean done = false;
        while (!done) {
            if ((diffUp % 2) == 0) {
                if ((diffLeft % 2) == 0) {
                    diffLeft = diffLeft / 2;
                    done = true;
                } else {
                    // Crosspoint
                    if (((orDiffLeft % 2) == 0) && ((orDiffUp % 2) == 0)) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Left
                            if ((remainderUp * 2) > remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Right
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if ((orDiffLeft > 0) && (orDiffUp == 0)) {
                            if (remainderUp > 0) {
                                // Upper-Left
                                if ((remainderUp * 2) > remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Left
                                if ((-remainderUp * 2) > remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            }
                        } else if (orDiffUp == 0) {
                            if (remainderUp > 0) {
                                // Upper-Right
                                if ((remainderUp * 2) > -remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2) > -remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Left
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        } else {
                            // Lower-Right
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        }
                    } else if ((orDiffLeft % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Left
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Right
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Left
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Right
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        }
                    } else if ((orDiffUp % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Right
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Left
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if ((orDiffLeft > 0) && (orDiffUp == 0)) {
                            if (remainderUp > 0) {
                                // Upper-Right
                                if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffUp == 0) {
                            if (remainderUp > 0) {
                                // Upper-Left
                                if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Left
                                if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Right
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Left
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        }
                    } else {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Right
                            if ((remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Left
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Right
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Left
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        }
                    }
                }
            } else {
                if ((diffLeft % 2) == 0) {
                    // Crosspoint
                    if (((orDiffLeft % 2) == 0) && ((orDiffUp % 2) == 0)) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Left
                            if ((remainderUp * 2) > remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Left
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        } else if ((orDiffUp > 0) && (orDiffLeft == 0)) {
                            if (remainderLeft > 0) {
                                // Upper-Left
                                if ((remainderUp * 2) > remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Upper-Right
                                if ((remainderUp * 2) > -remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffLeft == 0) {
                            if (remainderLeft > 0) {
                                // Lower-Left
                                if ((-remainderUp * 2) > remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2) > -remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Right
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Right
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        }
                    } else if ((orDiffLeft % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Left
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Left
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        } else if ((orDiffUp > 0) && (orDiffLeft == 0)) {
                            if (remainderLeft > 0) {
                                // Lower-Left
                                if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffLeft++;
                                } else {
                                    diffUp--;
                                }
                            } else {
                                // Lower-Right
                                if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffLeft--;
                                } else {
                                    diffUp--;
                                }
                            }
                        } else if (orDiffLeft == 0) {
                            if (remainderLeft > 0) {
                                // Upper-Left
                                if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffLeft++;
                                } else {
                                    diffUp++;
                                }
                            } else {
                                // Upper-Right
                                if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffLeft--;
                                } else {
                                    diffUp++;
                                }
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Right
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else {
                            // Upper-Right
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        }
                    } else if ((orDiffUp % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Right
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Left
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Right
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Left
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        }
                    } else {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Right
                            if ((remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Left
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Right
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Left
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        }
                    }
                } else {
                    if ((focus.getY() % 2) == 0) {
                        if (diffLeft < 0) {
                            diffLeft = diffLeft / 2;
                        } else {
                            diffLeft = (diffLeft / 2) + 1;
                        }
                    } else {
                        if (diffLeft < 0) {
                            diffLeft = (diffLeft / 2) - 1;
                        } else {
                            diffLeft = diffLeft / 2;
                        }
                    }
                    done = true;
                }
            }
        }
        return new Map.Position(focus.getX() - diffLeft, focus.getY() - diffUp);
    }


    /**
     * Returns 'true' if the Tile is near the top.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the top.
     */
    public boolean isMapNearTop(int y) {
        if (y < topRows) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the bottom.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the bottom.
     */
    public boolean isMapNearBottom(int y) {
        if (y >= (freeColClient.getGame().getMap().getHeight() - bottomRows)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the left.
     * @param x The x-coordinate of a Tile.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the left.
     */
    public boolean isMapNearLeft(int x, int y) {
        if (x < getLeftColumns(y)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the right.
     * @param x The x-coordinate of a Tile.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the right.
     */
    public boolean isMapNearRight(int x, int y) {
        if (x >= (freeColClient.getGame().getMap().getWidth() - getRightColumns(y))) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns the ImageLibrary that this GUI uses to draw stuff.
     * @return The ImageLibrary that this GUI uses to draw stuff.
     */
    public ImageLibrary getImageLibrary() {
        return lib;
    }

    /**
     * Gets the position of the given <code>Tile</code>
     * on the drawn map.
     * 
     * @param t The <code>Tile</code>.
     * @return The position of the given <code>Tile</code>,
     *      or <code>null</code> if the <code>Tile</code> is
     *      not drawn on the mapboard.
     */
    public Point getTilePosition(Tile t) {
        if (t.getY() >= topRow 
                && t.getY() <= bottomRow 
                && t.getX() >= leftColumn 
                && t.getX() <= rightColumn) {
            int x = ((t.getX() - leftColumn) * tileWidth) + leftColumnX;
            int y = ((t.getY() - topRow) * tileHeight / 2) + topRowY;
            if ((t.getY() % 2) != 0) {     
                x += tileWidth / 2;
            }
            return new Point(x, y);
        } else {
            return null;
        }
    }
    
    /**
     * Calculate the bounds of the rectangle containing a Tile on the screen,
     * and return it. If the Tile is not on-screen a maximal rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile, to include the space
     * needed by any units in the Tile.
     * @param tile The tile on the screen.
     * @return The bounds rectangle
     */
    public Rectangle getTileBounds(Tile tile) {
        return getTileBounds(tile.getX(), tile.getY());
    }
    
    /**
     * Calculate the bounds of the rectangle containing a Tile on the screen,
     * and return it. If the Tile is not on-screen a maximal rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile, to include the space
     * needed by any units in the Tile.
     * @param x The x-coordinate of the Tile
     * @param y The y-coordinate of the Tile
     * @return The bounds rectangle
     */
    public Rectangle getTileBounds(int x, int y) {
        Rectangle result = new Rectangle(0, 0, size.width, size.height);
        if (y >= topRow && y <= bottomRow && x >= leftColumn && x <= rightColumn) {
            result.y = ((y - topRow) * tileHeight / 2) + topRowY - tileHeight;
            result.x = ((x - leftColumn) * tileWidth) + leftColumnX;
            if ((y % 2) != 0) {
                result.x += tileWidth / 2;
            }
            result.width = tileWidth;
            result.height = tileHeight * 2;
        }
        return result;
    }


    /**
     * Force the next screen repaint to reposition the tiles on the window.
     */
    public void forceReposition() {
        bottomRow = -1;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileWidth() {
        return tileWidth;
    }
}
