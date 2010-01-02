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

package net.sf.freecol.client.gui.panel;

/**
 * Can be used as a single choice for the
 * {@link FreeColDialog#createChoiceDialog(String, String, ChoiceItem...) choice dialog}.
 */
public class ChoiceItem<T> {

    private String text;
    private T object;
    private boolean enabled;

    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param text The text that should be used to represent 
     *        this choice.
     * @param object The <code>Object</code> contained by this
     *        choice.
     * @param enable Sets if the option should be enabled or not       
     */
    public ChoiceItem(String text, T object, boolean enable) {
        this.text = text;
        this.object = object;
        this.enabled = enable;
    }
    
    
    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param text The text that should be used to represent 
     *        this choice.
     * @param object The <code>Object</code> contained by this
     *        choice.
     */
    public ChoiceItem(String text, T object) {
    	this(text, object, true);
    }


    /**
     * Creates a new <code>ChoiceItem</code> with the
     * given object.
     *
     * @param object The <code>Object</code> contained by this
     *        choice.
     */
    public ChoiceItem(T object) {
    	this(object.toString(), object, true);
    }


    /**
     * Gets the <code>Object</code> contained by this choice.
     * @return The <code>Object</code>.
     */
    public T getObject() {
        return object;
    }


    /**
     * Gets the choice as an <code>int</code>.
     *
     * @return The number representing this object.
     * @exception ClassCastException if the {@link #getObject object} is
     *            not an <code>Integer</code>.
     */
    public int getChoice() {
        return ((Integer) object).intValue();
    }
    
    /**
     * Checks if the option should be enabled or not
     * @return enable status
     */
    public boolean isEnabled(){
    	return this.enabled;
    }

    /**
     * Gets a textual presentation of this object.
     * @return The <code>text</code> set in the constructor.
     */
    public String toString() {
        return text;
    }
}
