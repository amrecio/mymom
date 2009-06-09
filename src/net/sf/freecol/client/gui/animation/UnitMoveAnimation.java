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

package net.sf.freecol.client.gui.animation;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.OutForAnimationCallback;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;

/**
 * Class for the animation of units movement.
 */
final class UnitMoveAnimation {
    private static final Logger logger = Logger.getLogger(UnitMoveAnimation.class.getName());

    /*
     * Display delay between one frame and another, in milliseconds.
     * 33ms == 30 fps
     */
    private static final int ANIMATION_DELAY = 33;

    private final Canvas canvas;
    private final Unit unit;
    private final Tile sourceTile;
    private final Tile destinationTile;
    
    /**
     * Constructor
     * @param canvas The <code>Canvas</code> where the animation will be drawn.
     * @param unit The <code>Unit</code> to be animated.
     * @param sourceTile The <code>Tile</code> the unit is moving from.
     * @param destinationTile The <code>Tile</code> the unit is moving to.
     */
    public UnitMoveAnimation(Canvas canvas, Unit unit, Tile sourceTile,
                             Tile destinationTile) {
        this.canvas = canvas;
        this.unit = unit;
        this.sourceTile = sourceTile;
        this.destinationTile = destinationTile;
    }
    
    /**
     * Do the animation.
     */
    public void animate() {
        FreeColClient client = canvas.getClient();
        final GUI gui = canvas.getGUI();
        final String key = (client.getMyPlayer() == unit.getOwner())
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        final int movementSpeed = client.getClientOptions().getInteger(key);
        final Point srcP = gui.getTilePosition(sourceTile);
        final Point dstP = gui.getTilePosition(destinationTile);
        
        if (srcP == null || dstP == null || movementSpeed <= 0) {
            return;
        }

        float scale = gui.getImageLibrary().getScalingFactor();
        final int movementRatio = (int) (Math.pow(2, movementSpeed + 1) * scale);
        final Rectangle r1 = gui.getTileBounds(sourceTile);
        final Rectangle r2 = gui.getTileBounds(destinationTile);
        final Rectangle bounds = r1.union(r2);
        
        gui.executeWithUnitOutForAnimation(unit, sourceTile,
                                           new OutForAnimationCallback() {
            public void executeWithUnitOutForAnimation(final JLabel unitLabel) {
                final Point srcPoint = gui.getUnitLabelPositionInTile(unitLabel,
                                                                      srcP);
                final Point dstPoint = gui.getUnitLabelPositionInTile(unitLabel,
                                                                      dstP);
                final double xratio = gui.getTileWidth() / gui.getTileHeight();
                final int signalX = (srcPoint.getX() == dstPoint.getX()) ? 0
                    : (srcPoint.getX() > dstPoint.getX()) ? -1 : 1;
                final int signalY = (srcPoint.getY() == dstPoint.getY()) ? 0
                    : (srcPoint.getY() > dstPoint.getY()) ? -1 : 1;

                // Painting the whole screen once to get rid of
                // disposed dialog-boxes.
                canvas.paintImmediately(canvas.getBounds());

                int dropFrames = 0;

                while (!srcPoint.equals(dstPoint)) {
                    long time = System.currentTimeMillis();
                    
                    srcPoint.x += signalX * xratio * movementRatio;
                    srcPoint.y += signalY * movementRatio;
                    if (signalX == -1 && srcPoint.x < dstPoint.x) {
                        srcPoint.x = dstPoint.x;
                    } else if (signalX == 1 && srcPoint.x > dstPoint.x) {
                        srcPoint.x = dstPoint.x;
                    }
                    if (signalY == -1 && srcPoint.y < dstPoint.y) {
                        srcPoint.y = dstPoint.y;
                    } else if (signalY == 1 && srcPoint.y > dstPoint.y) {
                        srcPoint.y = dstPoint.y;
                    }
                    if (dropFrames <= 0) {
                        unitLabel.setLocation(srcPoint);
                        canvas.paintImmediately(bounds);
                        
                        int timeTaken = (int)(System.currentTimeMillis() - time);
                        final int waitTime = ANIMATION_DELAY - timeTaken;

                        if (waitTime > 0) {
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException ex) {
                                //ignore
                            }
                            dropFrames = 0;
                        } else {
                            dropFrames = timeTaken / ANIMATION_DELAY - 1;
                        }
                    } else {
                        dropFrames--;
                    }
                }
            }
        });
    }
}
