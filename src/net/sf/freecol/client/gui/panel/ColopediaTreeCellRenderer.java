package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * This TreeCellRenderer is responsible for rendering tree items in the Colopedia.
 */
public class ColopediaTreeCellRenderer extends DefaultTreeCellRenderer {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";
    
    public ImageIcon icon;
    
    /**
     * The constructor makes sure that the backgrounds are transparent.
     */
    public ColopediaTreeCellRenderer() {
        setBackgroundNonSelectionColor(new Color(0,0,0,1));
    }
    
    /**
     * Returns the rendered Component
     * 
     * @return the rendered item's Component
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        
        if(leaf) {
            ColopediaTreeItem nodeItem = (ColopediaTreeItem)node.getUserObject();
            ImageIcon icon = nodeItem.getIcon();
            setIcon(icon);
        }
        return this;
    }
}
