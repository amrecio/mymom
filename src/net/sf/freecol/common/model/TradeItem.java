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



import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * One of the items a DiplomaticTrade consists of.
 *
 */
public abstract class TradeItem extends FreeColObject {

    /**
     * The game this TradeItem belongs to.
     */
    private Game game;

    /**
     *  The player who is to provide this item.
     */
    private Player source;

    /**
     * The player who is to receive this item.
     */
    private Player destination;

    /**
     * The ID, used to get a name, etc.
     */
    private String id;

        
    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param id a <code>String</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     */
    public TradeItem(Game game, String id, Player source, Player destination) {
        this.game = game;
        this.id = id;
        this.source = source;
        this.destination = destination;
    }

    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public TradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
    }

    /**
     * Get the <code>ID</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getId() {
        return id;
    }

    /**
     * Set the <code>ID</code> value.
     *
     * @param newID The new ID value.
     */
    public final void setId(final String newID) {
        this.id = newID;
    }

    /**
     * Get the <code>Game</code> value.
     *
     * @return a <code>Game</code> value
     */
    public final Game getGame() {
        return game;
    }

    /**
     * Set the <code>Game</code> value.
     *
     * @param newGame The new Game value.
     */
    public final void setGame(final Game newGame) {
        this.game = newGame;
    }

    /**
     * Get the <code>Source</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getSource() {
        return source;
    }

    /**
     * Set the <code>Source</code> value.
     *
     * @param newSource The new Source value.
     */
    public final void setSource(final Player newSource) {
        this.source = newSource;
    }

    /**
     * Get the <code>Destination</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getDestination() {
        return destination;
    }

    /**
     * Set the <code>Destination</code> value.
     *
     * @param newDestination The new Destination value.
     */
    public final void setDestination(final Player newDestination) {
        this.destination = newDestination;
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public abstract boolean isValid();


    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public abstract boolean isUnique();

    /**
     * Concludes the trade.
     *
     */
    public abstract void makeTrade();

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        this.id = in.getAttributeValue(null, "ID");
        String sourceID = in.getAttributeValue(null, "source");
        this.source = (Player) game.getFreeColGameObject(sourceID);
        String destinationID = in.getAttributeValue(null, "destination");
        this.destination = (Player) game.getFreeColGameObject(destinationID);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute("ID", this.id);
        out.writeAttribute("source", this.source.getId());
        out.writeAttribute("destination", this.destination.getId());
    }
    

}

