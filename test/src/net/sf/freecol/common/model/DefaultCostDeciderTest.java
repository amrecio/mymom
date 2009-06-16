/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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
package net.sf.freecol.common.model;

import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

/**
 * Tests for the {@link DefaultCostDecider} class.
 */
public class DefaultCostDeciderTest extends FreeColTestCase {
    private UnitType pioneerType = spec().getUnitType("model.unit.hardyPioneer");
    private UnitType galleonType = spec().getUnitType("model.unit.galleon");
    private GoodsType tradeGoodsType = spec().getGoodsType("model.goods.tradeGoods");
    
    private Game game;

    @Override
    public void setUp() {
        game = getStandardGame();
        Map map = getTestMap(plainsType);
        game.setMap(map);
    }

    @Override
    public void tearDown() {
        game = null;
    }

    /**
     * Checks that the decider returns the right cost for a plain to plain move.
     */
    public void testGetCostLandLand() {
        DefaultCostDecider decider = new DefaultCostDecider();
        Tile start = game.getMap().getTile(5, 5);
        Unit unit = new Unit(game, start, game.getCurrentPlayer(), spec().getUnitType(
                "model.unit.hardyPioneer"),
                Unit.UnitState.ACTIVE);
        for (Map.Direction dir : Map.Direction.values()) {
            Tile end = game.getMap().getNeighbourOrNull(dir, start);
            assertNotNull(end);
            int cost = decider.getCost(unit, start, game.getMap().getTile(5, 6), 100, 0);
            assertEquals(plainsType.getBasicMoveCost(), cost);
        }
    }

    /**
     * Checks that {@link  DefaultCostDecider#getMovesLeft() } and
     * {@link  DefaultCostDecider#isNewTurn() } return the expected values after a move.
     */
    public void testGetRemainingMovesAndNewTurn() {
        DefaultCostDecider decider = new DefaultCostDecider();
        Unit unit = new Unit(game, game.getMap().getTile(1, 1), game.getCurrentPlayer(),
                spec().getUnitType("model.unit.hardyPioneer"),
                UnitState.ACTIVE);
        int cost = decider.getCost(unit, game.getMap().getTile(1, 1), game.getMap().getTile(2, 2), 4,
                4);
        assertEquals(plainsType.getBasicMoveCost(), cost);
        assertEquals(4 - plainsType.getBasicMoveCost(), decider.getMovesLeft());
        assertFalse(decider.isNewTurn());
    }
    
    /**
     * Checks possible move of a land unit to an ocean tile
     * Verifies that is invalid
     */
    public void testInvalidMoveOfLandUnitToAnOceanTile() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(9, 9);
        assertTrue("Unit tile should be land",unitTile.isLand());
        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), pioneerType, UnitState.ACTIVE);
        
        Tile seaTile = map.getTile(10, 9);
        assertFalse("Tile should be ocean",seaTile.isLand());
        
        // Execute
        DefaultCostDecider decider = new DefaultCostDecider();
        int cost = decider.getCost(unit, unitTile, seaTile, 4,4);
        assertTrue("Move should be invalid",cost == DefaultCostDecider.ILLEGAL_MOVE);
    }
    
    /**
     * Checks possible move of a naval unit to a land tile without settlement
     * Verifies that is invalid
     */
    public void testInvalidMoveOfNavalUnitToALandTile() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(10, 9);
        assertFalse("Unit tile should be ocean",unitTile.isLand());

        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), galleonType, UnitState.ACTIVE);
        
        Tile landTile = map.getTile(9, 9);
        assertTrue("Tile should be land",landTile.isLand());        
        
        // Execute
        DefaultCostDecider decider = new DefaultCostDecider();
        int cost = decider.getCost(unit, unitTile, landTile, 4,4);
        assertTrue("Move should be invalid",cost == DefaultCostDecider.ILLEGAL_MOVE);
    }
    
    /**
     * Checks possible move of a unit through a tile with a settlement
     * Verifies that is invalid
     */
    public void testInvalidMoveThroughTileWithSettlement() {
        Map map = game.getMap();
        //Setup
        Tile settlementTile = map.getTile(2,1);
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.settlementTile(settlementTile).build();
        settlement.placeSettlement();

        Tile unitTile = map.getTile(1, 1);
        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), pioneerType, UnitState.ACTIVE);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);
        
        // Execute
        DefaultCostDecider decider = new DefaultCostDecider();
        int cost = decider.getCost(unit, unitTile, settlementTile, 4,4);
        assertTrue("Move should be invalid",cost == DefaultCostDecider.ILLEGAL_MOVE);
    }
    
    public void testMoveThroughTileWithEnemyUnit() {

        
        Map map = game.getMap();
        
        //Setup
        Tile enemyUnitTile = map.getTile(2,1);
        Player frenchPlayer = game.getPlayer("model.nation.french");
        new Unit(game, enemyUnitTile, frenchPlayer, pioneerType, UnitState.ACTIVE);
        
        Tile unitTile = map.getTile(1, 1);
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Unit unit = new Unit(game, unitTile, dutchPlayer, pioneerType, UnitState.ACTIVE);
        // unit is going somewhere else
        Tile unitDestination = map.getTile(3, 1);
        unit.setDestination(unitDestination);
        
        // Execute
        DefaultCostDecider decider = new DefaultCostDecider();
        int cost = decider.getCost(unit, unitTile, enemyUnitTile, 4,4);
        assertTrue("Move should be invalid",cost == DefaultCostDecider.ILLEGAL_MOVE);
    }
    
    /**
     * Checks possible move of a naval unit to a tile with a settlement
     */
    public void testNavalUnitMoveToTileWithSettlement() {
        // For this test we need a different map
        Map map = getCoastTestMap(plainsType);
        game.setMap(map);
        
        Tile unitTile = map.getTile(10, 9);
        assertFalse("Unit tile should be ocean",unitTile.isLand());

        Unit unit = new Unit(game, unitTile, game.getCurrentPlayer(), galleonType, UnitState.ACTIVE);
        
        Tile settlementTile = map.getTile(9, 9);
        assertTrue("Tile should be land",settlementTile.isLand());
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        Settlement settlement = builder.settlementTile(settlementTile).build();
        settlement.placeSettlement();

        // unit is trying go to settlement
        unit.setDestination(settlementTile);
        
        // Execute
        DefaultCostDecider decider = new DefaultCostDecider();
        int cost = decider.getCost(unit, unitTile, settlementTile, 4,4);
        assertTrue("Move should be invalid, nothing to trade",cost == DefaultCostDecider.ILLEGAL_MOVE);
        
        // Add goods to trade
        Goods goods = new Goods(game, null, tradeGoodsType, 50);
        unit.add(goods);
        
        cost = decider.getCost(unit, unitTile, settlementTile, 4,4);
        assertFalse("Move should be valid, has goods to trade",cost == DefaultCostDecider.ILLEGAL_MOVE);
        
        // Set players at war
        Player indianPlayer = settlement.getOwner();
        indianPlayer.changeRelationWithPlayer(unit.getOwner(), Stance.WAR);
        
        cost = decider.getCost(unit, unitTile, settlementTile, 4,4);
        assertTrue("Move should be invalid, players at war",cost == DefaultCostDecider.ILLEGAL_MOVE);
    }
}
