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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * A named region on the map.
 */
public class Region extends FreeColGameObject implements Nameable {

    public static enum RegionType { OCEAN, COAST, LAKE, RIVER, LAND, MOUNTAIN, DESERT }

    /**
     * The name of this Region.
     */
    private String name;

    /**
     * Key used to retrieve description from Messages.
     */
    private String nameKey;

    /**
     * The parent Region of this Region.
     */
    private Region parent;

    /**
     * Whether this Region is claimable. Ocean Regions and non-leaf
     * Regions should not be claimable.
     */
    private boolean claimable = false;

    /**
     * Whether this Region is discoverable. The Eastern Ocean regions
     * should not be discoverable. In general, non-leaf regions should
     * not be discoverable. The Pacific Ocean is an exception, however.
     */
    private boolean discoverable = false;

    /**
     * Which Turn the Region was discovered in.
     */
    private Turn discoveredIn;

    /**
     * Which Player the Region was discovered by.
     */
    private Player discoveredBy;

    /**
     * Whether the Region is already discovered when the game starts.
     */
    private boolean prediscovered = false;

    /**
     * How much discovering this Region contributes to your score.
     * This should be zero unless the Region is discoverable.
     */
    private int scoreValue = 0;

    /**
     * Describe type here.
     */
    private RegionType type;

    /**
     * The children Regions of this Region.
     */
    private List<Region> children;


