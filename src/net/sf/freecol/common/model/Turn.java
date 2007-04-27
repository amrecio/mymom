
package net.sf.freecol.common.model;

import net.sf.freecol.client.gui.i18n.Messages;


/**
* Represents a given turn in the game.
*/
public class Turn {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    public static final int STARTING_YEAR = 1492;
    public static final int SEASON_YEAR = 1600;


    private int turn;

    

    public Turn(int turn) {
        this.turn = turn;
    }

    
    /**
    * Increases the turn number by one.
    */
    public void increase() {
        turn++;
    }


    /**
    * Gets the turn number.
    * @return The number of turns.
    */
    public int getNumber() {
        return turn;
    }

    
    /**
    * Sets the turn number.
    * @param turn The number of turns.
    */
    public void setNumber(int turn) {
        this.turn = turn;
    }

    
    /**
    * Gets the age.
    * 
    * @return The age:
    *       <br>
    *       <br>1 - if before {@link #SEASON_YEAR}
    *       <br>2 - if between 1600 and 1700.
    *       <br>3 - if after 1700.
    */
    public int getAge() {
        if (getYear() < SEASON_YEAR) {
            return 1;
        } else if (getYear() < 1700) {
            return 2;
        } else {
            return 3;
        }
    }


    /**
    * Checks if this turn is equal to another turn.
    */
    public boolean equals(Object o) {

        if ( ! (o instanceof Turn) ) { return false; }

        return turn == ((Turn) o).turn;
    }

    
    /**
    * Gets the year this turn is in.
    * @return The calculated year based on the turn
    *       number.
    */
    public int getYear() {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return STARTING_YEAR + turn - 1;
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        return SEASON_YEAR + c/2 - 1;
    }


    /**
    * Returns a string representation of this turn.
    * @return A string with the format: "<i>[season] year</i>".
    *         Examples: "Spring 1602", "1503"...
    */
    public String toString() {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return Integer.toString(STARTING_YEAR + turn - 1);
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        return ((c%2==0) ? Messages.message("spring") : Messages.message("autumn"))
                + " " + Integer.toString(SEASON_YEAR + c/2 - 1);
    }

    /**
    * Returns a string representation of this turn suitable for
    * savegame files.
    * @return A string with the format: "<i>[season] year</i>".
    *         Examples: "1602_1_Spring", "1503"...
    */
    public String toSaveGameString() {
        if (STARTING_YEAR + turn - 1 < SEASON_YEAR) {
            return Integer.toString(STARTING_YEAR + turn - 1);
        }

        int c = turn - (SEASON_YEAR - STARTING_YEAR - 1);
        String result = Integer.toString(SEASON_YEAR + c/2 - 1);
        if (c % 2 == 0) {
            result += "_1_" + Messages.message("spring");
        } else {
            result += "_2_" + Messages.message("autumn");
        }
        return result;
    }
}
