/*
 * Copyright (c) 1998-2003 Regents of the University of California.
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
 * Title:       WebDAVResponse Interpreter
 * Description: This is the interpreter module that parses WebDAV responses.
 *              Some of the methods are not parsed, and the functions are left
 *              empty intentionally.
 * Copyright:   Copyright (c) 1998-2003 Regents of the University of California. All rights reserved.
 * @author      Robert Emmery
 * @date        2 April 1998
 * @author      Yuzo Kanomata, Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 1999
 * Changes:     Fixed parseGet to save the retrieved file properly
 *              parseMkCol now has functionality to refresh the display
 * @author      Joachim Feise (dav-exp@ics.uci.edu), Eric Giguere
 * @date        23 May 2000
 * Changes:     Added check for CDATA to improve interoperability for Sharemation's server
 *              Incorporated Eric Giguere's changes to getOwnerInfo(). Thanks!
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        12 January 2001
 * Changes:     Added support for https (SSL)
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        1 October 2001
 * Changes:     Change of package name
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 2003
 * Changes:     Integrated Brian Johnson's applet changes.
 *              Added better error reporting.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        27 April 2003
 * Changes:     Added shared lock functionality.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        23 September 2003
 * Changes:     Refactored code during DeltaV integration.
 */

package edu.uci.ics.DAVExplorer;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import HTTPClient.HTTPResponse;
import com.ms.xml.om.Element;
import com.ms.xml.om.ElementImpl;
import com.ms.xml.om.Document;
import com.ms.xml.om.TreeEnumeration;
import com.ms.xml.om.SiblingEnumeration;
import com.ms.xml.util.XMLOutputStream;
import com.ms.xml.util.Name;

public class WebDAVResponseInterpreter
{

    public WebDAVResponseInterpreter()
    {
    }

    public WebDAVResponseInterpreter( WebDAVRequestGenerator rg )
    {
        super();
        generator = rg;
        String classPath = System.getProperty("java.class.path");
        if (classPath == null)
        {
            WebDAVEditDir = null;
            return;
        }
        StringTokenizer paths = new StringTokenizer(classPath,":;");
        boolean found = false;
        while (paths.hasMoreTokens())
        {
            String nextPath = paths.nextToken();
            if (!nextPath.endsWith(new Character(File.separatorChar).toString()))
                nextPath += File.separatorChar;
            nextPath += WebDAVClassName + File.separatorChar;
            File classDir = new File(nextPath + "icons");
            if (!classDir.exists())
                continue;
            File editDir = new File(nextPath + EditDir);
            if (!editDir.exists())
                editDir.mkdir();
            WebDAVEditDir = nextPath + EditDir;
        }
        userPathDir = System.getProperty( "user.home" );
        if( userPathDir == null )
            userPathDir = "";
        else
            userPathDir += File.separatorChar;
    }

