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

//
// This is the WebDAV method wrapper class. It is implemented as
// a bean, which listens for a request event. Once the event
// occurs, appropriate Method from the WebDAV class library is
// called.
//
// Version: 0.2
// Author:  Robert Emmery
// Date:    3/25/98
///////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////
// The code has been modified to include povisions for the final
// WebDAV xml namespaces.  A small number of program errors have
// been corrected.
//
// Please use the following contact:
//
// dav-exp@ics.uci.edu
//
// Version: 0.41
// Changes by: Yuzo Kanomata and Joe Feise
// Date: 4/14/99
//
// Change List:
//   Added notification for IO exceptions during connect
//
// Date: 2001-Jan-12
// Joe Feise: Added support for https (SSL)

package DAVExplorer;

import java.io.*;
import HTTPClient.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

public class WebDAVManager
{
    public HTTPResponse Response;
    private WebDAVConnection Con;
    private String Hostname = null;
    private int Port;
    private String ProxyHostname = null;
    private int ProxyPort;
    private String MethodName;
    private String ResourceName;
    private NVPair[] Headers;
    private byte[] Body;
    private String ExtraInfo;
    private Vector Listeners = new Vector();
    private static String WebDAVPrefix = "http://";

    private boolean logging = false;
    private String logFilename = null;

    public WebDAVManager()
    {
    }

    public void sendRequest(WebDAVRequestEvent e)
    {
        String ProxyTempHost = null;
        int ProxyTempPort = 0;
        String proxy = GlobalData.getGlobalData().ReadConfigEntry("proxy");
        boolean useProxy = false;
        // find out if proxy is used
        if( (proxy.length() > 0) || proxy.startsWith(WebDAVPrefix) )
        {
            if( proxy.startsWith(WebDAVPrefix) )
                proxy = proxy.substring(WebDAVPrefix.length());
            StringTokenizer str = new StringTokenizer( proxy, "/" );
            if( !str.hasMoreTokens() )
            {
                GlobalData.getGlobalData().errorMsg("Invalid proxy name.");
                return;
            }
            proxy = str.nextToken();

            str = new StringTokenizer( proxy, ":" );
            if( !str.hasMoreTokens() )
            {
                GlobalData.getGlobalData().errorMsg("Invalid proxy name.");
                return;
            }
            useProxy = true;
            ProxyTempHost = str.nextToken();
            if( str.hasMoreTokens() )
            {
                try
                {
                    ProxyTempPort = Integer.parseInt( str.nextToken() );
                }
                catch (Exception ex)
                {
                    GlobalData.getGlobalData().errorMsg("Invalid proxy port number.");
                    Port = 0;
                    return;
                }
            }
        }

        WebDAVTreeNode tn = e.getNode();
        String TempHost = e.getHost();
        int TempPort = e.getPort();

        if( ((TempHost!=null) && (TempHost.length()>0) && !TempHost.equals(Hostname)) ||
            (TempPort!=Port) ||
            ((ProxyTempHost!=null) && (ProxyTempHost.length()>0) && !ProxyTempHost.equals(ProxyHostname)) ||
            (ProxyTempPort!=ProxyPort) )
        {
            try
            {
                if( useProxy )
                {
                    ProxyHostname = ProxyTempHost;
                    ProxyPort = ProxyTempPort;
                    HTTPConnection.setProxyServer( ProxyHostname, ProxyPort );
                }
                Hostname = TempHost;
                if (TempPort != 0)
                {
                    Port = TempPort;
                    if( GlobalData.getGlobalData().doSSL() )
                        Con = new WebDAVConnection( "https", Hostname, Port );
                    else
                        Con = new WebDAVConnection(Hostname, Port);
                }
                else
                {
                    Port = 0;
                    if( GlobalData.getGlobalData().doSSL() )
                        Con = new WebDAVConnection( "https", Hostname, 443 );
                    else
                        Con = new WebDAVConnection(Hostname);
                }
                Con.setLogging( logging, logFilename );
            }
            catch( HTTPClient.ProtocolNotSuppException httpException )
            {
                GlobalData.getGlobalData().errorMsg( "Error: Protocol not supported.\n" + httpException.toString() );
            }
        }
        if( Con == null )
        {
            // user hit enter twice? Creating a new WebDAVConnection can take
            // a while, especially if a proxy is involved
            // so we just return here
            return;
        }

        String user = e.getUser();
        String pass = e.getPass();

        if (user.length() > 0)
        {
            try
            {
                Con.addDigestAuthorization(Hostname,user, pass);
                Con.addBasicAuthorization(Hostname,user,pass);
            }
            catch (Exception exc)
            {
                System.out.println(exc);
            }
        }

        MethodName = e.getMethod();
        ResourceName = e.getResource();
        Headers = e.getHeaders();
        Body = e.getBody();
        ExtraInfo = e.getExtraInfo();
        try {
            Response = Con.Generic(MethodName, ResourceName, Body, Headers);

            WebDAVResponseEvent webdavResponse  = GenerateWebDAVResponse(Response,tn);
            fireResponse(webdavResponse);
        }
        catch (IOException exception)
        {
            GlobalData.getGlobalData().errorMsg("Connection error: \n" + exception);
        }
        catch (ModuleException exception)
        {
            GlobalData.getGlobalData().errorMsg("HTTPClient error: \n" + exception);
        }
    }

    public synchronized void addResponseListener(WebDAVResponseListener l)
    {
        Listeners.addElement(l);
    }

    public synchronized void removeResponseListener(WebDAVResponseListener l)
    {
        Listeners.removeElement(l);
    }

    public WebDAVResponseEvent GenerateWebDAVResponse(HTTPResponse Response, WebDAVTreeNode Node)
    {
        WebDAVResponseEvent e = new WebDAVResponseEvent(this,Hostname, Port, ResourceName,MethodName,Response,ExtraInfo, Node);
        return e;
    }

    public void fireResponse(WebDAVResponseEvent e)
    {
        Vector ls;
        synchronized (this)
        {
            ls = (Vector) Listeners.clone();
        }


        for (int i=0; i<ls.size();i++) {
            WebDAVResponseListener l = (WebDAVResponseListener) ls.elementAt(i);
            l.responseFormed(e);
        }
    }

    public void setLogging( boolean logging, String filename )
    {
        this.logging = logging;
        this.logFilename = filename;

        if( Con != null )
        {
            Con.setLogging( logging, filename );
        }
    }

    protected WebDAVConnection createProxyConnection( String Hostname, int Port )
    {
        if( Port != 0 )
        {
            return new WebDAVConnection( Hostname, Port );
        }
        else
        {
            return new WebDAVConnection( Hostname );
        }
    }
}
