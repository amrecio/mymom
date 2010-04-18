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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.panel.ImageProvider;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary {

    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());    
    
    public static final String UNIT_SELECT = "unitSelect.image",
                               DELETE = "delete.image",
                               PLOWED = "model.improvement.plow.image",
                               TILE_TAKEN = "tileTaken.image",
                               TILE_OWNED_BY_INDIANS = "nativeLand.image",
                               LOST_CITY_RUMOUR = "lostCityRumour.image",
                               DARKNESS = "halo.dark.image";

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
    }

    private ImageLibrary(String dataDirectory, float scalingFactor) {
        this.scalingFactor = scalingFactor;
    }


    /**
     * Performs all necessary init operations such as loading of data files.
     * 
     * @throws FreeColException If one of the data files could not be found. *
     */
    public void init() throws FreeColException {
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
    public ImageLibrary getScaledImageLibrary(float scalingFactor) throws FreeColException {
        return new ImageLibrary("", scalingFactor);
    }

    public Image scaleImage(Image image, float scale) {
        return image.getScaledInstance(Math.round(image.getWidth(null) * scale),
                                       Math.round(image.getHeight(null) * scale),
                                       Image.SCALE_SMOOTH);
    }

    public ImageIcon scaleIcon(ImageIcon icon, float scale) {
        return new ImageIcon(scaleImage(icon.getImage(), scale));
    }

    private Map<String, ImageIcon> scaleImages(Map<String, ImageIcon> input, float scale) {
        HashMap<String, ImageIcon> output = new HashMap<String, ImageIcon>();
        for (Entry<String, ImageIcon> entry : input.entrySet()) {
            output.put(entry.getKey(), scaleIcon(entry.getValue(), scale));
        }
        return output;
    }

    private ArrayList<ImageIcon> scaleImages(List<ImageIcon> input, float scale) {
        ArrayList<ImageIcon> output = new ArrayList<ImageIcon>();
        for (ImageIcon icon : input) {
            if (icon == null) {
                output.add(null);
            } else {
                output.add(scaleIcon(icon, scale));
            }
        }
        return output;
    }

    private Map<String, ArrayList<ImageIcon>> scaleImages2(Map<String, ArrayList<ImageIcon>> input, float scale) {
        HashMap<String, ArrayList<ImageIcon>> output = new HashMap<String, ArrayList<ImageIcon>>();
        for (Entry<String, ArrayList<ImageIcon>> entry : input.entrySet()) {
            if (entry.getValue() == null) {
                output.put(entry.getKey(), null);
            } else {
                output.put(entry.getKey(), scaleImages(entry.getValue(), scale));
            }
        }
        return output;
    }


    private ArrayList<ArrayList<ImageIcon>> scaleImages2(ArrayList<ArrayList<ImageIcon>> input, float scale) {
        ArrayList<ArrayList<ImageIcon>> output = new ArrayList<ArrayList<ImageIcon>>();
        for (ArrayList<ImageIcon> list : input) {
            if (list == null) {
                output.add(null);
            } else {
                output.add(scaleImages(list, scale));
            }
        }
        return output;
    }


    private EnumMap<Role, Map<UnitType, ImageIcon>> scaleUnitImages(EnumMap<Role, Map<UnitType, ImageIcon>> input,
                                                                    float f) {
        EnumMap<Role, Map<UnitType, ImageIcon>> result = new EnumMap<Role, Map<UnitType, ImageIcon>>(Role.class);
        for (Role role : Role.values()) {
            Map<UnitType, ImageIcon> oldMap = input.get(role);
            Map<UnitType, ImageIcon> newMap = new HashMap<UnitType, ImageIcon>();
            for (Entry<UnitType, ImageIcon> entry : oldMap.entrySet()) {
                ImageIcon oldIcon = entry.getValue();
                ImageIcon newIcon = new ImageIcon(oldIcon.getImage()
                                                  .getScaledInstance(Math.round(oldIcon.getIconWidth() * f),
                                                                     Math.round(oldIcon.getIconHeight() * f),
                                                                     Image.SCALE_SMOOTH));
                newMap.put(entry.getKey(), newIcon);
            }
            result.put(role, newMap);
        }
        return result;
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
        if (!tmpFile.exists() || !tmpFile.isFile() || !tmpFile.canRead()) {
            throw new FreeColException("The data file \"" + filePath + "\" could not be found.");
        }

        return new ImageIcon(filePath);
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
    }

    /**
     * Returns the portrait of this Founding Father.
     *
     * @param father a <code>FoundingFather</code> value
     * @return an <code>Image</code> value
     */
    public Image getFoundingFatherImage(FoundingFather father) {
        return ResourceManager.getImage(father.getId() + ".image");
    }

    /**
     * Returns the monarch-image for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public Image getMonarchImage(Nation nation) {
        return ResourceManager.getImage(nation.getId() + ".monarch.image");
    }

    /**
     * Returns the monarch-image icon for the given Nation.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public ImageIcon getMonarchImageIcon(Nation nation) {
        return ResourceManager.getImageIcon(nation.getId() + ".monarch.image");
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public ImageIcon getCoatOfArmsImageIcon(Nation nation) {
        return ResourceManager.getImageIcon(nation.getId() + ".coat-of-arms.image");
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public Image getCoatOfArmsImage(Nation nation) {
        return ResourceManager.getImage(nation.getId() + ".coat-of-arms.image");
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
        return ResourceManager.getImage(type.getId() + ".image", scalingFactor);
    }

    /**
     * Returns the bonus-ImageIcon at the given index.
     * 
     * @param type The type of the bonus-ImageIcon to return.
     */
    public ImageIcon getBonusImageIcon(ResourceType type) {
        return new ImageIcon(getBonusImage(type));
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
        int height = getCompoundTerrainImageHeight(type);
        // Currently used for hills and mountains
        if (type.getArtOverlay() != null) {
            Image overlayImage = getOverlayImage(type, 0, 0);
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, height - terrainImage.getHeight(null), null);
            g.drawImage(overlayImage, 0, height - overlayImage.getHeight(null), null);
            g.dispose();
            terrainImage = compositeImage;
        }
        if (type.isForested()) {
            Image forestImage = getForestImage(type);
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, height - terrainImage.getHeight(null), null);
            g.drawImage(forestImage, 0, height - forestImage.getHeight(null), null);
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
        int index = (x + y) % 2;
        return ResourceManager.getImage(type.getId() + ".overlay" + index + ".image");
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
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        // the pattern is mostly visible on ocean tiles this is an
        // attempt to break it up so it doesn't create big stripes or
        // chess-board effect
        int index = (( y % 8 <= 2) || ((x+y) % 2 == 0 )) ? 0 : 1;
        return ResourceManager.getImage(key + ".center" + index + ".image");
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
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        // the pattern is mostly visible on ocean tiles this is an
        // attempt to break it up so it doesn't create big stripes or
        // chess-board effect
        String index = (( y % 8 <= 2) || ((x+y) % 2 == 0 )) ? "_event" : "_odd";
        return ResourceManager.getImage(key + ".border_" + direction + index + ".image");
    }

    /**
     * Returns the river mouth terrain-image for the direction and magnitude.
     * 
     * @param direction a <code>Direction</code> value
     * @param magnitude an <code>int</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @return The terrain-image at the given index.
     */
    public Image getRiverMouthImage(Direction direction, int magnitude, int x, int y) {

        String key = "delta_" + direction + (magnitude == 1 ? "_small" : "_large");
        return ResourceManager.getImage(key);
    }

    /**
     * Returns the river image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getRiverImage(int index) {
        return ResourceManager.getImage("river" + index);
    }

    /**
     * Returns the beach image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getBeachImage(int index) {
        return ResourceManager.getImage("beach" + index);
    }

    /**
     * Returns the forest image for a terrain type.
     * 
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(TileType type) {
        return ResourceManager.getImage(type.getId() + ".forest");
    }

    /**
     * Returns the image with the given id.
     * 
     * @param id The id of the image to return.
     * @return The image.
     */
    public Image getMiscImage(String id) {
        return ResourceManager.getImage(id, scalingFactor);
    }

    /**
     * Returns the image with the given id.
     * 
     * @param id The id of the image to return.
     * @return The image.
     */
    public ImageIcon getMiscImageIcon(String id) {
        return new ImageIcon(getMiscImage(id));
    }

    /**
     * Returns the goods-image at the given index.
     * 
     * @param goodsType The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(GoodsType goodsType) {
        return ResourceManager.getImage(goodsType.getId() + ".image");
    }

    /**
     * Returns the goods-image for a goods type.
     * 
     * @param goodsType The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(GoodsType goodsType) {
        return ResourceManager.getImageIcon(goodsType.getId() + ".image");
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
     * Returns the color chip with the given color.
     * 
     * @param ownable an <code>Ownable</code> value
     * @param scale a <code>double</code> value
     * @return The color chip with the given color.
     */
    public Image getColorChip(Ownable ownable, double scale) {
        return ResourceManager.getChip(ownable.getOwner().getNationID() + ".chip", scale);
    }

    /**
     * Returns the mission chip with the given color.
     * 
     * @param ownable an <code>Ownable</code> value
     * @param expertMission Indicates whether or not the missionary is an
     *            expert.
     * @param scale a <code>double</code> value
     * @return The color chip with the given color.
     */
    public Image getMissionChip(Ownable ownable, boolean expertMission, double scale) {
        if (expertMission) {
            return ResourceManager.getChip(ownable.getOwner().getNationID()
                                           + ".mission.expert.chip", scale);
        } else {
            return ResourceManager.getChip(ownable.getOwner().getNationID()
                                           + ".mission.chip", scale);
        }
    }

    /**
     * Returns the alarm chip with the given color.
     * 
     * @param alarm The alarm level.
     * @param visited a <code>boolean</code> value
     * @param scale a <code>double</code> value
     * @return The alarm chip.
     */
    public Image getAlarmChip(Tension.Level alarm, final boolean visited, double scale) {
        if (visited) {
            return ResourceManager.getChip("alarmChip.visited."
                                           + alarm.toString().toLowerCase(), scale);
        } else {
            return ResourceManager.getChip("alarmChip." + alarm.toString().toLowerCase(), scale);
        }
    }

    /**
     * Returns the width of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(TileType type) {
        return getTerrainImage(type, 0, 0).getWidth(null);
    }

    /**
     * Returns the height of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(TileType type) {
        return getTerrainImage(type, 0, 0).getHeight(null);
    }

    /**
     * Returns the height of the terrain-image including overlays and
     * forests for the given terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getCompoundTerrainImageHeight(TileType type) {
        int height = getTerrainImageHeight(type);
        if (type != null) {
            if (type.getArtOverlay() != null) {
                height = Math.max(height, getOverlayImage(type, 0, 0).getHeight(null));
            }
            if (type.isForested()) {
                height = Math.max(height, getForestImage(type).getHeight(null));
            }
        }
        return height;
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlementType The type of settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(SettlementType settlementType) {
        return ResourceManager.getImage(settlementType.toString() + ".image", scalingFactor);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(Settlement settlement) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            // TODO: Put it in specification
            if (colony.isUndead()) {
                return getSettlementImage(SettlementType.UNDEAD);
            } else {
                int stockadeLevel = 0;
                if (colony.getStockade() != null) {
                    stockadeLevel = colony.getStockade().getLevel();
                }
                int unitCount = colony.getUnitCount();
                switch(stockadeLevel) {
                case 0:
                    if (unitCount <= 3) {
                        return getSettlementImage(SettlementType.SMALL_COLONY);
                    } else if (unitCount <= 7) {
                        return getSettlementImage(SettlementType.MEDIUM_COLONY);
                    } else {
                        return getSettlementImage(SettlementType.LARGE_COLONY);
                    }
                case 1:
                    if (unitCount > 7) {
                        return getSettlementImage(SettlementType.LARGE_STOCKADE);
                    } else if (unitCount > 3) {
                        return getSettlementImage(SettlementType.MEDIUM_STOCKADE);
                    } else {
                        return getSettlementImage(SettlementType.SMALL_STOCKADE);
                    }
                case 2:
                    if (unitCount > 7) {
                        return getSettlementImage(SettlementType.LARGE_FORT);
                    } else {
                        return getSettlementImage(SettlementType.MEDIUM_FORT);
                    }
                case 3:
                    return getSettlementImage(SettlementType.LARGE_FORTRESS);
                default:
                    return getSettlementImage(SettlementType.SMALL_COLONY);
                }
            }

        } else { // IndianSettlement
            return getSettlementImage(((IndianSettlement) settlement).getTypeOfSettlement());
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
        final Image im = ResourceManager.getImage(unitType.getId() + ".image", scalingFactor);
        return (im != null) ? new ImageIcon(im) : null;
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
        final String roleStr = (role != Role.DEFAULT) ? "." + role.getId() : "";
        final Image im = ResourceManager.getImage(unitType.getId() + roleStr + ".image", scalingFactor);
        return (im != null) ? new ImageIcon(im) : null;
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
        return getUnitImageIcon(unitType, Role.DEFAULT, grayscale);
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
        if (grayscale) {
            final String roleStr = (role != Role.DEFAULT) ? "." + role.getId() : "";
            final Image im = ResourceManager.getGrayscaleImage(unitType.getId() + roleStr + ".image", scalingFactor);
            return (im != null) ? new ImageIcon(im) : null;
        } else {
            return getUnitImageIcon(unitType, role);
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

    /**
     * Returns the scaled ImageIcon.
     * 
     * @param image an <code>Image</code> value
     * @param scale The scale of the ImageIcon to return.
     * @return The scaled ImageIcon.
     */
    public ImageIcon getScaledImageIcon(Image image, float scale) {
        return new ImageIcon(image.getScaledInstance(Math.round(image.getWidth(null) * scale),
                                                     Math.round(image.getHeight(null) * scale),
                                                     Image.SCALE_SMOOTH));
    }
    

}
