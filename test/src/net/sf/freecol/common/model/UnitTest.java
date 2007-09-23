package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class UnitTest extends FreeColTestCase {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    TileType plains = spec().getTileType("model.tile.plains");
    TileType desert = spec().getTileType("model.tile.desert");
    TileType grassland = spec().getTileType("model.tile.grassland");
    TileType prairie = spec().getTileType("model.tile.prairie");
    TileType tundra = spec().getTileType("model.tile.tundra");
    TileType savannah = spec().getTileType("model.tile.savannah");
    TileType marsh = spec().getTileType("model.tile.marsh");
    TileType swamp = spec().getTileType("model.tile.swamp");
    TileType arctic = spec().getTileType("model.tile.arctic");
        
    TileType plainsForest = spec().getTileType("model.tile.mixedForest");
    TileType desertForest = spec().getTileType("model.tile.scrubForest");
    TileType grasslandForest = spec().getTileType("model.tile.coniferForest");
    TileType prairieForest = spec().getTileType("model.tile.broadleafForest");
    TileType tundraForest = spec().getTileType("model.tile.borealForest");
    TileType savannahForest = spec().getTileType("model.tile.tropicalForest");
    TileType marshForest = spec().getTileType("model.tile.wetlandForest");
    TileType swampForest = spec().getTileType("model.tile.rainForest");
    TileType hills = spec().getTileType("model.tile.hills");
    TileType mountains = spec().getTileType("model.tile.mountains");

    TileImprovementType road = spec().getTileImprovementType("model.improvement.Road");
    TileImprovementType plow = spec().getTileImprovementType("model.improvement.Plow");

    /**
     * Test Plowing with a hardy pioneer
     * 
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, spec().getUnitType("model.unit.hardyPioneer"), Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.isPlowed());
        
        TileImprovementType plow = spec().getTileImprovementType("model.improvement.Plow");
        assertNotNull(plow);
        
        // How are improvements done?
        TileImprovement plainPlow = plain.findTileImprovementType(plow);
        assertNotNull(plainPlow);
        
        hardyPioneer.work(plainPlow);

        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.isPlowed());

        // Advance 1 turn
        game.newTurn();

        // Pioneer finished work but can only move on next turn
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.isPlowed());

        // Advance last turn
        game.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.isPlowed());
    }

    public void testColonyProfitFromEnhancement() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
        Tile plain58 = map.getTile(5, 8);

        // Found colony on 6,8
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.veteranSoldier"), Unit.ACTIVE, true, false, 0,
                false);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        soldier.setLocation(colony.getColonyTile(plain58));

        Unit hardyPioneer = new Unit(game, plain58, dutch, spec().getUnitType("model.unit.hardyPioneer"), Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(0, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertEquals(false, plain58.isPlowed());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        game.newTurn();

        assertEquals(false, plain58.isPlowed());
        assertEquals(8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        TileImprovement improvement = new TileImprovement(hardyPioneer.getGame(), hardyPioneer.getTile(), road);
        hardyPioneer.work(improvement);

        game.newTurn();

        assertEquals(true, plain58.isPlowed());
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + 8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());

        // Advance last turn
        game.newTurn();

        assertEquals(16 + 9, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(true, plain58.isPlowed());
    }

    /**
     * Test Building a road with a hardy pioneer.
     * 
     * The road is available directly, but the pioneer can only move on the next
     * turn.
     * 
     */
    public void testDoAssignedWorkHardyPioneerBuildRoad() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, spec().getUnitType("model.unit.hardyPioneer"),
                                     Unit.ACTIVE, false, false, 100, false);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getNumberOfTools());
        assertEquals(false, plain.hasRoad());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());

        // Now do it
        TileImprovement improvement = new TileImprovement(hardyPioneer.getGame(), hardyPioneer.getTile(), road);
        hardyPioneer.work(improvement);

        // After
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
        assertEquals(true, plain.hasRoad());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());

        // Advance 1 turn
        game.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(Unit.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getNumberOfTools());
    }

    public static int getWorkLeftForPioneerWork(UnitType unitType, TileType tileType, TileImprovementType whichWork) {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile tile = new Tile(game, tileType, 0, 0);

        Unit unit = new Unit(game, tile, dutch, unitType, Unit.ACTIVE, false, false, 100, false);
        
        unit.work(new TileImprovement(unit.getGame(), unit.getTile(), whichWork));

        return unit.getWorkLeft();
    }

    /**
     * Check for basic time requirements...
     * 
     */
    public void testDoAssignedWorkAmateurAndHardyPioneer() {

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        UnitType hardyPioneer = spec().getUnitType("model.unit.hardyPioneer");
    	
        { // Savanna
            assertEquals(7, getWorkLeftForPioneerWork(freeColonist, savannahForest, plow));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, savannahForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, savannah, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, savannah, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, savannahForest, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, savannahForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, savannah, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, savannah, road));
        }

        { // Tundra
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, tundraForest, plow));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, tundraForest, road));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, tundra, plow));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, tundra, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, tundraForest, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, tundraForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, tundra, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, tundra, road));
        }

        { // Plains
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, plainsForest, plow));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, plainsForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, plains, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, plains, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, plainsForest, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, plainsForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, plains, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, plains, road));
        }

        { // Hill
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, hills, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, hills, road));
        }

        { // Mountain
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, mountains, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, mountains, road));
        }

        { // Marsh
            assertEquals(7, getWorkLeftForPioneerWork(freeColonist, marshForest, plow));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, marshForest, road));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, marsh, plow));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, marsh, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, marshForest, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, marshForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, marsh, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, marsh, road));
        }

        { // Desert
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, desertForest, plow));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, desertForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, desert, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, desert, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, desertForest, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, desertForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, desert, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, desert, road));
        }

        { // Swamp
            assertEquals(8, getWorkLeftForPioneerWork(freeColonist, swampForest, plow));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, swampForest, road));
            assertEquals(8, getWorkLeftForPioneerWork(freeColonist, swamp, plow));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, swamp, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, swampForest, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, swampForest, road));
            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, swamp, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, swamp, road));
        }
    }

    /**
     * Make sure that a colony can only be build by a worker on the same tile as
     * the colony to be build.
     * 
     */
    public void testBuildColonySameTile() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                Unit.ACTIVE, true, false, 0, false);

        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(6, 9));
        soldier.setWorkType(Goods.FOOD);

        try {
            soldier.buildColony(colony);
            fail();
        } catch (IllegalStateException e) {
        }

        soldier.setLocation(map.getTile(6, 9));
        soldier.buildColony(colony);

        assertEquals(colony, map.getTile(6, 9).getSettlement());
    }
}
