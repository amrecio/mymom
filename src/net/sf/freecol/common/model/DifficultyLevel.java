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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;

/**
 * Represents a difficulty level.
 */
public class DifficultyLevel extends FreeColGameObjectType {

    private final Map<String, AbstractOption> levelOptions = new HashMap<String, AbstractOption>();;
    
    public DifficultyLevel(int index) {
        setIndex(index);
    }

    public AbstractOption getOption(String Id) throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID 'null'.");
        } else if (!levelOptions.containsKey(Id)) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID '" + Id
                    + "' returned 'null'.");
        } else {
            return levelOptions.get(Id);
        }
    }

    public Map<String, AbstractOption> getOptions() {
        return levelOptions;
    }
    
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
        throws XMLStreamException {

        final String id = in.getAttributeValue(null, "id");
        
        if (id == null){
            throw new XMLStreamException("invalid <" + getXMLElementTagName() + "> tag : no id attribute found.");
        }

        setId(in.getAttributeValue(null, "id"));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String optionType = in.getLocalName();
            if (IntegerOption.getXMLElementTagName().equals(optionType)) {
                IntegerOption option = new IntegerOption(in);
                levelOptions.put(option.getId(), option);
            } else if (BooleanOption.getXMLElementTagName().equals(optionType)) {
                BooleanOption option = new BooleanOption(in);
                levelOptions.put(option.getId(), option);
            } else {
                logger.finest("Parsing of " + optionType + " is not implemented yet");
                in.nextTag();
            }
        }

    }

}


