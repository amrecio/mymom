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

package net.sf.freecol.common.model;

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class MovementTest extends FreeColTestCase {


    TileType plains = spec().getTileType("model.tile.plains");
    TileType hills = spec().getTileType("model.tile.hills");
    TileType ocean = spec().getTileType("model.tile.ocean");

    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType braveType = spec().getUnitType("model.unit.brave");

    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType indianHorses = spec().getEquipmentType("model.equipment.indian.horses");
    EquipmentType indianMuskets = spec().getEquipmentType("model.equipment.indian.muskets");

    public void testMoveFromPlainsToPlains() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = plains.getBasicMoveCost();
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

        // Plowing should not change result
        assertTrue("No improvements", tile2.getTileImprovements().isEmpty());
        TileImprovement ti = new TileImprovement(game, tile2, spec().getTileImprovementType("model.improvement.plow"));
        ti.setTurnsToComplete(0);
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(ti);
        assertTrue("Plowed", tile2.getCompletedTileImprovements().size() == 1);
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));
    }

    public void testMoveFromPlainsToHills() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile2.setType(hills);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = hills.getBasicMoveCost();
        assertTrue(moveCost > colonist.getMovesLeft());
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testMoveAlongRoad() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        TileImprovementType roadType = spec().getTileImprovementType("model.improvement.road");
        TileImprovement road1 = new TileImprovement(game, tile1, roadType);
        assertTrue(road1.isRoad());
        assertFalse(road1.isComplete());
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(road1);
        assertTrue(tile1.hasRoad());

        TileImprovement road2 = new TileImprovement(game, tile2, roadType);
        road2.setTurnsToComplete(0);
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(road2);
        assertTrue(road2.isComplete());
        assertTrue(tile2.hasRoad());

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = 1;
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testMoveAlongRiver() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);

        TileImprovementType riverType = spec().getTileImprovementType("model.improvement.river");
        TileImprovement river1 = new TileImprovement(game, tile1, riverType);
        assertTrue(river1.isRiver());
        assertTrue(river1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(river1);
        assertTrue(tile1.hasRiver());

        TileImprovement river2 = new TileImprovement(game, tile2, riverType);
        river2.setTurnsToComplete(0);
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(river2);
        assertTrue(river2.isComplete());
        assertTrue(tile2.hasRiver());

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);

        int moveCost = 1;
        assertEquals(moveCost, colonist.getMoveCost(tile2));
        assertEquals(Math.min(moveCost, colonistType.getMovement()),
                     colonist.getMoveCost(tile2));

    }

    public void testScoutColony() {
        Game game = getGame();
        Map map = getTestMap(true);
        game.setMap(map);

        Player french = game.getPlayer("model.nation.french");
        Player dutch = game.getPlayer("model.nation.dutch");
        Player iroquois = game.getPlayer("model.nation.iroquois");

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(6, 8);
        tile1.setExploredBy(french, true);
        tile2.setExploredBy(french, true);
        tile3.setExploredBy(french, true);
        tile1.setExploredBy(dutch, true);
        tile2.setExploredBy(dutch, true);
        tile3.setExploredBy(dutch, true);
        tile1.setExploredBy(iroquois, true);
        tile3.setExploredBy(iroquois, true);

        Colony colony = getStandardColony();

        assertEquals(tile1.getColony(), colony);

        Unit colonist = new ServerUnit(game, tile2, french, colonistType);
        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_SETTLEMENT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(horses, 1);
        assertEquals(Unit.MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(muskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT,
                     colonist.getMoveType(tile1));

        Unit brave = new ServerUnit(game, tile3, iroquois, braveType);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianHorses, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianMuskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
    }

    public void testScoutIndianSettlement() {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);

        Player french = game.getPlayer("model.nation.french");
        Player inca = game.getPlayer("model.nation.inca");
        Player iroquois = game.getPlayer("model.nation.iroquois");

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(6, 8);
        tile1.setExploredBy(french, true);
        tile2.setExploredBy(french, true);
        tile3.setExploredBy(french, true);
        tile1.setExploredBy(iroquois, true);
        tile3.setExploredBy(iroquois, true);

        // Build settlement
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(inca).settlementTile(tile1).skillToTeach(null).build();

        Unit colonist = new ServerUnit(game, tile2, french, colonistType);
        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_CONTACT,
                     colonist.getMoveType(tile1));
        Player.makeContact(french, inca);
        assertEquals(Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(horses, 1);
        assertEquals(Unit.MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT,
                     colonist.getMoveType(tile1));
        colonist.changeEquipment(muskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT,
                     colonist.getMoveType(tile1));

        Unit brave = new ServerUnit(game, tile3, iroquois, braveType);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianHorses, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
        brave.changeEquipment(indianMuskets, 1);
        assertEquals(Unit.MoveType.ATTACK_SETTLEMENT, brave.getMoveType(tile1));
    }
}
