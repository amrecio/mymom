/*
 *  Player.java - Holds all the information about one player.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
* Represents a player. The player can be either a human player or an AI-player.
*
* <br><br>
*
* In addition to storing the name, nation e.t.c. of the player, it also stores
* various defaults for the player. One example of this is the
* {@link #getEntryLocation entry location}.
*/
public class Player extends FreeColGameObject {

    /** The nations a player can play. */
    public static final int DUTCH = 0,
                            ENGLISH = 1,
                            FRENCH = 2,
                            SPANISH = 3;

    /** The Indian tribes. Note that these values differ from IndianSettlement's by a value of 4.*/
    public static final int INCA = 4,
                            AZTEC = 5,
                            ARAWAK = 6,
                            CHEROKEE = 7,
                            IROQUOIS = 8,
                            SIOUX = 9,
                            APACHE = 10,
                            TUPI = 11;

    /** For future reference - the REF forces. */
    public static final int REF_DUTCH = 12,
                            REF_ENGLISH = 13,
                            REF_FRENCH = 14,
                            REF_SPANISH = 15;


    /** The maximum line of sight a unit can have in the game. */
    public static final int MAX_LINE_OF_SIGHT = 2;



    private String          name;
    private int             nation;

    // Represented on the network as "color.getRGB()":
    private Color           color;

    private boolean         admin;
    private int             gold;
    private Europe          europe;
    private boolean         ready;

    /** True if this is an AI player. */
    private boolean         ai;

    private int             crosses;
    private int             bells;
    private boolean         dead = false;


    private Location entryLocation;

    private Iterator nextActiveUnitIterator = new NextActiveUnitIterator(this);




    /**
    * Creates a new non-admin <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    */
    public Player(Game game, String name) {
        this(game, name, false);
    }

    /**
    * Creates an new AI <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin Whether or not this AI player shall be considered an Admin.
    * @param ai Whether or not this AI player shall be considered an AI player (usually true here).
    */
    public Player(Game game, String name, boolean admin, boolean ai) {
        this(game, name, admin);

        this.ai = ai;
    }

    /**
    * Creates a new <code>Player</code> with specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin 'true' if this Player is an admin,
    * 'false' otherwise.
    */
    public Player(Game game, String name, boolean admin) {
        super(game);

        this.name = name;
        this.admin = admin;

        nation = DUTCH;
        color = Color.ORANGE;
        europe = new Europe(game);

        // TODO (this is for testing only): Set to 0
        gold = 10000;

        crosses = 0;
        bells = 0;
    }


    /**
    * Initiates a new <code>Player</code> from an <code>Element</code>
    * and registers this <code>Player</code> at the specified game.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> in a DOM-parsed XML-tree that describes
    *                this object.
    */
    public Player(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }





    /**
    * Gets the default <code>Location</code> where the units
    * arriving from {@link Europe} will be put.
    *
    * @return The <code>Location</code>.
    * @see Unit#getEntryLocation
    */
    public Location getEntryLocation() {
        return entryLocation;
    }


