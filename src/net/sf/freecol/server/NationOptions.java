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

package net.sf.freecol.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Nation;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class NationOptions extends FreeColObject{

    /**
     * National advantages for European players only. The natives will
     * always have national advantages.
     */
    public static enum Advantages { NONE, FIXED, SELECTABLE };

    /**
     * Nations may be available to all players, to AI players only, or
     * to no players.
     */
    public static enum NationState { AVAILABLE, AI_ONLY, NOT_AVAILABLE };

    /**
     * Describe selectColors here.
     */
    private boolean selectColors;

    /**
     * Describe nationalAdvantages here.
     */
    private Advantages nationalAdvantages;

    /**
     * Describe nativeNations here.
     */
    private Map<Nation, NationState> nativeNations = new HashMap<Nation, NationState>();

    /**
     * Describe europeanNations here.
     */
    private Map<Nation, NationState> europeanNations = new HashMap<Nation, NationState>();

    /**
     * Get the <code>EuropeanNations</code> value.
     *
     * @return a <code>Map<Nation, NationState></code> value
     */
    public final Map<Nation, NationState> getEuropeanNations() {
        return europeanNations;
    }

    /**
     * Set the <code>EuropeanNations</code> value.
     *
     * @param newEuropeanNations The new EuropeanNations value.
     */
    public final void setEuropeanNations(final Map<Nation, NationState> newEuropeanNations) {
        this.europeanNations = newEuropeanNations;
    }

    /**
     * Get the <code>NativeNations</code> value.
     *
     * @return a <code>Map<Nation, NationState></code> value
     */
    public final Map<Nation, NationState> getNativeNations() {
        return nativeNations;
    }

    /**
     * Set the <code>NativeNations</code> value.
     *
     * @param newNativeNations The new NativeNations value.
     */
    public final void setNativeNations(final Map<Nation, NationState> newNativeNations) {
        this.nativeNations = newNativeNations;
    }

    /**
     * Get the <code>NationalAdvantages</code> value.
     *
     * @return an <code>Advantages</code> value
     */
    public final Advantages getNationalAdvantages() {
        return nationalAdvantages;
    }

    /**
     * Set the <code>NationalAdvantages</code> value.
     *
     * @param newNationalAdvantages The new NationalAdvantages value.
     */
    public final void setNationalAdvantages(final Advantages newNationalAdvantages) {
        this.nationalAdvantages = newNationalAdvantages;
    }

    /**
     * Get the <code>SelectColors</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean canSelectColors() {
        return selectColors;
    }

    /**
     * Set the <code>SelectColors</code> value.
     *
     * @param newSelectColors The new SelectColors value.
     */
    public final void setSelectColors(final boolean newSelectColors) {
        this.selectColors = newSelectColors;
    }

    /**
     * Describe <code>getDefaults</code> method here.
     *
     * @return a <code>NationOptions</code> value
     */
    public static final NationOptions getDefaults() {
        NationOptions result = new NationOptions();
        result.setSelectColors(true);
        result.setNationalAdvantages(Advantages.SELECTABLE);
        Map<Nation, NationState> defaultEuropeanNations = new HashMap<Nation, NationState>();
        for (Nation nation : Specification.getSpecification().getEuropeanNations()) {
            defaultEuropeanNations.put(nation, NationState.AVAILABLE);
        }
        result.setEuropeanNations(defaultEuropeanNations);
        Map<Nation, NationState> defaultNativeNations = new HashMap<Nation, NationState>();
        for (Nation nation : Specification.getSpecification().getIndianNations()) {
            defaultNativeNations.put(nation, NationState.AI_ONLY);
        }
        result.setNativeNations(defaultNativeNations);
        return result;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        //setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));

        selectColors = getAttribute(in, "selectColors", true);
        String advantages = getAttribute(in, "nationalAdvantages", "selectable").toUpperCase();
        nationalAdvantages = Enum.valueOf(Advantages.class, advantages);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("europeanNations")) {
                europeanNations.clear();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals("europeanNation")) {
                        String nationId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                        Nation nation = Specification.getSpecification().getNation(nationId);
                        NationState state = Enum.valueOf(NationState.class,
                                                         in.getAttributeValue(null, "state"));
                        europeanNations.put(nation, state);
                    }
                    in.nextTag();
                }
            } else if (in.getLocalName().equals("nativeNations")) {
                nativeNations.clear();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals("nativeNation")) {
                        String nationId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                        Nation nation = Specification.getSpecification().getNation(nationId);
                        NationState state = Enum.valueOf(NationState.class,
                                                         in.getAttributeValue(null, "state"));
                        nativeNations.put(nation, state);
                    }
                    in.nextTag();
                }
            }
        }
        in.nextTag(); // close this element
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
        //out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("selectColors", Boolean.toString(selectColors));
        out.writeAttribute("nationalAdvantages", nationalAdvantages.toString());
        // europeans
        out.writeStartElement("europeanNations");
        for (Map.Entry<Nation, NationState> entry : europeanNations.entrySet()) {
            out.writeStartElement("europeanNation");
            out.writeAttribute(ID_ATTRIBUTE_TAG, entry.getKey().getId());
            out.writeAttribute("state", entry.getValue().toString());
            out.writeEndElement();
        }
        out.writeEndElement();
        // natives
        out.writeStartElement("nativeNations");
        for (Map.Entry<Nation, NationState> entry : nativeNations.entrySet()) {
            out.writeStartElement("nativeNation");
            out.writeAttribute(ID_ATTRIBUTE_TAG, entry.getKey().getId());
            out.writeAttribute("state", entry.getValue().toString());
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeEndElement();
    }

    public static String getXMLElementTagName() {
        return "nationOptions";
    }

    // debugging only
    public String toString() {
        StringBuilder result = new StringBuilder(); 
        result.append("selectColors: " + selectColors + "\n");
        result.append("nationalAdvantages: " + nationalAdvantages.toString() + "\n");
        result.append("europeanNations:\n");
        for (Map.Entry<Nation, NationState> entry : europeanNations.entrySet()) {
            result.append("   " + entry.getKey().getId() + " " + entry.getValue().toString() + "\n");
        }
        result.append("nativeNations:\n");
        for (Map.Entry<Nation, NationState> entry : nativeNations.entrySet()) {
            result.append("   " + entry.getKey().getId() + " " + entry.getValue().toString() + "\n");
        }
        return result.toString();
    }
}