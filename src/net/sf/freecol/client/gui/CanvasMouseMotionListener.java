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

package net.sf.freecol.client.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMouseMotionListener extends AbstractCanvasListener implements MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseMotionListener.class.getName());

    // Temporary variable for checking if we need to recalculate the path when
    // dragging units.
    private Tile lastTile;
    

    /**
     * Creates a new listener for mouse movement.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param mapViewer The <code>MapViewer</code> that holds
     *     information such as screen resolution.
     */
    public CanvasMouseMotionListener(FreeColClient freeColClient,
                                     MapViewer mapViewer) {
        super(freeColClient, mapViewer);
    }


    /**
     * Invoked when the mouse has been moved.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseMoved(MouseEvent e) {

    	performAutoScrollIfActive(e);

        if (mapViewer.isGotoStarted()) {
            if (mapViewer.getActiveUnit() == null) {
                mapViewer.stopGoto();
            }

            Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());

            if (tile != null) {
                if (lastTile != tile) {
                    Unit active = mapViewer.getActiveUnit();
                    lastTile = tile;
                    if (active != null && active.getTile() != tile) {
                        PathNode dragPath = active.findPath(tile);
                        mapViewer.setGotoPath(dragPath);
                    } else {
                        mapViewer.setGotoPath(null);
                    }
                }
            }
        }
    }

    /**
     * Invoked when the mouse has been dragged.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseDragged(MouseEvent e) {

        performDragScrollIfActive(e);

        Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
        if (tile != null &&
            (e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
            // only perform the goto for the left mouse button
            if (mapViewer.isGotoStarted()) {
                Unit active = mapViewer.getActiveUnit();
                if (active == null) {
                    mapViewer.stopGoto();
                } else {
                    if (lastTile != tile) {
                        lastTile = tile;
                        if (active.getTile() != tile) {
                            PathNode dragPath = active.findPath(tile);
                            mapViewer.setGotoPath(dragPath);
                        } else {
                            mapViewer.setGotoPath(null);
                        }
                    }
                }
            } else {
                mapViewer.startGoto();
            }
        }
    }
}
