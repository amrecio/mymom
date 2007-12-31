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

package net.sf.freecol.common.model;

import net.sf.freecol.util.test.FreeColTestCase;

public class DemotionTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");

    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");

    public void testColonistDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        assertTrue(colonist.hasAbility("model.ability.canBeCaptured"));
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        assertTrue(soldier.hasAbility("model.ability.canBeCaptured"));
        soldier.equipWith(muskets, true);
        assertFalse(soldier.hasAbility("model.ability.canBeCaptured"));

        colonist.demote(soldier);
        assertTrue(colonist.getType() == colonistType);
        assertTrue(colonist.getOwner() == french);
        assertTrue(colonist.getTile() == tile2);

    }

    public void testSoldierDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit soldier1 = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier2.equipWith(muskets, true);

        soldier1.demote(soldier2);
        assertTrue(soldier1.getType() == colonistType);
        assertTrue(soldier1.getOwner() == dutch);
        assertTrue(soldier1.getTile() == tile1);
        assertTrue(soldier1.getEquipment().isEmpty());

        soldier1.demote(soldier2);
        assertTrue(soldier1.getType() == colonistType);
        assertTrue(soldier1.getOwner() == french);
        assertTrue(soldier1.getTile() == tile2);
    }

    public void testDragoonDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit dragoon = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        dragoon.demote(soldier);
        assertTrue(dragoon.getType() == colonistType);
        assertTrue(dragoon.getOwner() == dutch);
        assertTrue(dragoon.getTile() == tile1);
        assertTrue(dragoon.getEquipment().size() == 1);
        assertTrue(dragoon.getEquipment().get(0) == muskets);

        dragoon.demote(soldier);
        assertTrue(dragoon.getType() == colonistType);
        assertTrue(dragoon.getOwner() == dutch);
        assertTrue(dragoon.getTile() == tile1);
        assertTrue(dragoon.getEquipment().isEmpty());

        dragoon.demote(soldier);
        assertTrue(dragoon.getType() == colonistType);
        assertTrue(dragoon.getOwner() == french);
        assertTrue(dragoon.getTile() == tile2);
    }

    public void testScoutDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit scout = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        scout.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        scout.demote(soldier);
        scout.isDisposed();
    }

    public void testVeteranSoldierDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
        assertTrue(veteranType.getDowngrade(UnitType.CAPTURE) == colonistType);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit soldier1 = new Unit(game, tile1, dutch, veteranType, Unit.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier2.equipWith(muskets, true);

        soldier1.demote(soldier2);
        assertTrue(soldier1.getType() == veteranType);
        assertTrue(soldier1.getOwner() == dutch);
        assertTrue(soldier1.getTile() == tile1);
        assertTrue(soldier1.getEquipment().isEmpty());

        soldier1.demote(soldier2);
        assertTrue(soldier1.getType() == colonistType);
        assertTrue(soldier1.getOwner() == french);
        assertTrue(soldier1.getTile() == tile2);
    }

    public void testArtilleryDemotedBySoldier() {

        Game game = getStandardGame();
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        UnitType artilleryType = spec().getUnitType("model.unit.artillery");
        UnitType damagedArtilleryType = spec().getUnitType("model.unit.damagedArtillery");
        assertTrue(artilleryType.getDowngrade(UnitType.DEMOTION) == damagedArtilleryType);
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit artillery = new Unit(game, tile1, dutch, artilleryType, Unit.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        artillery.demote(soldier);
        assertTrue(artillery.getType() == damagedArtilleryType);
        assertTrue(artillery.getOwner() == dutch);
        assertTrue(artillery.getTile() == tile1);

        artillery.demote(soldier);
        assertTrue(artillery.isDisposed());
    }

}