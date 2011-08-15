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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class AIColonyTest extends FreeColTestCase {

    private static final BuildingType warehouse
        = spec().getBuildingType("model.building.warehouse");

    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType oreType
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType sugarType
        = spec().getGoodsType("model.goods.sugar");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final TileType forestType
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType savannahType
        = spec().getTileType("model.tile.savannah");
    private static final TileType mountainType
        = spec().getTileType("model.tile.mountains");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType lumberJackType
        = spec().getUnitType("model.unit.expertLumberJack");
    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");

    final int fullStock = 100;


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    // creates the special map for the tests
    // map will have:
    //    - a colony in (5,8) (built after)
    //    - a forest in (4,8) for lumber
    //    - a mountain in (6,8) for ore
    private Map buildMap(boolean withBuildRawMat){
        MapBuilder builder = new MapBuilder(getGame());
        builder.setBaseTileType(savannahType);
        if(withBuildRawMat){
            builder.setTile(4, 8, forestType);
            builder.setTile(6, 8, mountainType);
        }
        return builder.build();
    }

    /*
     * Tests worker allocation regarding building tasks
     */
    public void testBuildersAllocation() {
        Game game = ServerTestHelper.startServerGame(buildMap(true));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        //the number needs to be high to ensure allocation
        Colony colony = getStandardColony(6);
        game.setCurrentPlayer(colony.getOwner());

        colony.setCurrentlyBuilding(warehouse);
        final Building carpenterHouse
            = colony.getBuildingForProducing(hammersType);
        final Building blacksmithHouse
            = colony.getBuildingForProducing(toolsType);
        AIColony aiColony = aiMain.getAIColony(colony);
        ServerPlayer player = (ServerPlayer) colony.getOwner();

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());

        assertTrue("Colony should have been assigned a lumberjack",colony.getProductionOf(lumberType) > 0);
        assertTrue("Colony should have been assigned a carpenter",carpenterHouse.getUnitCount() > 0);

        // Simulate that enough hammers have been gathered, re-arrange and re-check
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());
        assertFalse("Colony does not need a carpenter",carpenterHouse.getUnitCount() > 0);
        assertTrue("Colony should have been assigned a ore miner",colony.getProductionOf(oreType) > 0);
        assertTrue("Colony should have been assigned a blacksmith",blacksmithHouse.getUnitCount() > 0);
    }

    /*
     * Tests worker allocation regarding building tasks when the
     * colony does not have tiles that provide the raw materials for
     * the build.
     */
    public void testBuildersAllocNoRawMatTiles() {
        Game game = ServerTestHelper.startServerGame(buildMap(false));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        //the number needs to be high to ensure allocation
        Colony colony = getStandardColony(6);
        game.setCurrentPlayer(colony.getOwner());

        // we need to ensure that there arent tiles with production of
        // the raw materials if this fails, the type of the tile in
        // buildMap() must be changed to meet this requirements
        String msg1 = "For the test to work, the colony cannot have tiles that produce lumber";
        String msg2 = "For the test to work, the colony cannot have tiles that produce ore";
        for (ColonyTile t : colony.getColonyTiles()) {
            Tile tile = t.getTile();
            assertTrue(msg1, tile.potential(lumberType, colonistType) == 0);
            assertTrue(msg2, tile.potential(oreType, colonistType) == 0);
        }

        colony.setCurrentlyBuilding(warehouse);
        final Building carpenterHouse
            = colony.getBuildingForProducing(hammersType);
        final Building blacksmithHouse
            = colony.getBuildingForProducing(toolsType);

        AIColony aiColony = aiMain.getAIColony(colony);
        ServerPlayer player = (ServerPlayer) colony.getOwner();
        aiColony.propertyChange(null); // force rearranging workers

        assertFalse("Colony couldnt have been assigned a lumberjack, no lumber",colony.getProductionOf(lumberType) > 0);
        assertFalse("Colony couldnt have been assigned a carpenter, no lumber",carpenterHouse.getUnitCount() > 0);

        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, fullStock);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());

        assertEquals("Colony couldnt have been assigned a lumberjack, no lumber",
                     colony.getProductionOf(lumberType), 0);
        assertTrue("Colony should have been assigned a carpenter, has lumber in stock",carpenterHouse.getUnitCount() > 0);

        // Simulate that enough hammers have been gathered, re-arrange and re-check
        colony.addGoods(hammersType, warehouse.getAmountRequiredOf(hammersType));

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());
        assertFalse("Colony does not need a carpenter",carpenterHouse.getUnitCount() > 0);
        assertFalse("Colony couldnt have been assigned a ore miner, no ore",colony.getProductionOf(oreType) > 0);
        assertFalse("Colony couldnt have been assigned a blacksmith, no ore",blacksmithHouse.getUnitCount() > 0);

        // Add ore to stock, re-arrange and re-check
        colony.addGoods(oreType, fullStock);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());

        assertFalse("Colony couldnt have been assigned a ore miner, no ore",colony.getProductionOf(oreType) > 0);
        assertTrue("Colony should have been assigned a blacksmith, has ore in stock",blacksmithHouse.getUnitCount() > 0);
    }

    /*
     * Tests expert allocation regarding raw materials where there are plenty already in stock
     */
    public void testExpertAllocColonyHasEnoughRawMat() {
        Game game = ServerTestHelper.startServerGame(getTestMap(forestType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        FreeColTestUtils.ColonyBuilder builder
            = FreeColTestUtils.getColonyBuilder();
        Colony colony = builder.addColonist(lumberJackType).build();
        game.setCurrentPlayer(colony.getOwner());

        ServerPlayer player = (ServerPlayer) colony.getOwner();
        assertEquals("Wrong number of units in colony",1,colony.getUnitCount());
        Unit lumberjack = colony.getUnitList().get(0);

        AIColony aiColony = aiMain.getAIColony(colony);

        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());

        final GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
        assertEquals("Lumberjack should have been assigned to collect lumber",
                     lumberType, lumberjack.getWorkType());

        // Add lumber to stock, re-arrange and re-check
        colony.addGoods(lumberType, fullStock);
        aiColony.propertyChange(null); // force rearranging workers
        aiColony.rearrangeWorkers(player.getConnection());

        String errMsg = "Lumberjack should not have been assigned to collect lumber, enough lumber in the colony";
        assertFalse(errMsg, lumberType == lumberjack.getWorkType());
    }

    public void testCheckConditionsForHorseBreed(){
        Game game = ServerTestHelper.startServerGame(getTestMap());
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony(1);
        AIColony aiColony = aiMain.getAIColony(colony);
        game.setCurrentPlayer(colony.getOwner());
        final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        GoodsType reqGoodsType = horsesType.getRawMaterial();

        int foodSurplus = colony.getFoodProduction() - colony.getConsumptionOf(reqGoodsType);
        assertTrue("Setup error, colony does not have food surplus", foodSurplus > 0);

        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        final EquipmentType horsesEqType = spec().getEquipmentType("model.equipment.horses");
        Unit scout = new ServerUnit(getGame(), colony.getTile(), colony.getOwner(),
                                    colonistType, horsesEqType);
        assertTrue("Scout should be mounted", scout.isMounted());

        assertEquals("Setup error, colony should not have horses in stock",
                     0, colony.getGoodsCount(horsesType));

        aiColony.checkConditionsForHorseBreed();

        assertEquals("Colony should now have horses in stock",
                     50, colony.getGoodsCount(horsesType));
        assertFalse("Scout should not be mounted", scout.isMounted());
    }

    public void testBestUnitForWorkLocation() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        game.setCurrentPlayer(colony.getOwner());
        Player dutch = getGame().getPlayer("model.nation.dutch");
        ColonyTile colonyTile = colony.getColonyTiles().get(0);

        assertNull(AIColony.bestUnitForWorkLocation(null, colonyTile, sugarType));

        List<Unit> units = new ArrayList<Unit>();
        assertNull(AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));

        final UnitType servantType = spec().getUnitType("model.unit.indenturedServant");
        Unit servant = new ServerUnit(getGame(), null, dutch, servantType);
        units.add(servant);
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        final UnitType criminalType = spec().getUnitType("model.unit.pettyCriminal");
        Unit criminal = new ServerUnit(getGame(), null, dutch, criminalType);
        units.add(criminal);
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        final UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist1 = new ServerUnit(getGame(), null, dutch, colonistType);
        units.add(colonist1);
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        Unit colonist2 = new ServerUnit(getGame(), null, dutch, colonistType);
        units.add(colonist2);
        colonist2.setWorkType(sugarType);
        colonist2.modifyExperience(100);
        // colonist2 has more sugar experience
        assertEquals(colonist2, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        colonist2.setWorkType(lumberType);
        colonist2.modifyExperience(100);
        // colonist1 has *less* experience to waste
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));
        // colonist2 has lumber experience, but production is zero
        assertEquals(null, AIColony.bestUnitForWorkLocation(units, colonyTile, lumberType));

        final UnitType convertType = spec().getUnitType("model.unit.indianConvert");
        Unit convert = new ServerUnit(getGame(), null, dutch, convertType);
        units.add(convert);
        assertEquals(convert, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(convert, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));
        units.remove(convert);

        final UnitType sugarPlanterType = spec().getUnitType("model.unit.masterSugarPlanter");
        Unit sugarPlanter = new ServerUnit(getGame(), null, dutch, sugarPlanterType);
        units.add(sugarPlanter);
        assertEquals(sugarPlanter, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        // prefer colonist over wrong type of expert
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));
        units.remove(sugarPlanter);

        final UnitType farmerType = spec().getUnitType("model.unit.expertFarmer");
        Unit farmer = new ServerUnit(getGame(), null, dutch, farmerType);
        units.add(farmer);
        // prefer colonist over wrong type of expert
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(farmer, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        units.add(convert);
        units.add(sugarPlanter);

        assertEquals(sugarPlanter, AIColony.bestUnitForWorkLocation(units, colonyTile, sugarType));
        assertEquals(farmer, AIColony.bestUnitForWorkLocation(units, colonyTile, grainType));

        Building townHall = new ServerBuilding(game, colony, spec().getBuildingType("model.building.townHall"));
        units.clear();
        units.add(servant);
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, townHall, null));

        units.add(criminal);
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, townHall, null));

        units.add(convert);
        assertEquals(servant, AIColony.bestUnitForWorkLocation(units, townHall, null));

        units.add(colonist1);
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, townHall, null));

        units.add(colonist2);
        // colonist1 has *less* experience to waste
        assertEquals(colonist1, AIColony.bestUnitForWorkLocation(units, townHall, null));

        units.add(sugarPlanter);
        // sugar planter can not be upgraded at all, but colonist could be
        assertEquals(sugarPlanter, AIColony.bestUnitForWorkLocation(units, townHall, null));

    }


    public void testBestDefender() {
        Game game = ServerTestHelper.startServerGame(getTestMap(savannahType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Colony colony = getStandardColony();
        assertEquals(artilleryType, AIColony.getBestDefender(colony));

    }

}
