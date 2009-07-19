/**
 *  Copyright (C) 2002-2009  The FreeCol Team
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


package net.sf.freecol.common.model.pathfinding;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;

/**
 * Class for determining the cost of a single move.
 * 
 * <br /><br />
 * 
 * This {@link CostDecider} is used as a default by
 * {@link Map#findPath(Unit, Tile, Tile) findPath} and 
 * {@link Map#search(Unit, Tile, GoalDecider, CostDecider, int, Unit) search} 
 * if no other <code>CostDecider</code> has been specified.
 */
class BaseCostDecider implements CostDecider {

    private int movesLeft;
    private boolean newTurn;
    protected MoveType moveType;
    
    /**
     * Determines the cost of a single move.
     * 
     * @param unit The <code>Unit</code> making the move.
     * @param oldTile The <code>Tile</code> we are moving from.
     * @param newTile The <code>Tile</code> we are moving to.
     * @param movesLeftBefore The moves left before making the move.
     * @return The cost of moving the given unit from the
     *      <code>oldTile</code> to the <code>newTile</code>.
     */    
    public int getCost(final Unit unit,
            final Tile oldTile,
            final Tile newTile,
            int movesLeftBefore,
            final int turns) {
        newTurn = false;
              
        if (!newTile.isExplored()) {
            // Not allowed to use an unexplored tile for a path:
            return ILLEGAL_MOVE;
        }

        if (newTile.isLand() && unit.isNaval()
                && (newTile.getSettlement() == null
                    || (newTile.getSettlement().getOwner() != unit.getOwner()))) {
            /*
             * It should really not be allowed to move a naval unit on
             * land, but the movetype returned below is not ILLEGAL_MOVE
             * for this case since naval units should be able to move from
             * a disbanded settlement:
             */
            return ILLEGAL_MOVE;
        }
        if (newTile.getSettlement() != null
                && newTile.getSettlement().getOwner() != unit.getOwner()) {
            return ILLEGAL_MOVE;
        }
        
        int moveCost = unit.getMoveCost(oldTile, newTile, movesLeftBefore);
        if (moveCost <= movesLeftBefore) {
            movesLeft = movesLeftBefore - moveCost;
        } else {
            // This move takes an extra turn to complete:
            movesLeftBefore = unit.getInitialMovesLeft();
            //final int mc = getCost(unit, oldTile, newTile, movesLeftBefore, turns+1);
            final int mc = unit.getMoveCost(oldTile, newTile, movesLeftBefore);
            moveCost = movesLeft + mc;
            movesLeft = movesLeftBefore - mc;
            newTurn = true;
        }
        
        moveType = unit.getMoveType(oldTile, newTile, movesLeftBefore, true);
        if (!moveType.isLegal()) {
            return ILLEGAL_MOVE;
        }
        
        return moveCost;
    }
    
    /**
     * Gets the number of moves left. This method should be
     * called after invoking {@link #getCost}.
     * 
     * @return The number of moves left.
     */
    public int getMovesLeft() {
        return movesLeft;
    }
    
    /**
     * Checks if a new turn is needed in order to make the
     * move. This method should be called after invoking 
     * {@link #getCost}.
     * 
     * @return <code>true</code> if the move requires a
     *      new turn and <code>false</code> otherwise.
     */      
    public boolean isNewTurn() {
        return newTurn;
    }
}