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

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.FreeColTestUtils.ColonyBuilder;

public class SchoolTest extends FreeColTestCase {
	
	private enum SchoolLevel { SCHOOLHOUSE, COLLEGE, UNIVERSITY };

    private UnitType freeColonistType = spec().getUnitType("model.unit.freeColonist");
    private UnitType indenturedServantType = spec().getUnitType("model.unit.indenturedServant");
    private UnitType pettyCriminalType = spec().getUnitType("model.unit.pettyCriminal");
    private UnitType expertOreMinerType = spec().getUnitType("model.unit.expertOreMiner");
    private UnitType expertLumberJackType = spec().getUnitType("model.unit.expertLumberJack");
    private UnitType masterCarpenterType = spec().getUnitType("model.unit.masterCarpenter");
    private UnitType masterBlacksmithType = spec().getUnitType("model.unit.masterBlacksmith");
    private UnitType veteranSoldierType = spec().getUnitType("model.unit.veteranSoldier");
    private UnitType elderStatesmanType = spec().getUnitType("model.unit.elderStatesman");
    private UnitType colonialRegularType = spec().getUnitType("model.unit.colonialRegular");


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

    private void trainForTurns(Colony colony, int requiredTurns) {
        trainForTurns(colony, requiredTurns, freeColonistType);
    }

    private void trainForTurns(Colony colony, int requiredTurns, UnitType unitType) {
        for (int turn = 0; turn < requiredTurns; turn++) {
           /* assertEquals("wrong number of units in turn " + turn + ": " + unitType,
                         1, getUnitList(colony, unitType).size()); */
            colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse")).newTurn();
        }
    }
    
    private Building addSchoolToColony(Game game, Colony colony, SchoolLevel level){
    	BuildingType schoolType = null;;
        switch(level){
        	case SCHOOLHOUSE:
        		schoolType = spec().getBuildingType("model.building.Schoolhouse");
        		break;
        	case COLLEGE:
        		schoolType = spec().getBuildingType("model.building.College");
        		break;
        	case UNIVERSITY:
        		schoolType = spec().getBuildingType("model.building.University");
        		break;
        	default:
        		fail("Setup error, cannot setup school");
        }
        colony.addBuilding(new Building(game, colony, schoolType));
        return colony.getBuilding(schoolType);
    }

    private void setProductionBonus(Colony colony, int value) {
        try {
            Field productionBonus = Colony.class.getDeclaredField("productionBonus");
            productionBonus.setAccessible(true);
            productionBonus.setInt(colony, value);
        } catch (Exception e) {
            // do nothing
        }
    }


    BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
    

    public void testUpgrades() {

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        UnitType servant = spec().getUnitType("model.unit.indenturedServant");
        UnitType criminal = spec().getUnitType("model.unit.pettyCriminal");
        UnitType carpenter = spec().getUnitType("model.unit.masterCarpenter");

        assertEquals(Unit.getUnitTypeTeaching(carpenter, colonist), carpenter);
        assertEquals(Unit.getUnitTypeTeaching(carpenter, servant), colonist);
        assertEquals(Unit.getUnitTypeTeaching(carpenter, criminal), servant);
    }

