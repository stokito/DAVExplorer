/*
 * @(#)RetryException.java				0.3 30/01/1998
 *
 *  This file is part of the HTTPClient package
 *  Copyright (C) 1996-1998  Ronald Tschalaer
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
 *  Ronald.Tschalaer@psi.ch
 *
 */

package HTTPClient;


/**
 * Signals that an exception was thrown and caught, and the request was
 * retried.
 *
 * @version	0.3  30/01/1998
 * @author	Ronald Tschal&auml;r
 */

class RetryException extends java.io.IOException
{
    /** the request to retry */
    Request     request  = null;

    /** the response associated with the above request */
    Response    response = null;

    /** the start of the liked list */
    RetryException first = null;

    /** the next exception in the list */
    RetryException next  = null;

    /** was this exception generated because of an abnormal connection reset? */
    boolean conn_reset = true;

    /** restart processing? */
    boolean restart = false;


    /**
     * Constructs an RetryException with no detail message.
     * A detail message is a String that describes this particular exception.
     */
    public RetryException()
    {
	super();
    }


    /**
     * Constructs an RetryException class with the specified detail message.
     * A detail message is a String that describes this particular exception.
     *
     * @param s the String containing a detail message
     */
    public RetryException(String s)
    {
	super(s);
    }


    // Methods

    /**
     * Inserts this exception into the list.
     *
     * @param re the retry exception after which to add this one
     */
    void addToListAfter(RetryException re)
    {
	if (re == null)  return;

	if (re.next != null)
	    this.next = re.next;
	re.next = this;
    }
}

