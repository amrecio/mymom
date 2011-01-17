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


package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;

import org.w3c.dom.Element;


/**
 * Mission for realizing a <code>Wish</code>.
 * @see Wish
 */
public class WishRealizationMission extends Mission {

    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());


    private Wish wish;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param wish The <code>Wish</code> which will be realized by
    *        the unit and this mission.
    */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit, Wish wish) {
        super(aiMain, aiUnit);
        this.wish = wish;
        
        if (wish == null) {
            throw new NullPointerException("wish == null");
        }
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public WishRealizationMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>WishRealizationMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public WishRealizationMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
    * Disposes this <code>Mission</code>.
    */
    public void dispose() {
        if (wish != null) {
            wish.setTransportable(null);
            wish = null;
        }
        super.dispose();
    }


    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();

        if (!isValid()) {
            return;
        }

        // Move towards the target.
        if (getUnit().getTile() != null) {
            if (wish.getDestination().getTile() != getUnit().getTile()) {
                Direction r = moveTowards(wish.getDestination().getTile());
                if (r == null || !moveButDontAttack(r)) return;
            }
            if (wish.getDestination().getTile() == getUnit().getTile()) {
                if (wish.getDestination() instanceof Colony) {
                    Colony colony = (Colony) wish.getDestination();

                    if (AIMessage.askWork(getAIUnit(), colony.getVacantWorkLocationFor(getUnit()))) {
                        //getUnit().setLocation(colony);
                        getAIUnit().setMission(new WorkInsideColonyMission(getAIMain(), getAIUnit(), (AIColony) getAIMain().getAIObject(colony)));
                    } else {
                        logger.warning("AIunit " + getAIUnit().getId()
                                       + " could not work in " + colony.getId());
                    }
                } else {
                    logger.warning("Unknown type of destination for: " + wish);
                }
            }
        }
    }



    /**
    * Returns the destination for this <code>Transportable</code>.
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the tansport is determined by
    * {@link TransportMission} in the latter case.
    *
    * @return The destination for this <code>Transportable</code>.
    */    
    public Tile getTransportDestination() {
        if (getUnit().isOnCarrier()) {
            return wish.getDestination().getTile();
        } else if (getUnit().getTile() == wish.getDestination().getTile()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(wish.getDestination().getTile()) == null) {
            return wish.getDestination().getTile();
        } else {
            return null;
        }
    }


    /**
    * Returns the priority of getting the unit to the
    * transport destination.
    *
    * @return The priority.
    */
    public int getTransportPriority() {
        if (getUnit().isOnCarrier()) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else if (getUnit().getLocation().getTile() == wish.getDestination().getTile()) {
            return 0;
        } else if (getUnit().getTile() == null || getUnit().findPath(wish.getDestination().getTile()) == null) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else {
            return 0;
        }
    }


    /**
    * Checks if this mission is still valid to perform.
    *
    * @return <code>true</code> if this mission is still valid to perform
    *         and <code>false</code> otherwise.
    */
    public boolean isValid() {
        if (wish == null) return false;
        Location l = wish.getDestination();
        if (l == null) {
            return false;
        }
        if (((FreeColGameObject) l).isDisposed()) {
            return false;
        }
        if (l instanceof Ownable && ((Ownable) l).getOwner() != getUnit().getOwner()) {
            return false;
        }
        if (l instanceof Colony) {
            Colony colony = (Colony) l;
            if (colony.getVacantWorkLocationFor(getUnit()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (wish.shouldBeStored()) {
            out.writeStartElement(getXMLElementTagName());

            out.writeAttribute("unit", getUnit().getId());
            out.writeAttribute("wish", wish.getId());

            out.writeEndElement();
        }
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        wish = (Wish) getAIMain().getAIObject(in.getAttributeValue(null, "wish"));
        if (wish == null) {
            final String wid = in.getAttributeValue(null, "wish");
            if (wid.startsWith(GoodsWish.getXMLElementTagName())) {
                wish = new GoodsWish(getAIMain(), wid);
            } else if (wid.startsWith(WorkerWish.getXMLElementTagName())) {
                wish = new WorkerWish(getAIMain(), wid);
            } else {
                logger.warning("Unknown type of Wish.");
            }
        }
        in.nextTag();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "wishRealizationMission".
    */
    public static String getXMLElementTagName() {
        return "wishRealizationMission";
    }
    
    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     * 
     * @return The <code>String</code>.
     */
    public String getDebuggingInfo() {
        if (wish == null) {
            return "No wish";
        } else {
            return wish.getDestination().getTile().getPosition() + " " + wish.getValue();
        }
    }

}
