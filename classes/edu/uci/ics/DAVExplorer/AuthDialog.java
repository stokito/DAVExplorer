/*
 * Copyright (c) 1999-2001 Regents of the University of California.
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

package DAVExplorer;

import HTTPClient.AuthorizationPrompter;
import HTTPClient.AuthorizationInfo;
import HTTPClient.NVPair;

public class AuthDialog implements AuthorizationPrompter
{

    public AuthDialog()
    {
    }

    /**
     * the method called by DefaultAuthHandler.
     *
     * @return the username/password pair
     */
    public NVPair getUsernamePassword(AuthorizationInfo info, boolean forProxy)
    {
        WebDAVLoginDialog dlg = new WebDAVLoginDialog( "Login", info.getRealm(), info.getScheme(), true );
        if( dlg.getUsername().equals( "" ) || dlg.getUserPassword().equals( "" ) )
            return null;

        NVPair answer;
        answer = new NVPair( dlg.getUsername(), dlg.getUserPassword() );
        return answer;
    }
}