    /**
     * Check that a free colonist can be taught something.
     * 
     */
    public void testExpertTeaching() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony(4);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit black = units.next();
        black.setType(masterBlacksmithType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(game, colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        assertTrue(schoolType.hasAbility("model.ability.teach"));
        assertTrue(colony.canTrain(ore));

        ore.setLocation(school);
        trainForTurns(colony, ore.getNeededTurnsOfTraining());
        assertEquals(expertOreMinerType, colonist.getType());
        colony.dispose();
    }

    public void testCollege() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getStandardColony(8);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit blackSmith = units.next();
        blackSmith.setType(masterBlacksmithType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(game, colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();

        blackSmith.setLocation(school);
        trainForTurns(colony, blackSmith.getNeededTurnsOfTraining());
        assertEquals(masterBlacksmithType, colonist.getType());
        colony.dispose();
    }

    public void testUniversity() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getStandardColony(10);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit elder = units.next();
        elder.setType(elderStatesmanType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(game, colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();
        school.upgrade();

        elder.setLocation(school);
        trainForTurns(colony, elder.getNeededTurnsOfTraining());
        assertEquals(elderStatesmanType, colonist.getType());
        colony.dispose();
    }

    /**
     * [ 1616384 ] Teaching
     * 
     * One LumberJack and one BlackSmith in a college. 4 Free Colonists, one as
     * LumberJack, one as BlackSmith two as Farmers.
     * 
     * After some turns (2 or 3 I don't know) a new LumberJack is ready.
     * Removing the teacher LumberJack replaced by an Ore Miner.
     * 
     * Next turn, a new BlackSmith id ready. Removing the teacher BlackSmith
     * replaced by a Veteran Soldier. There is still 2 Free Colonists as Farmers
     * in the Colony.
     * 
     * Waiting during more than 8 turns. NOTHING happens.
     * 
     * Changing the two Free Colonists by two other Free Colonists.
     * 
     * After 2 or 3 turns, a new Ore Miner and a new Veteran Soldier are ready.
     * 
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1616384&group_id=43225&atid=435578
     * 
     * CO: I think this is a special case of the testSingleGuyTwoTeachers. But
     * since already the TwoTeachersSimple case fails, I think that needs to be
     * sorted out first.
     */
    public void testTrackerBug1616384() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getStandardColony(8);

        // Setting the stage...
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);

        Unit colonist3 = units.next();
        colonist3.setType(freeColonistType);

        Unit colonist4 = units.next();
        colonist4.setType(freeColonistType);

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);

        Unit blacksmith = units.next();
        blacksmith.setType(masterBlacksmithType);

        Unit veteran = units.next();
        veteran.setType(veteranSoldierType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        // Build a college...
        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();

        blacksmith.setLocation(school);
        lumberjack.setLocation(school);

        // It should not take more than 15 turns (my guess) to get the whole
        // story over with.
        int maxTurns = 15;

        while (4 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(3, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());

        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD, true));
        ore.setLocation(school);

        while (3 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(2, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());

        blacksmith.setLocation(colony.getVacantColonyTileFor(blacksmith, Goods.FOOD, true));
        veteran.setLocation(school);

        while (2 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        ore.setLocation(colony.getVacantColonyTileFor(ore, Goods.FOOD, true));

        while (1 == getUnitList(colony, freeColonistType).size() && maxTurns-- > 0) {
            school.newTurn();
        }
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, veteranSoldierType).size());
        colony.dispose();

    }

    public void testTwoTeachersSimple() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getStandardColony(10);
        setProductionBonus(colony, 0);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit colonist2 = units.next();
        colonist2.setType(freeColonistType);

        Unit colonist3 = units.next();
        colonist3.setType(freeColonistType);

        Unit colonist4 = units.next();
        colonist4.setType(freeColonistType);

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit black = units.next();
        black.setType(masterBlacksmithType);