    public void handleResponse(WebDAVResponseEvent e)
        throws ResponseException
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::handleResponse" );
        }

        res = e.getResponse();
        Method = e.getMethodName();
        Extra = e.getExtraInfo();
        HostName = e.getHost();
        Port = e.getPort();

        // get the resource name, and unescape it
        // TODO: get encoding
        Resource = GlobalData.getGlobalData().unescape( e.getResource(), null, true );
        Node = e.getNode();

        try
        {
            if (res.getStatusCode() >= 300)
            {
                if( (res.getStatusCode() == 302) || (res.getStatusCode() == 301) )
                {
                    String location = res.getHeader( "Location" );
                    GlobalData.getGlobalData().errorMsg("The resource requested moved to " + location + "\nPlease try connecting to the new location." );
                }
                else if( ( Method.equals("MOVE") || Method.equals("DELETE"))&&( (res.getStatusCode() == 412) || (res.getStatusCode() == 423)) )
                {
                    // Do the processing for the two kinds of Methods
                    // That is discoverLock, but set the passed in String to
                    // set the Extra field to be return processed
                    if (Method.equals("MOVE"))
                    {
                    // check if this is the second trip
                        if (Extra.startsWith("rename2:"))
                        {
                            // Reset the name we attempted to change
                            ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_FIRST, "reset");
                            actionListener.actionPerformed(ae);

                            // Alert User of error
                            GlobalData.getGlobalData().errorMsg("Rename Failed\nStatus " + res.getStatusCode() + " " + res.getReasonLine() );
                        }
                        else  // first attempt
                        {
                            int pos = Extra.indexOf(":");
                            String tmp = Extra.substring(pos + 1);

                            clearStream();
                            generator.DiscoverLock("rename2:" + tmp);
                        }
                    }
                    else
                    {
                        // check if this is the second trip
                        if (Extra.startsWith("delete2:"))
                        {
                            GlobalData.getGlobalData().errorMsg("Delete Failed\nStatus " + res.getStatusCode() + " " + res.getReasonLine() );
                        }
                        else  // first attempt
                        {
                            clearStream();
                            generator.DiscoverLock("delete2:");
                        }
                    }
                }
                else
                    GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\n" + res.getStatusCode() + " " + res.getReasonLine());
                return;
            }
            if (Method.equals("MOVE"))
            {
                parseMove();
                return;
            }
        }
        catch (Exception ex)
        {
            // Most likely an error propagated from HTTPClient
            // We get this error if the server closes the connection
            // and the method is unknown to HTTPClient.
            // HTTPClient does an automatic retry for idempotent HTTP methods,
            // but not for our WebDAV methods, since it doesn't know about them.
            String debugOutput = System.getProperty( "debug", "false" );
            if( debugOutput.equals( "true" ) )
                System.out.println(ex);
            throw new ResponseException( "HTTP error" );
        }

        if (Method.equals("OPTIONS"))
            parseOptions();
        else if (Method.equals("PROPFIND"))
            parsePropFind();
        else if (Method.equals("PROPPATCH"))
            parsePropPatch();
        else if (Method.equals("MKCOL"))
            parseMkCol();
        else if (Method.equals("GET"))
            parseGet();
        else if (Method.equals("PUT"))
            parsePut();
        else if (Method.equals("DELETE"))
            parseDelete();
        else if (Method.equals("COPY"))
        {
            try
            {
                if (res.getStatusCode() == 201)
                {
                    executeCopy();
                }
            }
            catch(Exception ex)
            {
                System.out.println(ex);
                throw new ResponseException( "Copy error" );
            }
        }
        else if (Method.equals("LOCK"))
            parseLock( false );
        else if (Method.equals("UNLOCK"))
            parseUnlock();
        else
        {
            System.out.println("unsupported method.. cannot parse");
            return;
        }
    }

    protected void saveProps( Element parent, Element prop, int tabs )
    {
        Element newProp = null;
        if( prop.getType()  == Element.PCDATA )
        {
            newProp = new ElementImpl( null,Element.PCDATA );
            newProp.setText( prop.getText() );
        }
        else if( prop.getType() == Element.ELEMENT )
        {
            newProp = new ElementImpl( prop.getTagName(), Element.ELEMENT );
        }
        else
        {
            return;
        }

        parent.addChild( WebDAVXML.elemNewline,null );
        for( int t=0; t<tabs; t++ )
            parent.addChild( WebDAVXML.elemDSpace,null );
        parent.addChild( newProp,null );

        if( prop.numElements() > 0 )
        {
            Enumeration propEnum = prop.getElements();
            while (propEnum.hasMoreElements())
            {
                Element propEl = (Element) propEnum.nextElement();
                if ( (propEl.getType() == Element.ELEMENT) || (propEl.getType() == Element.PCDATA) )
                {
                    saveProps( newProp, propEl, tabs+1 );
                }
            }
            newProp.addChild( WebDAVXML.elemNewline,null );
            for( int t=0; t<tabs; t++ )
                newProp.addChild( WebDAVXML.elemDSpace,null );
        }
    }


    public void parseOptions()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseOptions" );
        }

        try
        {
            String davheader = res.getHeader( "DAV" );
            if( davheader == null )
            {
                // no WebDAV support
                GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nThe server does not support WebDAV\nat Resource " + Resource + ".");
                return;
            }
        }
        catch (Exception e)
        {
            GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nError encountered \nwhile parsing OPTIONS Response:\n" + e);
            stream = null;
            return;
        }

        if (Extra.equals("uribox"))
        {
            // we got here from entering a URI, so now we need to do a PROPFIND
            String str = HostName;
            if (Port > 0)
                str += ":" + Port;
            str += Resource;
            // 1999-June-08, Joachim Feise (dav-exp@ics.uci.edu):
            // workaround for IBM's DAV4J, which does not handle propfind properly
            // with the prop tag. To use the workaround, run DAV Explorer with
            // 'java -jar -Dpropfind=allprop DAVExplorer.jar'
            // Note that this prevents the detection of DeltaV information, since
            // RFC 3253 states in section 3.11 that "A DAV:allprop PROPFIND request
            // SHOULD NOT return any of the properties defined by this document."
            String doAllProp = System.getProperty( "propfind" );
            if( (doAllProp != null) && doAllProp.equalsIgnoreCase("allprop") )
            {
                if( generator.GeneratePropFind( str, "allprop", "one", null, null, false ) )
                {
                    generator.execute();
                }
            }
            else
            {
                String[] props;
                props = new String[6];
                props[0] = "displayname";
                props[1] = "resourcetype";
                props[2] = "getcontenttype";
                props[3] = "getcontentlength";
                props[4] = "getlastmodified";
                props[5] = "lockdiscovery";

                if( generator.GeneratePropFind( str, "prop", "one", props, null, false ) )
                {
                    generator.execute();
                }
            }
        }
    }


    public void parsePropFind()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parsePropFind" );
        }

        byte[] body = null;
        Document xml_doc = null;

        try
        {
            body = res.getData();
            stream = body;
            if (body == null)
            {
                GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nMissing XML body in\nPROPFIND response.");
                return;
            }
            ByteArrayInputStream byte_in = new ByteArrayInputStream(body);
            xml_doc = new Document();
            xml_doc.load( byte_in );
        }
        catch (Exception e)
        {
            GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nError encountered \nwhile parsing PROPFIND Response.\n" + e);
            stream = null;
            return;
        }

        printXML( body );

        if (Extra.equals("uribox"))
        {
            // we got here from entering a URI, so now we need to add the uri
            // to the tree
            if( Port > 0 )
            {
                fireInsertionEvent(HostName + ":" + Port + Resource);
            }
            else
            {
                fireInsertionEvent(HostName + Resource);
            }
        }
        else if( Extra.equals("exclusiveLock") || Extra.equals("sharedLock") || Extra.equals("unlock")
                 || Extra.equals("delete") || Extra.startsWith("rename:")
                 || Extra.equals("display") || Extra.equals("commit")
                 || Extra.startsWith("rename2:") || Extra.startsWith("delete2:") )
        {
            // get lock information out of XML tree
            String lockToken = null;
            String ownerInfo = "";
            String lockType = "";
            String lockScope = "";
            String lockTimeout = "";
            String lockDepth = "";

            String[] token = new String[2];
            token[0] = new String( WebDAVXML.ELEM_RESPONSE );
            token[1] = new String( WebDAVXML.ELEM_PROPSTAT );
            Element rootElem = skipElements( xml_doc, token );
            if( rootElem != null )
            {
                TreeEnumeration enumTree =  new TreeEnumeration( rootElem );
                while( enumTree.hasMoreElements() )
                {
                    Element current = (Element)enumTree.nextElement();
                    Name currentTag = current.getTagName();
                    if( currentTag != null )
                    {
                        if( currentTag.getName().equals( WebDAVXML.ELEM_STATUS ) )
                        {
                            int status = getStatus( current );
                            if( status < 300 )
                            {
                                // everything ok
                            }
                            else if( status < 400 )
                            {
                            }
                            else if( status < 500 )
                            {
                                if( Extra.equals("exclusiveLock") || Extra.equals("sharedLock") || Extra.equals("unlock") )
                                {
                                    GlobalData.getGlobalData().errorMsg( "This resource does not support locking." );
                                    return;
                                }
                            }
                            else
                            {
                                GlobalData.getGlobalData().errorMsg( "Server error: " + status );
                                return;
                            }
                        }
                        else if( currentTag.getName().equals( WebDAVXML.ELEM_PROP ) )
                        {
                            token = new String[3];
                            token[0] = new String( WebDAVXML.ELEM_PROP );
                            token[1] = new String( WebDAVProp.PROP_LOCKDISCOVERY );
                            token[2] = new String( WebDAVXML.ELEM_ACTIVE_LOCK );

                            rootElem = skipElements( current, token );
                            if( rootElem != null )
                            {
                                enumTree =  new TreeEnumeration( rootElem );
                                while( enumTree.hasMoreElements() )
                                {
                                    current = (Element)enumTree.nextElement();
                                    currentTag = current.getTagName();
                                    if( currentTag != null )
                                    {
                                        if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_TOKEN ) )
                                        {
                                            lockToken = getLockToken( current );
                                        }
                                        else if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_TYPE ) )
                                        {
                                            lockType = getLockType( current );
                                        }
                                        else if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_SCOPE ) )
                                        {
                                            lockScope = getLockScope( current );
                                        }
                                        else if( currentTag.getName().equals( WebDAVXML.ELEM_OWNER ) )
                                        {
                                            ownerInfo = getOwnerInfo( current );
                                        }
                                        else if( currentTag.getName().equals( WebDAVXML.ELEM_TIMEOUT ) )
                                        {
                                            lockTimeout = getLockTimeout( current );
                                        }
                                        else if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_DEPTH ) )
                                        {
                                            lockDepth = getLockDepth( current );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (lockToken != null)
            {
                lockToken.trim();
                int pos = lockToken.indexOf("opaque");
                if( pos >= 0 )
                    lockToken = lockToken.substring(pos);
            }
            if (Extra.equals("exclusiveLock"))
            {
                    String lockInfo = getLockInfo();
                    generator.GenerateLock( lockInfo, lockToken, true );
                    generator.execute();
            }
            else if (Extra.equals("sharedLock"))
            {
                    String lockInfo = getLockInfo();
                    generator.GenerateLock( lockInfo, lockToken, false );
                    generator.execute();
            }
            else if (Extra.equals("unlock"))
            {
                if (lockToken != null)
                {
                    generator.GenerateUnlock(lockToken);
                    generator.execute();
                }
            }
            else if (Extra.equals("delete"))
            {
                generator.setNode(Node); // sets the Node which will be operated
                generator.GenerateDelete(lockToken);
                generator.execute();
            }
            else if (Extra.startsWith("rename:"))
            {
                int pos = Extra.indexOf(":");
                String tmp = Extra.substring(pos + 1);
                pos = tmp.indexOf( ":" );
                String dest = null;
                String dir = null;
                if( pos >= 0 )
                {
                    dest = tmp.substring( 0, pos );
                    dir = tmp.substring( pos + 1 );
                }
                else
                    dest = tmp;

                clearStream();
                //Old
                generator.setNode(Node);
                generator.GenerateMove(dest, dir, false, true, lockToken, "rename");
                generator.execute();
            }
            else if(Extra.startsWith("rename2:"))
            {
                // gets the response to the query DiscoverLock
                generator.setSecondTime(true);
                generator.GenerateMove(null, null, false, true, lockToken, "rename2:");
                generator.setSecondTime(false);
                clearStream();
                generator.execute();
            }
            else if(Extra.startsWith("delete2:"))
            {
                // gets the response to the query DiscoverLock
                generator.setSecondTime(true);
                generator.GenerateDelete(lockToken);
                generator.setSecondTime(false);
                clearStream();
                generator.execute();
            }
            else if (Extra.equals("display"))
            {
                displayLock(lockType, lockScope, lockDepth, lockToken, lockTimeout, ownerInfo);
            }
            else if (Extra.equals("commit"))
            {
                String newRes = Resource.substring(1);
                String fileName =  WebDAVEditDir + File.separatorChar + newRes;
                File theFile = new File(fileName);
                if (!theFile.exists())
                {
                    GlobalData.getGlobalData().errorMsg("File not found!\n");
                    return;
                }
                else
                {
                    generator.GeneratePut(fileName, newRes, lockToken, null);
                    generator.execute();
                }
            }
        }
        else if (Extra.equals("properties"))
        {
            String locktoken = parseLock( true );
            Document ppatchDoc = new Document();
            ByteArrayOutputStream byte_prop = new ByteArrayOutputStream();
            XMLOutputStream  xml_prop = new XMLOutputStream(byte_prop);
            byte[] prop_out = null;
            String[] token = new String[1];
            token[0] = new String( WebDAVXML.ELEM_RESPONSE );
            Element rootElem = skipElements( xml_doc, token );
            if( rootElem != null )
            {
                TreeEnumeration enumTree =  new TreeEnumeration( rootElem );
                while( enumTree.hasMoreElements() )
                {
                    Element current = (Element)enumTree.nextElement();
                    Name currentTag = current.getTagName();
                    if( currentTag != null )
                    {
                        if( currentTag.getName().equals( WebDAVXML.ELEM_PROPSTAT ) )
                        {
                            token = new String[2];
                            token[0] = new String( WebDAVXML.ELEM_PROPSTAT );
                            token[1] = new String( WebDAVXML.ELEM_PROP );
                            rootElem = skipElements( current, token );
                            if( rootElem != null )
                            {
                                String host = HostName;
                                if (Port != 0)
                                    host = HostName + ":" + Port;
                                PropDialog pd = new PropDialog( rootElem, Resource, host, locktoken, true );
                            }
                        }
                    }
                }
            }
        }
        else if(Extra.equals("expand"))
        {
            // Allow for post processing in Main ResponseListener
        }
        else if(Extra.equals("index"))
        {
            // Allow for post processing in Main ResponseListener
        }
        else if(Extra.equals("select"))
        {
            // Allow for post processing in Main ResponseListener
        }
        else
        {
            //  "refresh"
            setRefresh(Node);
            fireInsertionEvent(null);
        }
    }

    public String getLockInfo()
    {
        // if lockinfo doesn't exist, use default
        return GlobalData.getGlobalData().ReadConfigEntry( "lockinfo", "DAV Explorer" );
    }

    public boolean Refreshing()
    {
        return refresh;
    }

    public void ResetRefresh()
    {
        refresh = false;
    }

    public void setRefresh( WebDAVTreeNode node )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::setRefresh" );
        }

        refresh = true;
        Node = node;
        // Piggy back on the Copy Response stuff
        clearStream();
        CopyResponseEvent e = new CopyResponseEvent( this, Node);
        copyListener.CopyEventResponse(e);
    }

    public void parsePropPatch()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parsePropPatch" );
        }
    }

    public void parseMkCol()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseMkCol" );
        }

        clearStream();

        if (Extra.equals("mkcol"))
        {
                CopyResponseEvent e = new CopyResponseEvent( this, Node);
                copyListener.CopyEventResponse(e);
        }
        else if (Extra.equals("mkcolbelow"))
        {
            // Piggy Back on Put Event,
            // This reloads the node on the selected collection,
            // but should not change the selection.
            WebDAVTreeNode parent = generator.getPossibleParentOfSelectedCollectionNode();
            PutEvent e = new PutEvent( this, Node, parent);
            putListener.PutEventResponse(e);
        }
    }

    public void parseGet()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseGet" );
        }

        // inform the user
        byte[] body = null;
        String fileName = "";
        try
        {
            FileOutputStream fout = null;
            String newRes = Resource.substring(1);
            if (Extra.equals("saveas"))
            {
                FileDialog fd = new FileDialog(GlobalData.getGlobalData().getMainFrame(), "Save As" , FileDialog.SAVE);
                int pos = newRes.lastIndexOf( "/" );
                if( pos >= 0 )
                    newRes = newRes.substring( pos + 1 );
                fd.setFile( newRes );
                fd.setVisible(true);
                String dir = fd.getDirectory();
                if( (dir == null) || dir.equals("") )
                    return;
                String fname = fd.getFile();
                if( (fname == null) || fname.equals("") )
                    return;
                fileName = dir + fname;
            }
            else
            {
                fileName =  WebDAVEditDir + File.separatorChar + newRes;
                // write the proper separator
                StringBuffer fName = new StringBuffer( fileName );
                for( int pos = 0; pos < fName.length(); pos++ )
                {
                    if( (fName.charAt(pos) == '/') || (fName.charAt(pos) == '\\') )
                        fName.setCharAt( pos, File.separatorChar );
                }
                fileName = fName.toString();
            }

            // create all subdirectories as necessary
            String dir = fileName.substring( 0, fileName.lastIndexOf( File.separatorChar ) );
            File theDir = new File( dir );
            theDir.mkdirs();

            File theFile = new File(fileName);
            boolean bSave = true;
            String os = (System.getProperty( "os.name" )).toLowerCase();
            String dirName = null;

            if( theFile.exists() && (os.indexOf("windows")==-1) )
            {
                if (!replaceFile(fileName))
                {
                    bSave = false;
                    if ( (Extra.equals("view")) || (Extra.equals("edit")) )
                    {
                        if( !launchAnyway() )
                        {
                            return;
                        }
                    }
                }
            }
            if( bSave )
            {
                body = res.getData();
                fout = new FileOutputStream(fileName);
                if (fout == null)
                    return;
                fout.write(body);
                fout.close();
            }

            if( Extra.equals("view") || Extra.equals("edit") )
            {
                String app = selectApplication();
                if( (app != null) && (app != "") )
                {
                    Runtime rt = Runtime.getRuntime();
                    String[] cmdarray =  new String[2];
                    cmdarray[0] = app;
                    cmdarray[1] = fileName;
                    rt.exec( cmdarray );
                }
            }
        }
        catch (Exception exc)
        {
            return;
        }
    }

    public void parsePut()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parsePut" );
        }

        // Piggy back on the Copy Response stuff
        clearStream();

        WebDAVTreeNode parent = generator.getPossibleParentOfSelectedCollectionNode();
        if (parent != null)
        {
            // Need to 1. maintain the selected node on both the
            // the Tree View and the File View.
            // Need to 2. reload the node to which the put has taken place.
            //   a. what if node loaded,
            //   b. what if node is not loaded
            PutEvent e = new PutEvent( this, Node, parent);
            putListener.PutEventResponse(e);
        }
        else
        {
            CopyResponseEvent e = new CopyResponseEvent( this, Node);
            copyListener.CopyEventResponse(e);
        }
        generator.resetParentNode();
    }

    public void parseDelete()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseDelete" );
        }

        // Piggy back on the Copy Response stuff
        clearStream();
        CopyResponseEvent e = new CopyResponseEvent( this, Node);
        copyListener.CopyEventResponse(e);
    }

    public void addCopyResponseListener( CopyResponseListener l)
    {
        // Add only one for now
        copyListener = l;
    }

    public void addPutListener( PutListener l)
    {
        // Add only one for now
        putListener = l;
    }

    public void addActionListener( ActionListener l)
    {
        // Add only one for now
        actionListener = l;
    }


    public void executeCopy()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::executeCopy" );
        }

        CopyResponseEvent e = new CopyResponseEvent( this, Node);

        copyListener.CopyEventResponse(e);
    }

    public void parseCopy()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseCopy" );
        }

        // inform the user
        setRefresh( Node );
        fireInsertionEvent(null);
    }

    public void parseMove()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseMove" );
        }

        try
        {
            if (res.getStatusCode() >= 300)
            {
                if( Extra.startsWith("rename"))
                {
                    ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_FIRST, "reset");
                    actionListener.actionPerformed(ae);
                }
                GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\n" + res.getStatusCode() + " " + res.getReasonLine());
            }
        }
        catch( Exception e )
        {
            System.out.println(e);
            return;
        }

        clearStream();

        CopyResponseEvent e = new CopyResponseEvent( this, Node);
        copyListener.CopyEventResponse(e);
    }

    public String parseLock( boolean secondary )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseLock" );
        }

        byte[] body = null;
        Document xml_doc = null;
        try
        {
            body = res.getData();
            stream = body;
            if (body == null)
            {
                if( !secondary )
                    GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nMissing XML body in\nLOCK response.");
                return null;
            }
            ByteArrayInputStream byte_in = new ByteArrayInputStream(body);
            xml_doc = new Document();
            xml_doc.load( byte_in );
        }
        catch (Exception e)
        {
            if( !secondary )
            {
                GlobalData.getGlobalData().errorMsg("DAV Interpreter:\n\nError encountered \nwhile parsing LOCK Response.\n" + e);
                stream = null;
            }
            return null;
        }

        if( !secondary )
            printXML( body );

        String lockToken = null;
        String[] token = new String[2];
        token[0] = new String( WebDAVProp.PROP_LOCKDISCOVERY );
        token[1] = new String( WebDAVXML.ELEM_ACTIVE_LOCK );

        Element rootElem = skipElements( xml_doc, token );
        if( rootElem != null )
        {
            TreeEnumeration enumTree =  new TreeEnumeration( rootElem );
            while( enumTree.hasMoreElements() )
            {
                Element current = (Element)enumTree.nextElement();
                Name currentTag = current.getTagName();
                if( currentTag != null )
                {
                    if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_TOKEN ) )
                    {
                        lockToken = getLockToken( current );
                        if( lockToken != null )
                        {
                            lockToken.trim();
                            int pos = lockToken.indexOf("opaque");
                            if( pos >= 0 )
                                lockToken = lockToken.substring(pos);
                        }
                        break;
                    }
                }
            }
        }
        if( !secondary )
            fireLockEvent( 0, lockToken );
        return lockToken;
    }

    public void parseUnlock()
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::parseUnlock" );
        }

        // inform the user
        fireLockEvent( 1, null );
    }


    protected DataNode parseProps( Element propElem, String ResourceName, String resName )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::parseProps" );
        }

        boolean isColl = false;
        boolean isLocked = false;
        String lockToken = null;
        String resDisplay = "";
        String resType = "";
        String resLength = "0";
        String resDate = "";

        String[] token = new String[1];
        token[0] = new String( WebDAVXML.ELEM_PROP );

        Element rootElem = skipElements( propElem, token );
        if( rootElem != null )
        {
            TreeEnumeration enumTree =  new TreeEnumeration( rootElem );
            while( enumTree.hasMoreElements() )
            {
                Element current = (Element)enumTree.nextElement();
                Name currentTag = current.getTagName();
                if( currentTag != null )
                {
                    if( currentTag.getName().equals( WebDAVProp.PROP_DISPLAYNAME ) )
                    {
                        resDisplay = getDisplayName( current );
                    }
                    else if( currentTag.getName().equals( WebDAVProp.PROP_LOCKDISCOVERY ) )
                    {
                        lockToken = lockDiscovery( current );
                        if( lockToken != null )
                            isLocked = true;
                    }
                    else if( currentTag.getName().equals( WebDAVProp.PROP_RESOURCETYPE ) )
                    {
                        isColl = getResourceType( current );
                    }
                    else if( currentTag.getName().equals( WebDAVProp.PROP_GETCONTENTTYPE ) )
                    {
                        resType = getContentType( current );
                    }
                    else if( currentTag.getName().equals( WebDAVProp.PROP_GETCONTENTLENGTH ) )
                    {
                        resLength = getContentLength( current );
                    }
                    else if( currentTag.getName().equals( WebDAVProp.PROP_GETLASTMODIFIED ) )
                    {
                        resDate = getLastModified( current );
                    }
                }
            }
        }

        // This is where we fill out the data node
        long size = 0;
        try
        {
            size = Long.parseLong(resLength);
        }
        catch( Exception e )
        {
            // ignore error, use default value
        }
        DataNode newNode = new DeltaVDataNode(isColl, isLocked, lockToken, resName,
                                              resDisplay, resType, size, resDate, null);
        return newNode;
    }


    protected String getDisplayName( Element displayName )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getDisplayName" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( displayName );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && tag.getName().equals( WebDAVProp.PROP_DISPLAYNAME ) )
            {
                Element token = (Element)treeEnum.nextElement();
                if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
                {
                    // TODO: get encoding
                    return GlobalData.getGlobalData().unescape( token.getText(), null, false );
                }
            }
        }
        return "";
    }


    protected String lockDiscovery( Element lockdiscovery )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::lockDiscovery" );
        }

        String[] token = new String[1];
        token[0] = new String( WebDAVXML.ELEM_ACTIVE_LOCK );
        int index = 0;
        TreeEnumeration enumTree =  new TreeEnumeration( lockdiscovery );
        while( enumTree.hasMoreElements() )
        {
            Element current = (Element)enumTree.nextElement();
            Name currentTag = current.getTagName();
            if( index >= 0 )
            {
                if( (currentTag != null) && (currentTag.getName().equals( token[index] )) )
                {
                    // we only care about the subtree from this point on
                    enumTree = new TreeEnumeration( current );
                    index++;
                }
                if( index >= token.length )
                    index = -1;
            }
            else if( currentTag != null )
            {
                if( currentTag.getName().equals( WebDAVXML.ELEM_LOCK_TOKEN ) )
                {
                    return getLockToken( current );
                }
            }
        }
        return null;
    }


    protected boolean getResourceType( Element resourcetype )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getResourceType" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( resourcetype );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && tag.getName().equals( WebDAVXML.ELEM_COLLECTION ) )
                return true;
        }
        return false;
    }

    
    protected String getContentType( Element contenttype )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getContentType" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( contenttype );
        while(treeEnum.hasMoreElements() )
        {
            Element token = (Element)treeEnum.nextElement();
            if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
            {
                // TODO: get encoding
                return GlobalData.getGlobalData().unescape( token.getText(), null, false );
            }
        }
        return "";
    }

    
    protected String getContentLength( Element contentlength )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getContentLength" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( contentlength );
        while(treeEnum.hasMoreElements() )
        {
            Element token = (Element)treeEnum.nextElement();
            if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
            {
                // TODO: get encoding
                return GlobalData.getGlobalData().unescape( token.getText(), null, false );
            }
        }
        return "0";
    }


    protected String getLastModified( Element lastmodified )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getLastModified" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( lastmodified );
        while(treeEnum.hasMoreElements() )
        {
            Element token = (Element)treeEnum.nextElement();
            if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
            {
                // TODO: get encoding
                return GlobalData.getGlobalData().unescape( token.getText(), null, false );
            }
        }
        return "";
    }


    public void clearStream()
    {
        stream = null;
    }

    public byte[] getXML()
    {
        return stream;
    }

    public synchronized void addInsertionListener(InsertionListener l)
    {
        listeners.addElement(l);
    }

    public synchronized void removeInsertionListener(InsertionListener l)
    {
        listeners.removeElement(l);
    }

    public synchronized void addMoveUpdateListener(ActionListener l)
    {
        moveListeners.addElement(l);
    }

    public synchronized void removeMoveUpdateListener(ActionListener l)
    {
        moveListeners.removeElement(l);
    }

    public synchronized void addLockListener(ActionListener l)
    {
        lockListeners.addElement(l);
    }

    public synchronized void removeLockListener(ActionListener l)
    {
        lockListeners.removeElement(l);
    }

    public void fireInsertionEvent( String str )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::fireInsertionEvent" );
        }

        Vector ls;

        synchronized (this)
        {
            ls = (Vector)listeners.clone();
        }
        ActionEvent e = new ActionEvent( this, 0, str );
        for( int i=0; i<ls.size(); i++ )
        {
            InsertionListener l = (InsertionListener) ls.elementAt(i);
            l.actionPerformed(e);
        }
    }


    public void fireMoveUpdate(String str)
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::fireMoveUpdate" );
        }

        Vector ls;

        synchronized (this)
        {
            ls = (Vector) moveListeners.clone();
        }
        ActionEvent e = new ActionEvent(this,0,str);
        for (int i=0;i<ls.size();i++)
        {
            ActionListener l = (ActionListener) ls.elementAt(i);
            l.actionPerformed(e);
        }
    }

    public void fireLockEvent(int id, String str)
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::fireLockEvent" );
        }

        Vector ls;

        synchronized (this)
        {
            ls = (Vector) lockListeners.clone();
        }
        ActionEvent e = new ActionEvent( this, id, str );
        for (int i=0;i<ls.size();i++)
        {
            ActionListener l = (ActionListener) ls.elementAt(i);
            l.actionPerformed(e);
        }
    }

    public String getResource()
    {
        return Resource;
    }

    public String getHost()
    {
        return HostName;
    }

    public boolean replaceFile(String fileName)
    {
        String str = new String(fileName + " exists.\nReplace?\n");
        int opt = JOptionPane.showConfirmDialog(null,str,"File Exists",JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public boolean launchAnyway()
    {
        String str = new String("View in application?");
        int opt = JOptionPane.showConfirmDialog(null,str,"Launch Application?",JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public String selectApplication()
    {
        String str = new String("Select the application to show this file");
        String ret = JOptionPane.showInputDialog(null,str,"Select Application",JOptionPane.QUESTION_MESSAGE);
        return ret;
    }

    public void displayLock(String LockType, String LockScope, String LockDepth, String LockToken, String LockTimeout, String LockOwner )
    {
        Object [] options = { "OK" };
        if (LockToken == null)
            LockToken = "";
        String str = "Lock Type:  " + LockType +
                     "\nLock Scope: " + LockScope +
                     "\nLock Depth: " + LockDepth +
                     "\nLock Owner: " + LockOwner +
                     "\nLock Token: " + LockToken+
                     "\nTimeout:    " + LockTimeout+"\n";
		JOptionPane.showOptionDialog(GlobalData.getGlobalData().getMainFrame(),str, "Lock Information", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,options, options[0]);
    }


    // singleton access
    public static WebDAVRequestGenerator getGenerator()
    {
        if( generator == null )
            generator = new WebDAVRequestGenerator();
        return generator;
    }


    protected String getLockToken( Element locktoken )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getLockToken" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( locktoken );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && tag.getName().equals( WebDAVXML.ELEM_HREF ) )
            {
                Element token = (Element)treeEnum.nextElement();
                if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
                {
                    // TODO: get encoding
                    return GlobalData.getGlobalData().unescape( token.getText(), null, false );
                }
            }
        }
        return null;
    }


    protected String getLockType( Element locktype )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getLockType" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( locktype );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && !tag.getName().equals( WebDAVXML.ELEM_LOCK_TYPE ) )
                return tag.getName();
        }
        return "";
    }


    protected String getLockScope( Element lockscope )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getLockScope" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( lockscope );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && !tag.getName().equals( WebDAVXML.ELEM_LOCK_SCOPE ) )
                return tag.getName();
        }
        return "";
    }

    /**
     * Function used to retreive the owner details on a lock.
     * A small correction has been made in the code by Eric Giguere to get the
     * owner name in cases where the parsers adds empty tags in the element tree
     * (bug from the parser).
     *
     * @author : Joachim Feise, Eric Giguere
     * @param ownerinfo The XML node that is at the root of the owner information
     * @version 1.1
     */
    protected String getOwnerInfo( Element ownerinfo )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getOwnerInfo" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( ownerinfo );
        Element head = null;
        Element current = null;
        Element href = null;
        Name tag = null;

        while(treeEnum.hasMoreElements() )
        {
            current = (Element)treeEnum.nextElement();
            if (current!=null)
                tag = current.getTagName();
            else
                tag = null;

            if( (tag!=null) && (tag.getName().equals( WebDAVXML.ELEM_OWNER )) ) {
                head = current;
                continue;
            }
            // True if we did find the "owner" tag
            if (head!=null) {
                // Tag HREF found
                if( (tag!=null) && (tag.getName().equals( WebDAVXML.ELEM_HREF )) )
                {
                    href = current;
                    continue;
                }
                // No Href found but we get a PCDATA or CDATA element so return its text
                else if ( (href==null) && (current.getType()==Element.PCDATA || current.getType() == Element.CDATA) )
                    return current.getText();
                // Href element found on previous iteration so we return the content
                // of its sub-element, if any
                else if ( (href!=null) && (current.getType()==Element.PCDATA || current.getType() == Element.CDATA) )
                {
                    // TODO: get encoding
                    return GlobalData.getGlobalData().unescape( current.getText(), null, true );
                }
            }
        }
        return "";
    }


    protected String getLockTimeout( Element locktimeout )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getLockTimeout" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( locktimeout );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            if( (current != null) && (current.getType() == Element.PCDATA || current.getType() == Element.CDATA) )
                return current.getText();
        }
        return "";
    }


    protected String getLockDepth( Element lockdepth )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getLockDepth" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( lockdepth );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            if( (current != null) && (current.getType() == Element.PCDATA || current.getType() == Element.CDATA) )
                return current.getText();
        }
        return "";
    }


    protected int getStatus( Element status )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::getStatus" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( status );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( (tag != null) && tag.getName().equals( WebDAVXML.ELEM_STATUS ) )
            {
                current = (Element)treeEnum.nextElement();
                if( (current != null) && (current.getType() == Element.PCDATA || current.getType() == Element.CDATA) )
                {
                    StringTokenizer text = new StringTokenizer( current.getText() );
                    if( text.countTokens() >= 2 )
                    {
                        if( text.nextToken().equals( HTTPString ) )
                        {
                            int value = Integer.parseInt( text.nextToken() );
                            return value;
                        }
                    }
                }
            }
        }
        return 0;
    }


    protected boolean checkHrefValue( Element el )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::checkHrefValue" );
        }

        TreeEnumeration treeEnum = new TreeEnumeration( el );
        while(treeEnum.hasMoreElements() )
        {
            Element current = (Element)treeEnum.nextElement();
            Name tag = current.getTagName();
            if( tag.getName().equals( WebDAVXML.ELEM_HREF ) )
            {
                Element token = (Element)treeEnum.nextElement();
                if( token.getType() == Element.PCDATA || token.getType() == Element.CDATA )
                {
                    // TODO: get encoding
                    String HrefValue = GlobalData.getGlobalData().unescape( token.getText(), null, true );
                    // stripping https://
                    int pos = HrefValue.indexOf( GlobalData.WebDAVPrefixSSL );
                    if( pos >= 0 )
                        HrefValue = HrefValue.substring( pos+GlobalData.WebDAVPrefixSSL.length() );
                    // stripping http://
                    pos = HrefValue.indexOf( GlobalData.WebDAVPrefix );
                    if( pos >= 0 )
                        HrefValue = HrefValue.substring( pos+GlobalData.WebDAVPrefix.length() );
                    pos = HrefValue.indexOf( "/" );
                    if( pos >= 0 )
                        HrefValue = HrefValue.substring( pos );
                    if (HrefValue.length() == 0)
                        HrefValue = "/";
                    if (HrefValue.equals(Resource))
                        return true;
                }
            }
        }
        return false;
    }

    public Element skipElements( Document xml_doc, String[] token )
    {
        Element rootElem = (Element)xml_doc.getRoot();
        return skipElements( rootElem, token );
    }

    public Element skipElements( Element rootElem, String[] token )
    {
        if( GlobalData.getGlobalData().getDebugResponse() )
        {
            System.err.println( "WebDAVResponseInterpreter::skipElements" );
        }

        int index = 0;
        TreeEnumeration enumTree =  new TreeEnumeration( rootElem );
        while( enumTree.hasMoreElements() )
        {
            Element current = (Element)enumTree.nextElement();
            Name currentTag = current.getTagName();
            if( index >= 0 )
            {
                if( (currentTag != null) && (currentTag.getName().equals( token[index] )) )
                {
                    if( currentTag.getName().equals( WebDAVXML.ELEM_HREF ) )
                    {
                        if( !checkHrefValue( current ) )
                            break;
                    }
                    else
                    {
                        // we only care about the subtree from this point on
                        // NOTE: do not get the href subtree, since the href tree
                        // is a sibling to the tree we need
                        enumTree = new TreeEnumeration( current );
                    }
                    index++;
                }
                if( index >= token.length )
                    return current;
            }
        }
        return null;
    }


    public DataNode parseResponse( Element respElem, String resourceName, Vector nodesChildren )
    {
        return parseResponse( respElem, resourceName, nodesChildren, null, null, null );
    }


    public DataNode parseResponse( Element respElem, String resourceName, Vector nodesChildren, DataNode dataNode, String userAgent, DefaultMutableTreeNode treeNode )
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::parseResponse" );
        }

        DataNode node = null;
        String resName = "";
        String fullName = "";
        Element current = null;

        if( respElem.numElements() == 0 )
            return null;

        SiblingEnumeration enumTree =  new SiblingEnumeration( respElem.getChild(0) );
        while( enumTree.hasMoreElements() )
        {
            current = (Element)enumTree.nextElement();
            Name currentTag = current.getTagName();
            if( currentTag != null )
            {
                if( currentTag.getName().equals( WebDAVXML.ELEM_HREF ) )
                {
                    TreeEnumeration enumHref =  new TreeEnumeration( current );
                    while( enumHref.hasMoreElements() )
                    {
                        Element token = (Element)enumHref.nextElement();
                        if( (token != null) && (token.getType() == Element.PCDATA || token.getType() == Element.CDATA) )
                        {
                            // workaround for broken MS Parser if filename has & in it
                            // not needed anymore with patch in the parser
                            // but left in here as a reminder just in case...
                            //if( resName.length() > 0 )
                            //    resName += "&";
                            //resName += new String(truncateResource(token.getText()));
                            //if( fullName.length() > 0 )
                            //    fullName += "&";
                            //fullName += new String(getFullResource(token.getText()));

                            // TODO: get encoding
                            resName = new String( truncateResource(GlobalData.getGlobalData().unescape(token.getText(), null, true)) );
                            fullName = new String( getFullResource(GlobalData.getGlobalData().unescape(token.getText(), null, true)) );
                        }
                    }
                }
                else if( currentTag.getName().equals( WebDAVXML.ELEM_PROPSTAT ) )
                {
                    if( resName != "" )
                    {
                        DataNode curnode = parseProps( current, resourceName, resName );
                        if( node == null )
                            node = curnode;
                        else
                        {
                            // update node values as necessary
                            if( curnode.getDisplay().length() != 0 )
                                node.setDisplay( curnode.getDisplay() );    // overwrite any old value
                            if( curnode.isLocked() && !node.isLocked() )
                            {
                                node.lock( curnode.getLockToken() );        // never change back to unlocked here
                            }
                            if( curnode.isCollection() )
                                node.makeCollection();                      // never change back to normal node
                            if( curnode.getType().length() != 0 )
                                node.setType( curnode.getType() );          // overwrite any old value
                            if( curnode.getSize()!=0 )
                                node.setSize( curnode.getSize() );          // overwrite any old value
                            if( curnode.getDate() != null )
                                node.setDate( curnode.getDate() );          // overwrite any old value
                        }
                    }
                }
            }
        }

        // save data node
        if( node != null )
        {
            String resourceNameStrp = "";
            if( resourceName.endsWith("/") )
                resourceNameStrp = resourceName.substring( 0, resourceName.length() - 1 );
            if ( (fullName.equals(resourceName)) || (fullName.equals(resourceNameStrp)) )
            {
                // this is the container
                String hostName = resName;
                if( treeNode != null )
                {
                    int pathLen = treeNode.getPath().length;
                    if (pathLen == 2)
                    {
                        if( GlobalData.getGlobalData().getSSL() )
                            hostName = GlobalData.WebDAVPrefixSSL + getHost() + "/" + resourceName;
                        else
                            hostName = GlobalData.WebDAVPrefix + getHost() + "/" + resourceName;
                    }
                }
                // update node values
                dataNode = new DataNode( node.isCollection(), node.isLocked(), node.getLockToken(),
                                         hostName, node.getDisplay(), node.getType(), node.getSize(),
                                         node.getDate(), null );
            }
            else
            {
                if( node.isCollection() )
                {
                    if( treeNode != null )
                    {
                        WebDAVTreeNode childNode = new WebDAVTreeNode( resName, userAgent );
                        childNode.setDataNode(node);
                        treeNode.insert(childNode,0);
                    }
                }
                else
                {
                    nodesChildren.addElement(node);
                }
            }
        }

        // handle the case when the server doesn't send properties for the container
        // itself
        if( dataNode == null && treeNode != null )
        {
            // create a container with as much data as we have
            int pathLen = treeNode.getPath().length;
            String hostName = resName;
            if (pathLen == 2)
            {
                if( GlobalData.getGlobalData().getSSL() )
                    hostName = GlobalData.WebDAVPrefixSSL + getHost() + "/" + resourceName;
                else
                    hostName = GlobalData.WebDAVPrefix + getHost() + "/" + resourceName;
            }
            // update node values
            dataNode = new DataNode( true, false, null,
                                     hostName, resourceName, "httpd/unix-directory", 0,
                                     "", null );
        }

        return dataNode;
    }


    private String truncateResource(String res)
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::truncateResource" );
        }

        int pos = res.indexOf(GlobalData.WebDAVPrefixSSL);
        if (pos >= 0)
            res = res.substring(GlobalData.WebDAVPrefixSSL.length());
        pos = res.indexOf(GlobalData.WebDAVPrefix);
        if (pos >= 0)
            res = res.substring(GlobalData.WebDAVPrefix.length());
        pos = res.indexOf("/");
        if( pos >= 0 )
            res = res.substring(pos);

        if (res.endsWith("/"))
            res = res.substring(0, res.length() - 1);
        pos = res.lastIndexOf("/");
        if (pos >= 0)
            res = res.substring(pos);
        if ((res.startsWith("/")) && (res.length() > 1))
            res = res.substring(1);
        if (res.length() == 0)
            res = "/";
        return res;
    }

    private String getFullResource(String res)
    {
        if( GlobalData.getGlobalData().getDebugTreeNode() )
        {
            System.err.println( "WebDAVTreeNode::getFullResource" );
        }

        int pos = res.indexOf(GlobalData.WebDAVPrefixSSL);
        if (pos >= 0)
            res = res.substring(GlobalData.WebDAVPrefixSSL.length());
        pos = res.indexOf(GlobalData.WebDAVPrefix);
        if (pos >= 0)
            res = res.substring(GlobalData.WebDAVPrefix.length());
        pos = res.indexOf("/");
        if( pos >= 0 )
            res = res.substring(pos);
        if (res.endsWith("/"))
            res = res.substring(0,res.length() - 1);
        if (res.length() == 0)
            res = "/";
        if ( (res.startsWith("/")) && (res.length() > 1) )
            res = res.substring(1);
        return res;
    }


    protected void printXML( byte[] body )
    {
        String debugOutput = System.getProperty( "debug", "false" );
        if( debugOutput.equals( "true" ) || debugXML )
        {
            System.out.println("Received xml:");
            XMLOutputStream out = new XMLOutputStream(System.out);
            ByteArrayInputStream tmpIn = new ByteArrayInputStream(body);
            Document tmpDoc = new Document();
            try
            {
                tmpDoc.load(tmpIn);
                tmpDoc.save(out);
            }
            catch (Exception e)
            {
            }
        }
    }

    public static void reset()
    {
        generator = null;
        stream = null;
        Method = null;
        Extra = null;
        res = null;
        HostName = null;
        Port = 0;
        Resource = null;
        listeners = new Vector();
        moveListeners = new Vector();
        lockListeners = new Vector();
        WebDAVEditDir = null;
        refresh = false;
        userPathDir = null;
        copyListener = null;
        putListener = null;
        actionListener = null;
    }


    protected static WebDAVRequestGenerator generator;
    protected static byte[] stream = null;
    protected static String Method;
    protected static String Extra;
    protected static HTTPResponse res;
    protected static String HostName;
    protected static int Port;
    protected static String Resource;
    protected static Vector listeners = new Vector();
    protected static Vector moveListeners = new Vector();
    protected static Vector lockListeners = new Vector();
    protected static String WebDAVEditDir = null;
    protected static boolean refresh = false;
    protected static String userPathDir;
    protected static CopyResponseListener copyListener;
    protected static PutListener putListener;
    protected static ActionListener actionListener;

    protected WebDAVTreeNode Node;
    protected boolean debugXML = false;

    protected final static String EditDir = "Edit";
    protected final static String WebDAVClassName = "DAVExplorer";
    protected final static String HTTPString = "HTTP/1.1";
}
