/*
 * Copyright (C) 2004 Regents of the University of California.
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

/**
 * Title:       ACLXML
 * Description: Defines the needed Generic ACL XML Elements from RFC 3744
 *              Updated as necessary.
 * Copyright:   Copyright (c) 2004 Regents of the University of California. All rights reserved.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        
 */

package edu.uci.ics.DAVExplorer;

/**
 * This class defines the needed Generic ACL XML Elements from RFC 3744.
 * Updated as necessary.
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc3744.txt">RFC 3744</a>
 */
public class ACLXML extends DeltaVXML
{
    public static final String ELEM_ACL = "acl";
    public static final String ELEM_ACE = "ace";
    public static final String ELEM_PRINCIPAL = "principal";
    public static final String ELEM_GRANT = "grant";
    public static final String ELEM_DENY = "deny";
    public static final String ELEM_PRIVILEGE = "privilege";
}
