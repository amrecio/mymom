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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * The <code>Feature</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public abstract class Feature extends FreeColObject {

    /**
     * The source of this Feature, e.g. a UnitType.
     */
    private FreeColGameObjectType source;

    /**
     * The first Turn in which this Feature applies.
     */
    private Turn firstTurn;

    /**
     * The last Turn in which this Feature applies.
     */
    private Turn lastTurn;

    /**
     * A list of Scopes limiting the applicability of this Feature.
     */
    private List<Scope> scopes;

    /**
     * Get the <code>TimeLimit</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean hasTimeLimit() {
        return (firstTurn != null || lastTurn != null);
    }

    /**
     * Get the <code>Scope</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean hasScope() {
        return !(scopes == null || scopes.isEmpty());
    }

    /**
     * Describe <code>getName</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getName() {
        return Messages.message(getId() + ".name");
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
     * Get the <code>firstTurn</code> value.
     *
     * @return a <code>Turn</code> value
     */
    public final Turn getFirstTurn() {
        return firstTurn;
    }

    /**
     * Set the <code>firstTurn</code> value.
     *
     * @param newFirstTurn The new FirstTurn value.
     */
    public final void setFirstTurn(final Turn newFirstTurn) {
        this.firstTurn = newFirstTurn;
    }

    /**
     * Get the <code>LastTurn</code> value.
     *
     * @return a <code>Turn</code> value
     */
    public final Turn getLastTurn() {
        return lastTurn;
    }

    /**
     * Set the <code>LastTurn</code> value.
     *
     * @param newLastTurn The new LastTurn value.
     */
    public final void setLastTurn(final Turn newLastTurn) {
        this.lastTurn = newLastTurn;
    }

    /**
     * Get the <code>Source</code> value.
     *
     * @return a <code>String</code> value
     */
    public final FreeColGameObjectType getSource() {
        return source;
    }

    /**
     * Set the <code>Source</code> value.
     *
     * @param newSource The new Source value.
     */
    public final void setSource(final FreeColGameObjectType newSource) {
        this.source = newSource;
    }

    /**
     * Returns true if the <code>appliesTo</code> method of at least
     * one <code>Scope</code> object returns true.
     *
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(final FreeColGameObjectType objectType) {
        if (!hasScope()) {
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

    /**
     * Returns true if the <code>appliesTo</code> method of at least
     * one <code>Scope</code> object returns true.
     *
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(final FreeColGameObjectType objectType, Turn turn) {
        if (turn != null &&
            (firstTurn != null && turn.getNumber() < firstTurn.getNumber() ||
             lastTurn != null && turn.getNumber() > lastTurn.getNumber())) {
            return false;
        } else {
            return appliesTo(objectType);
        }
    }

    /**
     * Returns true if the Feature has an lastTurn turn smaller than the
     * turn given.
     *
     * @param turn a <code>Turn</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isOutOfDate(Turn turn) {
        return (turn != null &&
                (lastTurn != null && turn.getNumber() > lastTurn.getNumber()));
    }


    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        if (getSource() != null) {
            out.writeAttribute("source", getSource().getId());
        }
        if (getFirstTurn() != null) {
            out.writeAttribute("firstTurn", String.valueOf(getFirstTurn().getNumber()));
        }
        if (getLastTurn() != null) {
            out.writeAttribute("lastTurn", String.valueOf(getLastTurn().getNumber()));
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        if (getScopes() != null) {
            for (Scope scope : getScopes()) {
                scope.toXMLImpl(out);
            }
        }
    }

    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        String sourceId = in.getAttributeValue(null, "source");
        if (sourceId == null) {
            setSource(null);
        } else {
            setSource(Specification.getSpecification().getType(sourceId));
        }

        String firstTurn = in.getAttributeValue(null, "firstTurn");
        if (firstTurn != null) {
            setFirstTurn(new Turn(Integer.parseInt(firstTurn)));
        }

        String lastTurn = in.getAttributeValue(null, "lastTurn");
        if (lastTurn != null) {
            setLastTurn(new Turn(Integer.parseInt(lastTurn)));
        }
    }

    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Scope.getXMLElementTagName().equals(childName)) {
                Scope scope = new Scope(in);
                if (getScopes() == null) {
                    setScopes(new ArrayList<Scope>());
                }
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

}