/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.common.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import net.sf.freecol.common.resources.ResourceFactory;
import net.sf.freecol.common.resources.ResourceMapping;


/**
 * Support for reading a FreeCol data file. The data file
 * is either a ZIP-file or a directory containing certain files.
 */
public class FreeColDataFile {
    private static final Logger logger = Logger.getLogger(FreeColDataFile.class.getName());
    
    private static final String RESOURCES_PROPERTIES_FILE = "resources.properties";

    /** The file this object represents. */
    private final File file;

    /**
     * A prefix string for the jar-entries (only if {@link #file} is
     * a ZIP-file).
     */
    private final String jarDirectory;

    /**
     * A inputstream directly to a savegame (only if {@link file} is
     * an outdated savegame).
     */
    private final InputStream supportOldSavegames;
    
    
    /**
     * Opens the given file for reading.
     * 
     * @param file The file to be read.
     */
    public FreeColDataFile(File file) {
        if (!file.exists()) {
            for (String ending : getFileEndings()) {
                final File tempFile = new File(file.getAbsolutePath() + ending);
                if (tempFile.exists()) {
                    file = tempFile;
                    break;
                }
            }
        }
        
        this.file = file;
        
        if (file.isDirectory()) {
            this.jarDirectory = null;
            this.supportOldSavegames = null;
        } else {
            // START SUPPORT OLD SAVEGAMES
            try {
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                in.mark(10);
                byte[] buf = new byte[5];
                in.read(buf, 0, 5);
                in.reset();
                if ((new String(buf)).startsWith("PK")) {
                    // START KEEP
                    this.jarDirectory = findJarDirectory(file.getName().substring(0, file.getName().lastIndexOf('.')), file);
                    // END KEEP
                    this.supportOldSavegames = null;
                } else { 
                    if (!(new String(buf)).equals("<?xml")) {
                        in = new BufferedInputStream(new GZIPInputStream(in));
                    }
                    this.supportOldSavegames = in;
                    this.jarDirectory = null;
                }
            } catch (IOException e) {
                throw new RuntimeException("IOException while trying to load a legacy savegame.", e);
            }
            // END SUPPORT OLD SAVEGAMES            
        }
    }
    
    /**
     * Finds the directory within the zip-file in case the data file
     * has been renamed.
     * 
     * @param expectedName The name the directory should have.
     * @param file The zip-file.
     * @return The name of the base directory in the zip-file.
     */
    private static String findJarDirectory(final String expectedName, File file) {
        JarFile jf = null;
        try {
            jf = new JarFile(file);
            final JarEntry entry = jf.entries().nextElement();
            final String en = entry.getName();
            final int index = (en.lastIndexOf('/') > 0) ? en.lastIndexOf('/') : en.length() - 1;
            final String name = en.substring(0, index);
            if (!name.equals(expectedName)) {
                logger.warning("Expected directory '" + expectedName + "', but found '" + name + "' in data file.");
            }
            return name;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while reading data file.", e);
            return expectedName;
        } finally {
            try {
                jf.close();
            } catch (Exception e) {}
        }
    }

    /**
     * Returns an input stream for the specified resource.
     * @param filename The filename of a resource within this collection of
     *      data. If this object represents a directory then the provided filename
     *      should be relative towards the path of the directory. In case
     *      of a compressed archieve it should be the path within the
     *      archive. 
     * @return
     */
    protected InputStream getInputStream(String filename) throws IOException {
        if (supportOldSavegames != null) {
            return supportOldSavegames;
        } else {
            return new BufferedInputStream(getURL(filename).openStream());
        }
    }
    
    private URL getURL(String filename) {
        try {
            if (file.isDirectory()) {
                return new URL("file", null, (new File(file, filename)).getAbsolutePath());
            } else {
                return new URL("jar:file:" + file.getAbsoluteFile() + "!/" + jarDirectory + "/" + filename);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception while reading ResourceMapping from: " + file, e);
            return null;
        }
    }
    
    /**
     * Creates a <code>ResourceMapping</code> from the file
     * {@value #RESOURCES_PROPERTIES_FILE}.
     * 
     * @return A <code>ResourceMapping</code> or <code>null</code>
     *      there is no resource mapping file.
     */
    public ResourceMapping getResourceMapping() {
        try {
            final Properties properties = new Properties();
            properties.load(getInputStream(RESOURCES_PROPERTIES_FILE));
            ResourceMapping rc = new ResourceMapping();
            if (supportOldSavegames != null) {
                return rc;
            }
            Enumeration<?> pn = properties.propertyNames();
            while (pn.hasMoreElements()) {
                final String key = (String) pn.nextElement();
                final URL resourceLocator = getURL(properties.getProperty(key));
                rc.add(key, ResourceFactory.createResource(resourceLocator));
            }  
            return rc;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception while reading ResourceMapping from: " + file, e);
            return null;
        }
    }
    
    /**
     * Returns a <code>FileFilter</code>.
     * @return The <code>FileFilter</code>.
     */
    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                final String name = f.getName();
                for (String ending : getFileEndings()) {
                    if (name.endsWith(ending)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    /**
     * File endings that are supported for this type of data file.
     * @return An array with a single element: ".zip".
     */
    protected String[] getFileEndings() {
        return new String[] {".zip"};   
    }
}