        Unit veteran = units.next();
        veteran.setType(veteranSoldierType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(game, colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();
        school.upgrade();

        black.setLocation(school);
        ore.setLocation(school);

        assertEquals(6, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(6, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(5, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(5, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(4, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        colony.dispose();
    }

    /**
     * Return a colony with a university and 10 elder statesmen
     * @return
     */
    public Colony getUniversityColony(){
        Colony colony = getStandardColony(10);

        for (Unit u : colony.getUnitList()){
            u.setType(elderStatesmanType);
        }

        BuildingType schoolType = spec().getBuildingType("model.building.University");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        return colony;
    }
    
    /**
     * If there are two teachers, but just one colonist to be taught.
     */
    public void testSingleGuyTwoTeachers() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit black = units.next();
        black.setType(masterBlacksmithType);

        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));

        // It should take 4 turns to train an expert lumber jack and 6 to train
        // a blacksmith
        // The lumber jack chould be finished teaching first.
        // But the school works for now as first come first serve
        black.setLocation(school);
        lumber.setLocation(school);

        trainForTurns(colony, black.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(2, getUnitList(colony, masterBlacksmithType).size());
        colony.dispose();
    }

    /**
     * If there are two teachers of the same kind, but just one colonist to be
     * taught, this should not mean any speed up.
     */
    public void testTwoTeachersOfSameKind() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit lumberjack1 = units.next();
        lumberjack1.setType(expertLumberJackType);

        Unit lumberjack2 = units.next();
        lumberjack2.setType(expertLumberJackType);

        lumberjack1.setLocation(school);
        lumberjack2.setLocation(school);

        trainForTurns(colony, lumberjack1.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(3, getUnitList(colony, expertLumberJackType).size());
        colony.dispose();
    }

    /**
     * If there are two teachers with the same skill level, the first to be put
     * in the school should be used for teaching.
     * 
     */
    public void testSingleGuyTwoTeachers2() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);

        Unit ore = units.next();
        ore.setType(expertOreMinerType);

        // It should take 3 turns to train an expert lumber jack and also 3 to
        // train a ore miner
        // First come first serve, the lumber jack wins.
        lumber.setLocation(school);
        ore.setLocation(school);

        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());

        school.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        colony.dispose();
    }

    /**
     * Test that an petty criminal becomes an indentured servant
     */
    public void testTeachPettyCriminals() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);

        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);

        teacher.setLocation(school);
        assertTrue(criminal.canBeStudent(teacher));

        // PETTY_CRIMINALS become INDENTURED_SERVANTS
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), pettyCriminalType);
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(indenturedServantType, criminal.getType());
        colony.dispose();
    }

    /**
     * The time to teach somebody does not depend on the one who is being
     * taught, but on the teacher.
     */
    public void testTeachPettyCriminalsByMaster() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);

        Unit teacher = units.next();
        teacher.setType(masterBlacksmithType);

        teacher.setLocation(school);

        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), pettyCriminalType);
        assertEquals(indenturedServantType, criminal.getType());
        colony.dispose();
    }

    /**
     * Test that an indentured servant becomes a free colonist
     * 
     */
    public void testTeachIndenturedServants() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);

        Unit teacher = units.next();
        teacher.setType(masterBlacksmithType);

        teacher.setLocation(school);
        assertEquals(teacher.getNeededTurnsOfTraining(), 4);
        trainForTurns(colony, teacher.getNeededTurnsOfTraining(), indenturedServantType);
        // Train to become free colonist
        assertEquals(freeColonistType, indenturedServant.getType());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving students around does not slow education. This behavior is 
     * there to simplify gameplay.
     */
    public void testTeacherStoresProgress() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony outsideColony = getStandardColony(1, 10, 8);
        Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
        Unit outsider = outsideUnits.next();
        outsider.setType(freeColonistType);
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit student = units.next();
        student.setType(freeColonistType);
        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);

        
        teacher.setLocation(school);

        // Train to become free colonist
        trainForTurns(colony, teacher.getNeededTurnsOfTraining() - 1);

        // We swap the colonist with another one
        student.setLocation(outsideColony);
        outsider.setLocation(colony);

        assertEquals(1, getUnitList(colony, freeColonistType).size());
        school.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(expertOreMinerType, outsider.getType());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving a teacher inside the colony should not reset its training.
     */
    public void testMoveTeacherInside() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        
        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);
        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // The ore miner is set in the school before the carpenter (note: the
        // carpenter is the only master of skill level 1).
        // In this case, the colonist will become a miner (and the criminal 
        // will become a servant).
        teacher1.setLocation(school);
        assertEquals(4, teacher1.getNeededTurnsOfTraining());
        teacher2.setLocation(school);
        assertEquals(4, teacher2.getNeededTurnsOfTraining());

        // wait a little
        school.newTurn();
        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        // Now we want the colonist to be a carpenter. We just want to 
        // shuffle the teachers.
        teacher2.setLocation(colony.getVacantColonyTileFor(teacher2, Goods.FOOD, true));
        // outside the colony is still considered OK (same Tile)
        teacher1.putOutsideColony();

        assertNull(teacher1.getStudent());
        assertNull(teacher2.getStudent());

        // Passing a turn outside school does not reset training at this time
        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());

        // Move teacher2 back to school
        teacher2.setLocation(school);

        school.newTurn();
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(3, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        
        // Move teacher1 back to school
        teacher1.setLocation(school);
        setProductionBonus(colony, 0);

        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());

        // Teacher1's student (petty criminal) should still be a petty criminal
        // Teacher2's student (free colonist) should have been promoted to master carpenter
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(2, getUnitList(colony, masterCarpenterType).size());

        school.newTurn();
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(null, teacher2.getStudent());

        // Teacher1's student (petty criminal) should have been promoted to indentured servant
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(2, getUnitList(colony, masterCarpenterType).size());
        
        /**
         * Since teacher1 can continue teaching his student, there
         * is no reason to shuffle teachers.
         */
        school.newTurn();
        assertEquals(indenturedServantType, teacher1.getStudent().getType());

    }
    
    public void testCaseTwoTeachersWithDifferentExp(){
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);
        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // First we let the teacher1 train for 3 turns
        teacher1.setLocation(school);
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        
        // Then teacher2 for 1 turn
        teacher1.setLocation(colony.getVacantColonyTileFor(teacher1, Goods.FOOD, true));
        teacher2.setLocation(school);
        school.newTurn();
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(1, teacher2.getTurnsOfTraining());
        
        // If we now also add teacher2 to the school, then 
        // Teacher1 will still be the teacher in charge
        teacher1.setLocation(school);
        school.newTurn();
        
        assertEquals(3, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
    }

    /**
     * Progress in teaching is bound to the teacher and not the learner.
     * 
     * Moving a teacher outside the colony should reset its training.
     */
    public void testMoveTeacherOutside() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony outsideColony = getStandardColony(1, 10, 8);
        Iterator<Unit> outsideUnits = outsideColony.getUnitIterator();
        Unit outsider = outsideUnits.next();
        outsider.setType(freeColonistType);

        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();
        Unit colonist = units.next();
        colonist.setType(freeColonistType);
        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);
        
        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);
        Unit teacher2 = units.next();
        teacher2.setType(masterCarpenterType);

        // The ore miner is set in the school before the carpenter (note: the
        // carpenter is the only master of skill level 1).
        // In this case, the colonist will become a miner (and the criminal 
        // will become a servant).
        teacher1.setLocation(school);
        teacher2.setLocation(school);

        // wait a little
        school.newTurn();
        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());
        assertEquals(2, teacher1.getTurnsOfTraining());
        assertEquals(2, teacher2.getTurnsOfTraining());
        
        // Now we move the teachers somewhere else
        teacher1.setLocation(getGame().getMap().getTile(6, 8));
        teacher2.setLocation(outsideColony.getVacantColonyTileFor(teacher2, Goods.FOOD, true));
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(0, getUnitList(colony, expertOreMinerType).size());
        assertEquals(0, getUnitList(colony, masterCarpenterType).size());
        
        // Put them back here
        teacher1.setLocation(school);
        teacher2.setLocation(school);
        assertEquals(0, teacher1.getTurnsOfTraining());
        assertEquals(0, teacher2.getTurnsOfTraining());

        setProductionBonus(colony, 0);
        
        // Check that 2 new turns aren't enough for training
        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        school.newTurn();
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());

        school.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, masterCarpenterType).size());
    }

    /**
     * Sons of Liberty should not influence teaching.
     */
    public void testSonsOfLiberty() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        colony.addGoods(Goods.BELLS, 10000);
        colony.newTurn();

        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist1 = units.next();
        colonist1.setType(freeColonistType);

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);

        lumberjack.setLocation(school);
        trainForTurns(colony, lumberjack.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertLumberJackType).size());
    }

    /**
     * Trains partly one colonist then put another teacher.
     * 
     * Should not save progress but start all over.
     */
    public void testPartTraining() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit lumberjack = units.next();
        lumberjack.setType(expertLumberJackType);

        Unit miner = units.next();
        miner.setType(expertOreMinerType);

        // Put LumberJack in School
        lumberjack.setLocation(school);
        assertTrue(lumberjack.getStudent() == colonist);
        assertTrue(colonist.getTeacher() == lumberjack);
        trainForTurns(colony, 2);

        // After 2 turns replace by miner. Progress starts from scratch.
        lumberjack.setLocation(colony.getVacantColonyTileFor(lumberjack, Goods.FOOD, true));
        assertTrue(lumberjack.getStudent() == null);
        assertTrue(colonist.getTeacher() == null);

        miner.setLocation(school);
        assertTrue(miner.getStudent() == colonist);
        assertTrue(colonist.getTeacher() == miner);
        trainForTurns(colony, miner.getNeededTurnsOfTraining());
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        colony.dispose();
    }

    /**
     * Test that free colonists are trained before indentured servants, which
     * are preferred to petty criminals.
     * 
     */
    public void testTeachingOrder() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));

        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit colonist = units.next();
        colonist.setType(freeColonistType);

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);

        Unit teacher = units.next();
        teacher.setType(expertOreMinerType);
        teacher.setLocation(school);

        assertTrue(colonist.canBeStudent(teacher));
        assertTrue(indenturedServant.canBeStudent(teacher));
        assertTrue(criminal.canBeStudent(teacher));

        // Colonist training
        assertEquals(teacher, colonist.getTeacher());
        assertEquals(colonist, teacher.getStudent());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(expertOreMinerType, colonist.getType());
        assertEquals(teacher.getStudent(), indenturedServant);
        assertEquals(colonist.getTeacher(), null);
        assertEquals(indenturedServant.getTeacher(), teacher);

        // Servant training
        school.newTurn();
        assertEquals(teacher, indenturedServant.getTeacher());
        assertEquals(indenturedServant, teacher.getStudent());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(freeColonistType, indenturedServant.getType());
        assertEquals(indenturedServant, teacher.getStudent());
        // remove servant from colony
        indenturedServant.setLocation(getGame().getMap().getTile(10,8));

        setProductionBonus(colony, 0);

        // Criminal training
        assertEquals(0, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        school.newTurn();
        assertEquals(teacher, criminal.getTeacher());
        assertEquals(criminal, teacher.getStudent());
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(indenturedServantType, criminal.getType());
    }

    /**
     * Test that an indentured servant cannot be promoted to free colonist and
     * learn a skill at the same time.
     */
    public void testTeachingDoublePromotion() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
        
        Colony colony = getUniversityColony();
        setProductionBonus(colony, 0);
        Building school = colony.getBuilding(schoolType);
        
        Iterator<Unit> units = colony.getUnitIterator();

        Unit indenturedServant = units.next();
        indenturedServant.setType(indenturedServantType);

        Unit criminal = units.next();
        criminal.setType(pettyCriminalType);

        Unit teacher1 = units.next();
        teacher1.setType(expertOreMinerType);

        Unit teacher2 = units.next();
        teacher2.setType(expertLumberJackType);

        // set location only AFTER all types have been set!
        teacher1.setLocation(school);
        teacher2.setLocation(school);

        // Training time
        trainForTurns(colony, teacher1.getNeededTurnsOfTraining(), pettyCriminalType);

        // indentured servant should have been promoted to free colonist
        // petty criminal should have been promoted to indentured servant
        assertEquals(1, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(1, getUnitList(colony, indenturedServantType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(freeColonistType, indenturedServant.getType());
        assertEquals(indenturedServantType, criminal.getType());
        
        // Train again
        school.newTurn();
        school.newTurn();
        school.newTurn();
        school.newTurn();
        assertEquals(2, getUnitList(colony, expertOreMinerType).size());
        assertEquals(1, getUnitList(colony, expertLumberJackType).size());
        assertEquals(1, getUnitList(colony, freeColonistType).size());
        assertEquals(0, getUnitList(colony, indenturedServantType).size());
        assertEquals(0, getUnitList(colony, pettyCriminalType).size());
        assertEquals(expertOreMinerType, indenturedServant.getType());
        assertEquals(freeColonistType, criminal.getType());
    }

    public void testColonialRegular() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));

        Colony colony = getStandardColony(10);
        setProductionBonus(colony, 0);
        Player owner = colony.getOwner();
        owner.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));

        Iterator<Unit> units = colony.getUnitIterator();

        Unit regular = units.next();
        regular.setType(colonialRegularType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        school.upgrade();
        school.upgrade();

        regular.setLocation(school);
        Unit student = regular.getStudent();
        assertEquals(freeColonistType, student.getType());

        trainForTurns(colony, freeColonistType.getEducationTurns(veteranSoldierType));
        assertEquals(veteranSoldierType, student.getType());

        colony.dispose();
    }

    public void testConcurrentUpgrade() {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getStandardColony(2);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit lumber = units.next();
        lumber.setType(expertLumberJackType);
        Unit student = units.next();
        student.setType(pettyCriminalType);

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        assertTrue(schoolType.hasAbility("model.ability.teach"));
        assertTrue(colony.canTrain(lumber));

        lumber.setLocation(school);
        school.newTurn();
        assertEquals(student, lumber.getStudent());

        // lumber jack can teach indentured servant
        student.setType(indenturedServantType);
        assertEquals(student, lumber.getStudent());

        // lumber jack can teach free colonist
        student.setType(freeColonistType);
        assertEquals(student, lumber.getStudent());

        // lumber jack can not teach expert
        student.setType(masterCarpenterType);
        assertNull(lumber.getStudent());
        assertNull(student.getTeacher());

        colony.dispose();
    }

    public void testEducationOption() {

        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");

    	Game game = getGame();
    	game.setMap(getTestMap(plainsType, true));
    	
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

        BuildingType schoolType = spec().getBuildingType("model.building.Schoolhouse");
        colony.addBuilding(new Building(getGame(), colony, schoolType));
        Building school = colony.getBuilding(spec().getBuildingType("model.building.Schoolhouse"));
        assertTrue(schoolType.hasAbility("model.ability.teach"));
        assertTrue(colony.canTrain(lumberJack));
        assertFalse(game.getGameOptions().getBoolean(GameOptions.EDUCATE_LEAST_SKILLED_UNIT_FIRST));
        lumberJack.setLocation(school);

        colonist1.setWorkType(cotton);
        colonist2.setWorkType(lumber);
        assertEquals(cotton, colonist1.getWorkType());
        assertEquals(expertLumberJackType.getExpertProduction(), colonist2.getWorkType());
        assertEquals(colonist2, school.findStudent(lumberJack));

        lumberJack.setStudent(null);
        colonist2.setTeacher(null);

        ((BooleanOption) game.getGameOptions().getObject(GameOptions.EDUCATE_LEAST_SKILLED_UNIT_FIRST))
        .setValue(true);
        criminal1.setWorkType(cotton);
        criminal2.setWorkType(lumber);
        assertEquals(criminal2, school.findStudent(lumberJack));

    }

    public void testProductionBonus() {

    	Game game = getGame();
    	game.setMap(getTestMap(plainsType, true));
        
        Colony colony = getUniversityColony();
        Building school = colony.getBuilding(schoolType);

        Iterator<Unit> units = colony.getUnitIterator();

        Unit carpenter = units.next();
        carpenter.setType(masterCarpenterType);
        carpenter.setLocation(school);

        Unit blacksmith = units.next();
        blacksmith.setType(masterBlacksmithType);
        blacksmith.setLocation(school);

        Unit statesman = units.next();
        statesman.setType(elderStatesmanType);
        statesman.setLocation(school);

        units.next().setType(freeColonistType);
        units.next().setType(freeColonistType);
        units.next().setType(freeColonistType);

        school.newTurn();

        for (int bonus = -2; bonus < 3; bonus++) {
            setProductionBonus(colony, bonus);
            assertEquals(4 - bonus, carpenter.getNeededTurnsOfTraining());
            assertEquals(6 - bonus, blacksmith.getNeededTurnsOfTraining());
            assertEquals(8 - bonus, statesman.getNeededTurnsOfTraining());
        }
    }
    
    public void testChangeTeachers(){
    	Game game = getGame();
    	game.setMap(getTestMap());

    	// Setup
    	ColonyBuilder colBuilder = FreeColTestUtils.getColonyBuilder();
    	colBuilder.initialColonists(3).addColonist(expertLumberJackType).addColonist(expertLumberJackType);
    	Colony colony = colBuilder.build();
    	Building school = addSchoolToColony(game, colony, SchoolLevel.COLLEGE);
    
    	Unit student = getUnitList(colony, freeColonistType).get(0);
    	List<Unit> teacherList = getUnitList(colony, expertLumberJackType);
    	Unit teacher1 = teacherList.get(0);
    	Unit teacher2 = teacherList.get(1);
    	assertTrue("Teacher1 should not have a student yet",teacher1.getStudent() == null);
    	assertTrue("Teacher2 should not have a student yet",teacher2.getStudent() == null);
    	// add first teacher
    	school.add(teacher1);
    	assertTrue("Teacher1 should now have a student",teacher1.getStudent() == student);
    	assertTrue("Student should have assigned teacher1",student.getTeacher() == teacher1);
    	// add a second teacher
    	school.add(teacher2);
    	assertTrue("Teacher1 should still have a student",teacher1.getStudent() == student);
    	assertTrue("Teacher2 should not have a student yet",teacher2.getStudent() == null);
    	assertTrue("Student should have assigned teacher1",student.getTeacher() == teacher1);
    	// change teacher
    	student.setTeacher(teacher2);
    	assertTrue("Teacher1 should not have a student now",teacher1.getStudent() == null);
    	assertTrue("Teacher2 should now have a student",teacher2.getStudent() == student);
    	assertTrue("Student should have assigned teacher2",student.getTeacher() == teacher2);
    }
}
