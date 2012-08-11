/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The effect of a natural disaster or other event. How the
 * probability of the effect is interpreted depends on the number of
 * effects value of the disaster or event. If the number of effects is
 * ALL, the probability is ignored. If it is ONE, then the probability
 * may be an arbitrary integer, and is used only for comparison with
 * other effects. If the number of effects is SEVERAL, however, the
 * probability must be a percentage.
 *
 * @see Disaster
 */
public class Effect extends FreeColGameObjectType {

    public static final String DAMAGED_UNIT = "model.disaster.effect.damageUnit";
    public static final String LOSS_OF_UNIT = "model.disaster.effect.lossOfUnit";
    public static final String LOSS_OF_MONEY = "model.disaster.effect.lossOfMoney";
    public static final String LOSS_OF_GOODS = "model.disaster.effect.lossOfGoods";
    public static final String LOSS_OF_TILE_PRODUCTION = "model.disaster.effect.lossOfTileProduction";
    public static final String LOSS_OF_BUILDING_PRODUCTION = "model.disaster.effect.lossOfBuildingProduction";

    /**
     * The probability of this effect.
     */
    private int probability;

    /**
     * Scopes that might limit this Effect to certain types of objects.
     */
    private List<Scope> scopes;


    protected Effect() {
        // empty constructor
    }

    /**
     * Creates a new <code>Effect</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Effect(XMLStreamReader in, Specification specification) throws XMLStreamException {
        setSpecification(specification);
        readFromXML(in);
    }

    public Effect(Effect template) {
        setSpecification(template.getSpecification());
        setId(template.getId());
        this.probability = template.probability;
        this.scopes = template.scopes;
        addFeatures(template);
    }

    /**
     * Get the <code>Probability</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getProbability() {
        return probability;
    }

    /**
     * Set the <code>Probability</code> value.
     *
     * @param newProbability The new Probability value.
     */
    public final void setProbability(final int newProbability) {
        this.probability = newProbability;
    }

    /**
     * Get the <code>Scopes</code> value.
     *
     * @return a <code>List<Scope></code> value
     */
    public final List<Scope> getScopes() {
        return scopes;
    }

    /**
     * Set the <code>Scopes</code> value.
     *
     * @param newScopes The new Scopes value.
     */
    public final void setScopes(final List<Scope> newScopes) {
        this.scopes = newScopes;
    }

    /**
     * Returns true if the <code>appliesTo</code> method of at least
     * one <code>Scope</code> object returns true.
     *
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(final FreeColGameObjectType objectType) {
        if (scopes.isEmpty()) {
            return true;
        } else {
            for (Scope scope : scopes) {
                if (scope.appliesTo(objectType)) {
                    return true;
                }
            }
            return false;
        }
    }


    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        probability = getAttribute(in, "probability", 0);
    }

    @Override
    protected void readChild(XMLStreamReader in)
        throws XMLStreamException {
        String childName = in.getLocalName();
        if (Scope.getXMLElementTagName().equals(childName)) {
            Scope scope = new Scope(in);
            if (scopes == null) {
                scopes = new ArrayList<Scope>();
            }
            scopes.add(scope);
        } else {
            super.readChild(in);
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
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("probability", Integer.toString(probability));
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        if (getScopes() != null) {
            for (Scope scope : getScopes()) {
                scope.toXMLImpl(out);
            }
        }
    }


    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "effect";
    }

    public String toString() {
        String result = getId() + " [probability: " + probability + "%]";
        if (getScopes() != null) {
            for (Scope scope : getScopes()) {
                result += " " + scope;
            }
        }
        return result;
    }

}
