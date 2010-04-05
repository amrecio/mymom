/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import net.sf.freecol.common.model.Limit.Operator;
import net.sf.freecol.common.model.Operand.OperandType;
import net.sf.freecol.common.model.Operand.ScopeLevel;
import net.sf.freecol.util.test.FreeColTestCase;

public class LimitTest extends FreeColTestCase {

    public void testOperand() {

        UnitType carpenter = spec().getUnitType("model.unit.masterCarpenter");
        UnitType frigate = spec().getUnitType("model.unit.frigate");

        Operand operand = new Operand();
        assertEquals(OperandType.NONE, operand.getOperandType());
        assertEquals(ScopeLevel.NONE, operand.getScopeLevel());

        operand.setType("model.unit.frigate");
        assertTrue(operand.appliesTo(frigate));
        assertFalse(operand.appliesTo(carpenter));

    }

    public void testWagonTrainLimit() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(3);
        Building armory = new Building(getGame(), colony, spec().getBuildingType("model.building.armory"));
        colony.addBuilding(armory);

        UnitType wagonTrain = spec().getUnitType("model.unit.wagonTrain");
        UnitType artillery = spec().getUnitType("model.unit.artillery");

        Limit wagonTrainLimit = wagonTrain.getLimits().get(0);

        assertTrue(colony.canBuild(artillery));
        assertFalse(wagonTrainLimit.getLeftHandSide().appliesTo(artillery));

        assertTrue(wagonTrainLimit.evaluate(colony));
        assertTrue(colony.canBuild(wagonTrain));

        Unit wagon = new Unit(game, colony.getTile(), dutch, wagonTrain, Unit.UnitState.ACTIVE);
        assertEquals(Colony.NoBuildReason.LIMIT_EXCEEDED, colony.getNoBuildReason(wagonTrain));
        assertFalse(wagonTrainLimit.evaluate(colony));
        assertFalse(colony.canBuild(wagonTrain));
        assertTrue(colony.canBuild(artillery));

    }

    public void testIndependenceLimit() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Colony colony = getStandardColony(3);
        
        Operand lhs = new Operand();
        lhs.setScopeLevel(ScopeLevel.PLAYER);
        lhs.setMethodName("getSoL");

        Operand rhs = new Operand(50);

        Limit limit = new Limit("model.limit.independence", lhs, Operator.GE, rhs);

        assertFalse(limit.evaluate(dutch));

        colony.incrementLiberty(10000);
        colony.updateSoL();
        assertTrue(limit.evaluate(dutch));

    }


}