    /**
     * Creates a new <code>Region</code> instance.
     *
     * @param game a <code>Game</code> value
     */
    public Region(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>Region</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     */
    public Region(Game game, String id) {
        super(game, id);
    }

    /**
     * Initiates a new <code>Region</code> from an XML representation.
     * 
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occurred during parsing.
     */
    public Region(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>NameKey</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getNameKey() {
        return nameKey;
    }

    /**
     * Set the <code>NameKey</code> value.
     *
     * @param newNameKey The new NameKey value.
     */
    public final void setNameKey(final String newNameKey) {
        this.nameKey = newNameKey;
    }

    /**
     * Get the <code>Name</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Returns the name or default name of this Region.
     *
     * @return a <code>String</code> value
     */
    public String getDisplayName() {
        if (prediscovered) {
            return Messages.message(nameKey);
        } else if (name == null) {
            return Messages.message("model.region." + type.toString());
        } else {
            return name;
        }
    }

    /**
     * Get the <code>Parent</code> value.
     *
     * @return a <code>Region</code> value
     */
    public final Region getParent() {
        return parent;
    }

    /**
     * Set the <code>Parent</code> value.
     *
     * @param newParent The new Parent value.
     */
    public final void setParent(final Region newParent) {
        this.parent = newParent;
    }

    /**
     * Get the <code>Children</code> value.
     *
     * @return a <code>List<Region></code> value
     */
    public final List<Region> getChildren() {
        return children;
    }

    /**
     * Set the <code>Children</code> value.
     *
     * @param newChildren The new Children value.
     */
    public final void setChildren(final List<Region> newChildren) {
        this.children = newChildren;
    }

    /**
     * Get the <code>Claimable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isClaimable() {
        return claimable;
    }

    /**
     * Set the <code>Claimable</code> value.
     *
     * @param newClaimable The new Claimable value.
     */
    public final void setClaimable(final boolean newClaimable) {
        this.claimable = newClaimable;
    }

    /**
     * Get the <code>Discoverable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isDiscoverable() {
        return discoverable;
    }

    /**
     * Set the <code>Discoverable</code> value.
     *
     * @param newDiscoverable The new Discoverable value.
     */
    public final void setDiscoverable(final boolean newDiscoverable) {
        this.discoverable = newDiscoverable;
    }

    /**
     * Get the <code>Prediscovered</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isPrediscovered() {
        return prediscovered;
    }

    /**
     * Set the <code>Prediscovered</code> value.
     *
     * @param newPrediscovered The new Prediscovered value.
     */
    public final void setPrediscovered(final boolean newPrediscovered) {
        this.prediscovered = newPrediscovered;
    }

    /**
     * Get the <code>ScoreValue</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the <code>ScoreValue</code> value.
     *
     * @param newScoreValue The new ScoreValue value.
     */
    public final void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>RegionType</code> value
     */
    public final RegionType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final RegionType newType) {
        this.type = newType;
    }

    /**
     * Returns true if this is the whole map Region.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns true if this is a leaf node.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLeaf() {
        return children == null;
    }

    /**
     * Returns a discoverable Region or null. If this region is
     * discoverable, it is returned. If not, a discoverable parent is
     * returned, unless there is none. This is intended for
     * discovering the Pacific Ocean when discovering one of its
     * sub-Regions.
     *
     * @return a <code>Region</code> value
     */
    public Region getDiscoverableRegion() {
        if (isDiscoverable()) {
            return this;
        } else if (parent != null) {
            return parent.getDiscoverableRegion();
        } else {
            return null;
        }
    }

    /**
     * Get the <code>DiscoveredIn</code> value.
     *
     * @return a <code>Turn</code> value
     */
    public final Turn getDiscoveredIn() {
        return discoveredIn;
    }

    /**
     * Set the <code>DiscoveredIn</code> value.
     *
     * @param newDiscoveredIn The new DiscoveredIn value.
     */
    public final void setDiscoveredIn(final Turn newDiscoveredIn) {
        this.discoveredIn = newDiscoveredIn;
    }

    /**
     * Get the <code>DiscoveredBy</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getDiscoveredBy() {
        return discoveredBy;
    }

    /**
     * Set the <code>DiscoveredBy</code> value.
     *
     * @param newDiscoveredBy The new DiscoveredBy value.
     */
    public final void setDiscoveredBy(final Player newDiscoveredBy) {
        this.discoveredBy = newDiscoveredBy;
    }

    /**
     * Mark the Region as discovered.
     *
     * @param player a <code>Player</code> value
     * @param turn a <code>Turn</code> value
     * @param newName a <code>String</code> value
     */
    public void discover(Player player, Turn turn, String newName) {
        discoveredBy = player;
        discoveredIn = turn;
        name = newName;
        discoverable = false;
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("ID", getId());
        out.writeAttribute("nameKey", nameKey);
        out.writeAttribute("type", type.toString());
        if (name != null) {
            out.writeAttribute("name", name);
        }
        if (prediscovered) {
            out.writeAttribute("prediscovered", Boolean.toString(prediscovered));
        }
        if (claimable) {
            out.writeAttribute("claimable", Boolean.toString(claimable));
        }
        if (discoverable) {
            out.writeAttribute("discoverable", Boolean.toString(discoverable));
        }
        if (parent != null) {
            out.writeAttribute("parent", parent.getId());
        }
        if (discoveredIn != null) {
            out.writeAttribute("discoveredIn", String.valueOf(discoveredIn.getNumber()));
        }
        if (discoveredBy != null) {
            out.writeAttribute("discoveredBy", discoveredBy.getId());
        }
        if (scoreValue > 0) {
            out.writeAttribute("scoreValue", String.valueOf(scoreValue));
        }
        if (children != null) {
            String[] childArray = new String[children.size()];
            for (int index = 0; index < childArray.length; index++) {
                childArray[index] = children.get(index).getId();
            }
            toArrayElement("children", childArray, out);
        }
        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        nameKey = in.getAttributeValue(null, "nameKey");
        name = in.getAttributeValue(null, "name");
        claimable = getAttribute(in, "claimable", false);
        discoverable = getAttribute(in, "discoverable", false);
        prediscovered = getAttribute(in, "prediscovered", false);
        scoreValue = getAttribute(in, "scoreValue", 0);
        type = Enum.valueOf(RegionType.class, in.getAttributeValue(null, "type"));
        int turn = getAttribute(in, "discoveredIn", -1);
        if (turn > 0) {
            discoveredIn = new Turn(turn);
        }
        String playerID = in.getAttributeValue(null, "discoveredBy");
        if (playerID != null) {
            discoveredBy = getGame().getPlayer(playerID);
        }
        String parentString = in.getAttributeValue(null, "parent");
        if (parentString != null) {
            parent = getGame().getMap().getRegion(parentString);
        }
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("children")) {
                String[] childArray = readFromArrayElement("children", in, new String[0]);
                children = new ArrayList<Region>();
                for (String child : childArray) {
                    children.add(getGame().getMap().getRegion(child));
                }
            }
        }

    }            

    /**
    * Gets the tag name of the root element representing this object.
    * @return "region".
    */
    public static String getXMLElementTagName() {
        return "region";
    }

    public String toString() {
        return nameKey;
    }
}
