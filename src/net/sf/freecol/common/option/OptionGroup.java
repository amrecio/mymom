/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Used for grouping objects of {@link Option}s.
 */
public class OptionGroup extends AbstractOption<OptionGroup> {

    private static Logger logger = Logger.getLogger(OptionGroup.class.getName());

    private List<Option> options = new ArrayList<Option>();

    private Map<String, Option> optionMap = new HashMap<String, Option>();

    /**
     * Creates a new <code>OptionGroup</code>.
     * @param id The identifier for this option.
     */
    public OptionGroup(String id) {
        super(id);
    }

    /**
     * Creates a new  <code>OptionGroup</code>.
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public OptionGroup(XMLStreamReader in) throws XMLStreamException {
        this(NO_ID);
        readFromXML(in);
    }

    /**
     * Adds the given <code>Option</code>.
     * @param option The <code>Option</code> that should be
     *               added to this <code>OptionGroup</code>.
     */
    public void add(Option option) {
        String id = option.getId();
        if (optionMap.containsKey(id)) {
            for (int index = 0; index < options.size(); index++) {
                if (id.equals(options.get(index).getId())) {
                    options.remove(index);
                    options.add(index, option);
                    break;
                }
            }
        } else {
            options.add(option);
        }
        optionMap.put(id, option);
        if (option instanceof OptionGroup) {
            addOptionGroup((OptionGroup) option);
        }
    }

    private void addOptionGroup(OptionGroup group) {
        for (Option option : group.getOptions()) {
            optionMap.put(option.getId(), option);
            if (option instanceof OptionGroup) {
                addOptionGroup((OptionGroup) option);
            }
        }
    }


    public List<Option> getOptions() {
        return options;
    }

    public Option getOption(String id) {
        return optionMap.get(id);
    }

    /**
     * Gets the integer value of an option.
     *
     * @param id The id of the option.
     * @return The value.
     * @exception IllegalArgumentException If there is no integer
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public int getInteger(String id) {
        try {
            return ((IntegerOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }


    /**
     * Sets the integer value of an option.
     *
     * @param id The id of the option.
     * @param value the new value of the option.
     * @exception IllegalArgumentException If there is no integer
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public void setInteger(String id, int value) {
        try {
            ((IntegerOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }


    /**
     * Gets the boolean value of an option.
     *
     * @param id The id of the option.
     * @return The value.
     * @exception IllegalArgumentException If there is no boolean
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public boolean getBoolean(String id) {
        try {
            return ((BooleanOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }

    /**
     * Sets the boolean value of an option.
     *
     * @param id The id of the option.
     * @param value the new value of the option.
     * @exception IllegalArgumentException If there is no boolean
     *            value associated with the specified option.
     * @exception NullPointerException if the given <code>Option</code> does not exist.
     */
    public void setBoolean(String id, boolean value) {
        try {
            ((BooleanOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }

    /**
     * Gets the string value of an option.
     *
     * @param id String, option ID
     * @return String option value.
     * @throws IllegalArgumentException If the specified option is not of String type
     * @throws NullPointerException if the given <code>Option</code> does not exist.
     */
    public String getString(String id) {
        try {
            return ((StringOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String value associated with the specified option.");
        }
    }


    /**
     * Sets the string value of an option.
     *
     * @param id String, option ID
     * @param value String, the new value of the option
     * @throws IllegalArgumentException If the specified option is not of String type
     * @throws NullPointerException if the given <code>Option</code> does not exist.
     */
    public void setString(String id, String value) {
        try {
            ((StringOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String value associated with the specified option.");
        }
    }


    /**
     * Removes all of the <code>Option</code>s from this <code>OptionGroup</code>.
     */
    public void removeAll() {
        options.clear();
        optionMap.clear();
    }


    /**
     * Returns an <code>Iterator</code> for the <code>Option</code>s.
     * @return The <code>Iterator</code>.
     */
    public Iterator<Option> iterator() {
        return options.iterator();
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
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        Iterator<Option> oi = options.iterator();
        while (oi.hasNext()) {
            (oi.next()).toXML(out);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        if (id != null) {
            setId(id);
        }
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String optionId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            Option option = getOption(optionId);
            if (option == null) {
                addNewOption(in);
            } else {
                option.readFromXML(in);
            }
        }
    }

    private void addNewOption(XMLStreamReader in) throws XMLStreamException {
        String optionType = in.getLocalName();
        AbstractOption option = null;
        if (OptionGroup.getXMLElementTagName().equals(optionType)) {
            option = new OptionGroup(in);
        } else if (IntegerOption.getXMLElementTagName().equals(optionType)) {
            option = new IntegerOption(in);
        } else if (BooleanOption.getXMLElementTagName().equals(optionType)) {
            option = new BooleanOption(in);
        } else if (RangeOption.getXMLElementTagName().equals(optionType)) {
            option = new RangeOption(in);
        } else if (SelectOption.getXMLElementTagName().equals(optionType)) {
            option = new SelectOption(in);
        } else if (LanguageOption.getXMLElementTagName().equals(optionType)) {
            option = new LanguageOption(in);
        } else if (FileOption.getXMLElementTagName().equals(optionType)) {
            option = new FileOption(in);
        } else if (PercentageOption.getXMLElementTagName().equals(optionType)) {
            option = new PercentageOption(in);
        } else if (AudioMixerOption.getXMLElementTagName().equals(optionType)) {
            option = new AudioMixerOption(in);
        } else if (StringOption.getXMLElementTagName().equals(optionType)) {
            option = new StringOption(in);
        } else if ("action".equals(optionType)) {
            logger.finest("Skipping action " + in.getAttributeValue(null, "id"));
            // TODO: load FreeColActions from client options?
            in.nextTag();
            return;
        } else {
            logger.finest("Parsing of option type '" + optionType + "' is not implemented yet");
            in.nextTag();
            return;
        }
        add(option);
        option.setGroup(this.getId());
    }


    /**
     * Returns the name of this <code>Option</code>.
     *
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.message(getId() + ".name");
    }

    /**
     * Gives a short description of this <code>Option</code>. Can for
     * instance be used as a tooltip text.
     *
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.message(getId() + ".shortDescription");
    }

    /**
     * Returns the OptionGroup itself.
     *
     * @return an <code>Object</code> value
     */
    public OptionGroup getValue() {
        return this;
    }

    /**
     * Copy the options of another OptionGroup.
     *
     * @param value an <code>Object</code> value
     */
    @SuppressWarnings("unchecked")
    public void setValue(OptionGroup value) {
        for (Option other : value.getOptions()) {
            if (other instanceof AbstractOption) {
                AbstractOption mine = (AbstractOption) getOption(other.getId());
                mine.setValue(((AbstractOption) other).getValue());
            }
        }
    }

    /**
     * Debug print helper.
     *
     * @return Human-readable description of this OptionGroup.
     */
    @Override
    public String toString() {
        StringBuilder g = new StringBuilder();
        g.append(getName() + "<");
        for (Option o : getOptions()) {
            g.append(" ");
            if (o instanceof OptionGroup) {
                g.append(((OptionGroup)o).toString());
            } else if (o instanceof ListOption) {
                g.append(((ListOption)o).toString());
            } else {
                g.append(o.getId()); // TODO: add useful toString() to others
            }
        }
        g.append(" >\n");
        return g.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "optionGroup".
     */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }
}
