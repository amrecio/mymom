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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public abstract class FreeColObject {

    protected static Logger logger = Logger.getLogger(FreeColObject.class.getName());

    public static final int INFINITY = Integer.MAX_VALUE;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    public static final String NO_ID = "NO_ID";

    public static final String ID_ATTRIBUTE = "ID";

    /**
     * XML tag name for value attribute.
     */
    protected static final String VALUE_TAG = "value";

    /**
     * XML tag name for ID attribute.
     */
    // this is what we use for the specification
    // TODO: standardize on this spelling
    public static final String ID_ATTRIBUTE_TAG = "id";

    /**
     * XML tag name for array elements.
     */
    protected static final String ARRAY_SIZE = "xLength";

    /**
     * XML attribute tag to denote partial updates.
     */
    protected static final String PARTIAL_ATTRIBUTE = "PARTIAL";

    /**
     * Unique identifier of an object
     */
    private String id;

    /**
     * The <code>specification</code> this object uses. May be null.
     */
    private Specification specification;

    private PropertyChangeSupport pcs = null;


    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    protected void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Describe <code>getSpecification</code> method here.
     *
     * @return a <code>Specification</code> value
     */
    public Specification getSpecification() {
        return specification;
    }

    /**
     * Sets the specification for this object. This method should only
     * ever be used by the object's constructor.
     *
     * @param specification a <code>Specification</code> value
     */
    protected void setSpecification(Specification specification) {
        this.specification = specification;
    }

    /**
     * Debugging tool, dump object XML to System.err.
     */
    public void dumpObject() {
        save(System.err);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file the save file
     * @throws FileNotFoundException
     */
    public void save(File file) throws FileNotFoundException {
        save(new FileOutputStream(file));
    }

    /**
     * Writes the object to the given output stream
     *
     * @param out the OutputStream
     */
    public void save(OutputStream out) {
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = null;
        try {
            xsw = xof.createXMLStreamWriter(out, "UTF-8");
            xsw.writeStartDocument("UTF-8", "1.0");
            this.toXML(xsw, null, true, true);
            xsw.writeEndDocument();
            xsw.flush();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception writing object.", e);
        } finally {
            try {
                if (xsw != null) xsw.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception closing save stream.", e);
            }
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document) {
        // since the player is null, showAll must be true
        return toXMLElement(null, document, true, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document) {
        return toXMLElement(player, document, true, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document,
                                boolean showAll, boolean toSavedGame) {
        return toXMLElement(player, document, showAll, toSavedGame, null);
    }

    /**
     * This method writes a partial XML-representation of this object to
     * an element using only the mandatory and specified fields.
     *
     * @param document The <code>Document</code>.
     * @param fields The fields to write.
     * @return An XML-representation of this object.
     */
    public Element toXMLElementPartial(Document document, String... fields) {
        return toXMLElement(null, document, true, false, fields);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @param fields An array of field names, which if non-null
     *               indicates this should be a partial write.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document,
                                boolean showAll, boolean toSavedGame,
                                String[] fields) {
        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            if (fields == null) {
                toXML(xsw, player, showAll, toSavedGame);
            } else {
                toXMLPartialImpl(xsw, fields);
            }
            xsw.close();

            DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
                return (Element)document.importNode(tempDocument.getDocumentElement(), true);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                logger.log(Level.WARNING, "ParserConfigurationException", pce);
                throw new IllegalStateException("ParserConfigurationException: "
                    + pce.getMessage());
            } catch (SAXException se) {
                logger.log(Level.WARNING, "SAXException", se);
                throw new IllegalStateException("SAXException: "
                    + se.getMessage());
            } catch (IOException ie) {
                logger.log(Level.WARNING, "IOException", ie);
                throw new IllegalStateException("IOException: "
                    + ie.getMessage());
            }
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException: "
                + e.getMessage());
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * All attributes will be made visible.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(XMLStreamWriter, Player, boolean, boolean)
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out);
    }

    /**
     * This method writes an XML-representation of this object with
     * a specified tag to the given stream.
     *
     * Almost all FreeColObjects end up calling this, and implementing
     * their own write{Attributes,Children} methods which begin by
     * calling their superclass.  This allows a clean nesting of the
     * serialization routines throughout the class hierarchy.
     *
     * All attributes will be made visible.
     *
     * @param out The target stream.
     * @param tag The tag to use.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void toXML(XMLStreamWriter out, String tag)
        throws XMLStreamException {
        out.writeStartElement(tag);
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     * To be overridden by any object that uses
     * the toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        if (getId() == null) {
            logger.warning("FreeColObject with null id: " + toString());
        } else {
            out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        }
    }

    /**
     * Write the children of this object to a stream.
     * To be overridden by any object that has children and uses the
     * toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        // do nothing
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player,
                      boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // FreeColObjects are not to contain data that varies with
        // the observer, so the extra arguments are moot here.
        // However, this method is overridden in FreeColGameObject
        // where they are meaningful, and we need a version here for
        // toXMLElement() to call.
        toXMLImpl(out);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out)
        throws XMLStreamException;

    /**
     * This method writes a partial XML-representation of this object to
     * the given stream using only the mandatory and specified fields.
     * Ideally this would be abstract, but as not all FreeColObject-subtypes
     * need partial updates we provide a non-operating stub here which is
     * to be overridden where needed.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        throw new UnsupportedOperationException("Partial update of unsupported type.");
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
     */
    public void readFromXMLElement(Element element) {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                logger.log(Level.WARNING, "TransformerException", e);
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.log(Level.WARNING, "XMLStreamException", e);
            throw new IllegalStateException("XMLStreamException");
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
     * @param specification a <code>Specification</code> value
     */
    public void readFromXMLElement(Element element, Specification specification) {
        setSpecification(specification);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                logger.log(Level.WARNING, "TransformerException", e);
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.log(Level.WARNING, "XMLStreamException", e);
            throw new IllegalStateException("XMLStreamException");
        }
    }


    /**
     * Initializes this object from an XML-representation of this object,
     * unless the PARTIAL_ATTRIBUTE tag is present which indicates
     * a partial update of an existing object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        if (in.getAttributeValue(null, PARTIAL_ATTRIBUTE) == null) {
            readAttributes(in);
            readChildren(in);
        } else {
            readFromXMLPartialImpl(in);
        }
    }

    /**
     * Reads an XML-representation of an array.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected int[] readFromArrayElement(String tagName, XMLStreamReader in, int[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }

        int[] array = new int[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];

        for (int x=0; x<array.length; x++) {
            array[x] = Integer.parseInt(in.getAttributeValue(null, "x" + Integer.toString(x)));
        }

        in.nextTag();
        return array;
    }

    /**
     * Reads an XML-representation of a list.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param type The type of the items to be added. This type
     *      needs to have a constructor accepting a single
     *      <code>String</code>.
     * @return The list.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected <T> List<T> readFromListElement(String tagName, XMLStreamReader in, Class<T> type)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            throw new XMLStreamException(tagName + " expected, not:" + in.getLocalName());
        }
        final int length = Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE));
        List<T> list = new ArrayList<T>(length);
        for (int x = 0; x < length; x++) {
            try {
                final String value = in.getAttributeValue(null, "x" + Integer.toString(x));
                final T object;
                if (value != null) {
                    Constructor<T> c = type.getConstructor(type);
                    object = c.newInstance(new Object[] {value});
                } else {
                    object = null;
                }
                list.add(object);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException(tagName + " end expected, not: " + in.getLocalName());
        }
        return list;
    }

    /**
     * Reads an XML-representation of an array.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected String[] readFromArrayElement(String tagName, XMLStreamReader in, String[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        String[] array = new String[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];
        for (int x=0; x<array.length; x++) {
            array[x] = in.getAttributeValue(null, "x" + Integer.toString(x));
        }

        in.nextTag();
        return array;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @return an <code>int</code> value
     */
    public boolean hasAttribute(XMLStreamReader in, String attributeName) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        return attributeString != null;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue an <code>int</code> value
     * @return an <code>int</code> value
     */
    public int getAttribute(XMLStreamReader in, String attributeName, int defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        int result = defaultValue;
        if (attributeString != null) {
            try {
                result = Integer.parseInt(attributeString);
            } catch(NumberFormatException e) {
                logger.warning("Attribute '" + attributeName + "' should be an integer, not '"
                               + attributeString + "'.");
            }
        }
        return result;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue an <code>int</code> value
     * @return an <code>int</code> value
     */
    public <T extends Enum<T>> T getAttribute(XMLStreamReader in, String attributeName,
                                              Class<T> returnType, T defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        T result = defaultValue;
        if (attributeString != null) {
            try {
                result = Enum.valueOf(returnType, attributeString);
            } catch(NumberFormatException e) {
                logger.warning("Attribute '" + attributeName + "' should be a "
                               + defaultValue.getClass().getName() + " value, not '"
                               + attributeString + "'.");
            }
        }
        return result;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue a <code>float</code> value
     * @return an <code>int</code> value
     */
    public float getAttribute(XMLStreamReader in, String attributeName, float defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        float result = defaultValue;
        if (attributeString != null) {
            try {
                result = Float.parseFloat(attributeString);
            } catch(NumberFormatException e) {
                logger.warning("Attribute '" + attributeName + "' should be a float, not '"
                               + attributeString + "'.");
            }
        }
        return result;
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue a <code>boolean</code> value
     * @return an <code>boolean</code> value
     */
    public static boolean getAttribute(XMLStreamReader in, String attributeName, boolean defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return Boolean.parseBoolean(attributeString);
        } else {
            return defaultValue;
        }
    }

    /**
     * Return an attribute value or the default value.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param attributeName An attribute name
     * @param defaultValue an <code>String</code> value
     * @return an <code>String</code> value
     */
    public String getAttribute(XMLStreamReader in, String attributeName, String defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return attributeString;
        } else {
            return defaultValue;
        }
    }

    /**
     * Write an ID attribute if object is not null.
     *
     * @param out a <code>XMLStreamWriter</code> value
     * @param attributeName a <code>String</code> value
     * @param object a <code>FreeColObject</code> value
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
        FreeColObject object)
        throws XMLStreamException {
        if (object != null) {
            out.writeAttribute(attributeName, object.getId());
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        String newId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        // @compat 0.9.x
        if (newId == null) {
            setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        // end compatibility code
        } else {
            setId(newId);
        }
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * Reads a single child object.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if an error occurs
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        // do nothing
    }

    /**
     * Updates this object from an XML-representation of this object.
     * Ideally this would be abstract, but as not all FreeColObject-subtypes
     * need partial updates we provide a non-operating stub here which is
     * to be overridden where needed.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        throw new UnsupportedOperationException("Partial update of unsupported type");
    }

    //  ---------- PROPERTY CHANGE SUPPORT DELEGATES ----------

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        if (pcs != null) {
            pcs.firePropertyChange(event);
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners(propertyName);
        }
    }

    public boolean hasListeners(String propertyName) {
        if (pcs == null) {
            return false;
        } else {
            return pcs.hasListeners(propertyName);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Gets the feature container for this object, if any.
     * None is provided here, but select subclasses will override.
     *
     * @return Null.
     */
    public FeatureContainer getFeatureContainer() {
        return null;
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The id of the ability to test.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id) {
        return hasAbility(id, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColGameObjectType fcgot) {
        return hasAbility(id, fcgot, null);
    }

    /**
     * Is an ability present in this object?
     * Subclasses with complex ability handling should override this
     * routine.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public boolean hasAbility(String id, FreeColGameObjectType fcgot,
                              Turn turn) {
        return FeatureContainer.hasAbility(getFeatureContainer(),
                                           id, fcgot, turn);
    }

    /**
     * Checks if this object contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return FeatureContainer.containsAbilityKey(getFeatureContainer(),
                                                   key);
    }

    /**
     * Gets a copy of the abilities of this object.
     *
     * @return A set of abilities.
     */
    public Set<Ability> getAbilities() {
        return FeatureContainer.getAbilities(getFeatureContainer());
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     *
     * @param id The id of the ability to test.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilitySet(String id) {
        return getAbilitySet(id, null);
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilitySet(String id,
                                            FreeColGameObjectType fcgot) {
        return getAbilitySet(id, fcgot, null);
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     * Subclasses with complex ability handling should override this
     * routine.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of abilities.
     */
    public Set<Ability> getAbilitySet(String id,
                                      FreeColGameObjectType fcgot,
                                      Turn turn) {
        return FeatureContainer.getAbilitySet(getFeatureContainer(),
                                              id, fcgot, turn);
    }

    /**
     * Add the given ability to this object.
     *
     * @param ability An <code>Ability</code> to add.
     * @return True if the ability was added.
     */
    public boolean addAbility(Ability ability) {
        return FeatureContainer.addAbility(getFeatureContainer(), ability);
    }

    /**
     * Remove the given ability from this object.
     *
     * @param ability An <code>Ability</code> to remove.
     * @return The ability removed.
     */
    public Ability removeAbility(Ability ability) {
        return FeatureContainer.removeAbility(getFeatureContainer(), ability);
    }

    /**
     * Remove all abilities with a given Id.
     *
     * @param id The id of the abilities to remove.
     */
    public void removeAbilities(String id) {
        FeatureContainer.removeAbilities(getFeatureContainer(), id);
    }


    /**
     * Checks if this object contains a given modifier key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsModifierKey(String key) {
        return FeatureContainer.containsModifierKey(getFeatureContainer(),
                                                    key);
    }

    /**
     * Gets a copy of the modifiers of this object.
     *
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifiers() {
        return FeatureContainer.getModifiers(getFeatureContainer());
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     *
     * @param id The id of the modifier to test.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifierSet(String id) {
        return getModifierSet(id, null);
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifierSet(String id,
                                              FreeColGameObjectType fcgot) {
        return getModifierSet(id, fcgot, null);
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     * Subclasses with complex modifier handling should override this
     * routine.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifierSet(String id,
                                        FreeColGameObjectType fcgot,
                                        Turn turn) {
        return FeatureContainer.getModifierSet(getFeatureContainer(), 
                                               id, fcgot, turn);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id) {
        return applyModifier(number, id, null);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id,
                                     FreeColGameObjectType fcgot) {
        return applyModifier(number, id, fcgot, null);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id,
                                     FreeColGameObjectType fcgot, Turn turn) {
        return FeatureContainer.applyModifier(getFeatureContainer(),
                                              number, id, fcgot, turn);
    }

    /**
     * Add the given modifier to this object.
     *
     * @param modifier An <code>Modifier</code> to add.
     * @return True if the modifier was added.
     */
    public boolean addModifier(Modifier modifier) {
        return FeatureContainer.addModifier(getFeatureContainer(), modifier);
    }

    /**
     * Remove the given modifier from this object.
     *
     * @param modifier An <code>Modifier</code> to remove.
     * @return The modifier removed.
     */
    public Modifier removeModifier(Modifier modifier) {
        return FeatureContainer.removeModifier(getFeatureContainer(), modifier);
    }

    /**
     * Remove all abilities with a given Id.
     *
     * @param id The id of the abilities to remove.
     */
    public void removeModifiers(String id) {
        FeatureContainer.removeModifiers(getFeatureContainer(), id);
    }


    /**
     * Adds all the features in an object to this object.
     *
     * @param fco The <code>FreeColObject</code> to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer.addFeatures(getFeatureContainer(), fco);
    }

    /**
     * Removes all the features in an object from this object.
     *
     * @param fco The <code>FreeColObject</code> to find features to remove in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer.removeFeatures(getFeatureContainer(), fco);
    }


    /**
     * Gets the tag name used to serialize this object, generally the
     * class name starting with a lower case letter. This method
     * should be overridden by all subclasses that need to be
     * serialized.
     *
     * @return <code>null</code>.
     */
    public static String getXMLElementTagName() {
        return null;
    }
}
