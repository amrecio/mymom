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

import java.util.Iterator;

import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class PlayerTest extends FreeColTestCase {
    UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");

    public void testUnits() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(spec().getTileType("model.tile.plains"));
        game.setMap(map);
        map.getTile(4, 7).setExploredBy(dutch, true);
        map.getTile(4, 8).setExploredBy(dutch, true);
        map.getTile(5, 7).setExploredBy(dutch, true);
        map.getTile(5, 8).setExploredBy(dutch, true);

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");

        Unit unit1 = new Unit(game, map.getTile(4, 7), dutch, freeColonist, UnitState.ACTIVE);
        Unit unit2 = new Unit(game, map.getTile(4, 8), dutch, freeColonist, UnitState.ACTIVE);
        Unit unit3 = new Unit(game, map.getTile(5, 7), dutch, freeColonist, UnitState.ACTIVE);
        Unit unit4 = new Unit(game, map.getTile(5, 8), dutch, freeColonist, UnitState.ACTIVE);

        int count = 0;
        Iterator<Unit> unitIterator = dutch.getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next();
            count++;
        }
        assertTrue(count == 4);

        assertTrue(dutch.getUnit(unit1.getId()) == unit1);
        assertTrue(dutch.getUnit(unit2.getId()) == unit2);
        assertTrue(dutch.getUnit(unit3.getId()) == unit3);
        assertTrue(dutch.getUnit(unit4.getId()) == unit4);

        String id = unit1.getId();
        unit1.dispose();
        assertTrue(dutch.getUnit(id) == null);

        unit2.setOwner(french);
        assertTrue(dutch.getUnit(unit2.getId()) == null);
        assertTrue(french.getUnit(unit2.getId()) == unit2);

    }
    
    public void testEuropeanPlayer(Player player) {
        assertTrue(player.canBuildColonies());
        assertTrue(player.canHaveFoundingFathers());
        assertTrue(player.canMoveToEurope());
        assertTrue(player.canRecruitUnits());
        assertEquals(player.getPlayerType(), Player.PlayerType.COLONIAL);
        assertFalse(player.isDead());
        assertTrue(player.isEuropean());
        assertFalse(player.isIndian());
        assertFalse(player.isREF());
    }
    
    public void testIndianPlayer(Player player) {
        assertFalse(player.canBuildColonies());
        assertFalse(player.canHaveFoundingFathers());
        assertFalse(player.canMoveToEurope());
        assertFalse(player.canRecruitUnits());
        assertEquals(player.getPlayerType(), Player.PlayerType.NATIVE);
        assertFalse(player.isDead());
        assertFalse(player.isEuropean());
        assertTrue(player.isIndian());
        assertFalse(player.isREF());
    }
    
    public void testRoyalPlayer(Player player) {
        assertFalse(player.canBuildColonies());
        assertFalse(player.canHaveFoundingFathers());
        assertTrue(player.canMoveToEurope());
        assertFalse(player.canRecruitUnits());
        assertEquals(player.getPlayerType(), Player.PlayerType.ROYAL);
        assertFalse(player.isDead());
        assertTrue(player.isEuropean());
        assertFalse(player.isIndian());
        assertTrue(player.isREF());
    }
    
    public void testPlayers() {
        Game game = getStandardGame();
        
        // europeans
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Player english = game.getPlayer("model.nation.english");
        Player spanish = game.getPlayer("model.nation.spanish");
        Player portuguese = game.getPlayer("model.nation.portuguese");
        Player swedish = game.getPlayer("model.nation.swedish");
        Player danish = game.getPlayer("model.nation.danish");
        Player russian = game.getPlayer("model.nation.russian");
        testEuropeanPlayer(dutch);
        testEuropeanPlayer(french);
        testEuropeanPlayer(english);
        testEuropeanPlayer(spanish);
        //testEuropeanPlayer(portuguese);
        //testEuropeanPlayer(swedish);
        //testEuropeanPlayer(danish);
        //testEuropeanPlayer(russian);
        
        // indians
        Player inca = game.getPlayer("model.nation.inca");
        Player aztec = game.getPlayer("model.nation.aztec");
        Player arawak = game.getPlayer("model.nation.arawak");
        Player cherokee = game.getPlayer("model.nation.cherokee");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Player sioux = game.getPlayer("model.nation.sioux");
        Player apache = game.getPlayer("model.nation.apache");
        Player tupi = game.getPlayer("model.nation.tupi");
        testIndianPlayer(inca);
        testIndianPlayer(aztec);
        testIndianPlayer(arawak);
        testIndianPlayer(cherokee);
        testIndianPlayer(iroquois);
        testIndianPlayer(sioux);
        testIndianPlayer(apache);
        testIndianPlayer(tupi);
        
        // royal
        /* this works differently now
        Player dutchREF = game.getPlayer("model.nation.dutchREF");
        Player frenchREF = game.getPlayer("model.nation.frenchREF");
        Player englishREF = game.getPlayer("model.nation.englishREF");
        Player spanishREF = game.getPlayer("model.nation.spanishREF");
        Player portugueseREF = game.getPlayer("model.nation.portugueseREF");
        Player swedishREF = game.getPlayer("model.nation.swedishREF");
        Player danishREF = game.getPlayer("model.nation.danishREF");
        Player russianREF = game.getPlayer("model.nation.russianREF");
        testRoyalPlayer(dutchREF);
        testRoyalPlayer(frenchREF);
        testRoyalPlayer(englishREF);
        testRoyalPlayer(spanishREF);
        testRoyalPlayer(portugueseREF);
        testRoyalPlayer(swedishREF);
        testRoyalPlayer(danishREF);
        testRoyalPlayer(russianREF);
        assertEquals(dutchREF, dutch.getREFPlayer());
        assertEquals(frenchREF, french.getREFPlayer());
        assertEquals(englishREF, english.getREFPlayer());
        assertEquals(spanishREF, spanish.getREFPlayer());
        assertEquals(portugueseREF, portuguese.getREFPlayer());
        assertEquals(swedishREF, swedish.getREFPlayer());
        assertEquals(danishREF, danish.getREFPlayer());
        assertEquals(russianREF, russian.getREFPlayer());
        */
    }

    public void testDeclarationOfWarFromPeace(){
    	String errMsg = "";
    	Game game = getStandardGame();
        
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        
        int initialTensionValue = 500;
        
        // setup
        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.PEACE);
        dutch.setTension(french, new Tension(initialTensionValue));
        french.setTension(dutch, new Tension(initialTensionValue));
        
        // verify initial conditions
        int initialDutchTension = dutch.getTension(french).getValue();
        int initialFrenchTension = french.getTension(dutch).getValue();
        
        errMsg ="The Dutch must be at peace with the French";
        assertEquals(errMsg,Stance.PEACE,dutch.getStance(french));
        errMsg ="The French must be at peace with the Dutch";
        assertEquals(errMsg,Stance.PEACE,french.getStance(dutch));
        errMsg = "Wrong initial dutch tension";
        assertEquals(errMsg, initialTensionValue, initialDutchTension);
        errMsg = "Wrong initial french tension";
        assertEquals(errMsg, initialTensionValue, initialFrenchTension);
        
        // execute
        // French declare war
        french.changeRelationWithPlayer(dutch, Stance.WAR);
        
        // verify results
        errMsg ="The Dutch should be at war with the French";
        assertTrue(errMsg,dutch.getStance(french) == Stance.WAR);
        errMsg ="The French should be at war with the Dutch";
        assertTrue(errMsg,french.getStance(dutch) == Stance.WAR);
        
        int currDutchTension = dutch.getTension(french).getValue();
        int currFrenchTension = french.getTension(dutch).getValue();
        
        int expectedDutchTension = Math.min(1000,initialDutchTension + Tension.TENSION_ADD_DECLARE_WAR_FROM_PEACE);
        int expectedFrenchTension = initialFrenchTension;
        
        errMsg = "Wrong dutch tension";
        assertEquals(errMsg, expectedDutchTension, currDutchTension);
        errMsg = "Wrong french tension";
        assertEquals(errMsg, expectedFrenchTension, currFrenchTension);
    }
    
    public void testTension(){
    	String errMsg = "";
    	Game game = getStandardGame();
        
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");

        int initialTension = 500;
        int change = 250;

        dutch.setTension(french, new Tension(initialTension));
        french.setTension(dutch, new Tension(initialTension));
        
        dutch.modifyTension(french, change);

        int expectedDutchTension = initialTension + change;
        int expectedFrenchTension = initialTension;
        
        errMsg = "Dutch tension value should have changed";
        assertEquals(errMsg, expectedDutchTension, dutch.getTension(french).getValue());
        errMsg = "French tension value should have remained the same";
        assertEquals(errMsg, expectedFrenchTension ,french.getTension(dutch).getValue());
    }
    
    public void testCheckGameOverNoUnits() {
        Game game = getStandardGame();
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        assertTrue("Should be game over due to no units",Player.checkForDeath(dutch));
    }
    
    public void testCheckNoGameOverEnoughMoney() {
        Game game = getStandardGame();
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        dutch.modifyGold(10000);
        
        assertFalse("Should not be game, enough money",Player.checkForDeath(dutch));
    }
    
    public void testCheckNoGameOverHasColonistInNewWorld() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        new Unit(game, map.getTile(4, 7), dutch, freeColonist, UnitState.ACTIVE);
        
        assertFalse("Should not be game over, has units",Player.checkForDeath(dutch));
    }
    
    public void testCheckGameOver1600Threshold() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        UnitType galleon = spec().getUnitType("model.unit.galleon");
        new Unit(game, dutch.getEurope(), dutch, freeColonist, UnitState.SENTRY);
        new Unit(game, dutch.getEurope(), dutch, galleon, UnitState.SENTRY);
        assertFalse("Should not be game over, not 1600 yet",Player.checkForDeath(dutch));
        
        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",Player.checkForDeath(dutch));
    }
    
    public void testCheckGameOverUnitsGoingToEurope() {
        Game game = getStandardGame();
        
        Map map = getTestMap(spec().getTileType("model.tile.highSeas"));
        game.setMap(map);
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        
        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game,map.getTile(6, 8) , dutch, galleonType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, galleon, dutch, freeColonist, UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",1,galleon.getUnitCount());
        galleon.moveToEurope();
        
        assertFalse("Should not be game over, units between new world and europe",Player.checkForDeath(dutch));
        
        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",Player.checkForDeath(dutch));
    }
    
    public void testCheckGameOverUnitsGoingToNewWorld() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Player dutch = game.getPlayer("model.nation.dutch");
        
        Unit galleon = new Unit(game,dutch.getEurope() , dutch, galleonType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, galleon, dutch, freeColonist, UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Galleon should have a colonist onboard",1,galleon.getUnitCount());
        galleon.moveToAmerica();

        assertFalse("Should not be game over, units between new world and europe",Player.checkForDeath(dutch));
        
        game.setTurn(new Turn(1600));
        assertTrue("Should be game over, no new world presence after 1600",Player.checkForDeath(dutch));
    }
    
    public void testAddAnotherPlayersUnit(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Player dutch =  game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        
        assertEquals("Wrong number of units for dutch player",0,dutch.getUnits().size());
        assertEquals("Wrong number of units for french player",0,french.getUnits().size());
        
        Unit colonist = new Unit(game, map.getTile(6, 8), dutch, freeColonist, UnitState.ACTIVE);
        assertTrue("Colonist should be dutch", colonist.getOwner() == dutch);
        assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());
        
        try{
            french.setUnit(colonist);
            fail("An IllegalStateException should have been raised");
        }
        catch(IllegalStateException e){
            assertTrue("Colonist owner should not have been changed", colonist.getOwner() == dutch);
            assertEquals("Wrong number of units for dutch player",1,dutch.getUnits().size());
            assertEquals("Wrong number of units for french player",0,french.getUnits().size());
            
        }

    }
}
