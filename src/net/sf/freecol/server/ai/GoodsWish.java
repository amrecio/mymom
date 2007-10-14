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


package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import net.sf.freecol.FreeCol;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;


/**
* Represents the need for goods within a <code>Colony</code>.
* <br><br>
* TODO: Deal in amounts of goods.
*/
public class GoodsWish extends Wish {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GoodsWish.class.getName());

    
    private GoodsType goodsType;

    /**
    * Creates a new <code>GoodsWish</code>.
    *
    * @param aiMain The main AI-object.
    * @param destination The <code>Location</code> in which the
    *       {@link Wish#getTransportable transportable} assigned to
    *       this <code>GoodsWish</code> will have to reach.
    * @param value The value identifying the importance of
    *       this <code>Wish</code>.
    * @param goodsType The type of goods needed for releasing this wish
    *       completly.
    */
    public GoodsWish(AIMain aiMain, Location destination, int value, GoodsType goodsType) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextID());

        if (destination == null) {
            throw new NullPointerException("destination == null");
        }       

        this.destination = destination;
        this.value = value;
        this.goodsType = goodsType;
    }


    /**
    * Creates a new <code>GoodsWish</code> from the given XML-representation.
    *
    * @param aiMain The main AI-object.
    * @param element The root element for the XML-representation of a <code>GoodsWish</code>.
    */
    public GoodsWish(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>GoodsWish</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */
     public GoodsWish(AIMain aiMain, String id) {
         super(aiMain, id);
     }
     
     /**
      * Creates a new <code>GoodsWish</code>.
      * 
      * @param aiMain The main AI-object.
      * @param in The input stream containing the XML.
      * @throws XMLStreamException if a problem was encountered
      *      during parsing.
      */
     public GoodsWish(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
         super(aiMain, in.getAttributeValue(null, "ID"));
         readFromXML(in);
     }

     
     /**
      * Returns the type of unit needed for releasing this wish.
      * @return The {@link Unit#getType type of unit}.
      */
     public GoodsType getGoodsType() {
         return goodsType;
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
         
         out.writeAttribute("ID", getID());
         
         out.writeAttribute("destination", destination.getID());
         if (transportable != null) {
             out.writeAttribute("transportable", transportable.getID());
         }
         out.writeAttribute("value", Integer.toString(value));
         
         out.writeAttribute("goodsType", Integer.toString(goodsType.getIndex()));
         
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
         destination = (Location) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "destination"));
         
         final String transportableStr = in.getAttributeValue(null, "transportable");
         if (transportableStr != null) {
             transportable = (Transportable) getAIMain().getAIObject(transportableStr);
             if (transportable == null) {
                 transportable = new AIGoods(getAIMain(), transportableStr);
             }
         } else {
             transportable = null;
         }
         value = Integer.parseInt(in.getAttributeValue(null, "value"));
         
         int goodsTypeIndex = Integer.parseInt(in.getAttributeValue(null, "goodsType"));
         goodsType = FreeCol.getSpecification().getGoodsType(goodsTypeIndex);
         
         in.nextTag();
     }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "GoodsWish"
    */
    public static String getXMLElementTagName() {
        return "GoodsWish";
    }
}
