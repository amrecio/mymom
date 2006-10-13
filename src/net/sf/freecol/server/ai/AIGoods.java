
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.ai.mission.TransportMission;

import org.w3c.dom.Element;


/**
* Objects of this class contains AI-information for a single {@link Goods}.
*/
public class AIGoods extends AIObject implements Transportable {
    private static final Logger logger = Logger.getLogger(AIGoods.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int IMPORTANT_DELIVERY = 110;
    public static final int FULL_DELIVERY = 100;

    private Goods goods;
    private Location destination;
    private int transportPriority;
    private AIUnit transport = null;


    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param location The location of the goods.
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @param destination The destination of the goods. This is the
     *      <code>Location</code> to which the goods should be transported.
     */
    public AIGoods(AIMain aiMain, Location location, int type, int amount, Location destination) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextID());

        goods = new Goods(aiMain.getGame(), location, type, amount);
        this.destination = destination;
    }


    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */    
    public AIGoods(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */    
    public AIGoods(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }
    
    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */    
    public AIGoods(AIMain aiMain, String id) {
        super(aiMain, id);
        uninitialized = true;
    }

    /**
     * Aborts the given <code>Wish</code>.
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w) {
        if (destination == w.getDestination()) {
            destination = null;
        }
        if (w.getTransportable() == this) {
            w.dispose();
        }
    }

    /**
    * Returns the source for this <code>Transportable</code>.
    * This is normally the location of the
    * {@link #getTransportLocatable locatable}.
    *
    * @return The source for this <code>Transportable</code>.
    */
    public Location getTransportSource() {
        return goods.getLocation();
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
    public Location getTransportDestination() {
        return destination;
    }
    

    /**
    * Gets the <code>Locatable</code> which should be transported.
    * @return The <code>Locatable</code>.
    */
    public Locatable getTransportLocatable() {
        return getGoods();
    }


    /**
    * Gets the priority of transporting this <code>Transportable</code>
    * to it's destination.
    *
    * @return The priority of the transport.
    */
    public int getTransportPriority() {
        if (goods.getAmount() <= 100) {
            return goods.getAmount();
        } else {
            return transportPriority;
        }
    }

    
    /**
    * Increases the transport priority of this <code>Transportable</code>.
    * This method gets called every turn the <code>Transportable</code>
    * have not been put on a carrier's transport list.
    */    
    public void increaseTransportPriority() {
        transportPriority++;
    }

    
    /**
    * Gets the carrier responsible for transporting this <code>Transportable</code>.
    *
    * @return The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
    */
    public AIUnit getTransport() {
        return transport;
    }

    /**
     * Disposes this object.
     */
    public void dispose() {
        if (transport != null
                && transport.getMission() != null
                && transport.getMission() instanceof TransportMission) {
            TransportMission tm = (TransportMission) transport.getMission();
            tm.removeFromTransportList(this);
        }
        super.dispose();
    }
    
    /**
    * Sets the carrier responsible for transporting this <code>Transportable</code>.
    *
    * @param transport The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
    */
    public void setTransport(AIUnit transport) {
        this.transport = transport;

        if (transport.getMission() instanceof TransportMission
            && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
            ((TransportMission) transport.getMission()).addToTransportList(this);
        }
    }
    

    /**
     * Sets the priority of getting the goods to the {@link #getTransportDestination}.
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority) {
        this.transportPriority = transportPriority;
    }

    
    /**
    * Gets the goods this <code>AIGoods</code> is controlling.
    * @return The <code>Goods</code>.
    */
    public Goods getGoods() {
        return goods;
    }


    /**
     * Sets the goods this <code>AIGoods</code> is controlling.
     * @param goods The <code>Goods</code>.
     */    
    public void setGoods(Goods goods) {        
        if (goods == null) {
            throw new NullPointerException();
        }
        this.goods = goods;
    }
    
    
    /**
     * Returns the ID of this {@link AIObject}.
     * @return The ID.
     */
    public String getID() {
        return id;
    }

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", id);
        if (destination != null) {
            out.writeAttribute("destination", destination.getID());
        }
        out.writeAttribute("transportPriority", Integer.toString(transportPriority));
        if (transport != null) {
            out.writeAttribute("transport", transport.getID());
        }
        goods.toXML(out, null);

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        id = in.getAttributeValue(null, "ID");
        final String destinationStr = in.getAttributeValue(null, "destination");
        if (destinationStr != null) {
            destination = (Location) getAIMain().getFreeColGameObject(destinationStr);
        } else {
            destination = null;
        }
        if (destination == null) {
            logger.warning("Could not find destination: " + destination);
        }
        transportPriority = Integer.parseInt(in.getAttributeValue(null, "transportPriority"));
        
        final String transportStr = in.getAttributeValue(null, "transport");
        if (transportStr != null) {
            transport = (AIUnit) getAIMain().getAIObject(transportStr);
            if (transport == null) {
                transport = new AIUnit(getAIMain(), transportStr);
            }
        } else {
            transport = null;
        }
        
        in.nextTag();
        if (goods != null) {
            goods.readFromXML(in);
        } else {
            goods = new Goods(getAIMain().getGame(), in);
        }
        in.nextTag();
    }

    
    /**
     * Returns a <code>String</code>-representation of this object.
     * @return A <code>String</code> representing this objecy for debugging purposes.
     */
    public String toString() {
        return "AIGoods@" + hashCode() + " type: " + getGoods().getName() + " amount: " + getGoods().getAmount();
    }
    

    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiGoods"
    */
    public static String getXMLElementTagName() {
        return "aiGoods";
    }
}
