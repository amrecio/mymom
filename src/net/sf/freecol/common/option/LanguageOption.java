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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Option for selecting a language. The possible choices are determined
 * using the available language files in "data/strings".
 */
public class LanguageOption extends AbstractOption {

    private static Logger logger = Logger.getLogger(LanguageOption.class.getName());

    private static final Map<String, Language> languages = new HashMap<String, Language>();

    public static final String AUTO = "automatic";

    private Language DEFAULT = new Language(AUTO, getLocale(AUTO));

    private Map<String, String> languageNames = new HashMap<String, String>();

    private static final String[][] languageNamesHelper = {
        {"nds", "Plattd\u00fc\u00fctsch"},
        {"pms", "Piemont\u00e9s"}
    };


    private static Comparator<Language> languageComparator = new Comparator<Language>() {
        public int compare(Language l1, Language l2) {
            if (l1.getKey().equals(AUTO)) {
                if (l2.getKey().equals(AUTO)) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (l2.getKey().equals(AUTO)) {
                return 1;
            } else {
                return l1.toString().compareTo(l2.toString());
            }
        }
    };


    private Language value;

    /**
     * Creates a new <code>LanguageOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     */
    public LanguageOption(String id) {
        this(id, null);
    }

    public LanguageOption(String id, OptionGroup optionGroup) {
        super(id, optionGroup);
        if (languages.size() == 0) {
            prepareLanguages();
        }
        value = DEFAULT;
    }


    private void prepareLanguages() {
        for (String[] pair : languageNamesHelper) {
            languageNames.put(pair[0], pair[1]);
        }
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>String</code> value
     */
    public final Language getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final Language newValue) {
        final Language oldValue = this.value;
        this.value = newValue;

        if (!newValue.equals(oldValue)) {
            firePropertyChange("value", oldValue, value);
        }
    }

    /**
     * Returns a list of the available languages.
     * @return The available languages in a human readable format.
     */
    public Language[] getOptions() {
        findLanguages();
        List<Language> names = new ArrayList<Language>(languages.values());
        Collections.sort(names, languageComparator);
        return names.toArray(new Language[0]);
    }

    /**
     * Finds the languages available in the default directory.
     */
    private void findLanguages() {

        languages.put(AUTO, DEFAULT);
        File i18nDirectory = new File(FreeCol.getDataDirectory(), Messages.STRINGS_DIRECTORY);
        File[] files = i18nDirectory.listFiles();
        if (files == null) {
            throw new RuntimeException("No language files could be found in the <" + i18nDirectory + 
                                       "> folder. Make sure you ran the ant correctly.");
        }
        for (File file : files) {
            if (file.getName() == null) {
                continue;
            }
            if (file.getName().startsWith(Messages.FILE_PREFIX + "_")) {
                try {
                    final String languageID = file.getName().substring(16, file.getName().indexOf("."));
                    languages.put(languageID, new Language(languageID, getLocale(languageID)));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception in findLanguages()", e);
                    continue;
                }
            }
        }
    }


    /**
     * Returns the <code>Locale</code> decided by the given name. 
     * 
     * @param languageID A String using the same format as
     *         {@link #getStringValue()}.
     * @return The Locale.
     */
    public static Locale getLocale(String languageID) {
        if (languageID == null || AUTO.equals(languageID)) {
            return Locale.getDefault();
        }
         
        try {
            String language, country = "", variant = "";
            StringTokenizer st = new StringTokenizer(languageID, "_", true);
            language = st.nextToken();
            if (st.hasMoreTokens()) {
                // Skip _
                st.nextToken();
            }
            if (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (!token.equals("_")) {
                    country = token;
                }
                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.equals("_") && st.hasMoreTokens()) {
                        token = st.nextToken();
                    }
                    if (!token.equals("_")) {
                        variant = token;
                    }
                }
            }
            return new Locale(language, country, variant);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot choose locale: " + languageID, e);
            return Locale.getDefault();
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
        // Start element:
        out.writeStartElement(getId());

        out.writeAttribute("value", getValue().getKey());

        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        findLanguages();
        final Language oldValue = this.value;
        
        Language newValue = languages.get(in.getAttributeValue(null, "value"));
        if (newValue == null) {
            newValue = languages.get(AUTO);
        }
        setValue(newValue);
        in.nextTag();
    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "selectOption".
     */
    public static String getXMLElementTagName() {
        return "languageOption";
    }


    public class Language {

        /**
         * Describe key here.
         */
        private String key;

        /**
         * Describe locale here.
         */
        private Locale locale;


        public Language(String key, Locale locale) {
            this.key = key;
            this.locale = locale;
        }

        /**
         * Get the <code>Key</code> value.
         *
         * @return a <code>String</code> value
         */
        public final String getKey() {
            return key;
        }

        /**
         * Set the <code>Key</code> value.
         *
         * @param newKey The new Key value.
         */
        public final void setKey(final String newKey) {
            this.key = newKey;
        }

        /**
         * Get the <code>Locale</code> value.
         *
         * @return a <code>Locale</code> value
         */
        public final Locale getLocale() {
            return locale;
        }

        /**
         * Set the <code>Locale</code> value.
         *
         * @param newLocale The new Locale value.
         */
        public final void setLocale(final Locale newLocale) {
            this.locale = newLocale;
        }

        public String toString() {
            if (getKey().equals(AUTO)) {
                return Messages.message("clientOptions.gui.languageOption.autoDetectLanguage");
            } else {
                String name = locale.getDisplayName(locale);
                if (name.equals(key) && languageNames.containsKey(key)) {
                    name = languageNames.get(key);
                }
                return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
            }
        }

        public boolean equals(Object o) {
            if ((o instanceof Language) &&
                ((Language) o).getKey().equals(key)) {
                return true;
            } else {
                return false;
            }
        }

    }

}