    /**
    * Sets the <code>Location</code> where the units
    * arriving from {@link Europe} will be put as a default.
    *
    * @return The <code>Location</code>.
    * @see #getEntryLocation
    */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }


    /**
    * Checks if this <code>Player</code> has explored the given <code>Tile</code>.
    * @param tile The <code>Tile</code>.
    * @return <i>true</i> if the <code>Tile</code> has been explored and
    *         <i>false</i> otherwise.
    */
    public boolean hasExplored(Tile tile) {
        return tile.isExplored();
    }


    /**
    * Checks if this <code>Player</code> can see the given
    * <code>Tile</code>. The <code>Tile</code> can be seen if
    * it is in a {@link Unit}'s line of sight.
    *
    * @param The given <code>Tile</code>.
    * @return <i>true</i> if the <code>Player</code> can see
    *         the given <code>Tile</code> and <i>false</i>
    *         otherwise.
    */
    public boolean canSee(Tile tile) {
        if (tile == null) {
            return false;
        }

        // First check this tile:
        if (tile.getFirstUnit() != null && tile.getFirstUnit().getOwner().equals(this)) {
            return true;
        }


        // Check the tiles in a MAX_LINE_OF_SIGHT radius around the given tile:
        Vector surroundingTiles = getGame().getMap().getSurroundingTiles(tile, MAX_LINE_OF_SIGHT);

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    if (unit.getLineOfSight() >= t.getDistanceTo(tile)) {
                        return true;
                    }
                }
            } else if (t != null && t.getColony() != null && t.getColony().getOwner().equals(this)) {
                return true;
            }
        }

        return false;
    }


    /**
    * Gets called when this player's turn has ended.
    */
    public void endTurn() {
        // Will be implemented later.
    }


    /**
    * Returns the europe object that this player has.
    * @return The europe object that this player has.
    */
    public Europe getEurope() {
        return europe;
    }


    /**
    * Returns the amount of gold that this player has.
    * @return The amount of gold that this player has.
    */
    public int getGold() {
        return gold;
    }

    /**
    * Determines whether this player is an AI player.
    * @return Whether this player is an AI player.
    */
    public boolean isAI() {
        return ai;
    }

    /**
    * Modifies the amount of gold that this player has. The argument
    * can be both positive and negative.
    *
    * @param amount The amount of gold that should be added to this
    *               player's gold amount (can be negative!).
    * @exception IllegalArgumentException if the player gets a negativ
    *            amount of gold after adding <code>amount</code>.
    */
    public void modifyGold(int amount) {
        if ((gold + amount) >= 0) {
            gold += amount;
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
    * Gets a new active unit.
    * @return A <code>Unit</code> that can be made active.
    */
    public Unit getNextActiveUnit() {
        return (Unit) nextActiveUnitIterator.next();
    }


    /**
    * Checks if this player is an admin.
    * @return <i>true</i> if the player is an admin and <i>false</i> otherwise.
    */
    public boolean isAdmin() {
        return admin;
    }


    /**
    * Checks if this player is dead.
    * A <code>Player</code> dies when it looses the game.
    */
    public boolean isDead() {
        return dead;
    }


    /**
    * Sets this player to be dead or not.
    */
    public void setDead(boolean dead) {
        this.dead = dead;
    }


    /**
    * Returns the name of this player.
    * @return The name of this player.
    */
    public String getName() {
        return name;
    }


    /**
    * Returns the name of this player.
    * @return The name of this player.
    */
    public String getUsername() {
        return name;
    }


    /**
    * Returns the nation of this player.
    * @return The nation of this player.
    */
    public int getNation() {
        return nation;
    }


    /**
    * Returns the nation of this player as a String.
    * @return The nation of this player as a String.
    */
    public String getNationAsString() {
        switch (nation) {
            case DUTCH:
                return "Dutch";
            case ENGLISH:
                return "English";
            case FRENCH:
                return "French";
            case SPANISH:
                return "Spanish";
            default:
                return "INVALID";
        }
    }


    /**
    * Returns the color of this player.
    * @return The color of this player.
    */
    public Color getColor() {
        return color;
    }


    /**
    * Returns the color of this player as a String.
    * @return The color of this player as a String.
    */
    public String getColorAsString() {
        final String[] colorNames = {"Black", "Blue", "Cyan", "Gray", "Green", "Magenta",
                "Orange", "Pink", "Red", "White", "Yellow"};
        final Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN,
                Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE, Color.YELLOW};

        for (int i = 0; i < colors.length; i++) {
            if (color.equals(colors[i])) {
                return colorNames[i];
            }
        }
        return "UNSUPPORTED: see Player::getColorAsString";
    }


    /**
    * Sets the nation for this player.
    * @param n The new nation for this player.
    */
    public void setNation(int n) {
        nation = n;
    }

    /**
    * Sets the nation for this player.
    * @param n The new nation for this player.
    * @throw FreeColException In case the given nation is invalid.
    */
    public void setNation(String n) throws FreeColException {
        if (n.toLowerCase().equals("dutch")) {
            setNation(DUTCH);
        } else if (n.toLowerCase().equals("english")) {
            setNation(ENGLISH);
        } else if (n.toLowerCase().equals("french")) {
            setNation(FRENCH);
        } else if (n.toLowerCase().equals("spanish")) {
            setNation(SPANISH);
        } else {
            throw new FreeColException("Invalid nation '" + n + "'.");
        }
    }

    /**
    * Sets the color for this player.
    * @param c The new color for this player.
    */
    public void setColor(Color c) {
        color = c;
    }

    /**
    * Sets the color for this player.
    * @param c The new color for this player.
    */
    public void setColor(String c) {
        if (c.toLowerCase().equals("black")) {
            setColor(Color.BLACK);
        } else if (c.toLowerCase().equals("blue")) {
            setColor(Color.BLUE);
        } else if (c.toLowerCase().equals("cyan")) {
            setColor(Color.CYAN);
        } else if (c.toLowerCase().equals("gray")) {
            setColor(Color.GRAY);
        } else if (c.toLowerCase().equals("green")) {
            setColor(Color.GREEN);
        } else if (c.toLowerCase().equals("magenta")) {
            setColor(Color.MAGENTA);
        } else if (c.toLowerCase().equals("orange")) {
            setColor(Color.ORANGE);
        } else if (c.toLowerCase().equals("pink")) {
            setColor(Color.PINK);
        } else if (c.toLowerCase().equals("red")) {
            setColor(Color.RED);
        } else if (c.toLowerCase().equals("white")) {
            setColor(Color.WHITE);
        } else if (c.toLowerCase().equals("yellow")) {
            setColor(Color.YELLOW);
        }
    }


    /**
    * Checks if this <code>Player</code> is ready to start the game.
    */
    public boolean isReady() {
        return ready;
    }


    /**
    * Sets this <code>Player</code> to be ready/not ready for
    * starting the game.
    */
    public void setReady(boolean ready) {
        this.ready = ready;
    }


    /**
    * Gets an <code>Iterator</code> containing all the units this player owns.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {
        ArrayList units = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    Iterator childUnitIterator = u.getUnitIterator();
                    while (childUnitIterator.hasNext()) {
                        Unit childUnit = (Unit) childUnitIterator.next();
                        units.add(childUnit);
                    }

                    units.add(u);
                }
            }
        }

        return units.iterator();
    }


    /**
    * Increments the player's cross count, with benefits thereof.
    * @param num The number of crosses to add.
    */
    public void incrementCrosses(int num) {
        crosses += num;
    }


    public void setCrosses(int crosses) {
        this.crosses = crosses;
    }


    /**
    * Checks to see whether or not a colonist can emigrate, and does so if possible.
    * @return Whether a new colonist should immigrate.
    */
    public boolean checkEmigrate() {
        if (crosses >= getCrossesRequired()) {
            crosses -= getCrossesRequired();
            return true;
        }
        return false;
    }


    /**
    * Gets the number of crosses required to cause a new colonist to emigrate.
    * @return The number of crosses required to cause a new colonist to emigrate.
    */
    public int getCrossesRequired() {
        // The book I have tells me the crosses needed is:
        // [(colonist count in colonies + total colonist count) * 2] + 8.
        // So every unit counts as 2 unless they're in a colony,
        // wherein they count as 4.
        // This does that, I think. -sjm
        int count = 8;

        //ArrayList units = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    Iterator childUnitIterator = u.getUnitIterator();
                    while (childUnitIterator.hasNext()) {
                        Unit childUnit = (Unit) childUnitIterator.next();
                        //units.add(childUnit);
                        count += 2;
                    }

                    count += 2;
                }
            } else if (t != null && t.getSettlement() != null && (t.getSettlement() instanceof Colony)) {
              count += (((Colony)t.getSettlement()).getUnitCount()) * 4; // Units in colonies count doubly. -sjm
            }
        }

        Iterator europeUnitIterator = getEurope().getUnitIterator();
        while (europeUnitIterator.hasNext()) {
            europeUnitIterator.next();
            count += 2;
        }

        if (nation == ENGLISH) count = (count * 2) / 3;

        return count;
    }


    /**
    * Gets the price for a recruit in europe.
    */
    public int getRecruitPrice() {
        return (getCrossesRequired() - crosses) * 10;
    }


    /**
    * Increments the player's bell count, with benefits thereof.
    * @param num The number of bells to add.
    */
    public void incrementBells(int num) {
        bells += num;
        //TODO: founding fathers
    }


    /**
    * Prepares this <code>Player</code> for a new turn.
    */
    public void newTurn() {
        // Nothing to do.
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Player".
    */
    public Element toXMLElement(Player player, Document document) {
        Element playerElement = document.createElement(getXMLElementTagName());

        playerElement.setAttribute("ID", getID());
        playerElement.setAttribute("username", name);
        playerElement.setAttribute("nation", Integer.toString(nation));
        playerElement.setAttribute("color", Integer.toString(color.getRGB()));
        playerElement.setAttribute("admin", Boolean.toString(admin));
        playerElement.setAttribute("gold", Integer.toString(gold));
        playerElement.setAttribute("crosses", Integer.toString(crosses));
        playerElement.setAttribute("bells", Integer.toString(bells));
        playerElement.setAttribute("ready", Boolean.toString(ready));
        playerElement.setAttribute("dead", Boolean.toString(dead));

        playerElement.setAttribute("ai", Boolean.toString(ai));

        if (entryLocation != null) {
            playerElement.setAttribute("entryLocation", entryLocation.getID());
        }

        if (equals(player)) {
            playerElement.appendChild(europe.toXMLElement(player, document));
        }

        return playerElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param playerElement The DOM-element ("Document Object Model") made to represent this "Player".
    */
    public void readFromXMLElement(Element playerElement) {
        setID(playerElement.getAttribute("ID"));

        name = playerElement.getAttribute("username");
        nation = Integer.parseInt(playerElement.getAttribute("nation"));
        color = new Color(Integer.parseInt(playerElement.getAttribute("color")));
        admin = (new Boolean(playerElement.getAttribute("admin"))).booleanValue();
        gold = Integer.parseInt(playerElement.getAttribute("gold"));
        crosses = Integer.parseInt(playerElement.getAttribute("crosses"));
        bells = Integer.parseInt(playerElement.getAttribute("bells"));
        ready = (new Boolean(playerElement.getAttribute("ready"))).booleanValue();
        ai = (new Boolean(playerElement.getAttribute("ai"))).booleanValue();
        dead = (new Boolean(playerElement.getAttribute("dead"))).booleanValue();

        if (playerElement.hasAttribute("entryLocation")) {
            entryLocation = (Location) getGame().getFreeColGameObject(playerElement.getAttribute("entryLocation"));
        }

        Element europeElement = getChildElement(playerElement, Europe.getXMLElementTagName());
        if (europeElement != null) {
            if (europe != null) {
                europe.readFromXMLElement(europeElement);
            } else {
                europe = new Europe(getGame(), europeElement);
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "player"
    */
    public static String getXMLElementTagName() {
        return "player";
    }





    /**
    * An <code>Iterator</code> of {@link Unit}s that can be made active.
    */
    public class NextActiveUnitIterator implements Iterator {

        private Iterator unitIterator = null;
        private Player owner;



        /**
        * Creates a new <code>NextActiveUnitIterator</code>.
        * @param owner The <code>Player</code> that needs an iterator of it's units.
        */
        public NextActiveUnitIterator(Player owner) {
            this.owner = owner;
        }






        public boolean hasNext() {
            if (unitIterator != null && unitIterator.hasNext()) {
                return true;
            } else {
                unitIterator = createUnitIterator();

                if (unitIterator.hasNext()) {
                    return true;
                } else {
                    return false;
                }
            }
        }


        public Object next() {
            if (unitIterator != null && unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && !(u.getLocation() instanceof WorkLocation) && u.getTile() != null) {
                    return u;
                } else {
                    return next();
                }
            } else {
                unitIterator = createUnitIterator();

                if (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && !(u.getLocation() instanceof WorkLocation)) {
                        return u;
                    } else {
                        return next();
                    }
                } else {
                    return null;
                }
            }
        }


        /**
        * Removes from the underlying collection the last element returned by the
        * iterator (optional operation).
        *
        * @exception UnsupportedOperationException no matter what.
        */
        public void remove() {
            throw new UnsupportedOperationException();
        }


        /**
        * Returns an <code>Iterator</code> for the units of this player that can be active.
        */
        private Iterator createUnitIterator() {
            ArrayList units = new ArrayList();
            Map map = getGame().getMap();

            Iterator tileIterator = map.getWholeMapIterator();
            while (tileIterator.hasNext()) {
                Tile t = map.getTile((Map.Position) tileIterator.next());

                if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(owner)) {
                    Iterator unitIterator = t.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = (Unit) unitIterator.next();

                        Iterator childUnitIterator = u.getUnitIterator();
                        while (childUnitIterator.hasNext()) {
                            Unit childUnit = (Unit) childUnitIterator.next();

                            if ((childUnit.getMovesLeft() > 0) && (childUnit.getState() == Unit.ACTIVE) && u.getTile() != null) {
                                units.add(childUnit);
                            }
                        }

                        if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && u.getTile() != null) {
                            units.add(u);
                        }
                    }
                }
            }

            return units.iterator();
        }
    }
}

