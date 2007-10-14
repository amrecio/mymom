
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;


/**
 * The <code>Ability</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Ability extends Feature {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private boolean value = true;

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     */
    public Ability(String id) {
        this(id, null, true);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value a <code>boolean</code> value
     */
    public Ability(String id, boolean value) {
        this(id, null, value);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value a <code>boolean</code> value
     */
    public Ability(String id, String source, boolean value) {
        setId(id);
        setSource(source);
        this.value = value;
    }
    
    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param element an <code>Element</code> value
     */
    public Ability(Element element) {
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Ability(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }
    
    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final boolean newValue) {
        this.value = newValue;
    }

    // -- Serialization --


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        setSource(in.getAttributeValue(null, "source"));
        value = getAttribute(in, "value", true);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("scope".equals(childName)) {
                Scope scope = new Scope(in);
                getScopes().add(scope);
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                       !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }        

    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", getId());
        if (getSource() != null) {
            out.writeAttribute("source", getSource());
        }
        out.writeAttribute("value", String.valueOf(value));

        for (Scope scope : getScopes()) {
            scope.toXMLImpl(out);
        }

        out.writeEndElement();
    }

    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "ability";
    }

}
