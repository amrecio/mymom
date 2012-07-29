/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyTest extends FreeColTestCase {

    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType churchType
        = spec().getBuildingType("model.building.church");
    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");
    private static final BuildingType warehouseType
        = spec().getBuildingType("model.building.warehouse");
    private static final BuildingType warehouseExpansionType
        = spec().getBuildingType("model.building.warehouseExpansion");
    private static final BuildingType weaversHouseType
        = spec().getBuildingType("model.building.weaverHouse");

    private static final GoodsType bellsGoodsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType clothGoodsType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType cottonGoodsType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType grainGoodsType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammerGoodsType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType lumberGoodsType
        = spec().getGoodsType("model.goods.lumber");

    private static final TileType plainsTileType
        = spec().getTileType("model.tile.plains");

    private static final UnitType cottonPlanterType
        = spec().getUnitType("model.unit.masterCottonPlanter");
    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType masterWeaverType
        = spec().getUnitType("model.unit.masterWeaver");
    private static final UnitType wagonTrainType
        = spec().getUnitType("model.unit.wagonTrain");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");

    private static final EquipmentType muskets
        = spec().getEquipmentType("model.equipment.muskets");

    public void testCurrentlyBuilding() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();
        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Colony should be building a warehouse",
                     warehouseType, colony.getCurrentlyBuilding());

        colony.setCurrentlyBuilding(churchType);
        assertEquals("Colony should be building a church",
                     churchType, colony.getCurrentlyBuilding());
    }

    public void testBuildQueueDoesNotAcceptBuildingDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should have 1 entry",
                     1, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 1 entry",
                     1, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(churchType);
        assertEquals("Building queue should have 2 entries",
                     2, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(warehouseType);
        assertEquals("Building queue should still have 2 entries",
                     2, colony.getBuildQueue().size());
    }

    public void testBuildQueueAcceptsUnitDoubles() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        colony.setCurrentlyBuilding(wagonTrainType);
        // default item will be added to new colony's build queue
        assertEquals("Building queue should have 2 entry",
                     2, colony.getBuildQueue().size());

        colony.setCurrentlyBuilding(wagonTrainType);
        assertEquals("Building queue should have 3 entries",
                     3, colony.getBuildQueue().size());
    }

    public void testOccupationWithFood() {
        int population = 3;

        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(population);

        assertTrue("colony produces less food than it consumes",
                   colony.getFoodProduction() > colony.getFoodConsumption()
                   + freeColonistType.getConsumptionOf(grainGoodsType));

        // colonist with no skill or experience will produce food
        Unit colonist = new ServerUnit(game, colony.getTile(),
                                       colony.getOwner(), freeColonistType);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(grainGoodsType, colonist.getWorkType());

        // colonist with experience in producing farmed goods will
        // produce that type of goods
        colonist.setLocation(colony.getTile());
        colonist.setWorkType(cottonGoodsType);
        colonist.modifyExperience(100);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(cottonGoodsType, colonist.getWorkType());

        // expert will produce expert goods
        colonist.setLocation(colony.getTile());
        colonist.setType(cottonPlanterType);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof ColonyTile);
        assertEquals(cottonGoodsType, colonist.getWorkType());

        // expert will produce expert goods
        colonist.setLocation(colony.getTile());
        colonist.setType(elderStatesmanType);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        assertEquals(townHallType,
                     ((Building) colonist.getLocation()).getType());

    }

    private Modifier createTeaPartyModifier(Turn turn) {
        Modifier template = spec()
            .getModifiers("model.modifier.colonyGoodsParty").get(0);
        return Modifier.makeTimedModifier("model.goods.bells", template, turn);
    }

    public void testTeaParty() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(5);

        colony.addModifier(createTeaPartyModifier(game.getTurn()));
        colony.addModifier(createTeaPartyModifier(game.getTurn()));
        colony.addModifier(createTeaPartyModifier(game.getTurn()));

        int modifierCount = 0;
        for (Modifier existingModifier : colony.getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY_SOURCE.equals(existingModifier.getSource())) {
                modifierCount++;
            }
        }
        assertEquals(1, modifierCount);

        Turn newTurn = new Turn(game.getTurn().getNumber() + 1);
        colony.addModifier(createTeaPartyModifier(newTurn));

        modifierCount = 0;
        for (Modifier existingModifier : colony.getModifierSet("model.goods.bells")) {
            if (Specification.COLONY_GOODS_PARTY_SOURCE.equals(existingModifier.getSource())) {
                modifierCount++;
            }
        }
        assertEquals(2, modifierCount);

    }

    public void testOccupationWithoutFood() {
        int population = 1;
        GoodsType food = spec().getPrimaryFoodType();

        assertEquals(2, freeColonistType.getConsumptionOf(food));
        assertEquals(2, masterWeaverType.getConsumptionOf(food));

        Game game = getGame();
        game.setMap(getTestMap(spec().getTileType("model.tile.arctic"), true));
        Colony colony = getStandardColony(population);

        assertTrue("colony produces more food than it consumes",
                   colony.getFoodProduction() < colony.getFoodConsumption() +
                   freeColonistType.getConsumptionOf(food));

        // colonist produces bells because they require no input
        Unit colonist = new ServerUnit(game, colony.getTile(),
                                       colony.getOwner(), freeColonistType);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        Building townHall = colony.getBuilding(townHallType);
        assertEquals(townHall, colonist.getLocation());
        assertEquals(townHall, colony.getBuildingFor(colonist));
        assertEquals(bellsGoodsType, colonist.getWorkType());

        colonist.setLocation(colony.getTile());
        colonist.setWorkType(cottonGoodsType);
        colonist.modifyExperience(100);
        nonServerJoinColony(colonist, colony);
        assertEquals(townHallType, colony.getBuildingFor(colonist).getType());
        assertEquals(townHallType, ((Building) colonist.getLocation()).getType());
        assertEquals(bellsGoodsType, colonist.getWorkType());

        colonist.setLocation(colony.getTile());
        colonist.setType(cottonPlanterType);
        nonServerJoinColony(colonist, colony);
        assertEquals(townHallType, colony.getBuildingFor(colonist).getType());
        assertEquals(townHallType, ((Building) colonist.getLocation()).getType());
        assertEquals(bellsGoodsType, colonist.getWorkType());

        // colonist produces cloth, because there is cotton now
        colonist.setLocation(colony.getTile());
        colonist.setType(masterWeaverType);
        colony.addGoods(cottonGoodsType, 100);
        nonServerJoinColony(colonist, colony);
        assertTrue(colonist.getLocation() instanceof Building);
        assertEquals(weaversHouseType, colony.getBuildingFor(colonist).getType());
        assertEquals(weaversHouseType, ((Building) colonist.getLocation()).getType());
        assertEquals(clothGoodsType, colonist.getWorkType());
    }

    public void testFoundColony() {
        Game game = getGame();
        Map map = getCoastTestMap(plainsTileType);
        game.setMap(map);

        Colony colony = getStandardColony(3, 1, 8);
        Player dutch = colony.getOwner();
        Tile colonyTile = colony.getTile();
        assertEquals(colonyTile.getType(), plainsTileType);

        Unit colonist = colony.getUnitList().get(0);
        Tile workedTile = null;
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (ct.isColonyCenterTile()) continue;
            workedTile = ct.getWorkTile();
            if (workedTile.isInUse()) {
                break;
            } else if (workedTile.getType() == plainsTileType) {
                colonist.setWorkType(spec().getPrimaryFoodType());
                ct.add(colonist);
                break;
            }
        }
        assertTrue(workedTile.isInUse());

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        Player indianPlayer = camp.getOwner();
        Tile campTile = camp.getTile();
        assertEquals(campTile.getType(), plainsTileType);

        Tile nativeTile = null;
        for (Tile t : camp.getTile().getSurroundingTiles(1)) {
            if (t.getType() == plainsTileType) {
                nativeTile = t;
                break;
            }
        }
        assertEquals(nativeTile.getOwner(), indianPlayer);

        Player french = game.getPlayer("model.nation.french");
        Colony frenchColony = getStandardColony(3, 9, 8);
        frenchColony.changeOwner(french);
        assertEquals(frenchColony.getTile().getType(), plainsTileType);
        Tile frenchTile = null;
        for (Tile t : frenchColony.getTile().getSurroundingTiles(1)) {
            if (t.getType() == plainsTileType) {
                frenchTile = t;
                break;
            }
        }
        assertEquals(frenchTile.getOwner(), french);

        Tile landTile = map.getTile(1, 1);
        Tile lcrTile = map.getTile(2, 2);
        lcrTile.add(new LostCityRumour(game, lcrTile,
                LostCityRumour.RumourType.NO_SUCH_RUMOUR, "fake"));
        Tile waterTile = map.getTile(12, 12);
        assertTrue(!waterTile.isLand());

        assertTrue("Can own empty tile",
                   dutch.canOwnTile(landTile));
        assertFalse("Europeans can not own tile with an LCR on it",
                    dutch.canOwnTile(lcrTile));
        assertTrue("Natives can own tile with an LCR on it",
                    indianPlayer.canOwnTile(campTile));
        assertTrue("Europeans can own water tile",
                   dutch.canOwnTile(waterTile));
        assertFalse("Natives can not own water tile",
                   indianPlayer.canOwnTile(waterTile));

        assertEquals("Can found on land",
                     NoClaimReason.NONE,
                     dutch.canClaimToFoundSettlementReason(landTile));
        assertEquals("Can found on unsettleable tile",
                     NoClaimReason.TERRAIN,
                     dutch.canClaimToFoundSettlementReason(waterTile));
        assertEquals("Can not found on LCR",
                     NoClaimReason.RUMOUR,
                     dutch.canClaimToFoundSettlementReason(lcrTile));
        assertEquals("Can not found on water",
                     NoClaimReason.TERRAIN,
                     indianPlayer.canClaimToFoundSettlementReason(waterTile));
        assertEquals("Can not found on settlement",
                     NoClaimReason.SETTLEMENT,
                     dutch.canClaimToFoundSettlementReason(campTile));
        assertEquals("Can not found on tile in use",
                     NoClaimReason.WORKED,
                     dutch.canClaimToFoundSettlementReason(workedTile));
        assertEquals("Can not found on European tile",
                     NoClaimReason.EUROPEANS,
                     dutch.canClaimToFoundSettlementReason(frenchTile));
        assertEquals("Can not found on native tile",
                     NoClaimReason.NATIVES,
                     dutch.canClaimToFoundSettlementReason(nativeTile));

        assertEquals("Can use land",
                     NoClaimReason.NONE,
                     dutch.canClaimForSettlementReason(landTile));
        assertEquals("Can use unsettleable tile",
                     NoClaimReason.NONE,
                     dutch.canClaimForSettlementReason(waterTile));
        assertEquals("Europeans can not use LCR",
                     NoClaimReason.RUMOUR,
                     dutch.canClaimForSettlementReason(lcrTile));
        assertEquals("Natives can use LCR",
                     NoClaimReason.NONE,
                     indianPlayer.canClaimForSettlementReason(lcrTile));
        assertEquals("Europeans can use water",
                     NoClaimReason.NONE,
                     dutch.canClaimForSettlementReason(waterTile));
        assertEquals("Natives can not use water",
                     NoClaimReason.WATER,
                     indianPlayer.canClaimForSettlementReason(waterTile));
        assertEquals("Can not use on settlement",
                     NoClaimReason.SETTLEMENT,
                     dutch.canClaimForSettlementReason(campTile));
        assertEquals("Can not use tile in use",
                     NoClaimReason.WORKED,
                     dutch.canClaimForSettlementReason(workedTile));
        assertEquals("Can not use European tile",
                     NoClaimReason.EUROPEANS,
                     dutch.canClaimForSettlementReason(frenchTile));
        assertEquals("Can not use native tile",
                     NoClaimReason.NATIVES,
                     dutch.canClaimForSettlementReason(nativeTile));
    }

    public void testUnderSiege() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(5);
        Tile tile = colony.getTile().getAdjacentTile(Direction.N);
        Player iroquois = game.getPlayer("model.nation.iroquois");

        assertFalse("No enemy units present.", colony.isUnderSiege());

        Unit brave = new ServerUnit(game, tile, iroquois, braveType);
        assertFalse("Not at war with the Iroquois.", colony.isUnderSiege());

        // declare war
        colony.getOwner().setStance(iroquois, Player.Stance.WAR);
        iroquois.setStance(colony.getOwner(), Player.Stance.WAR);

        assertTrue("At war with the Iroquois.", colony.isUnderSiege());

        Unit soldier = new ServerUnit(game, colony.getTile(), colony.getOwner(),
                                      freeColonistType, muskets);
        assertFalse("Equal number of friendly and enemy combat units.",
                    colony.isUnderSiege());

        Unit brave2 = new ServerUnit(game, tile, iroquois, braveType);
        assertTrue("Enemy combat units outnumber friendly combat units.",
                   colony.isUnderSiege());

        Unit colonist = new ServerUnit(game, colony.getTile(), colony.getOwner(),
                                       freeColonistType);
        assertTrue("Enemy combat units outnumber friendly combat units.",
                   colony.isUnderSiege());

        colonist.changeEquipment(muskets, 1);
        assertFalse("Equal number of friendly and enemy combat units.",
                    colony.isUnderSiege());
    }


    public void testUpkeep() {
        Game game = getGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony(5);

        assertEquals("New colonies should not require upkeep.",
                     0, colony.getUpkeep());

        int churchUpkeep = churchType.getUpkeep();
        colony.getBuilding(churchType).upgrade();
        assertEquals(churchUpkeep, colony.getUpkeep());

        int lumberMillUpkeep = lumberMillType.getUpkeep();
        colony.getBuilding(carpenterHouseType).upgrade();
        assertEquals(churchUpkeep + lumberMillUpkeep, colony.getUpkeep());
    }

}
