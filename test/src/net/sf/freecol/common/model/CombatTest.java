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

import java.util.Iterator;
import java.util.Set;

import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class CombatTest extends FreeColTestCase {

    private static final EquipmentType muskets
        = spec().getEquipmentType("model.equipment.muskets");
    private static final EquipmentType horses
        = spec().getEquipmentType("model.equipment.horses");
    private static final EquipmentType tools
        = spec().getEquipmentType("model.equipment.tools");
    private static final EquipmentType indianMuskets
        = spec().getEquipmentType("model.equipment.indian.muskets");
    private static final EquipmentType indianHorses
        = spec().getEquipmentType("model.equipment.indian.horses");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType hills
        = spec().getTileType("model.tile.hills");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");

    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");
    private static final UnitType colonialType
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType artilleryType
        = spec().getUnitType("model.unit.artillery");
    private static final UnitType damagedArtilleryType
        = spec().getUnitType("model.unit.damagedArtillery");
    private static final UnitType colonialRegularType
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType kingsRegularType
        = spec().getUnitType("model.unit.kingsRegular");
    private static final UnitType indianConvertType
        = spec().getUnitType("model.unit.indianConvert");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");

    EquipmentType[] dragoonEquipment = new EquipmentType[] { horses, muskets };


    public void testColonistAttackedByVeteran() throws Exception {
        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        FreeColTestCase.spec();
        Tile tile1 = map.getTile(5, 8);
        tile1.setType(hills);
        assertEquals(hills, tile1.getType());
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setStateUnchecked(Unit.UnitState.FORTIFIED);
        Unit soldier = new ServerUnit(game, tile2, french, veteranType, muskets, horses);
        soldier.setMovesLeft(1);

        Modifier bigMovementPenalty = spec().getModifiers(SimpleCombatModel.BIG_MOVEMENT_PENALTY)
            .get(0);
        Modifier attackBonus = spec().getModifiers(SimpleCombatModel.ATTACK_BONUS).get(0);
        Modifier fortified = spec().getModifiers(SimpleCombatModel.FORTIFIED).get(0);

        Set<Modifier> veteranModifierSet = veteranType.getModifierSet("model.modifier.offence");
        assertEquals(1, veteranModifierSet.size());
        Modifier veteranModifier = veteranModifierSet.iterator().next();

        Set<Modifier> musketModifierSet = muskets.getModifierSet("model.modifier.offence");
        assertEquals(1, musketModifierSet.size());
        Modifier musketModifier = musketModifierSet.iterator().next();

        Set<Modifier> horsesModifierSet = horses.getModifierSet("model.modifier.offence");
        assertEquals(1, horsesModifierSet.size());
        Modifier horsesModifier = horsesModifierSet.iterator().next();

        Set<Modifier> offenceModifiers = combatModel.getOffensiveModifiers(soldier, colonist);
        assertEquals(6, offenceModifiers.size());
        assertTrue(offenceModifiers.contains(bigMovementPenalty));
        offenceModifiers.remove(bigMovementPenalty);
        assertTrue(offenceModifiers.contains(veteranModifier));
        offenceModifiers.remove(veteranModifier);
        assertTrue(offenceModifiers.contains(musketModifier));
        offenceModifiers.remove(musketModifier);
        assertTrue(offenceModifiers.contains(horsesModifier));
        offenceModifiers.remove(horsesModifier);
        assertTrue(offenceModifiers.contains(attackBonus));
        offenceModifiers.remove(attackBonus);
        // this was also added by the combat model
        assertEquals(Specification.BASE_OFFENCE_SOURCE, offenceModifiers.iterator().next().getSource());

        Set<Modifier> hillsModifierSet = hills.getDefenceBonus();
        assertFalse(soldier.hasAbility("model.ability.ambushBonus"));
        assertFalse(colonist.hasAbility("model.ability.ambushPenalty"));
        assertEquals(1, hillsModifierSet.size());
        Modifier hillsModifier = hillsModifierSet.iterator().next();

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(soldier, colonist);
        assertEquals(3, defenceModifiers.size());
        assertTrue(defenceModifiers.contains(hillsModifier));
        defenceModifiers.remove(hillsModifier);
        assertTrue(defenceModifiers.contains(fortified));
        defenceModifiers.remove(fortified);
        // this was also added by the combat model
        assertEquals(Specification.BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

    }

    public void testGalleonAttackedByPrivateer() throws Exception {

        Game game = getStandardGame();
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(ocean);
        game.setMap(map);
        FreeColTestCase.spec();
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit galleon = new ServerUnit(game, tile1, dutch, galleonType);
        Unit privateer = new ServerUnit(game, tile2, french, privateerType);

        /**
         * Only base modifiers should apply.
         */
        Set<Modifier> offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        assertEquals(1, offenceModifiers.size());
        assertEquals(Specification.BASE_OFFENCE_SOURCE, offenceModifiers.iterator().next().getSource());

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals(Specification.BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

        /**
         * Fortification should have no effect.
         */
        galleon.setStateUnchecked(Unit.UnitState.FORTIFIED);
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        assertEquals(1, defenceModifiers.size());
        assertEquals(Specification.BASE_DEFENCE_SOURCE, defenceModifiers.iterator().next().getSource());

        /**
         * Penalties due to cargo.
         */
        GoodsType lumberType = spec().getGoodsType("model.goods.lumber");
        Goods goods1 = new Goods(game, null, lumberType, 50);
        privateer.add(goods1);
        offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        Iterator<Modifier> privIt = offenceModifiers.iterator();
        assertEquals(2, offenceModifiers.size());
        assertEquals(Specification.BASE_OFFENCE_SOURCE, privIt.next().getSource());
        Modifier goodsPenalty1 = privIt.next();
        assertEquals(Specification.CARGO_PENALTY_SOURCE, goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

        Goods goods2 = new Goods(game, null, lumberType, 150);
        galleon.add(goods2);
        assertEquals(2, galleon.getVisibleGoodsCount());
        defenceModifiers = combatModel.getDefensiveModifiers(privateer, galleon);
        Iterator<Modifier> gallIt = defenceModifiers.iterator();
        assertEquals(2, defenceModifiers.size());
        assertEquals(Specification.BASE_DEFENCE_SOURCE, gallIt.next().getSource());
        Modifier goodsPenalty2 = gallIt.next();
        assertEquals(Specification.CARGO_PENALTY_SOURCE, goodsPenalty2.getSource());
        assertEquals(-25f, goodsPenalty2.getValue());

        /**
         * Francis Drake
         */
        FoundingFather drake = spec().getFoundingFather("model.foundingFather.francisDrake");
        Set<Modifier> drakeModifiers = drake.getFeatureContainer()
            .getModifierSet("model.modifier.offence", privateerType);
        assertEquals(1, drakeModifiers.size());
        Modifier drakeModifier = drakeModifiers.iterator().next();

        french.addFather(drake);
        drakeModifiers = french.getFeatureContainer().getModifierSet("model.modifier.offence",
                                                                     privateerType);
        assertEquals(1, drakeModifiers.size());
        assertEquals(drakeModifier, drakeModifiers.iterator().next());

        offenceModifiers = combatModel.getOffensiveModifiers(privateer, galleon);
        privIt = offenceModifiers.iterator();
        assertEquals(3, offenceModifiers.size());
        assertEquals(Specification.BASE_OFFENCE_SOURCE, privIt.next().getSource());
        Modifier newDrakeModifier = privIt.next();
        assertEquals(drakeModifier, newDrakeModifier);
        goodsPenalty1 = privIt.next();
        assertEquals(Specification.CARGO_PENALTY_SOURCE, goodsPenalty1.getSource());
        assertEquals(-12.5f, goodsPenalty1.getValue());

        // Verify that the move is correctly interpreted
        assertEquals("Wrong move type", MoveType.ATTACK_UNIT,
                     privateer.getMoveType(tile1));
    }



    public void testDefendColonyWithUnarmedColonist() {
        Game game = getGame();
        Map map = getTestMap(true);
        game.setMap(map);

        Colony colony = getStandardColony();

        @SuppressWarnings("unused")
        SimpleCombatModel combatModel = new SimpleCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new ServerUnit(getGame(), tile2, inca, braveType,
                                       indianHorses, indianMuskets);

        assertEquals(colonist, colony.getDefendingUnit(attacker));
        assertEquals(colonist, colony.getTile().getDefendingUnit(attacker));

        Unit defender = new ServerUnit(getGame(), colony.getTile(), dutch,
                                       colonistType);
        assertFalse("Colonist should not be defensive unit",defender.isDefensiveUnit());
        assertEquals(defender, colony.getTile().getDefendingUnit(attacker));

    }

    public void testDefendColonyWithRevere() {
    	Game game = getGame();
    	Map map = getTestMap(true);
    	game.setMap(map);

        Colony colony = getStandardColony();

        SimpleCombatModel combatModel = new SimpleCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit colonist = colony.getUnitIterator().next();
        Unit attacker = new ServerUnit(getGame(), tile2, inca, braveType, indianHorses, indianMuskets);

        assertEquals(colonist, colony.getDefendingUnit(attacker));

        dutch.addFather(spec().getFoundingFather("model.foundingFather.paulRevere"));
        for (EquipmentType equipment : dragoonEquipment) {
            for (AbstractGoods goods : equipment.getGoodsRequired()) {
                colony.addGoods(goods);
            }
        }

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(attacker, colonist);
        for (Modifier defenceModifier : muskets.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
        for (Modifier defenceModifier : horses.getModifierSet("model.modifier.defence")) {
            assertFalse(defenceModifiers.contains(defenceModifier));
        }
    }

    public void testDefendSettlement() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);

        SimpleCombatModel combatModel = new SimpleCombatModel();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");

        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(inca, true);

        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(inca).settlementTile(tile1).skillToTeach(null).capital(true).build();

        //IndianSettlement settlement = new IndianSettlement(game, inca, tile1, true, null, false, null);
        Unit defender = new ServerUnit(game, settlement, inca, braveType);
        Unit attacker = new ServerUnit(game, tile2, dutch, colonistType, horses, muskets);

        for (EquipmentType equipment : dragoonEquipment) {
            for (AbstractGoods goods : equipment.getGoodsRequired()) {
                settlement.addGoods(goods);
            }
        }

        Set<Modifier> defenceModifiers = combatModel.getDefensiveModifiers(attacker, defender);
        for (Modifier defenceModifier : indianMuskets.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
        for (Modifier defenceModifier : indianHorses.getModifierSet("model.modifier.defence")) {
            assertTrue(defenceModifiers.contains(defenceModifier));
        }
    }

    public void testAttackIgnoresMovementPoints() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setType(hills);
        assertEquals(hills, tile1.getType());

        dutch.setStance(french, Player.Stance.WAR);
        french.setStance(dutch, Player.Stance.WAR);

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setStateUnchecked(Unit.UnitState.FORTIFIED);
        Unit soldier = new ServerUnit(game, tile2, french, veteranType,
                                      muskets, horses);
        soldier.setStateUnchecked(Unit.UnitState.FORTIFIED);

        assertEquals(tile1, colonist.getLocation());
        assertEquals(tile2, soldier.getLocation());

        assertEquals(Unit.MoveType.ATTACK_UNIT,
                     soldier.getMoveType(tile2, tile1, 9));
        assertEquals(Unit.MoveType.ATTACK_UNIT,
                     soldier.getMoveType(tile2, tile1, 1));
        assertEquals(Unit.MoveType.MOVE_NO_MOVES,
                     soldier.getMoveType(tile2, tile1, 0));

    }

    public void testSpanishAgainstNatives() throws Exception {

        Game game = getStandardGame();
        Player spanish = game.getPlayer("model.nation.spanish");
        Player tupi = game.getPlayer("model.nation.tupi");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        tile1.setType(hills);
        assertEquals(hills, tile1.getType());

        spanish.setStance(tupi, Player.Stance.WAR);
        tupi.setStance(spanish, Player.Stance.WAR);

        Unit soldier = new ServerUnit(game, tile1, spanish, colonistType,
                                      muskets);
        Unit brave = new ServerUnit(game, tile2, tupi, braveType);

        assertEquals(tile1, soldier.getLocation());
        assertEquals(tile2, brave.getLocation());

        Set<Modifier> offenceModifiers = combatModel.getOffensiveModifiers(soldier, brave);
        Modifier offenceAgainst = null;
        for (Modifier modifier : offenceModifiers) {
            if (Modifier.OFFENCE_AGAINST.equals(modifier.getId())) {
                offenceAgainst = modifier;
                break;
            }
        }
        assertNotNull(offenceAgainst);
        assertEquals(50, (int) offenceAgainst.getValue());

    }

    public void testAttackShipWithLandUnit() {

        Game game = getStandardGame();
        Player spanish = game.getPlayer("model.nation.spanish");
        Player tupi = game.getPlayer("model.nation.tupi");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        SimpleCombatModel combatModel = new SimpleCombatModel();

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);

        tile1.setType(hills);
        assertEquals(hills, tile1.getType());
        tile2.setType(ocean);
        assertEquals(ocean, tile2.getType());

        spanish.setStance(tupi, Player.Stance.WAR);
        tupi.setStance(spanish, Player.Stance.WAR);

        Unit galleon = new ServerUnit(game, tile2, spanish, galleonType);
        Unit brave = new ServerUnit(game, tile1, tupi, braveType);

        assertEquals(tile1, brave.getLocation());
        assertEquals(tile2, galleon.getLocation());

        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_LAND,
                     galleon.getMoveType(tile2, tile1, 3));

        assertEquals(Unit.MoveType.MOVE_NO_ACCESS_EMBARK,
                     brave.getMoveType(tile1, tile2, 3));

    }


}
