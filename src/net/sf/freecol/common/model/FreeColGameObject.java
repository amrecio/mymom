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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;

/**
 * The superclass of all game objects in FreeCol.
 */
abstract public class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    private Game game;
    private boolean disposed = false;
    private boolean uninitialized;

    protected FreeColGameObject() {    
        logger.info("FreeColGameObject without ID created.");
        uninitialized = false;
    }
    

    /**
     * Creates a new <code>FreeColGameObject</code> with an automatically assigned 
     * ID and registers this object at the specified <code>Game</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     */
    public FreeColGameObject(Game game) {
        this.game = game;

        if (game != null) {
            //game.setFreeColGameObject(id, this);            
            String nextID = getRealXMLElementTagName() + ":" + game.getNextID();
            if (nextID != null) {
                setId(nextID);
            }
        } else if (this instanceof Game) {
            setId("0");
        } else {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }
        
        uninitialized = false;
    }
    
    
    /**
     * Initiates a new <code>FreeColGameObject</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public FreeColGameObject(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        uninitialized = false;
    }

    /**
     * Initiates a new <code>FreeColGameObject</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public FreeColGameObject(Game game, Element e) {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        uninitialized = false;
    }

    /**
     * Initiates a new <code>FreeColGameObject</code> 
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public FreeColGameObject(Game game, String id) {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        setId(id);
        
        uninitialized = true;
    }
    
    
    /**
     * Gets the game object this <code>FreeColGameObject</code> belongs to.
     * @return The <code>game</code>.
     */
    public Game getGame() {
        return game;
    }


    /**
     * Gets the <code>GameOptions</code> that is associated with the 
     * {@link Game} owning this <code>FreeColGameObject</code>.
     * 
     * @return The same <code>GameOptions</code>-object as returned
     *       by <code>getGame().getGameOptions()</code>.
     */
    public GameOptions getGameOptions() {
        return game.getGameOptions();
    }

    
    /**
     * Sets the game object this <code>FreeColGameObject</code> belongs to.
     * @param game The <code>game</code>.
     */
    public void setGame(Game game) {
        this.game = game;
    }    


    /**
     * Removes all references to this object.
     */
    public void dispose() {
        disposed = true;
        getGame().removeFreeColGameObject(getId());
    }
    

    /**
     * Checks if this object has been disposed.
     * @return <code>true</code> if this object has been disposed.
     * @see #dispose
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Checks if this <code>FreeColGameObject</code> 
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     * 
     * @return <code>true</code> if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }

    /**
     * Updates the id. This method should be overwritten
     * by server model objects.
     */
    public void updateID() {
        
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream for the purpose of storing this object
     * as a part of a saved game.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(XMLStreamWriter, Player, boolean, boolean)
     */
    public void toSavedXML(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, null, true, true);
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out, null, false, false);
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
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    abstract protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, 
                                      boolean toSavedGame) throws XMLStreamException;


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
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public final void toXML(XMLStreamWriter out, Player player, boolean showAll,
                            boolean toSavedGame) throws XMLStreamException {
        if (toSavedGame && !showAll) {
            throw new IllegalArgumentException("'showAll' should be true when saving a game.");
        }
        toXMLImpl(out, player, showAll, toSavedGame);
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        uninitialized = false;
        super.readFromXML(in);
    }

    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "unknown".
     */
    public static String getXMLElementTagName() {
        return "unknown";
    }

    /**
     * Gets the ID's integer part of this object. The age of two
     * FreeColGameObjects can be compared by comparing their integer
     * IDs.
     *
     * @return The unique ID of this object.
     */
    public Integer getIntegerID() {
        String stringPart = getRealXMLElementTagName() + ":";
        return new Integer(getId().substring(stringPart.length()));
    }

    private String getRealXMLElementTagName() {
        String tagName = "";
        try {
            Method m = getClass().getMethod("getXMLElementTagName", (Class[]) null);
            tagName = (String) m.invoke((Object) null, (Object[]) null);
        } catch (Exception e) {}
        return tagName;
    }

    /**
     * Sets the unique ID of this object. When setting a new ID to this object,
     * it it automatically registered at the corresponding <code>Game</code>
     * with the new ID.
     *
     * @param newID the unique ID of this object,
     */
    public final void setId(String newID) {
        if (game != null && !(this instanceof Game)) {
            if (!newID.equals(getId())) {
                if (getId() != null) {
                    game.removeFreeColGameObject(getId());
                }

                super.setId(newID);
                game.setFreeColGameObject(newID, this);
            }
        } else {
            super.setId(newID);
        }
    }

    
    /**
     * Sets the ID of this object for temporary use with
     * <code>toXMLElement</code>. This method does not
     * register the object.
     *
     * @param newID the unique ID of this object,
     */
    public void setFakeID(String newID) {
        super.setId(newID);
    }

    
    /**
     * Creates a <code>ModelMessage</code> and uses
     * <code>Player.addModelMessage(modelMessage)</code> to register
     * it.
     *
     * <br><br><br>
     *
     * Example:<br><br>
     *
     * Using <code>addModelMessage(this, someType, "messageID",
     * "%test1%", "ok1", "%test2%", "ok2")</code> with the entry
     * "messageID=This is %test1% and %test2%" in {@link
     * net.sf.freecol.client.gui.i18n.Messages Messages}, would give
     * the following message: "This is ok1 and ok2".
     *
     * @param source The source of the message. This is what the
     *               message should be associated with. In addition,
     *               the owner of the source is the player getting the
     *               message.
     * @param type The type of message. This is used for filtering
     * purposes.
     * @param messageID The ID of the message to display. See: {@link
     * net.sf.freecol.client.gui.i18n.Messages Messages}.
     * @param data Contains the data to be displayed in the message.
     * @see net.sf.freecol.client.gui.Canvas Canvas
     * @see Player#addModelMessage(ModelMessage)
     * @see ModelMessage
     */    
    protected void addModelMessage(FreeColGameObject source, ModelMessage.MessageType type,
                                   String messageID, String... data) {
        addModelMessage(source, type, null, messageID, data);
    }

    /**
     * Creates a <code>ModelMessage</code> and uses
     * <code>Player.addModelMessage(modelMessage)</code> to register
     * it.
     *
     * <br><br><br>
     *
     * Example:<br><br>
     *
     * Using <code>addModelMessage(this, someType, someDisplay,
     * "messageID", "%test1%", "ok1", "%test2%", "ok2")</code> with
     * the entry "messageID=This is %test1% and %test2%" in {@link
     * net.sf.freecol.client.gui.i18n.Messages Messages}, would give
     * the following message: "This is ok1 and ok2".
     *
     * @param source The source of the message. This is what the
     *               message should be associated with. In addition,
     *               the owner of the source is the player getting the
     *               message.
     * @param type The type of message. This is used for filtering
     * purposes.
     * @param display The Object to display if that is not the
     * source. A message about <code>Goods</code> in a
     * <code>Colony</code> might wish to display the goods instead of
     * the colony, for example.
     * @param messageID The ID of the message to display. See: {@link
     * net.sf.freecol.client.gui.i18n.Messages Messages}.
     * @param data Contains the data to be displayed in the message.
     * @see net.sf.freecol.client.gui.Canvas Canvas
     * @see Player#addModelMessage(ModelMessage)
     * @see ModelMessage
     */    
    protected void addModelMessage(FreeColGameObject source, ModelMessage.MessageType type, 
                                   FreeColObject display, String messageID, String... data) {
        ModelMessage message = new ModelMessage(source, type, display, messageID, data);
        if (message.getSource() == null) {
            logger.warning("ModelMessage with ID " + message.getId() + " has null source.");
        } else if (message.getOwner() == null) {
            logger.warning("ModelMessage with ID " + message.getId() + " has null owner.");
        }
        message.getOwner().addModelMessage(message);
    }


    /**
     * Checks if this object has the specified ID.
     *
     * @param id The ID to check against.
     * @return <i>true</i> if the specified ID match the ID of this object and
     *         <i>false</i> otherwise.
     */
    public boolean hasID(String id) {
        return getId().equals(id);
    }

    /**
     * Checks if the given <code>FreeColGameObject</code> equals this object.
     *
     * @param o The <code>FreeColGameObject</code> to compare against this object.
     * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
     */
    public boolean equals(FreeColGameObject o) {
        if (o != null) {
            return Utils.equals(this.getGame(), o.getGame()) && getId().equals(o.getId());
        } else {
            return false;
        }
    }
    
    /**
     * Checks if the given <code>FreeColGameObject</code> equals this object.
     *
     * @param o The <code>FreeColGameObject</code> to compare against this object.
     * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
     */
    public boolean equals(Object o) {
        return (o instanceof FreeColGameObject) ? equals((FreeColGameObject) o) : false;
    }
        
    public int hashCode() {
        return getId().hashCode();
    }

    
    /**
     * Returns a string representation of the object.
     * @return The <code>String</code>
     */
    public String toString() {
        return getClass().getName() + ": " + getId() + " (super's hash code: " +
            Integer.toHexString(super.hashCode()) + ")";
    }

    public <T extends FreeColGameObject> T getFreeColGameObject(XMLStreamReader in, String attributeName,
                                                                Class<T> returnClass, T defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return returnClass.cast(getGame().getFreeColGameObject(attributeString));
        } else {
            return defaultValue;
        }
    }

}
