/*
 * @(#)Cookie2.java					0.3 30/01/1998
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

import java.io.File;
import java.net.URL;
import java.net.ProtocolException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;


/**
 * This class represents an http cookie as specified in the
 * <A HREF="ftp://ds.internic.net/internet-drafts/draft-ietf-http-state-man-mec-05.txt">
 * HTTP State Management Mechanism spec</A> (also known as a version 1 cookie).
 *
 * @version	0.3  30/01/1998
 * @author	Ronald Tschal&auml;r
 * @since	V0.3
 */

public class Cookie2 extends Cookie
{
    protected int     version;
    protected boolean discard;
    protected String  comment;
    protected URL     comment_url;
    protected int[]   port_list;
    protected String  port_list_str;

    protected boolean path_set;
    protected boolean port_set;
    protected boolean domain_set;


    /**
     * This is private. Use <code>parse()</code> to create cookies.
     *
     * @see #parse(java.lang.String, HTTPClient.RoRequest)
     */
    protected Cookie2(RoRequest req)
    {
	super(req);
	version       = -1;
	discard       = false;
	comment       = null;
	comment_url   = null;
	port_list     = null;
	port_list_str = null;

	path_set      = false;
	port_set      = false;
	domain_set    = false;
    }


    /**
     * Parses the Set-Cookie2 header into an array of Cookies.
     *
     * @param set_cookie the Set-Cookie header received from the server
     * @param req the request used
     * @return an array of Cookies as parsed from the Set-Cookie header
     * @exception ProtocolException if an error occurs during parsing
     */
    protected static Cookie[] parse(String set_cookie, RoRequest req)
		throws ProtocolException
    {
	Vector cookies;
	try
	    { cookies = Util.parseHeader(set_cookie); }
	catch (ParseException pe)
	    { throw new ProtocolException(pe.getMessage()); }

        Cookie cookie_arr[] = new Cookie[cookies.size()];
	int cidx=0;
	for (int idx=0; idx<cookie_arr.length; idx++)
	{
	    HttpHeaderElement c_elem =
			(HttpHeaderElement) cookies.elementAt(idx);


	    // set NAME and VALUE

	    if (c_elem.getValue() == null)
		throw new ProtocolException("Bad Set-Cookie2 header: " +
					    set_cookie + "\nMissing value " +
					    "for cookie '" + c_elem.getName() +
					    "'");
	    Cookie2 curr = new Cookie2(req);
	    curr.name    = c_elem.getName();
	    curr.value   = c_elem.getValue();


	    // set all params

	    NVPair[] params = c_elem.getParams();
	    boolean discard_set = false, secure_set = false;
	    for (int idx2=0; idx2<params.length; idx2++)
	    {
		String name = params[idx2].getName().toLowerCase();

		// check for required value parts
		if ((name.equals("version")  ||  name.equals("max-age")  ||
		     name.equals("domain")  ||  name.equals("path")  ||
		     name.equals("comment")  ||  name.equals("commenturl"))  &&
		    params[idx2].getValue() == null)
		{
		    throw new ProtocolException("Bad Set-Cookie2 header: " +
						set_cookie + "\nMissing value "+
						"for " + params[idx2].getName()+
						" attribute in cookie '" +
						c_elem.getName() + "'");
		}


		if (name.equals("version"))		// Version
		{
		    if (curr.version != -1)  continue;
		    try
		    {
			curr.version =
				Integer.parseInt(params[idx2].getValue());
		    }
		    catch (NumberFormatException nfe)
		    {
			throw new ProtocolException("Bad Set-Cookie2 header: " +
						    set_cookie + "\nVersion '" +
						    params[idx2].getValue() +
						    "' not a number");
		    }
		}
		else if (name.equals("path"))		// Path
		{
		    if (curr.path_set)  continue;
		    curr.path = params[idx2].getValue();
		    curr.path_set = true;
		}
		else if (name.equals("domain"))		// Domain
		{
		    if (curr.domain_set)  continue;
		    if (params[idx2].getValue().charAt(0) != '.')
			curr.domain = "." + params[idx2].getValue();
		    else
			curr.domain = params[idx2].getValue();
		    curr.domain_set = true;
		}
		else if (name.equals("max-age"))	// Max-Age
		{
		    if (curr.expires != null)  continue;
		    int age;
		    try
			{ age = Integer.parseInt(params[idx2].getValue()); }
		    catch (NumberFormatException nfe)
		    {
			throw new ProtocolException("Bad Set-Cookie2 header: " +
					    set_cookie + "\nMax-Age '" +
					    params[idx2].getValue() +
					    "' not a number");
		    }
		    curr.expires =
			    new Date(System.currentTimeMillis() + age*1000L);
		}
		else if (name.equals("port"))		// Port
		{
		    if (curr.port_set)  continue;

		    if (params[idx2].getValue() == null)
		    {
			curr.port_list    = new int[1];
			curr.port_list[0] = req.getConnection().getPort();
			curr.port_set     = true;
			continue;
		    }

		    curr.port_list_str = params[idx2].getValue();
		    StringTokenizer tok =
			    new StringTokenizer(params[idx2].getValue(), ",");
		    curr.port_list = new int[tok.countTokens()];
		    for (int idx3=0; idx3<curr.port_list.length; idx++)
		    {
			String port = tok.nextToken().trim();
			try
			    { curr.port_list[idx3] = Integer.parseInt(port); }
			catch (NumberFormatException nfe)
			{
			    throw new ProtocolException("Bad Set-Cookie2 header: " +
						    set_cookie + "\nPort '" +
						    port + "' not a number");
			}
		    }
		    curr.port_set = true;
		}
		else if (name.equals("discard"))	// Domain
		{
		    if (discard_set)  continue;
		    curr.discard = true;
		    discard_set  = true;
		}
		else if (name.equals("secure"))		// Secure
		{
		    if (secure_set)  continue;
		    curr.secure = true;
		    secure_set  = true;
		}
		else if (name.equals("comment"))	// Comment
		{
		    if (curr.comment != null)  continue;
		    curr.comment = params[idx2].getValue();
		}
		else if (name.equals("commenturl"))	// CommentURL
		{
		    if (curr.comment_url != null)  continue;
		    try
			{ curr.comment_url = new URL(params[idx2].getValue()); }
		    catch (MalformedURLException mue)
		    {
			throw new ProtocolException("Bad Set-Cookie2 header: " +
						set_cookie + "\nCommentURL '" +
						params[idx2].getValue() +
						"' not a valid URL");
		    }
		}
		// ignore unknown element
	    }


	    // check version

	    if (curr.version == -1)
		throw new ProtocolException("Bad Set-Cookie2 header: " +
					    set_cookie + "\nMissing Version " +
					    "attribute");
	    if (curr.version != 1)  continue;	// ignore unknown version


	    // check validity

	    if (!Util.getPath(req.getRequestURI()).startsWith(curr.path))
		continue;

	    if (!curr.domain.equalsIgnoreCase("localhost")  &&
		curr.domain.substring(1, curr.domain.length()-1).
		indexOf('.') == -1)  continue;

	    String host = req.getConnection().getHost();
	    if (!host.endsWith(curr.domain))  continue;

	    if (host.substring(0, host.length()-curr.domain.length()).
		indexOf('.') != -1)  continue;

	    if (curr.port_set)
	    {
		int idx2=0;
		for (idx2=0; idx2<curr.port_list.length; idx++)
		    if (curr.port_list[idx2] == req.getConnection().getPort())
			break;
		if (idx2 == curr.port_list.length)  continue;
	    }


	    // setup defaults

	    if (curr.expires == null)  curr.discard = true;

	    cookie_arr[cidx++] = curr;
	}

	if (cidx < cookie_arr.length)
	    cookie_arr = Util.resizeArray(cookie_arr, cidx);

	return cookie_arr;
    }


