/*
 * @(#)ModuleException.java				0.3-1 10/02/1999
 *
 *  This file is part of the HTTPClient package
 *  Copyright (C) 1996-1999  Ronald Tschal�r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free
 *  Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 *  MA 02111-1307, USA
 *
 *  For questions, suggestions, bug-reports, enhancement-requests etc.
 *  I may be contacted at:
 *
 *  ronald@innovation.ch
 *
 */

package HTTPClient;


/**
 * Signals that an exception occured in a module.
 *
 * @version	0.3-1  10/02/1999
 * @author	Ronald Tschal�r
 * @since	V0.3
 */

public class ModuleException extends Exception
{

    /**
     * Constructs an ModuleException with no detail message. A detail
     * message is a String that describes this particular exception.
     */
    public ModuleException()
    {
	super();
    }


    /**
     * Constructs an ModuleException class with the specified detail message.
     * A detail message is a String that describes this particular exception.
     *
     * @param msg the String containing a detail message
     */
    public ModuleException(String msg)
    {
	super(msg);
    }
}
