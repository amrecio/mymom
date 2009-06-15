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

package net.sf.freecol.client.gui.i18n;

import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;

public class MessagesTest extends FreeColTestCase {

    public static final String noSuchKey = "should.not.exist.and.thus.return.null";

    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {

    	assertEquals("Press enter in order to end the turn.", Messages.message("infoPanel.endTurnPanel.text"));	
        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        // With parameters
        assertEquals("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.",
                     Messages.message("model.goods.food.description"));

        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));
    }

    public void testMessageStringVarargs() {
        
        try {
            Messages.message(null);
            fail("We should never get here");
        } catch (NullPointerException npe) {
            // Expected
        }

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        // With parameters for "Gold: %gold% | Tax: %tax%% | Year: %year%"
        assertEquals("Score: 1050    |    Gold: silver    |    Tax: 13%    |    Year: %year%", 
                     Messages.message("menuBar.statusLine", "%score%", "1050", "%gold%", "silver", "%tax%", "13"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                + "A new colonist is born whenever a colony has 200 units of food or more.", Messages.message(
                "model.goods.food.description"));

        try {
            Messages.message("menuBar.statusLine", "%tax%");
            fail("We should never have gotten here.");
        } catch (RuntimeException re) {
            // Expected
        }

        
        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));
        
        assertEquals(noSuchKey, Messages.message(noSuchKey, 
                                            "%gold%", "silver", "%tax%", "13"));
    }

    public void testChangeLocaleSettings() {
        Messages.setMessageBundle(Locale.US);

        assertEquals("Trade Advisor", Messages.message("menuBar.report.trade"));

        Messages.setMessageBundle(Locale.GERMANY);

        assertEquals("Handelsberater", Messages.message("menuBar.report.trade"));
    }
    
    // Tests if messages with special chars (like $) are well processed
    public void testMessageWithSpecialChars(){
    	String errMsg = "Error setting up test.";
    	String expected = "You establish the colony of %colony%.";
        String message = Messages.message("model.history.FOUND_COLONY");
    	assertEquals(errMsg, expected, message);
        
        String colNameWithSpecialChars="$specialColName";
        errMsg = "Wrong message";
        expected = "You establish the colony of $specialColName.";
        try{
        	message = Messages.message("model.history.FOUND_COLONY","%colony%",colNameWithSpecialChars);
        }
        catch(IllegalArgumentException e){
        	if(e.getMessage().contains("Illegal group reference")){
        		fail("Does not process messages with special chars");
        	}
        	throw e;
        }
        assertEquals(errMsg, expected, message);
    }
}
