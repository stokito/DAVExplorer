/*
 * Copyright (c) 2001-2005 Regents of the University of California.
 * All rights reserved.
 *
 * This software was developed at the University of California, Irvine.
 *
 * Redistribution and use in source and binary forms are permitted
 * provided that the above copyright notice and this paragraph are
 * duplicated in all such forms and that any documentation,
 * advertising materials, and other materials related to such
 * distribution and use acknowledge that the software was developed
 * by the University of California, Irvine.  The name of the
 * University may not be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 * WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */


package edu.uci.ics.DAVExplorer;

import java.util.Enumeration;
import java.util.Vector;


/**
 * Title:       Property Node
 * Description: Nodes for the property tree
 * Copyright:   Copyright (c) 2001-2005 Regents of the University of California. All rights reserved.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        5 October 2001
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        08 February 2004
 * Changes:     Added Javadoc templates
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        4 February 2005
 * Changes:     Some refactoring
 */
public class PropNode
{
    /**
     * Constructor
     * @param tag
     * @param ns
     * @param value
     * @param modified
     */
    public PropNode( String tag, String ns, String value, boolean modified )
    {
        this.tag = tag;
        this.ns = ns;
        this.value = value;
        this.modified = modified;
    }


    /**
     * Constructor
     * @param tag
     * @param ns
     * @param value
     */
    public PropNode( String tag, String ns, String value )
    {
        this.tag = tag;
        this.ns = ns;
        this.value = value;
        this.modified = false;
    }


    /**
     * 
     * @return
     */
    public String getTag()
    {
        return tag;
    }


    /**
     * 
     * @return
     */
    public String getNamespace()
    {
        return ns;
    }


    /**
     * 
     * @param ns
     */
    public void setNamespace( String ns )
    {
        this.ns = ns;
        modified = true;
    }


    /**
     * 
     * @return
     */
    public String getValue()
    {
        if( value == null )
            return "";
        return value;
    }


    /**
     * 
     * @param value
     */
    public void setValue( String value )
    {
        this.value = value;
        modified = true;
    }


    /**
     * 
     * @return
     */
    public String toString()
    {
        return getTag();
    }


    /**
     * 
     * @return
     */
    public PropNode getParent()
    {
        return parent;
    }


    /**
     * 
     * @param parent
     */
    public void setParent( PropNode parent )
    {
        this.parent = parent;
    }


    /**
     * 
     * @param child
     */
    public void addChild( Object child )
    {
        children.add( child );
    }


    /**
     * 
     * @param child
     */
    public void removeChild( Object child )
    {
        children.remove(child);
        removedChildren.add(child);
    }


    /**
     * 
     * @return
     */
    public boolean isModified()
    {
        return modified;
    }


    /**
     * 
     * @return
     */
    public Object[] getChildren()
    {
        return children.toArray();
    }


    /**
     * 
     * @return
     */
    public Object[] getRemovedChildren()
    {
        return removedChildren.toArray();
    }


    /**
     *
     */
    public void clear()
    {
        modified = false;
        removedChildren.clear();
    }


    /**
     * 
     * @return
     */
    public boolean isDAVProp()
    {
        // check if the property is defined in RFC2518 or if it is part of
        // a defined property hierarchy (e.g., lockdiscovery)
        if( (ns!=null) && ns.equals(WebDAVProp.DAV_SCHEMA) )
        {
            Enumeration props = WebDAVProp.getDavProps();
            while( props.hasMoreElements() )
            {
                String prop = (String)props.nextElement();
                if( tag.equals(prop) )
                    return true;
                if( (parent!=null) && parent.isDAVProp() )
                    return true;
            }
        }
        return false;
    }


    private Vector children = new Vector();
    private Vector removedChildren = new Vector();
    private String tag;
    private String ns;
    private String value;
    private boolean modified;
    private PropNode parent;
}
