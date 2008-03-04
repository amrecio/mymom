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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.UnloadAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.TransportMission;


/**
 * Allows the user to obtain more info about a certain tile
 * or to activate a specific unit on the tile.
 */
public final class TilePopup extends JPopupMenu {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TilePopup.class.getName());

    private final Canvas canvas;
    private final GUI gui;

    private boolean hasAnItem = false;

    /**
     * The constructor that will insert the MenuItems.
     *
     * @param tile The <code>Tile</code> to create a popup for.
     *       The popup menu also appears near this <code>Tile</code>.
     * @param freeColClient The main controller object for the client.
     * @param canvas The component containing the map.
     * @param gui An object with methods used for making the popup.
     */
    public TilePopup(final Tile tile, final FreeColClient freeColClient, final Canvas canvas, GUI gui) {
        super(Messages.message("tile",
                               "%x%", String.valueOf(tile.getX()),
                               "%y%", String.valueOf(tile.getY())));

        this.canvas = canvas;
        this.gui = gui;

        if (tile == null) {
            return;
        }

        final Unit activeUnit = gui.getActiveUnit();
        if (activeUnit != null) {
            //final Image gotoImage = (Image) UIManager.get("cursor.go.image");
            //JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"), new ImageIcon(gotoImage));
            JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"));
            gotoMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        freeColClient.getInGameController().setDestination(activeUnit, tile);
                        if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                            freeColClient.getInGameController().moveToDestination(activeUnit);
                        }
                    }
                });
            add(gotoMenuItem);
            hasAnItem = true;
            addSeparator();
        }

        for (final Unit currentUnit : tile.getUnitList()) {

            addUnit(currentUnit, !currentUnit.isUnderRepair(), false);

            for (Unit unit : currentUnit.getUnitList()) {
                addUnit(unit, true, true);
            }

            boolean hasGoods = false;
            for (Goods goods: currentUnit.getGoodsList()) {
                addGoods(goods, false, true);
                hasGoods = true;
            }

            if (hasGoods) {
                JMenuItem dumpItem = new JMenuItem(Messages.message("dumpCargo"));
                dumpItem.setAction(new UnloadAction(freeColClient));
                /*
                dumpItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            canvas.showDumpCargoDialog(currentUnit);
                        }
                    });
                */
                add(dumpItem);
            }
        }

        if (tile.getUnitCount() > 0) {
            addSeparator();
        }

        Settlement settlement = tile.getSettlement();
        if (settlement != null) {
            if (settlement.getOwner() == freeColClient.getMyPlayer()) {
                addColony(((Colony) settlement));
            } else if (settlement instanceof IndianSettlement) {
                addIndianSettlement((IndianSettlement) settlement);
            }
            if (hasItem()) {
                addSeparator();
            }
        }

        addTile(tile);

        // START DEBUG
        if (FreeCol.isInDebugMode()
            && freeColClient.getFreeColServer() != null) {
            addSeparator();
            JMenu takeOwnership = new JMenu("Take ownership");
            takeOwnership.setOpaque(false);
            boolean notEmpty = false;
            for (final Unit currentUnit : tile.getUnitList()) {
                JMenuItem toMenuItem = new JMenuItem(currentUnit.toString());
                toMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            // TODO: is this really necessary?
                            Player mp = (Player) freeColClient.getFreeColServer().getGame()
                                .getFreeColGameObject(freeColClient.getMyPlayer().getId());
                            currentUnit.setOwner(mp);
                            for (Unit unit : currentUnit.getUnitList()) {
                                unit.setOwner(mp);
                            }
                        }
                    });
                takeOwnership.add(toMenuItem);
                notEmpty = true;
                if (currentUnit.isCarrier()) {
                    final AIUnit au = (AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(currentUnit);
                    if (au.getMission() != null && au.getMission() instanceof TransportMission) {
                        JMenuItem menuItem = new JMenuItem("Transport list for: " + currentUnit.toString() +
                                                           " (" + currentUnit.hashCode() + ")");
                        menuItem.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent event) {
                                    canvas.showInformationMessage(au.getMission().toString());
                                }
                            });
                    }
                }
            }
            if (tile.getSettlement() != null) {
                if (!notEmpty) {
                    takeOwnership.addSeparator();
                }
                JMenuItem toMenuItem = new JMenuItem(tile.getSettlement().toString());
                toMenuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            // TODO: is this really necessary?
                            Player mp = (Player) freeColClient.getFreeColServer().getGame()
                                .getFreeColGameObject(freeColClient.getMyPlayer().getId());
                            tile.getSettlement().setOwner(mp);
                        }
                    });
                takeOwnership.add(toMenuItem);
                notEmpty = true;
            }
            if (notEmpty) {
                add(takeOwnership);
                hasAnItem = true;
            }
        }
        // END DEBUG
    }

    /**
     * Adds a unit entry to this popup.
     * @param unit The unit that will be represented on the popup.
     * @param enabled The initial state for the menu item.
     * @param indent Should be <code>true</code> if the text should be
     *      indented on the menu.
     */
    private void addUnit(final Unit unit, boolean enabled, boolean indent) {
        String text;
        if(unit.getState() == UnitState.IMPROVING) {
            text = ((indent ? "    " : "") +
                    Messages.message("model.unit.nationUnit",
                                     "%nation%", unit.getOwner().getNationAsString(),
                                     "%unit%", unit.getName()) +
                    " ( " + unit.getOccupationIndicator() + ": " + unit.getWorkLeft() +  " turns )");

        } else {
            text = ((indent ? "    " : "") +
                    Messages.message("model.unit.nationUnit",
                                     "%nation%", unit.getOwner().getNationAsString(),
                                     "%unit%", unit.getName()) +
                    " ( " + unit.getOccupationIndicator() + " )");
        }
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    gui.setActiveUnit(unit);
                }
            });
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        if (!enabled) {
            menuItem.setEnabled(false);
        }
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a goods entry to this popup.
     * @param goods The goods that will be represented on the popup.
     * @param enabled The initial state for the menu item.
     * @param indent Should be <code>true</code> if the text should be
     *      indented on the menu.
     */
    private void addGoods(Goods goods, boolean enabled, boolean indent) {
        String text = (indent ? "    " : "") + goods.toString();
        JMenuItem menuItem = new JMenuItem(text);
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        if (!enabled) {
            menuItem.setEnabled(false);
        }
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a colony entry to this popup.
     * @param colony The colony that will be represented on the popup.
     */
    private void addColony(final Colony colony) {
        JMenuItem menuItem = new JMenuItem(colony.toString());
        menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            canvas.showColonyPanel(colony);
                        }
                    });

        add(menuItem);
        hasAnItem = true;
    }


    /**
     * Adds an indian settlement entry to this popup.
     * @param settlement The Indian settlement that will be represented on the popup.
     */
    private void addIndianSettlement(final IndianSettlement settlement) {
        JMenuItem menuItem = new JMenuItem(settlement.getLocationName());
        menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            canvas.showIndianSettlementPanel(settlement);
                        }
                    });
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a tile entry to this popup.
     * @param tile The tile that will be represented on the popup.
     */
    private void addTile(final Tile tile) {
        JMenuItem menuItem = new JMenuItem(tile.getName());
        menuItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            canvas.showTilePanel(tile);
                        }
                    });

        add(menuItem);
        /**
         * Don't set hasAnItem to true, we want the tile panel to open
         * automatically whenever there is no other item on the list.
         */
        // hasAnItem = true;
    }

    /**
     * Returns true if this popup has at least one menuitem so that we know that we can
     * show it to the user. Returns false if there are no menuitems.
     * @return true if this popup has at least one menuitem, false otherwise.
     */
    public boolean hasItem() {
        return hasAnItem;
    }

}
