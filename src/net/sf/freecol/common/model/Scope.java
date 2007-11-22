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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The <code>Scope</code> class determines whether a given
 * <code>FreeColGameObjectType</code> fulfills certain requirements.
 */
public final class Scope extends FreeColObject implements Cloneable {


    /**
     * The ID of a <code>FreeColGameObjectType</code>.
     */
    private String type;

    /**
     * The ID of an <code>Ability</code>.
     */
    private String abilityID;

    /**
     * The value of an <code>Ability</code>.
     */
    private boolean abilityValue = true;

    /**
     * The name of an <code>Method</code>.
     */
    private String methodName;

    /**
     * The <code>String</code> representation of the value of an
     * <code>Method</code>.
     */
    private String methodValue;


    /**
     * Creates a new <code>Scope</code> instance.
     *
     */
    public Scope() {}

    /**
     * Creates a new <code>Scope</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Scope(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }


    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final String newType) {
        this.type = newType;
    }

    /**
     * Get the <code>AbilityID</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getAbilityID() {
        return abilityID;
    }

    /**
     * Set the <code>AbilityID</code> value.
     *
     * @param newAbilityID The new AbilityID value.
     */
    public void setAbilityID(final String newAbilityID) {
        this.abilityID = newAbilityID;
    }

    /**
     * Get the <code>AbilityValue</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAbilityValue() {
        return abilityValue;
    }

    /**
     * Set the <code>AbilityValue</code> value.
     *
     * @param newAbilityValue The new AbilityValue value.
     */
    public void setAbilityValue(final boolean newAbilityValue) {
        this.abilityValue = newAbilityValue;
    }

    /**
     * Get the <code>MethodName</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Set the <code>MethodName</code> value.
     *
     * @param newMethodName The new MethodName value.
     */
    public void setMethodName(final String newMethodName) {
        this.methodName = newMethodName;
    }

    /**
     * Get the <code>MethodValue</code> value.
     *
     * @return an <code>String</code> value
     */
    public String getMethodValue() {
        return methodValue;
    }

    /**
     * Set the <code>MethodValue</code> value.
     *
     * @param newMethodValue The new MethodValue value.
     */
    public void setMethodValue(final String newMethodValue) {
        this.methodValue = newMethodValue;
    }


    /**
     * Describe <code>appliesTo</code> method here.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(FreeColGameObjectType object) {
        if (type != null && !type.equals(object.getId())) {
            return false;
        }
        if (abilityID != null && object.hasAbility(abilityID) != abilityValue) {
            return false;
        }
        if (methodName != null) {
            try {
                Method method = object.getClass().getMethod(methodName);
                if (!method.invoke(object).toString().equals(methodValue)) {
                    return false;
                }
            } catch(Exception e) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object o) {
        if (o instanceof Scope) {
            Scope otherScope = (Scope) o;
            if (type == null) {
                if (otherScope.getType() != type) {
                    return false;
                }
            } else if (!type.equals(otherScope.getType())) {
                return false;
            }
            if (abilityID == null) {
                if (otherScope.getAbilityID() != abilityID) {
                    return false;
                }
            } else if (!abilityID.equals(otherScope.getAbilityID())) {
                return false;
            }
            if (abilityValue != otherScope.isAbilityValue()) {
                return false;
            }
            if (methodName == null) {
                if (otherScope.getMethodName() != methodName) {
                    return false;
                }
            } else if (!methodName.equals(otherScope.getMethodName())) {
                return false;
            }
            if (methodValue == null) {
                if (otherScope.getMethodValue() != methodValue) {
                    return false;
                }
            } else if (!methodValue.equals(otherScope.getMethodValue())) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        type = in.getAttributeValue(null, "type");
        abilityID = in.getAttributeValue(null, "ability-id");
        abilityValue = getAttribute(in, "ability-value", true);
        methodName = in.getAttributeValue(null, "method-name");
        methodValue = in.getAttributeValue(null, "method-value");
        in.nextTag();
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

        out.writeAttribute("type", type);
        out.writeAttribute("ability-id", abilityID);
        out.writeAttribute("ability-value", String.valueOf(abilityValue));
        out.writeAttribute("method-name", methodName);
        out.writeAttribute("method-value", methodValue);

        out.writeEndElement();
    }
    
    public static String getXMLElementTagName() {
        return "scope";
    }


}
