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

// Changelog
//
// Date: 2001-Jan-9
// Joe Feise: Added support for https (SSL), moved reading of debug
// properties here

package DAVExplorer;

import java.awt.Cursor;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import java.util.*;
import java.io.*;

/**
 * This singleton class defines various global data structures
 * and functions useful everywhere
 *
 * @version 0.3  9 January 2001
 * @author  Joachim Feise
 * @since   V0.1
 */

class GlobalData
{
    /** Debug variables */
    boolean debugAll      = false;
    boolean debugRequest  = debugAll | false;
    boolean debugResponse = debugAll | false;
    boolean debugTreeView = debugAll | false;
    boolean debugTreeNode = debugAll | false;
    boolean debugFileView = debugAll | false;

    /** SSL variable */
    boolean ssl = false;

    JFrame mainFrame = null;
    Cursor origCursor = null;
    private static final String fileName = "DAVExplorer.dat";
    private static final String tmpFileName = "DAVExplorer.tmp";

    private static GlobalData globalData = null;

    protected GlobalData()
    {
        init( true );
    }

    static GlobalData getGlobalData()
    {
        if( globalData == null )
            globalData = new GlobalData();
        return globalData;
    }

    public boolean getDebugAll()
    {
        return debugAll;
    }

    public void setDebugAll( boolean debug )
    {
        debugAll = debug;
        init( false );
    }

    public boolean getDebugRequest()
    {
        return debugRequest;
    }

    public void setDebugRequest( boolean debug )
    {
        debugRequest = debug;
        init( false );
    }

    public boolean getDebugResponse()
    {
        return debugResponse;
    }

    public void setDebugResponse( boolean debug )
    {
        debugResponse = debug;
        init( false );
    }

    public boolean getDebugTreeView()
    {
        return debugTreeView;
    }

    public void setDebugTreeView( boolean debug )
    {
        debugTreeView = debug;
        init( false );
    }

    public boolean getDebugTreeNode()
    {
        return debugTreeNode;
    }

    public void setDebugTreeNode( boolean debug )
    {
        debugTreeNode = debug;
        init( false );
    }

    public boolean getDebugFileView()
    {
        return debugFileView;
    }

    public void setDebugFileView( boolean debug )
    {
        debugFileView = debug;
        init( false );
    }

    public JFrame getMainFrame()
    {
        return mainFrame;
    }

    public void setMainFrame( JFrame frame )
    {
        mainFrame = frame;
        if( mainFrame != null )
            origCursor = mainFrame.getCursor(); // save original cursor
    }

    public void errorMsg(String str)
    {
        JOptionPane pane = new JOptionPane();
        Object[] options = { "OK" };
        pane.showOptionDialog(mainFrame,str,"Error Message", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    }

    public void setCursor( Cursor c )
    {
        if( mainFrame != null )
            mainFrame.setCursor( c );
    }

    public void resetCursor()
    {
        if( mainFrame != null && origCursor != null )
            mainFrame.setCursor( origCursor );
    }

    public boolean doSSL()
    {
        return ssl;
    }


    public String ReadConfigEntry( String token )
    {
        String info = "";
        String userPath = System.getProperty( "user.home" );
        if (userPath == null)
            userPath = "";
        else
            userPath += File.separatorChar;
        String filePath = null;
        File theFile = new File(userPath + fileName);
        if (theFile.exists())
            filePath = userPath + fileName;
        if (filePath != null)
        {
            try
            {
                FileInputStream fin = new FileInputStream(filePath);
                BufferedReader in = new BufferedReader(new InputStreamReader(fin));
                boolean found = false;
                do
                {
                    String line = in.readLine();
                    StringTokenizer filetokens = new StringTokenizer( line, "= \t" );
                    if( (filetokens.nextToken()).equals(token) )
                    {
                        info = filetokens.nextToken();
                        found = true;
                    }
                }
                while( !found );
                in.close();
            }
            catch (Exception fileEx)
            {
            }
        }
        return info;
    }


    public void WriteConfigEntry( String token, String data )
    {
        String userPath = System.getProperty( "user.home" );
        if (userPath == null)
            userPath = "";
        else
            userPath += File.separatorChar;
        String filePath = userPath + fileName;
        String tmpFilePath = userPath + tmpFileName;
        try
        {
            FileOutputStream fout = new FileOutputStream( tmpFilePath );
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fout));
            File theFile = new File(filePath);
            if ( theFile.exists() )
            {
                FileInputStream fin = new FileInputStream(filePath);
                BufferedReader in = new BufferedReader(new InputStreamReader(fin));
                String line = null;
                do
                {
                    line = in.readLine();
                    if( line != null )
                    {
                        StringTokenizer filetokens = new StringTokenizer( line, "= \t" );
                        if( !(filetokens.nextToken()).equals(token) )
                        {
                            out.write( line );
                            out.newLine();
                        }
                    }
                }
                while( line != null );
                in.close();
            }
            out.write( token );
            out.write( "=" );
            out.write( data );
            out.newLine();
            out.close();

            if( theFile.exists() )
                theFile.delete();
            File theNewFile = new File( tmpFilePath );
            theNewFile.renameTo( theFile );
        }
        catch (Exception fileEx)
        {
            System.out.println( fileEx.toString() );
        }
    }


    private void init( boolean readFromProperties )
    {
        if( readFromProperties )
        {
            String doLog = System.getProperty( "debug" );
            if( doLog != null )
            {
                if( doLog.equalsIgnoreCase("all") )
                    setDebugAll( true );
                else if( doLog.equalsIgnoreCase( "request" ) )
                    setDebugRequest( true );
                else if( doLog.equalsIgnoreCase( "response" ) )
                    setDebugResponse( true );
                else if( doLog.equalsIgnoreCase( "treeview" ) )
                    setDebugTreeView( true );
                else if( doLog.equalsIgnoreCase( "treenode" ) )
                    setDebugTreeNode( true );
                else if( doLog.equalsIgnoreCase( "fileview" ) )
                    setDebugFileView( true );
            }
            String doSSL = System.getProperty( "ssl", "false" );
            if( doSSL.equalsIgnoreCase( "true" ) )
                ssl = true;
        }

        debugRequest |= debugAll;
        debugResponse |= debugAll;
        debugTreeView |= debugAll;
        debugTreeNode |= debugAll;
        debugFileView |= debugAll;

    }
}

