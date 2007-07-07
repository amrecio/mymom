
package net.sf.freecol.client.gui.panel;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Unit;

/**
* The transferhandler that is capable of creating ImageSelection objects.
* Those ImageSelection objects are Transferable. The DefaultTransferHandler
* should be attached to JPanels or custom JLabels.
*/
public final class DefaultTransferHandler extends TransferHandler {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(DefaultTransferHandler.class.getName());

    public static final DataFlavor flavor = new DataFlavor(ImageSelection.class, "ImageSelection");

    private final Canvas canvas;
    private final FreeColPanel parentPanel;

    /**
    * The constructor to use.
    * @param canvas The <code>Canvas</code>.
    * @param parentPanel The layered pane that holds all kinds of information.
    */
    public DefaultTransferHandler(Canvas canvas, FreeColPanel parentPanel) {
        this.canvas = canvas;
        this.parentPanel = parentPanel;
    }

    /**
    * Returns the action that can be done to an ImageSelection on the given component.
    * @return The action that can be done to an ImageSelection on the given component.
    */
    public int getSourceActions(JComponent comp) {
        return COPY_OR_MOVE;
    }


    /**
    * Returns 'true' if the given component can import a selection of the
    * flavor that is indicated by the second parameter, 'false' otherwise.
    * @param comp The component that needs to be checked.
    * @param flavor The flavor that needs to be checked for.
    * @return 'true' if the given component can import a selection of the
    * flavor that is indicated by the second parameter, 'false' otherwise.
    */
    public boolean canImport(JComponent comp, DataFlavor[] flavor) {
        if (!(comp instanceof UnitLabel) &&
            !(comp instanceof GoodsLabel) &&
            !(comp instanceof MarketLabel) &&
            !(comp instanceof JPanel) &&
            !(comp instanceof JLabel)) {
            return false;
        }
        for (int i = 0; i < flavor.length; i++) {
            if (flavor[i].equals(DefaultTransferHandler.flavor)) {
                return true;
            }
        }
        return false;
    }

    /**
    * Creates a Transferable (an ImageSelection to be precise) of the
    * data that is represented by the given component and returns that
    * object.
    * @param comp The component to create a Transferable of.
    * @return The resulting Transferable (an ImageSelection object).
    */
    public Transferable createTransferable(JComponent comp) {
        if (comp instanceof UnitLabel) {
            return new ImageSelection((UnitLabel)comp);
        } else if (comp instanceof GoodsLabel) {
            return new ImageSelection((GoodsLabel)comp);
        } else if (comp instanceof MarketLabel) {
            return new ImageSelection((MarketLabel)comp);
        }
        return null;
    }

