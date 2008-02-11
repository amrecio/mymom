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

package net.sf.freecol.client.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.panel.ImageProvider;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;

/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary extends ImageProvider {

    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());

    public static final int UNIT_SELECT = 0, PLOWED = 4, TILE_TAKEN = 5, TILE_OWNED_BY_INDIANS = 6,
            LOST_CITY_RUMOUR = 7, DARKNESS = 8, MISC_COUNT = 10;

    private static final int numberOfUnitTypes = FreeCol.getSpecification().numberOfUnitTypes();

    /**
     * These finals are for quick reference. These should be made softcoded when next possible.
     */
    public static final int TERRAIN_COUNT = 16, BONUS_COUNT = 9, GOODS_COUNT = 20, FOREST_COUNT = 9;

    public static final int MONARCH_COUNT = 4;

    public static final int UNIT_BUTTON_WAIT = 0, UNIT_BUTTON_DONE = 1, UNIT_BUTTON_FORTIFY = 2,
            UNIT_BUTTON_SENTRY = 3, UNIT_BUTTON_CLEAR = 4, UNIT_BUTTON_PLOW = 5, UNIT_BUTTON_ROAD = 6,
            UNIT_BUTTON_BUILD = 7, UNIT_BUTTON_DISBAND = 8, UNIT_BUTTON_ZOOM_IN = 9, UNIT_BUTTON_ZOOM_OUT = 10,
            UNIT_BUTTON_COUNT = 11;

    private static final int COLONY_SMALL = 0, COLONY_MEDIUM = 1, COLONY_LARGE = 2, COLONY_STOCKADE = 3,
            COLONY_FORT = 4, COLONY_FORTRESS = 5, COLONY_MEDIUM_STOCKADE = 6, COLONY_LARGE_STOCKADE = 7,
            COLONY_LARGE_FORT = 8, COLONY_UNDEAD = 9, COLONY_COUNT = 10,

            INDIAN_SETTLEMENT_CAMP = 0,
            // INDIAN_SETTLEMENT_VILLAGE = 1,
            // INDIAN_SETTLEMENT_AZTEC = 2,
            // INDIAN_SETTLEMENT_INCA = 3,

            INDIAN_COUNT = 4;

    private static final String path = new String("images/"), extension = new String(".png"),
            unitsDirectory = new String("units/"),
            terrainDirectory = new String("terrain/"),
            tileName = new String("center"), borderName = new String("border"),
            unexploredDirectory = new String("unexplored/"), unexploredName = new String("unexplored"),
            riverDirectory = new String("river/"), riverName = new String("river"),
            miscDirectory = new String("misc/"), miscName = new String("Misc"),
            unitButtonDirectory = new String("order-buttons/"), unitButtonName = new String("button"),
            colonyDirectory = new String("colonies/"), colonyName = new String("Colony"),
            indianDirectory = new String("indians/"), indianName = new String("Indians"),
            monarchDirectory = new String("monarch/"), monarchName = new String("Monarch");

    private final String dataDirectory;

    /**
     * A Vector of Image objects.
     */
    private Vector<ImageIcon> rivers, // Holds ImageIcon objects
        misc, // Holds ImageIcon objects
        colonies, // Holds ImageIcon objects
        indians, // Holds ImageIcon objects
        monarch; // Holds ImageIcon objects

    private ImageIcon[] units, // Holds ImageIcon objects
        unitsGrayscale; // Holds ImageIcon objects of units in grayscale

    private Hashtable<String, ImageIcon> terrain1, terrain2, overlay1, overlay2,
            forests, bonus, goods;

    private Hashtable<String, Vector<ImageIcon>> border1, border2, coast1, coast2;

    // Holds the unit-order buttons
    private Vector<Vector<ImageIcon>> unitButtons; 

    private EnumMap<Tension.Level, Image> alarmChips;

    private Hashtable<Color, Image> colorChips;

    private Hashtable<Color, Image> missionChips;

    private Hashtable<Color, Image> expertMissionChips;

    /**
     * The scaling factor used when creating this
     * <code>ImageLibrary</code>. The value
     * <code>1</code> is used if this object is not
     * a result of a scaling operation.
     */
    private final float scalingFactor;


    /**
     * The constructor to use.
     * 
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary() throws FreeColException {
        // This is the location of the data directory when running FreeCol
        // from its default location in the CVS repository.
        // dataDirectory = "";
        // init(true);
        this("");
    }

    /**
     * A constructor that takes a directory as FreeCol's home.
     * 
     * @param freeColHome The home of the freecol files.
     * @throws FreeColException If one of the data files could not be found.
     */
    public ImageLibrary(String freeColHome) throws FreeColException {
        this.scalingFactor = 1;
        // TODO: normally this check shouldn't be here. init(false) is the way
        // to go.
        if (freeColHome.equals("")) {
            dataDirectory = "data/";
            init(true);
        } else {
            dataDirectory = freeColHome;
            init(false);
        }
    }

    /**
     * Private constructor used for cloning and getting a
     * scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The scaling factor.
     * @see #getScaledImageLibrary
     */
    private ImageLibrary(float scalingFactor,
            ImageIcon[] units,
            ImageIcon[] unitsGrayscale,
            Vector<ImageIcon> rivers,
            Vector<ImageIcon> misc,
            Vector<ImageIcon> colonies,
            Vector<ImageIcon> indians,
            Hashtable<String, ImageIcon>  terrain1,
            Hashtable<String, ImageIcon>  terrain2,
            Hashtable<String, ImageIcon> overlay1,
            Hashtable<String, ImageIcon> overlay2,
            Hashtable<String, ImageIcon> forests,
            Hashtable<String, ImageIcon> bonus,
            Hashtable<String, ImageIcon> goods,
            Hashtable<String, Vector<ImageIcon>> border1,
            Hashtable<String, Vector<ImageIcon>> border2,
            Hashtable<String, Vector<ImageIcon>> coast1,
            Hashtable<String, Vector<ImageIcon>> coast2,
            Vector<Vector<ImageIcon>> unitButtons,
            EnumMap<Tension.Level, Image> alarmChips,
            Hashtable<Color, Image> colorChips,
            Hashtable<Color, Image> missionChips,
            Hashtable<Color, Image> expertMissionChips) {
        dataDirectory = "";

        this.scalingFactor = scalingFactor;
        this.units = units;
        this.unitsGrayscale = unitsGrayscale;
        this.rivers = rivers;
        this.misc = misc;
        this.colonies = colonies;
        this.indians = indians;
        this.terrain1 = terrain1;
        this.terrain2 = terrain2;
        this.overlay1 = overlay1;
        this.overlay2 = overlay2;
        this.forests = forests;
        this.bonus = bonus;
        this.goods = goods;
        this.border1 = border1;
        this.border2 = border2;
        this.coast1 = coast1;
        this.coast2 = coast2;

        this.unitButtons = unitButtons;
        this.alarmChips = alarmChips;
        this.colorChips = colorChips;
        this.missionChips = missionChips;
        this.expertMissionChips = expertMissionChips;

        scaleImages(scalingFactor);
    }


    /**
     * Performs all necessary init operations such as loading of data files.
     * 
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found. *
     */
    private void init(boolean doLookup) throws FreeColException {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();

        Class<FreeCol> resourceLocator = net.sf.freecol.FreeCol.class;

        loadUnits(gc, resourceLocator, doLookup);
        loadTerrain(gc, resourceLocator, doLookup);
        loadForests(gc, resourceLocator, doLookup);
        loadRivers(gc, resourceLocator, doLookup);
        loadMisc(gc, resourceLocator, doLookup);
        loadUnitButtons(gc, resourceLocator, doLookup);
        loadColonies(gc, resourceLocator, doLookup);
        loadIndians(gc, resourceLocator, doLookup);
        loadGoods(gc, resourceLocator, doLookup);
        loadBonus(gc, resourceLocator, doLookup);
        loadMonarch(gc, resourceLocator, doLookup);

        alarmChips = new EnumMap<Tension.Level, Image>(Tension.Level.class);
        colorChips = new Hashtable<Color, Image>();
        missionChips = new Hashtable<Color, Image>();
        expertMissionChips = new Hashtable<Color, Image>();
    }

    /**
     * Returns the scaling factor used when creating this ImageLibrary.
     * @return 1 unless {@link #getScaledImageLibrary} was used to create
     *      this object.
     */
    public float getScalingFactor() {
        return scalingFactor;
    }

    /**
     * Gets a scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     * @return A new <code>ImageLibrary</code>.
     */
    public ImageLibrary getScaledImageLibrary(float scalingFactor) {
        return new ImageLibrary(scalingFactor, units, unitsGrayscale, rivers,
                misc, colonies, indians, terrain1, terrain2, overlay1, overlay2, forests, bonus, goods, border1, border2, coast1, coast2, unitButtons,
                alarmChips, colorChips, missionChips, expertMissionChips);
    }

    /**
     * Scales the images in this <code>ImageLibrary</code>
     * using the given factor.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     */
    private void scaleImages(float scalingFactor) {
        units = scaleImages(units, scalingFactor);
        unitsGrayscale = scaleImages(unitsGrayscale, scalingFactor);
        rivers = scaleImages(rivers, scalingFactor);
        misc = scaleImages(misc, scalingFactor);
        colonies = scaleImages(colonies, scalingFactor);
        indians = scaleImages(indians, scalingFactor);
        //monarch = scaleImages(monarch);

        terrain1 = scaleImages3(terrain1, scalingFactor, Image.SCALE_FAST);
        terrain2 = scaleImages3(terrain2, scalingFactor, Image.SCALE_FAST);
        overlay1 = scaleImages3(overlay1, scalingFactor);
        overlay2 = scaleImages3(overlay2, scalingFactor);
        forests = scaleImages3(forests, scalingFactor);
        bonus = scaleImages3(bonus, scalingFactor);
        goods = scaleImages3(goods, scalingFactor);
        
        border1 = scaleImages2(border1, scalingFactor);
        border2 = scaleImages2(border2, scalingFactor);
        coast1 = scaleImages2(coast1, scalingFactor);
        coast2 = scaleImages2(coast2, scalingFactor);
        //unitButtons = scaleImages2(unitButtons);
        /*
        alarmChips = scaleImages(alarmChips, scalingFactor);
        colorChips = scaleImages(colorChips, scalingFactor);
        missionChips = scaleImages(missionChips, scalingFactor);
        expertMissionChips = scaleImages(expertMissionChips, scalingFactor);
        */
    }

    private Image[] scaleImages(Image[] list, float f) {
        Image[] output = new Image[list.length];
        for (int i=0; i<list.length; i++) {
            Image im = list[i];
            if (im != null) {
                output[i] = im.getScaledInstance(Math.round(im.getWidth(null) * f), Math.round(im.getHeight(null) * f), Image.SCALE_SMOOTH);
            }
        }

        return output;
    }

    private Hashtable<Color, Image> scaleImages(Hashtable<Color, Image> hashtable, float f) {
        Hashtable<Color, Image> output = new Hashtable<Color, Image>();
        for (Color c : hashtable.keySet()) {
            Image im = hashtable.get(c);
            output.put(c, im.getScaledInstance(Math.round(im.getWidth(null) * f), Math.round(im.getHeight(null) * f), Image.SCALE_SMOOTH));
        }
        return output;
    }
    
    private Hashtable<String, ImageIcon> scaleImages3(Hashtable<String, ImageIcon> hashtable, float f) {
        return scaleImages3(hashtable, f, Image.SCALE_SMOOTH);
    }
    
    private Hashtable<String, ImageIcon> scaleImages3(Hashtable<String, ImageIcon> hashtable, float f, int scalingMethod) {
        Hashtable<String, ImageIcon> output = new Hashtable<String, ImageIcon>();
        for (String key : hashtable.keySet()) {
            Image im = hashtable.get(key).getImage();
            output.put(key, new ImageIcon(im.getScaledInstance(Math.round(im.getWidth(null) * f), Math.round(im.getHeight(null) * f), scalingMethod)));
        }
        return output;
    }
    
    private Hashtable<String, Vector<ImageIcon>> scaleImages2(Hashtable<String, Vector<ImageIcon>> hashtable, float f) {
        Hashtable<String, Vector<ImageIcon>> output = new Hashtable<String, Vector<ImageIcon>>();
        for (String key : hashtable.keySet()) {
            if (hashtable.get(key) == null) {
                output.put(key, null);
            } else {
                Vector<ImageIcon> outputV = new Vector<ImageIcon>();
                for (ImageIcon icon : hashtable.get(key)) {
                    if (icon == null) {
                        outputV.add(null);
                    } else {
                        Image im = icon.getImage();
                        outputV.add(new ImageIcon(im.getScaledInstance(Math.round(im.getWidth(null) * f), Math.round(im.getHeight(null) * f), Image.SCALE_SMOOTH)));
                    }
                }
                output.put(key, outputV);
            }
        }
        return output;
    }

    private Vector<ImageIcon> scaleImages(Vector<ImageIcon> list, float f) {
        Vector<ImageIcon> output = new Vector<ImageIcon>();
        for (ImageIcon im : list) {
            if (im != null) {
                output.add(new ImageIcon(im.getImage().getScaledInstance(Math.round(im.getIconWidth() * f), Math.round(im.getIconHeight() * f), Image.SCALE_SMOOTH)));
            } else {
                output.add(null);
            }
        }
        return output;
    }

    private ImageIcon[] scaleImages(ImageIcon[] input, float f) {
        ImageIcon[] output = new ImageIcon[input.length];
        for (int index = 0; index < input.length; index++) {
            ImageIcon inputIcon = input[index];
            if (inputIcon != null) {
                output[index] = new ImageIcon(inputIcon.getImage().getScaledInstance(Math.round(inputIcon.getIconWidth() * f), Math.round(inputIcon.getIconHeight() * f), Image.SCALE_SMOOTH));
            }
        }
        return output;
    }

    private Vector<Vector<ImageIcon>> scaleImages2(Vector<Vector<ImageIcon>> list, float f) {
        Vector<Vector<ImageIcon>> output = new Vector<Vector<ImageIcon>>();
        for (Vector<ImageIcon> v : list) {
            Vector<ImageIcon> outputV = new Vector<ImageIcon>();
            output.add(outputV);
            for (ImageIcon im : v) {
                if (im != null) {
                    outputV.add(new ImageIcon(im.getImage().getScaledInstance(Math.round(im.getIconWidth() * f), Math.round(im.getIconHeight() * f), Image.SCALE_SMOOTH)));
                } else {
                    outputV.add(null);
                }
            }
        }
        return output;
    }

    /**
     * Finds the image file in the given <code>filePath</code>.
     * 
     * @param filePath The path to the image file.
     * @param doLookup If <i>true</i> then the <code>resourceLocator</code>
     *            is used when searching for the image file.
     * @return An ImageIcon with data loaded from the image file.
     * @exception FreeColException If the image could not be found.
     */
    private ImageIcon findImage(String filePath, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        if (doLookup) {
            URL url = resourceLocator.getResource(filePath);
            if (url != null) {
                return new ImageIcon(url);
            }
        }

        File tmpFile = new File(filePath);
        if ((tmpFile == null) || !tmpFile.exists() || !tmpFile.isFile() || !tmpFile.canRead()) {
            throw new FreeColException("The data file \"" + filePath + "\" could not be found.");
        }

        return new ImageIcon(filePath);
    }

    /**
     * Loads the unit-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnits(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {

        ArrayList<ImageIcon> unitIcons = new ArrayList<ImageIcon>();
        ArrayList<ImageIcon> unitIconsGrayscale = new ArrayList<ImageIcon>();

        for (Role role : Role.values()) {
            String filePath = dataDirectory + path + unitsDirectory;
            String fileName = null;

            if (role == Role.DEFAULT) {
                fileName = "unit" + extension;
            } else {
                String roleName = role.toString().toLowerCase();
                filePath += roleName + "/";
                fileName = roleName + extension;
            }
            ImageIcon defaultIcon = findImage(filePath + fileName, resourceLocator, doLookup);
            ImageIcon defaultIconGrayscale = convertToGrayscale(defaultIcon.getImage());

            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                fileName = unitType.getArt() + extension;
                try {
                    ImageIcon unitIcon = findImage(filePath + fileName, resourceLocator, doLookup);
                    unitIcons.add(unitIcon);
                    unitIconsGrayscale.add(convertToGrayscale(unitIcon.getImage()));
                } catch (FreeColException e) {
                    logger.fine("Using default icon for UnitType " + unitType.getName());
                    unitIcons.add(defaultIcon);
                    unitIconsGrayscale.add(defaultIconGrayscale);
                }
            }
        }

        units = unitIcons.toArray(new ImageIcon[0]);
        unitsGrayscale = unitIconsGrayscale.toArray(new ImageIcon[0]);

        /*
         * If all units are patched together in one graphics file then this is
         * the way to load them into different images:
         * 
         * Image unitsImage = new ImageIcon(url).getImage(); BufferedImage
         * tempImage = gc.createCompatibleImage(42, 63,
         * Transparency.TRANSLUCENT);
         * tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
         * units.add(tempImage);
         */
    }

    /**
     * Loads the terrain-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadTerrain(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        terrain1 = new Hashtable<String, ImageIcon>();
        terrain2 = new Hashtable<String, ImageIcon>();
        overlay1 = new Hashtable<String, ImageIcon>();
        overlay2 = new Hashtable<String, ImageIcon>();
        border1 = new Hashtable<String, Vector<ImageIcon>>();
        border2 = new Hashtable<String, Vector<ImageIcon>>();
        coast1 = new Hashtable<String, Vector<ImageIcon>>();
        coast2 = new Hashtable<String, Vector<ImageIcon>>();
        
        for (TileType type : FreeCol.getSpecification().getTileTypeList()) {
            String filePath = dataDirectory + path + type.getArtBasic() + tileName;
            terrain1.put(type.getId(), findImage(filePath + "0" + extension, resourceLocator, doLookup));
            terrain2.put(type.getId(), findImage(filePath + "1" + extension, resourceLocator, doLookup));

            if (type.getArtOverlay() != null) {
                filePath = dataDirectory + path + type.getArtOverlay();
                overlay1.put(type.getId(), findImage(filePath + "0" + extension, resourceLocator, doLookup));
                overlay2.put(type.getId(), findImage(filePath + "1" + extension, resourceLocator, doLookup));
            }
            
            Vector<ImageIcon> tempVector1 = new Vector<ImageIcon>();
            Vector<ImageIcon> tempVector2 = new Vector<ImageIcon>();
            for (Direction direction : Direction.values()) {
                filePath = dataDirectory + path + type.getArtBasic() + borderName + "_" +
                    direction.toString();
                tempVector1.add(findImage(filePath + "_even" + extension, resourceLocator, doLookup));
                tempVector2.add(findImage(filePath + "_odd" + extension, resourceLocator, doLookup));
            }

            border1.put(type.getId(), tempVector1);
            border2.put(type.getId(), tempVector2);
            
            if (type.getArtCoast() != null) {
                tempVector1 = new Vector<ImageIcon>();
                tempVector2 = new Vector<ImageIcon>();
                for (Direction direction : Direction.values()) {
                    filePath = dataDirectory + path + type.getArtCoast() + borderName + "_" +
                        direction.toString();
                    tempVector1.add(findImage(filePath + "_even" + extension, resourceLocator, doLookup));
                    tempVector2.add(findImage(filePath + "_odd" + extension, resourceLocator, doLookup));
                }
                
                coast1.put(type.getId(), tempVector1);
                coast2.put(type.getId(), tempVector2);
            }
        }
        
        String unexploredPath = dataDirectory + path + terrainDirectory + unexploredDirectory + tileName;
        terrain1.put(unexploredName, findImage(unexploredPath + "0" + extension, resourceLocator, doLookup));
        terrain2.put(unexploredName, findImage(unexploredPath + "1" + extension, resourceLocator, doLookup));
        
        Vector<ImageIcon> unexploredVector1 = new Vector<ImageIcon>();
        Vector<ImageIcon> unexploredVector2 = new Vector<ImageIcon>();
        for (Direction direction : Direction.values()) {
            unexploredPath = dataDirectory + path + terrainDirectory + unexploredDirectory + borderName + 
                "_" + direction.toString();
            unexploredVector1.add(findImage(unexploredPath + "_even" + extension, resourceLocator, doLookup));
            unexploredVector2.add(findImage(unexploredPath + "_odd" + extension, resourceLocator, doLookup));
        }

        border1.put(unexploredName, unexploredVector1);
        border2.put(unexploredName, unexploredVector2);
    }

    /**
     * Loads the river images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadRivers(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        int combinations = 81;
        rivers = new Vector<ImageIcon>(combinations);
        for (int i = 0; i < combinations; i++) {
            String filePath = dataDirectory + path + riverDirectory + riverName + i + extension;
            rivers.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the forest images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadForests(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        forests = new Hashtable<String, ImageIcon>();
        
        for (TileType type : FreeCol.getSpecification().getTileTypeList()) {
            if (type.getArtForest() != null) {
                String filePath = dataDirectory + path + type.getArtForest();
                forests.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
            }
        }
    }

    /**
     * Loads miscellaneous images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadMisc(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        misc = new Vector<ImageIcon>(MISC_COUNT);

        for (int i = 0; i < MISC_COUNT; i++) {
            String filePath = dataDirectory + path + miscDirectory + miscName + i + extension;
            misc.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the unit-order buttons from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadUnitButtons(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        unitButtons = new Vector<Vector<ImageIcon>>(4);
        for (int i = 0; i < 4; i++) {
            unitButtons.add(new Vector<ImageIcon>(UNIT_BUTTON_COUNT));
        }

        for (int i = 0; i < 4; i++) {
            String subDirectory;
            switch (i) {
            case 0:
                subDirectory = new String("order-buttons00/");
                break;
            case 1:
                subDirectory = new String("order-buttons01/");
                break;
            case 2:
                subDirectory = new String("order-buttons02/");
                break;
            case 3:
                subDirectory = new String("order-buttons03/");
                break;
            default:
                subDirectory = new String("");
                break;
            }
            for (int j = 0; j < UNIT_BUTTON_COUNT; j++) {
                String filePath = dataDirectory + path + unitButtonDirectory + subDirectory + unitButtonName + j
                        + extension;
                unitButtons.get(i).add(findImage(filePath, resourceLocator, doLookup));
            }
        }
    }

    /**
     * Loads the colony pictures from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadColonies(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        colonies = new Vector<ImageIcon>(COLONY_COUNT);

        for (int i = 0; i < COLONY_COUNT; i++) {
            String filePath = dataDirectory + path + colonyDirectory + colonyName + i + extension;
            colonies.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the indian settlement pictures from files into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net.sf.freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadIndians(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        indians = new Vector<ImageIcon>(INDIAN_COUNT);

        for (int i = 0; i < INDIAN_COUNT; i++) {
            String filePath = dataDirectory + path + indianDirectory + indianName + i + extension;
            indians.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the goods-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadGoods(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        goods = new Hashtable<String, ImageIcon>();
        
        for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()) {
            String filePath = dataDirectory + path + type.getArt();
            goods.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
        }

        /*
         * If all units are patched together in one graphics file then this is
         * the way to load them into different images:
         * 
         * Image unitsImage = new ImageIcon(url).getImage(); BufferedImage
         * tempImage = gc.createCompatibleImage(42, 63,
         * Transparency.TRANSLUCENT);
         * tempImage.getGraphics().drawImage(unitsImage, 0, 0, null);
         * units.add(tempImage);
         */
    }

    /**
     * Loads the bonus-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadBonus(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        bonus = new Hashtable<String, ImageIcon>();
        
        for (ResourceType type : FreeCol.getSpecification().getResourceTypeList()) {
            String filePath = dataDirectory + path + type.getArt();
            bonus.put(type.getId(), findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Loads the monarch-images from file into memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param resourceLocator The class that is used to locate data files.
     * @param doLookup Must be set to 'false' if the path to the image files has
     *            been manually provided by the user. If set to 'true' then a
     *            lookup will be done to search for image files from
     *            net.sf.freecol, in this case the images need to be placed in
     *            net/sf/freecol/images.
     * @throws FreeColException If one of the data files could not be found.
     */
    private void loadMonarch(GraphicsConfiguration gc, Class<FreeCol> resourceLocator, boolean doLookup)
            throws FreeColException {
        monarch = new Vector<ImageIcon>(MONARCH_COUNT);

        for (int i = 0; i < MONARCH_COUNT; i++) {
            String filePath = dataDirectory + path + monarchDirectory + monarchName + i + extension;
            monarch.add(findImage(filePath, resourceLocator, doLookup));
        }
    }

    /**
     * Generates a color chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param c The color of the color chip to create.
     */
    private void loadColorChip(GraphicsConfiguration gc, Color c) {
        BufferedImage tempImage = gc.createCompatibleImage(11, 17);
        Graphics g = tempImage.getGraphics();
        if (c.equals(Color.BLACK)) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.BLACK);
        }
        g.drawRect(0, 0, 10, 16);
        g.setColor(c);
        g.fillRect(1, 1, 9, 15);
        colorChips.put(c, tempImage);
    }

    /**
     * Generates a mission chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param c The color of the color chip to create.
     * @param expertMission Should be <code>true</code> if the mission chip
     *            should represent an expert missionary.
     */
    private void loadMissionChip(GraphicsConfiguration gc, Color c, boolean expertMission) {
        BufferedImage tempImage = gc.createCompatibleImage(10, 17);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        if (expertMission) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.DARK_GRAY);
        }
        g.fillRect(0, 0, 10, 17);

        GeneralPath cross = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        cross.moveTo(4, 1);
        cross.lineTo(6, 1);
        cross.lineTo(6, 4);
        cross.lineTo(9, 4);
        cross.lineTo(9, 6);
        cross.lineTo(6, 6);
        cross.lineTo(6, 16);
        cross.lineTo(4, 16);
        cross.lineTo(4, 6);
        cross.lineTo(1, 6);
        cross.lineTo(1, 4);
        cross.lineTo(4, 4);
        cross.closePath();

        if (expertMission && c.equals(Color.BLACK)) {
            g.setColor(Color.DARK_GRAY);
        } else if ((!expertMission) && c.equals(Color.DARK_GRAY)) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(c);
        }
        g.fill(cross);

        if (expertMission) {
            expertMissionChips.put(c, tempImage);
        } else {
            missionChips.put(c, tempImage);
        }
    }

    /**
     * Generates a alarm chip image and stores it in memory.
     * 
     * @param gc The GraphicsConfiguration is needed to create images that are
     *            compatible with the local environment.
     * @param alarm The alarm level.
     */
    private void loadAlarmChip(GraphicsConfiguration gc, Tension.Level alarm) {
        BufferedImage tempImage = gc.createCompatibleImage(10, 17);
        Graphics2D g = (Graphics2D) tempImage.getGraphics();

        g.setColor(Color.BLACK);
        g.drawRect(0, 0, 10, 16);

        switch(alarm) {
        case HAPPY:
            g.setColor(Color.GREEN);
            break;
        case CONTENT:
            g.setColor(Color.BLUE);
            break;
        case DISPLEASED:
            g.setColor(Color.YELLOW);
            break;
        case ANGRY:
            g.setColor(Color.ORANGE);
            break;
        case HATEFUL:
            g.setColor(Color.RED);
            break;
        }

        g.fillRect(1, 1, 8, 15);
        g.setColor(Color.BLACK);

        g.fillRect(4, 3, 2, 7);
        g.fillRect(4, 12, 2, 2);

        alarmChips.put(alarm, tempImage);
    }

    /**
     * Returns the monarch-image for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public Image getMonarchImage(int nation) {
        return monarch.get(nation).getImage();
    }

    /**
     * Returns the monarch-image icon for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public ImageIcon getMonarchImageIcon(int nation) {
        return monarch.get(nation);
    }

    /**
     * Returns the bonus-image for the given tile.
     * 
     * @param tile
     * @return the bonus-image for the given tile.
     */
    public Image getBonusImage(Tile tile) {
        if (tile.hasResource()) {
            return getBonusImage(tile.getTileItemContainer().getResource().getType());
        } else {
            return null;
        }
    }

    public Image getBonusImage(ResourceType type) {
        return getBonusImageIcon(type).getImage();
    }

    /**
     * Returns the bonus-ImageIcon at the given index.
     * 
     * @param type The type of the bonus-ImageIcon to return.
     */
    public ImageIcon getBonusImageIcon(ResourceType type) {
        return bonus.get(type.getId());
    }

    public ImageIcon getScaledBonusImageIcon(ResourceType type, float scale) {
        return getScaledImageIcon(getBonusImageIcon(type), scale);
    }


    /**
     * Converts an image to grayscale
     * 
     * @param image Source image to convert
     * @return The image in grayscale
     */
    private ImageIcon convertToGrayscale(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        ColorConvertOp filter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage srcImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        srcImage.createGraphics().drawImage(image, 0, 0, null);
        return new ImageIcon(filter.filter(srcImage, null));
    }


    /**
     * Returns the scaled terrain-image for a terrain type (and position 0, 0).
     * 
     * @param type The type of the terrain-image to return.
     * @param scale The scale of the terrain image to return.
     * @return The terrain-image
     */
    public Image getScaledTerrainImage(TileType type, float scale) {
        // Index used for drawing the base is the artBasic value
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        Image terrainImage = getTerrainImage(type, 0, 0);
        int width = getTerrainImageWidth(type);
        int height = getTerrainImageHeight(type);
        // Currently used for hills and mountains
        if (type.getArtOverlay() != null) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getOverlayImage(type, 0, 0), 0, 0, null);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (type.isForested()) {
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, 0, null);
            g.drawImage(getForestImage(type), 0, 0, null);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (scale == 1f) {
            return terrainImage;
        } else {
            return terrainImage.getScaledInstance((int) (width * scale), (int) (height * scale), Image.SCALE_SMOOTH);
        }
    }

    /**
     * Returns the overlay-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getOverlayImage(TileType type, int x, int y) {
        if ((x + y) % 2 == 0) {
            return overlay1.get(type.getId()).getImage();
        } else {
            return overlay2.get(type.getId()).getImage();
        }
    }

    /**
     * Returns the terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getTerrainImage(TileType type, int x, int y) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        if ((x + y) % 2 == 0) {
            return terrain1.get(key).getImage();
        } else {
            return terrain2.get(key).getImage();
        }
    }

    /**
     * Returns the border terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getBorderImage(TileType type, Direction direction, int x, int y) {

        int borderType = direction.ordinal();
        
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }

        if ((x + y) % 2 == 0) {
            return border1.get(key).get(borderType).getImage();
        } else {
            return border2.get(key).get(borderType).getImage();
        }
    }

    /**
     * Returns the coast terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getCoastImage(TileType type, Direction direction, int x, int y) {

        int borderType = direction.ordinal();
        
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }

        if ((x + y) % 2 == 0) {
            return coast1.get(key).get(borderType).getImage();
        } else {
            return coast2.get(key).get(borderType).getImage();
        }
    }

    /**
     * Returns the river image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getRiverImage(int index) {
        return rivers.get(index).getImage();
    }

    /**
     * Returns the forest image for a terrain type.
     * 
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(TileType type) {
        return forests.get(type.getId()).getImage();
    }

    /**
     * Returns the image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getMiscImage(int index) {
        return misc.get(index).getImage();
    }

    /**
     * Returns the image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public ImageIcon getMiscImageIcon(int index) {
        return misc.get(index);
    }

    /**
     * Returns the unit-button image at the given index in the given state.
     * 
     * @param index The index of the image to return.
     * @param state The state (normal, highlighted, pressed, disabled)
     * @return The image pointer
     */
    public ImageIcon getUnitButtonImageIcon(int index, int state) {
        return unitButtons.get(state).get(index);
    }

    /**
     * Returns the indian settlement image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getIndianSettlementImage(int index) {
        return indians.get(index).getImage();
    }

    /**
     * Returns the goods-image at the given index.
     * 
     * @param g The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(GoodsType g) {
        return getGoodsImageIcon(g).getImage();
    }

    /**
     * Returns the goods-image for a goods type.
     * 
     * @param g The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(GoodsType g) {
        return goods.get(g.getId());
    }

    /**
     * Returns the scaled goods-ImageIcon for a goods type.
     * 
     * @param type The type of the goods-ImageIcon to return.
     * @param scale The scale of the goods-ImageIcon to return.
     * @return The goods-ImageIcon at the given index.
     */
    public ImageIcon getScaledGoodsImageIcon(GoodsType type, float scale) {
        return getScaledImageIcon(getGoodsImageIcon(type), scale);
    }

    /**
     * Returns the colony image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image pointer
     */
    public Image getColonyImage(int index) {
        return colonies.get(index).getImage();
    }

    /**
     * Returns the color chip with the given color.
     * 
     * @param color The color of the color chip to return.
     * @return The color chip with the given color.
     */
    public Image getColorChip(Color color) {
        Image colorChip = colorChips.get(color);
        if (colorChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadColorChip(gc, color);
            colorChip = colorChips.get(color);
        }
        return colorChip;
    }

    /**
     * Returns the mission chip with the given color.
     * 
     * @param color The color of the color chip to return.
     * @param expertMission Indicates whether or not the missionary is an
     *            expert.
     * @return The color chip with the given color.
     */
    public Image getMissionChip(Color color, boolean expertMission) {
        Image missionChip;
        if (expertMission) {
            missionChip = expertMissionChips.get(color);
        } else {
            missionChip = missionChips.get(color);
        }

        if (missionChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadMissionChip(gc, color, expertMission);

            if (expertMission) {
                missionChip = expertMissionChips.get(color);
            } else {
                missionChip = missionChips.get(color);
            }
        }
        return missionChip;
    }

    /**
     * Returns the alarm chip with the given color.
     * 
     * @param alarm The alarm level.
     * @return The alarm chip.
     */
    public Image getAlarmChip(Tension.Level alarm) {
        Image alarmChip = alarmChips.get(alarm);

        if (alarmChip == null) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
            loadAlarmChip(gc, alarm);
            alarmChip = alarmChips.get(alarm);
        }
        return alarmChip;
    }

    /**
     * Returns the width of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(TileType type) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        return terrain1.get(key).getIconWidth();
    }

    /**
     * Returns the height of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(TileType type) {
        String key;
        if (type != null) {
            key = type.getId();
        } else {
            key = unexploredName;
        }
        return terrain1.get(key).getIconHeight();
    }

    /**
     * Returns the width of the Colony-image at the given index.
     * 
     * @param index The index of the Colony-image.
     * @return The width of the Colony-image at the given index.
     */
    public int getColonyImageWidth(int index) {
        return colonies.get(index).getIconWidth();
    }

    /**
     * Returns the height of the Colony-image at the given index.
     * 
     * @param index The index of the Colony-image.
     * @return The height of the Colony-image at the given index.
     */
    public int getColonyImageHeight(int index) {
        return colonies.get(index).getIconHeight();
    }

    /**
     * Returns the width of the IndianSettlement-image at the given index.
     * 
     * @param index The index of the IndianSettlement-image.
     * @return The width of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageWidth(int index) {
        return indians.get(index).getIconWidth();
    }

    /**
     * Returns the height of the IndianSettlement-image at the given index.
     * 
     * @param index The index of the IndianSettlement-image.
     * @return The height of the IndianSettlement-image at the given index.
     */
    public int getIndianSettlementImageHeight(int index) {
        return indians.get(index).getIconHeight();
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public int getSettlementGraphicsType(Settlement settlement) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            Building stockade = colony.getStockade();

            // TODO: Put it in specification
            if (colony.isUndead()) {
                return COLONY_UNDEAD;
            } else if (stockade == null) {
                if (colony.getUnitCount() <= 3) {
                    return COLONY_SMALL;
                } else if (colony.getUnitCount() <= 7) {
                    return COLONY_MEDIUM;
                } else {
                    return COLONY_LARGE;
                }
            } else if (!colony.hasAbility("model.ability.bombardShips")) {
                if (colony.getUnitCount() > 7) {
                    return COLONY_LARGE_STOCKADE;
                } else if (colony.getUnitCount() > 3) {
                    return COLONY_MEDIUM_STOCKADE;
                } else {
                    return COLONY_STOCKADE;
                }
            } else if (stockade != null &&
                       stockade.getType().getUpgradesTo() != null) {
                if (colony.getUnitCount() > 7) {
                    return COLONY_LARGE_FORT;
                } else {
                    return COLONY_FORT;
                }
            } else {
                return COLONY_FORTRESS;
            }

        } else { // IndianSettlement
            return INDIAN_SETTLEMENT_CAMP;

            /*
             * TODO: Use when we have graphics: IndianSettlement
             * indianSettlement = (IndianSettlement) settlement; if
             * (indianSettlement.getKind() == IndianSettlement.CAMP) { return
             * INDIAN_SETTLEMENT_CAMP; } else if (indianSettlement.getKind() ==
             * IndianSettlement.VILLAGE) { return INDIAN_SETTLEMENT_VILLAGE; }
             * else { //CITY if (indianSettlement.getTribe() ==
             * IndianSettlement.AZTEC) return INDIAN_SETTLEMENT_AZTEC; else //
             * INCA return INDIAN_SETTLEMENT_INCA; }
             */
        }
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     * 
     * @param unit The unit whose graphics type is needed.
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit) {
        return getUnitImageIcon(unit.getType(), unit.getRole());
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType) {
        // Role.DEFAULT.ordinal() == 0
        return units[unitType.getIndex()];
    }
    
    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role) {
        return units[role.ordinal() * numberOfUnitTypes + unitType.getIndex()];
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     *
     * @param unit an <code>Unit</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit, boolean grayscale) {
        return getUnitImageIcon(unit.getType(), unit.getRole(), grayscale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, boolean grayscale) {
        // Role.DEFAULT.ordinal() == 0
        if (grayscale) {
            return unitsGrayscale[unitType.getIndex()];
        } else {
            return units[unitType.getIndex()];
        }
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role, boolean grayscale) {
        int index = role.ordinal() * numberOfUnitTypes + unitType.getIndex();
        if (grayscale) {
            return unitsGrayscale[index];
        } else {
            return units[index];
        }
    }

    /**
     * Returns the scaled ImageIcon.
     * 
     * @param inputIcon an <code>ImageIcon</code> value
     * @param scale The scale of the ImageIcon to return.
     * @return The scaled ImageIcon.
     */
    public ImageIcon getScaledImageIcon(ImageIcon inputIcon, float scale) {
        Image image = inputIcon.getImage();
        return new ImageIcon(image.getScaledInstance(Math.round(image.getWidth(null) * scale),
                                                     Math.round(image.getHeight(null) * scale),
                                                     Image.SCALE_SMOOTH));
    }
    

}
