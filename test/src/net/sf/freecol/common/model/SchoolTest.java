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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.FreeColTestUtils.ColonyBuilder;


public class SchoolTest extends FreeColTestCase {

    private enum SchoolLevel { SCHOOLHOUSE, COLLEGE, UNIVERSITY };

    private static final BuildingType schoolType
        = spec().getBuildingType("model.building.schoolhouse");
    private static final BuildingType collegeType
        = spec().getBuildingType("model.building.college");
    private static final BuildingType universityType
        = spec().getBuildingType("model.building.university");

    private static final UnitType colonialRegularType
        = spec().getUnitType("model.unit.colonialRegular");
    private static final UnitType elderStatesmanType
        = spec().getUnitType("model.unit.elderStatesman");
    private static final UnitType expertLumberJackType
        = spec().getUnitType("model.unit.expertLumberJack");
    private static final UnitType expertOreMinerType
        = spec().getUnitType("model.unit.expertOreMiner");
    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType indenturedServantType
        = spec().getUnitType("model.unit.indenturedServant");
    private static final UnitType pettyCriminalType
        = spec().getUnitType("model.unit.pettyCriminal");
    private static final UnitType masterBlacksmithType
        = spec().getUnitType("model.unit.masterBlacksmith");
    private static final UnitType masterCarpenterType
        = spec().getUnitType("model.unit.masterCarpenter");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");


    private Building addSchoolToColony(Game game, Colony colony,
                                       SchoolLevel level) {
        BuildingType type = null;;
        switch (level) {
        case SCHOOLHOUSE:
            type = schoolType;
            break;
        case COLLEGE:
            type = collegeType;
            break;
        case UNIVERSITY:
            type = universityType;
            break;
        default:
            fail("Setup error, cannot setup school");
        }
        colony.addBuilding(new ServerBuilding(game, colony, type));
        return colony.getBuilding(type);
    }

    /**
     * Returns a list of all units in this colony of the given type.
     *
     * @param type The type of the units to include in the list. For instance
     *            Unit.EXPERT_FARMER.
     * @return A list of all the units of the given type in this colony.
     */
    private List<Unit> getUnitList(Colony colony, UnitType type) {
        List<Unit> units = new ArrayList<Unit>() ;
        for (Unit unit : colony.getUnitList()) {
            if (type.equals(unit.getType())) {
                units.add(unit);
            }
        }
        return units;
    }

    public void testUpgrades() {
        assertEquals(Unit.getUnitTypeTeaching(masterCarpenterType,
                                              freeColonistType),
                     masterCarpenterType);
        assertEquals(Unit.getUnitTypeTeaching(masterCarpenterType,
                                              indenturedServantType),
                     freeColonistType);
        assertEquals(Unit.getUnitTypeTeaching(masterCarpenterType,
                                              pettyCriminalType),
                     indenturedServantType);
    }

    public void testEducationOption() {
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");

        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony(5);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit lumberJack = units.next();
        lumberJack.setType(expertLumberJackType);
        Unit criminal1 = units.next();
        criminal1.setType(pettyCriminalType);
        Unit criminal2 = units.next();
        criminal2.setType(pettyCriminalType);
        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);
        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);

        colony.addBuilding(new ServerBuilding(getGame(), colony, schoolType));
        Building school = colony.getBuilding(schoolType);
        assertTrue(school.canTeach());
        assertTrue(colony.canTrain(lumberJack));
        assertTrue(spec().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION));
        lumberJack.setLocation(school);

        colonist1.setWorkType(cotton);
        colonist2.setWorkType(lumber);
        assertEquals(cotton, colonist1.getWorkType());
        assertEquals(expertLumberJackType.getExpertProduction(), colonist2.getWorkType());
        assertEquals(null, colony.findStudent(lumberJack));

        lumberJack.setStudent(null);
        colonist2.setTeacher(null);

        spec().getBooleanOption(GameOptions.ALLOW_STUDENT_SELECTION).setValue(false);
        criminal1.setWorkType(cotton);
        criminal2.setWorkType(lumber);
        assertEquals(criminal2, colony.findStudent(lumberJack));

    }

    public void testChangeTeachers(){
        Game game = getGame();
        game.setMap(getTestMap());

        // Setup
        ColonyBuilder colBuilder = FreeColTestUtils.getColonyBuilder();
        colBuilder.initialColonists(3).addColonist(expertLumberJackType)
            .addColonist(expertLumberJackType);
        Colony colony = colBuilder.build();
        Building school = addSchoolToColony(game, colony, SchoolLevel.COLLEGE);

        Unit student = getUnitList(colony, freeColonistType).get(0);
        List<Unit> teacherList = getUnitList(colony, expertLumberJackType);
        Unit teacher1 = teacherList.get(0);
        Unit teacher2 = teacherList.get(1);
        assertNull("Teacher1 should not have a student yet",
                   teacher1.getStudent());
        assertNull("Teacher2 should not have a student yet",
                   teacher2.getStudent());

        // add first teacher
        school.add(teacher1);
        assertEquals("Teacher1 should now have a student",
                     teacher1.getStudent(), student);
        assertEquals("Student should have assigned teacher1",
                     student.getTeacher(), teacher1);

        // add a second teacher
        school.add(teacher2);
        assertEquals("Teacher1 should still have a student",
                     teacher1.getStudent(), student);
        assertNull("Teacher2 should not have a student yet",
                   teacher2.getStudent());
        assertEquals("Student should have assigned teacher1",
                     student.getTeacher(), teacher1);

        // change teacher
        student.setTeacher(teacher2);
        assertNull("Teacher1 should not have a student now",
                   teacher1.getStudent());
        assertEquals("Teacher2 should now have a student",
                     teacher2.getStudent(), student);
        assertEquals("Student should have assigned teacher2",
                     student.getTeacher(), teacher2);
    }
}
