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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Represents an option where the result is a value between 0 and 100.
 */
public class PercentageOption extends IntegerOption {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(PercentageOption.class.getName());

    /**
     * Creates a new <code>RangeOption</code>.
     * 
     * @param in The <code>XMSStreamReader</code> to read the data from
     */
    public PercentageOption(XMLStreamReader in) throws XMLStreamException {
        super(in);
    }

    /**
     * Creates a new <code>RangeOption</code>.
     * 
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     * @param optionGroup The OptionGroup this Option belongs to.
     * @param defaultOption The default value.
     */
    public PercentageOption(String id, OptionGroup optionGroup, int defaultOption) {
        super(id, optionGroup, 0, 100, defaultOption);
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *  
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", getId());
        out.writeAttribute("value", Integer.toString(getValue()));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, "id");
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String value = in.getAttributeValue(null, "value");
        
        if (id == null && getId().equals("NO_ID")){
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no value nor default value found.");
        }
 
        if(getId() == NO_ID) {
            setId(id);
        }
        if(value != null) {
            setValue(Integer.parseInt(value));
        } else {
            setValue(Integer.parseInt(defaultValue));
        }
        in.nextTag();
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "percentageOption".
    */
    public static String getXMLElementTagName() {
        return "percentageOption";
    }
}
