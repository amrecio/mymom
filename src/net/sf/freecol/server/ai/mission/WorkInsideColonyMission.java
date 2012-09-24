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

package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for working inside an AI colony.
 */
public class WorkInsideColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(WorkInsideColonyMission.class.getName());

    private static final String tag = "AI worker";

    /** The AI colony to work inside. */
    private AIColony aiColony;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param aiColony The <code>AIColony</code> the unit should be
     *        working in.
     */
    public WorkInsideColonyMission(AIMain aiMain, AIUnit aiUnit,
                                   AIColony aiColony) {
        super(aiMain, aiUnit);

        this.aiColony = aiColony;
        uninitialized = false;
    }

    /**
     * Creates a new <code>WorkInsideColonyMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WorkInsideColonyMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Convenience accessor for the colony to work in.
     *
     * @return The <code>AIColony</code> to work in.
     */
    public AIColony getAIColony() {
        return aiColony;
    }


    // Fake Transportable interface

    /**
     * Gets the destination for units with this mission.
     *
     * @return Usually the colony tile unless the unit is there
     *         already or can get there itself.
     */
    public Tile getTransportDestination() {
        final Tile colonyTile = (Tile)getTarget();
        return (shouldTakeTransportToTile(colonyTile))
            ? colonyTile
            : null;
    }


    // Mission interface

    /**
     * Gets the mission target.
     *
     * @return The mission target location.
     */
    public Location getTarget() {
        return (aiColony == null || aiColony.getColony() == null) ? null
            : aiColony.getColony().getTile();
    }

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : (!aiUnit.getUnit().isPerson()) ? Mission.UNITNOTAPERSON
            : ((reason = invalidTargetReason(loc, aiUnit.getUnit().getOwner()))
                != null) ? reason
            : null;
    }

    // Omitted invalidReason(AIUnit), not needed.

    /**
     * Why is this mission invalid?
     *
     * @return A reason for mission invalidity, or null if none found.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), getTarget());
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs this mission.
     */
    public void doMission() {
        String reason = invalidReason();
        if (reason != null) {
            logger.finest(tag + " broken(" + reason + "): " + this);
            return;
        }

        travelToTarget(tag, getTarget(),
                       CostDeciders.avoidSettlementsAndBlockingUnits());
    }


    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (isValid()) {
            toXML(out, getXMLElementTagName());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("colony", aiColony.getId());
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        aiColony = (AIColony) getAIMain()
            .getAIObject(in.getAttributeValue(null, "colony"));
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "workInsideColonyMission".
     */
    public static String getXMLElementTagName() {
        return "workInsideColonyMission";
    }
}