    /**
     * @return the version as an int
     */
    public int getVersion()
    {
	return version;
    }

 
    /**
     * @return true if the cookie should be discarded at the end of the
     *         session; false otherwise
     */
    public boolean discard()
    {
	return discard;
    }
 

    /**
     * @param  req  the request to be sent
     * @return true if this cookie should be sent with the request
     */
    protected boolean sendWith(RoRequest req)
    {
	HTTPConnection con = req.getConnection();

	boolean port_match = !port_set;
	if (port_set)
	    for (int idx=0; idx<port_list.length; idx++)
		if (port_list[idx] == con.getPort())
		{
		    port_match = true;
		    break;
		}

	return (con.getHost().endsWith(domain)  &&  port_match  &&
		Util.getPath(req.getRequestURI()).startsWith(path)  &&
		(!secure || con.getProtocol().equals("https") ||
		 con.getProtocol().equals("shttp")));
    }
 

    protected String toExternalForm()
    {
	StringBuffer cookie = new StringBuffer();

	if (version == 1)
	{
	    /*
	    cookie.append("$Version=");
	    cookie.append(version);
	    */
	    cookie.append("; ");

	    cookie.append(name);
	    cookie.append("=");
	    cookie.append(value);

	    if (path_set)
	    {
		cookie.append("; ");
		cookie.append("$Path=");
		cookie.append(path);
	    }

	    if (domain_set)
	    {
		cookie.append("; ");
		cookie.append("$Domain=");
		cookie.append(domain);
	    }

	    if (port_set)
	    {
		cookie.append("; ");
		cookie.append("$Port");
		if (port_list_str != null)
		{
		    cookie.append("=\"");
		    cookie.append(port_list_str);
		    cookie.append('\"');
		}
	    }
	}
	else
	    throw new Error("Internal Error: unknown version " + version);

	return cookie.toString();
    }


    /**
     * Create a string containing all the cookie fields. The format is that
     * used in the Set-Cookie header.
     */
    public String toString()
    {
	String string = name + "=" + value;

	if (version == 1)
	{
	    string += "; Version=" + version;
	    string += "; Path=" + path;
	    string += "; Domain=" + domain;
	    if (port_set)
	    {
		string += "; Port=\"" + port_list[0];
		for (int idx=1; idx<port_list.length; idx++)
		    string += "," + port_list[idx];
		string += "\"";
	    }
	    if (expires != null)   string += "; Max-Age=" +
			((expires.getTime() - new Date().getTime()) / 1000L);
	    if (discard)           string += "; Discard";
	    if (secure)            string += "; Secure";
	    if (comment != null)   string += "; Comment=\"" + comment + "\"";
	    if (comment_url != null)  string += "; CommentURL=\"" + comment_url + "\"";
	}
	else
	    throw new Error("Internal Error: unknown version " + version);

	return string;
    }


    /**
     * Read cookies from a file into a hashtable. Each entry in the
     * hashtable will have the same key and value (i.e. we do a
     * put(cookie, cookie)).
     *
     * @param file the cookie file
     * @param list the hashtable to fill
     */
    static void readFromFile(File file, Hashtable list)
    {
	// in windows, skip the mm2048.dat and mm256.dat files
    }


    /**
     * Saves the cookies in the hashtable to a file.
     *
     * @param file the cookie file
     * @param list the hashtable of cookies
     */
    static void saveToFile(File file, Hashtable list)
    {
    }
}