    /**
    * Imports the data represented by the given Transferable into
    * the given component. Returns 'true' on success, 'false' otherwise.
    * @param comp The component to import the data to.
    * @param t The Transferable that holds the data.
    * @return 'true' on success, 'false' otherwise.
    */
    public boolean importData(JComponent comp, Transferable t) {
        try {
            JLabel data;
            
            /* 
                This variable is used to temporarily keep the old selected unit,
                while moving cargo from one carrier to another:
            */
            UnitLabel oldSelectedUnit = null;

            // Check flavor.
            if (t.isDataFlavorSupported(DefaultTransferHandler.flavor)) {
                data = (JLabel)t.getTransferData(DefaultTransferHandler.flavor);
            } else {
                logger.warning("Data flavor is not supported!");
                return false;
            }

            // Do not allow a transferable to be dropped upon itself:
            if (comp == data) {
                return false;
            }

            // Make sure we don't drop onto other Labels.
            if (comp instanceof UnitLabel) {

                /*
                  If the unit/cargo is dropped on a carrier in port (EuropePanel.InPortPanel),
                  then the ship is selected and the unit is added to its cargo.

                  If not, assume that the user wished to drop the unit/cargo on the panel below.
                */
                if (((UnitLabel) comp).getUnit().isCarrier() && ((UnitLabel) comp).getParent() instanceof EuropePanel.InPortPanel) {
                    if (data instanceof UnitLabel && ((UnitLabel) data).getUnit().getLocation() instanceof Unit
                            || data instanceof GoodsLabel && ((GoodsLabel) data).getGoods().getLocation() instanceof Unit) {
                        oldSelectedUnit = ((EuropePanel) parentPanel).getSelectedUnitLabel();
                    }
                    ((EuropePanel) parentPanel).setSelectedUnitLabel((UnitLabel) comp);
                    comp = ((EuropePanel) parentPanel).getCargoPanel();
                } else if (((UnitLabel) comp).getUnit().isCarrier() && ((UnitLabel) comp).getParent() instanceof ColonyPanel.InPortPanel) {
                    if (data instanceof UnitLabel && ((UnitLabel) data).getUnit().getLocation() instanceof Unit
                            || data instanceof GoodsLabel && ((GoodsLabel) data).getGoods().getLocation() instanceof Unit) {
                        oldSelectedUnit = ((ColonyPanel) parentPanel).getSelectedUnitLabel();
                    }
                    ((ColonyPanel) parentPanel).setSelectedUnitLabel((UnitLabel) comp);
                    comp = ((ColonyPanel) parentPanel).getCargoPanel();
                } else {
                    try {
                        comp = (JComponent)comp.getParent();
                    } catch (ClassCastException e) {
                        return false;
                    }

                    // This is because we use an extra panel for layout in this particular case; may find a better solution later.
                    try {
                        if ((JComponent)comp.getParent() instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel) {
                            comp = (JComponent)comp.getParent();
                        }
                    } catch (ClassCastException e) {}
                }
            } else if ((comp instanceof GoodsLabel) || (comp instanceof MarketLabel)) {
                try {
                    comp = (JComponent)comp.getParent();
                } catch (ClassCastException e) {
                    return false;
                }
            }

            // t is already in comp:
            if (data.getParent() == comp) {
                return false;
            }

            if (data instanceof UnitLabel) {

                // Check if the unit can be dragged to comp.

                Unit unit = ((UnitLabel)data).getUnit();

                if (unit.isUnderRepair()) {
                    return false;
                }
                
                if ((unit.getState() == Unit.TO_AMERICA) && (!(comp instanceof EuropePanel.ToEuropePanel))) {
                    return false;
                }

                if ((unit.getState() == Unit.TO_EUROPE) && (!(comp instanceof EuropePanel.ToAmericaPanel))) {
                    return false;
                }

                if ((unit.getState() != Unit.TO_AMERICA) && ((comp instanceof EuropePanel.ToEuropePanel))) {
                    return false;
                }

                if (!unit.isNaval() && (comp instanceof EuropePanel.InPortPanel
                        || comp instanceof ColonyPanel.InPortPanel
                        || comp instanceof EuropePanel.ToEuropePanel
                        || comp instanceof EuropePanel.ToAmericaPanel)) {
                    return false;
                }

                if (comp instanceof EuropePanel.MarketPanel || comp instanceof ColonyPanel.WarehousePanel) {
                    return false;
                }

                if (unit.isNaval() && (comp instanceof EuropePanel.CargoPanel
                        || comp instanceof EuropePanel.DocksPanel
                        || comp instanceof ColonyPanel.OutsideColonyPanel
                        || comp instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel
                        || comp instanceof ColonyPanel.TilePanel.ASingleTilePanel
                        || comp instanceof ColonyPanel.CargoPanel)) {
                    return false;
                }


                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    // Do this in the 'add'-methods instead:
                    //data.getParent().remove(data);

                    if (comp instanceof EuropePanel.ToEuropePanel) {
                        ((EuropePanel.ToEuropePanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.ToAmericaPanel) {
                        ((EuropePanel.ToAmericaPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.DocksPanel) {
                        ((EuropePanel.DocksPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.BuildingsPanel.ASingleBuildingPanel) {
                        ((ColonyPanel.BuildingsPanel.ASingleBuildingPanel) comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.OutsideColonyPanel) {
                        ((ColonyPanel.OutsideColonyPanel) comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.CargoPanel) {
                        ((ColonyPanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.TilePanel.ASingleTilePanel) {
                        ((ColonyPanel.TilePanel.ASingleTilePanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();

                    if (oldSelectedUnit != null) {
                        if ((oldSelectedUnit).getParent() instanceof EuropePanel.InPortPanel) {
                            ((EuropePanel) parentPanel).setSelectedUnit(oldSelectedUnit.getUnit());
                        } else {
                            ((ColonyPanel) parentPanel).setSelectedUnit(oldSelectedUnit.getUnit());
                        }
                    }

                    return true;
                }
            } else if (data instanceof GoodsLabel) {

                // Check if the unit can be dragged to comp.

                //Goods g = ((GoodsLabel)data).getGoods();

                // Import the data.

                if (((GoodsLabel) data).isPartialChosen()) {
                    int amount = getAmount();
                    if (amount == -1) {
                        return false;
                    }
                    ((GoodsLabel) data).getGoods().setAmount(amount);
                }

                if (!(comp instanceof ColonyPanel.WarehousePanel || comp instanceof ColonyPanel.CargoPanel
                        || comp instanceof EuropePanel.MarketPanel || comp instanceof EuropePanel.CargoPanel)
                    || (comp instanceof EuropePanel.CargoPanel && !((EuropePanel.CargoPanel) comp).isActive())
                    || (comp instanceof ColonyPanel.CargoPanel && !((ColonyPanel.CargoPanel) comp).isActive())) {

                    return false;
                }


                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    //data.getParent().remove(data);

                    if (comp instanceof ColonyPanel.WarehousePanel) {
                        ((ColonyPanel.WarehousePanel)comp).add(data, true);
                    } else if (comp instanceof ColonyPanel.CargoPanel) {
                        ((ColonyPanel.CargoPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.MarketPanel) {
                        ((EuropePanel.MarketPanel)comp).add(data, true);
                    } else if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();

                    if (oldSelectedUnit != null) {
                        if (oldSelectedUnit.getParent() instanceof EuropePanel.InPortPanel) {
                            ((EuropePanel) parentPanel).setSelectedUnit(oldSelectedUnit.getUnit());
                        } else {
                            ((ColonyPanel) parentPanel).setSelectedUnit(oldSelectedUnit.getUnit());
                        }
                    }

                    return true;
                }
            } else if (data instanceof MarketLabel) {

                // Check if the unit can be dragged to comp.

                //Goods g = ((GoodsLabel)data).getGoods();

                // Import the data.

                if (((MarketLabel) data).isPartialChosen()) {
                    int amount = getAmount();
                    if (amount == -1) {
                        return false;
                    }
                    ((MarketLabel) data).setAmount(amount);
                }


                if (comp instanceof JLabel) {
                    logger.warning("Oops, I thought we didn't have to write this part.");
                    return true;
                } else if (comp instanceof JPanel) {
                    // Be not removing MarketLabels from their home. -sjm
                    //data.getParent().remove(data);

                    if (comp instanceof EuropePanel.CargoPanel) {
                        ((EuropePanel.CargoPanel)comp).add(data, true);
                    } else {
                        logger.warning("The receiving component is of an invalid type.");
                        return false;
                    }

                    comp.revalidate();
                    return true;
                }
            }

            logger.warning("The dragged component is of an invalid type.");

        } catch (Exception e) {
            // TODO: Suggest a reconnect.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
        }

        return false;
    }

    
    /**
    * Displays an input dialog box where the user should specify a goods transfer amount.
    */
    private int getAmount() {
        String s = canvas.showInputDialog("goodsTransfer.text", "100", "ok", "cancel");
        int amount = -1;

        while (s != null && amount == -1) {
            try {
                amount = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                canvas.errorMessage("notANumber");
                s = canvas.showInputDialog("goodsTransfer.text", "100", "ok", "cancel");
            }
        }

        if (s == null) {
            return -1;
        }

        return amount;
    }




    /*__________________________________________________
      Methods/inner-classes below have been copied from
      TransferHandler in order to allow partial loading.
      --------------------------------------------------
    */


    private static FreeColDragGestureRecognizer recognizer = null;

    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        int srcActions = getSourceActions(comp);
        int dragAction = srcActions & action;
        if (!(e instanceof MouseEvent)) {
            dragAction = NONE;
        }

        if (dragAction != NONE && !GraphicsEnvironment.isHeadless()) {
            if (recognizer == null) {
                recognizer = new FreeColDragGestureRecognizer(new FreeColDragHandler());
            }

            recognizer.gestured(comp, (MouseEvent) e , srcActions, dragAction);
        } else {
            exportDone(comp, null, NONE);
        }
    }


    /**
     * This is the default drag handler for drag and drop operations that
     * use the <code>TransferHandler</code>.
     */
    private static class FreeColDragHandler implements DragGestureListener, DragSourceListener {

        private boolean scrolls;


        // --- DragGestureListener methods -----------------------------------

        /**
         * A Drag gesture has been recognized.
         */
        public void dragGestureRecognized(DragGestureEvent dge) {
            JComponent c = (JComponent) dge.getComponent();
            DefaultTransferHandler th = (DefaultTransferHandler) c.getTransferHandler();
            Transferable t = th.createTransferable(c);

            if (t != null) {
                scrolls = c.getAutoscrolls();
                c.setAutoscrolls(false);
                try {
                    if (c instanceof JLabel && ((JLabel) c).getIcon() instanceof ImageIcon) {
                        Toolkit tk = Toolkit.getDefaultToolkit();
                        ImageIcon imageIcon = ((ImageIcon) ((JLabel) c).getIcon());
                        Dimension bestSize = tk.getBestCursorSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());

                        if (bestSize.width == 0 || bestSize.height == 0) {
                            dge.startDrag(null, t, this);
                            return;
                        }

                        Image image;
                        if (bestSize.width > bestSize.height) {
                            bestSize.height = (int) ((((double) bestSize.width) / ((double) imageIcon.getIconWidth())) * imageIcon.getIconHeight());
                        } else {
                            bestSize.width = (int) ((((double) bestSize.height) / ((double) imageIcon.getIconHeight())) * imageIcon.getIconWidth());                            
                        }
                        image = imageIcon.getImage().getScaledInstance(bestSize.width, bestSize.height, Image.SCALE_DEFAULT);

                        /*
                          We have to use a MediaTracker to ensure that the
                          image has been scaled before we use it.
                         */
                        MediaTracker mt = new MediaTracker(c);
                        mt.addImage(image, 0, bestSize.width, bestSize.height);
                        try {
                            mt.waitForID(0);
                        } catch (InterruptedException e) {
                            dge.startDrag(null, t, this);
                            return;
                        }
                        
                        Point point = new Point(bestSize.width / 2, bestSize.height / 2);
                        Cursor cursor;
                        try {
                            cursor = tk.createCustomCursor(image, point, "freeColDragIcon");                            
                        } catch (RuntimeException re) {
                            cursor = null;
                        }
                        //Point point = new Point(0, 0);
                        dge.startDrag(cursor, t, this);                    
                    } else {
                        dge.startDrag(null, t, this);
                    }

                    return;
                } catch (RuntimeException re) {
                    c.setAutoscrolls(scrolls);
                }
            }

            th.exportDone(c, null, NONE);
        }

        // --- DragSourceListener methods -----------------------------------

        /**
         * as the hotspot enters a platform dependent drop site.
         */
        public void dragEnter(DragSourceDragEvent dsde) {
        }


        /**
         * as the hotspot moves over a platform dependent drop site.
         */
        public void dragOver(DragSourceDragEvent dsde) {
        }


        /**
         * as the hotspot exits a platform dependent drop site.
         */
        public void dragExit(DragSourceEvent dsde) {
        }


        /**
         * as the operation completes.
         */
        public void dragDropEnd(DragSourceDropEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent c = (JComponent)dsc.getComponent();
            
            if (dsde.getDropSuccess()) {
                ((DefaultTransferHandler) c.getTransferHandler()).exportDone(c, dsc.getTransferable(), dsde.getDropAction());
            } else {
                ((DefaultTransferHandler) c.getTransferHandler()).exportDone(c, null, NONE);
            }
            c.setAutoscrolls(scrolls);
        }


        public void dropActionChanged(DragSourceDragEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent comp = (JComponent)dsc.getComponent();
            updatePartialChosen(comp, dsde.getUserAction() == MOVE);
        }
        
        
        private void updatePartialChosen(JComponent comp, boolean partialChosen) {
            if (comp instanceof GoodsLabel) {
                ((GoodsLabel) comp).setPartialChosen(partialChosen);
            } else if (comp instanceof MarketLabel) {
                ((MarketLabel) comp).setPartialChosen(partialChosen);
            }
        }
    }


    private static class FreeColDragGestureRecognizer extends DragGestureRecognizer {

        FreeColDragGestureRecognizer(DragGestureListener dgl) {
            super(DragSource.getDefaultDragSource(), null, NONE, dgl);
        }

        void gestured(JComponent c, MouseEvent e, int srcActions, int action) {
            setComponent(c);
            setSourceActions(srcActions);
            appendEvent(e);

            fireDragGestureRecognized(action, e.getPoint());
        }


        /**
         * register this DragGestureRecognizer's Listeners with the Component.
         */
        protected void registerListeners() {
        }


        /**
         * unregister this DragGestureRecognizer's Listeners with the Component.
         *
         * subclasses must override this method
         */
        protected void unregisterListeners() {
        }
    }
}
