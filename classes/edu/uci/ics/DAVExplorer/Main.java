/*
 * Copyright (c) 1998-2001 Regents of the University of California.
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
 * Title:       Main
 * Description: This is the class containing the main() function, which merely
 *              instantiates the Main JFrame.
 *              The Main class creates the user interface and adds the appropriate
 *              listeners.
 * Copyright:   Copyright (c) 1998-2001 Regents of the University of California. All rights reserved.
 * @author      Robert Emmery (dav-exp@ics.uci.edu)
 * @date        2 April 1998
 * @author      Yuzo Kanomata, Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 1999
 * Changes:     Changed treeView.fireSelectionEvent(); to treeView.initTree();
 *              This is the same function, but with a better name.
 *              Added Create Folder functionality.
 *              Added filename selection for export file
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        3 December 1999
 * Changes:     Removed the authentication dialog and listener, since authentication
 *              is now handled as AuthenticationHandler in HTTPClient
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        20 June 2000
 * Changes:     Better reporting in case the connection is closed
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        12 January 2001
 * Changes:     Added support for https (SSL), moved properties loading to GlobalData
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        2 August 2001
 * Changes:     Using HTTPClient authentication module
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        29 September 2001
 * Changes:     Now sending Options request at initial connection to check for
 *              DAV support on the server. Only then the Propfind is sent.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        1 October 2001
 * Changes:     Change of package name
 */


package edu.uci.ics.DAVExplorer;

import HTTPClient.AuthorizationInfo;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.CookieModule;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.event.EventListenerList;
import javax.swing.text.Keymap;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;
import java.io.File;

public class Main extends JFrame
{
    public final static String VERSION = "0.80-dev";
    public final static String UserAgent = "UCI DAV Explorer/" + VERSION;

    public Main(String frameName)
    {
        super (frameName);
//        Uncomment the following 8 lines if you want system's L&F
//        try
//        {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        }
//        catch (Exception except)
//        {
//            System.out.println("Error Loading L&F");
//        }

        GlobalData.getGlobalData().setMainFrame( this );

        authTable = new Hashtable();
        //authHost = null;

        treeView = new WebDAVTreeView();
        treeView.setUserAgent( UserAgent );
        fileView = new WebDAVFileView();

        CommandMenu = new WebDAVMenu();
        setJMenuBar(CommandMenu);

        MenuListener_Gen menuListener = new MenuListener_Gen();

        CommandMenu.addWebDAVMenuListener( menuListener );

        // Set the HTTPClient authentication handler
        ((DefaultAuthHandler)AuthorizationInfo.getAuthHandler()).setAuthorizationPrompter(new AuthDialog());

        // allow all cookies
        CookieModule.setCookiePolicyHandler( null );

        WebDAVToolBar toolbar = new WebDAVToolBar();
        toolbar.addActionListener( menuListener );
        URIBox uribox = new URIBox();
        uribox.addActionListener(new URIBoxListener_Gen());

        treeView.addViewSelectionListener( fileView );
        fileView.addViewSelectionListener( treeView );

        requestGenerator = new WebDAVRequestGenerator();
        requestGenerator.addRequestListener(new RequestListener());

        // Get the rename Event
        fileView.addRenameListener(new RenameListener());

        requestGenerator.setUserAgent( UserAgent );


        responseInterpreter = new WebDAVResponseInterpreter( requestGenerator );
        responseInterpreter.addInsertionListener(new InsertionListener());
        responseInterpreter.addMoveUpdateListener(new MoveUpdateListener());
        responseInterpreter.addLockListener(new LockListener());
        responseInterpreter.addActionListener(fileView); // Listens for a reset
                                                        // for a unsucessful
                                                        // Rename request

        // Add the CopyEvent Listener
        responseInterpreter.addCopyResponseListener(treeView);
        responseInterpreter.addPutListener(treeView);

        webdavManager = new WebDAVManager();
        webdavManager.addResponseListener(new ResponseListener());

        JScrollPane fileScrPane = fileView.getScrollPane();
        JScrollPane treeScrPane = treeView.getScrollPane();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treeScrPane,fileScrPane);
        splitPane.setContinuousLayout(true);

        JPanel p = new JPanel();
        p.setSize(800,600);
        GridBagLayout gridbag = new GridBagLayout();
        p.setLayout(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(toolbar,c);
        p.add(toolbar);

        c.gridy= GridBagConstraints.RELATIVE;
        c.gridheight = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(uribox,c);
        p.add(uribox);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(splitPane,c);
        p.add(splitPane);

        getContentPane().add(p);
        treeView.initTree();
        pack();
        setVisible(true);

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we_Event)
            {
                System.exit(0);
            }
        } );
    }


    public final static void main(String[] argv)
    {
        Main mFrame = new Main("DAV Explorer");
    }


    class URIBoxListener_Gen implements WebDAVURIBoxListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String str = e.getActionCommand();
            if (!str.endsWith("/"))
                str += "/";
            requestGenerator.setExtraInfo("uribox");
            if( requestGenerator.GenerateOptions( str ) )
            {
                requestGenerator.execute();
            }
        }
    }

    class InsertionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String str = e.getActionCommand();
            if (str == null)
            {
                treeView.refresh();
            } else{
                treeView.addRowToRoot(str,false);

        }
        }
    }

    class MoveUpdateListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String str = e.getActionCommand();
            if (str == null)
                fileView.update();
            else
            {
                fileView.resetName();
            }
        }
    }

    class TableSelectListener_Gen implements ViewSelectionListener
    {
        public void selectionChanged(ViewSelectionEvent e)
        {
            requestGenerator.tableSelectionChanged(e);
        }
    }

    class TableSelectListener_Tree implements ViewSelectionListener
    {
        public void selectionChanged(ViewSelectionEvent e)
        {
            treeView.tableSelectionChanged(e);
        }
    }

    class TreeSelectListener_Gen implements ViewSelectionListener
    {
        public void selectionChanged(ViewSelectionEvent e)
        {
            requestGenerator.treeSelectionChanged(e);
        }
    }

    class LockListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String token = e.getActionCommand();
            if ( e.getID() == 0 )
            {
                fileView.setLock();
                treeView.setLock( fileView.getName(), token );
            }
            else
            {
                fileView.resetLock();
                treeView.resetLock( fileView.getName() );
            }
        }
    }

    class DisplayLockListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            requestGenerator.DiscoverLock("display");
        }
    }

    class RenameListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String str = e.getActionCommand();
            if (str != null)
            {
                String s = fileView.getOldSelectedResource();
                WebDAVTreeNode n = fileView.getParentNode();
                requestGenerator.setResource(s, n);

                boolean retval = false;
                if( fileView.isSelectedLocked() )
                {
                    retval = requestGenerator.GenerateMove(str, fileView.getParentPath(), false, true, fileView.getSelectedLockToken(), "rename:" );
                }
                else
                {
                    retval = requestGenerator.GenerateMove(str, fileView.getParentPath(), false, true, null , "rename:" );
                }
                if( retval )
                {
                    requestGenerator.execute();
                }
            }
        }
    }

    class TreeSelectListener_Table implements ViewSelectionListener
    {
        public void selectionChanged(ViewSelectionEvent e)
        {
            fileView.treeSelectionChanged(e);
        }
    }

    class RequestListener implements WebDAVRequestListener
    {
        public void requestFormed(WebDAVRequestEvent e)
        {
            webdavManager.sendRequest(e);
        }
    }

    class ResponseListener implements WebDAVResponseListener
    {
        public void responseFormed(WebDAVResponseEvent e)
        {
            // This call process the info from the server
            try
            {
                responseInterpreter.handleResponse(e);
            }
            catch( ResponseException ex )
            {
                GlobalData.getGlobalData().errorMsg( "HTTP error or Server timeout,\nplease retry the last operation" );
                fireWebDAVCompletion( responseInterpreter, false );
                return;
            }
            fireWebDAVCompletion( responseInterpreter, true );

            // Post processing
            // These are actions designed to take place after the
            // response has been loaded
            String extra = e.getExtraInfo();
            String method = e.getMethodName();

            if ( method.equals("COPY") )
            {
                // Skip
            }
            else if (method.equals("PUT"))
            {
                // Skip
            }
            else if (extra == null)
            {
                // Skip
            }
            else if( extra.equals("expand") || extra.equals("index") )
            {
                WebDAVTreeNode tn = e.getNode();
                if (tn != null)
                {
                    tn.finishLoadChildren();

                }
            }
            else if ( extra.equals("select") )
            {
                WebDAVTreeNode tn = e.getNode();
                if (tn != null)
                {
                    tn.finishLoadChildren();


                    treeView.setSelectedNode(tn);
                }

            }
            else if ( extra.equals("uribox") )
            {
                WebDAVTreeNode tn = e.getNode();
            }
            else if ( extra.equals("copy") )
            {
            }
            else if (extra.equals("delete"))
            {
            }
            else if (extra.equals("mkcol"))
            {
            }
        }
    }

    class MenuListener_Gen implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String command = e.getActionCommand();
            if (command.equals("Exit"))
                System.exit(0);

            else if (command.equals("Get File"))
            {
                saveAsDocument();
            }
            else if (command.equals("Write File"))
            {
                FileDialog fd = new FileDialog(GlobalData.getGlobalData().getMainFrame(), "Write File" , FileDialog.LOAD);

                if (writeToDir != null)
                {
                    fd.setDirectory(writeToDir);
                }

                fd.setVisible(true);

                String dirName =fd.getDirectory();

                String fName = fd.getFile();

                if( (dirName!=null) && !dirName.equals("") && (fName != null ) && !fName.equals("") )
                {
                    writeToDir = dirName;
                    String fullPath = dirName + fName;
                    String token = treeView.getLockToken( fName );

                    // Get the current Node so that we can update it later
                    String s = "";

                    WebDAVTreeNode n2 = fileView.getSelectedCollection();

                    s = fileView.getSelected();
                    if (s == null)
                    {
                        s = "";
                    }
                    WebDAVTreeNode parent = fileView.getParentNode();

                    boolean retval = false;
                    if (n2 == null)
                    {
                        requestGenerator.setResource(s, parent);
                        retval = requestGenerator.GeneratePut(fullPath, s, token , null);
                    }
                    else
                    {
                        requestGenerator.setResource(s, n2);
                        retval = requestGenerator.GeneratePut( fullPath, s, token , parent);
                    }
                    if( retval ){
                        requestGenerator.execute();
            }
                }
            }
            else if (command.equals("Lock"))
            {
                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();
                    //WebDAVTreeNode n2 = fileView.getSelectedCollection();
                    requestGenerator.setResource(s, n);
                    lockDocument();
                }
            }
            else if (command.equals("Unlock"))
            {
                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();
                    requestGenerator.setResource(s, n);
                    unlockDocument();
                }
            }
            else if (command.equals("Copy"))
            {
                // Yuzo: I have the semantics set so that
                // we get a string if something is selected in the
                // FileView.
                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();

                    // This sets the resource name to s and the node to n
                    // This is neccessary so that n is passed to
                    // response interpretor, whc then routes a
                    // message to the Tree Model.
                    // This will then call for a rebuild of the Model at the
                    // Parent node n.
                    requestGenerator.setResource(s, n);

                    String prompt = "Enter the name of the copy:";
                    String title = "Copy Resource";
                    String defaultName = requestGenerator.getDefaultName( "_copy" );
                    //String overwrite = "Overwrite existing resource?";
                    String fname = selectName( title, prompt, defaultName );
                    if( fname != null )
                    {
                        if( requestGenerator.GenerateCopy( fname, true, true ) )
                        {
                            requestGenerator.execute();
                        }
                    }
                }
            }
            else if (command.equals("Move"))
            {
                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();

                    // This sets the resource name to s and the node to n
                    // This is neccessary so that n is passed to
                    // response interpretor, whc then routes a
                    // message to the Tree Model.
                    // This will then call for a rebuild of the Model at the
                    // Parent node n.
                    requestGenerator.setResource(s, n);

                    String prompt = "Enter the new name of the resource:";
                    String title = "Move Resource";
                    //String overwrite = "Overwrite existing resource?";
                    String fname = selectName( title, prompt );
                    if( fname != null )
                    {
                        boolean retval = false;
                        if( fileView.isSelectedLocked() )
                            retval = requestGenerator.GenerateMove( fname, null, false, true, fileView.getSelectedLockToken(), "rename:" );
                        else
                            retval = requestGenerator.GenerateMove( fname, null, false, true, null , "rename:" );

                        if( retval )
                            requestGenerator.execute();
                    }
                }
            }
            else if (command.equals("Delete"))
            {

                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    //deleteDocument( treeView.isCollection( s ) );
                    WebDAVTreeNode n = fileView.getSelectedCollection();
                    if ( n == null)
                    {
                        deleteDocument( false );
                    }
                    else
                    {
                        deleteDocument(  true );
                    }
                }
            }
            else if (command.equals("Create Collection"))
            {
                WebDAVTreeNode n = fileView.getParentNode();
                String prompt = new String( "Enter collection name:" );
                String title = new String( "Create Collection" );
                String dirname = selectName( title, prompt );

                if( dirname != null )
                {
                    WebDAVTreeNode selected = fileView.getSelectedCollection();
                    if( treeView.isRemote( fileView.getParentPath() ) )
                    {
                        boolean retval = false;
                        if (selected == null)
                        {
                            requestGenerator.setNode(n);
                            requestGenerator.setExtraInfo("mkcol");
                            retval = requestGenerator.GenerateMkCol( fileView.getParentPath(), dirname );
                        }
                        else
                        {
                            requestGenerator.setNode( selected );
                            requestGenerator.setExtraInfo("mkcolbelow");
                            retval = requestGenerator.GenerateMkCol( fileView.getSelected(), dirname );
                        }
                        if( retval ){
                            requestGenerator.execute();
            }
                    }
                    else
                    {
                        if( !treeView.getCurrentPath().endsWith( String.valueOf(File.separatorChar) ) )
                            dirname = fileView.getSelected() + File.separatorChar + dirname;
                        else
                            dirname = fileView.getSelected() + dirname;
                        File f = new File( dirname );
                        boolean result = f.mkdir();
                        if ( selected == null )
                        {
                                treeView.refreshLocal( n );
                        }
                        else
                        {
                            treeView.refreshLocalNoSelection(selected);
                        }
                    }
                }
            }
            else if (command.equals("Clear Auth Buffer"))
            {
                authTable.clear();
            }
            else if (command.equals("Edit Proxy Info"))
            {
                WebDAVProxyInfo proxyInfo = new WebDAVProxyInfo(GlobalData.getGlobalData().getMainFrame(), "Proxy Info", true);
            }
            else if (command.equals("Edit Lock Info"))
            {
                WebDAVLockInfo lockInfo = new WebDAVLockInfo(GlobalData.getGlobalData().getMainFrame(), "Lock Info", true);
            }
            else if (command.equals("HTTP Logging"))
            {
                boolean logging = false;
                String logFilename = null;
                if( CommandMenu.getLogging() )
                {
                    JOptionPane pane = new JOptionPane();
                    String message = new String( "WARNING: The logfile may get very large,\nsince all data is logged.\n" +
                                                 "Hit Cancel now if you don't want to log the data." );
                    int opt = pane.showConfirmDialog( GlobalData.getGlobalData().getMainFrame(), message, "HTTP Logging", JOptionPane.OK_CANCEL_OPTION );
                    if( opt == JOptionPane.OK_OPTION )
                    {
                        JFileChooser fd = new JFileChooser();
                        fd.setDialogType( JFileChooser.SAVE_DIALOG );
                        fd.setFileSelectionMode( JFileChooser.FILES_ONLY );
                        fd.setDialogTitle( "Select Logging File" );
                        String os = (System.getProperty( "os.name" )).toLowerCase();
                        String dirName = null;
                        if( os.indexOf( "windows" ) == -1 )
                            dirName = System.getProperty("user.home");
                        if( dirName == null )
                            dirName = new Character(File.separatorChar).toString();
                        fd.setCurrentDirectory( new File(dirName) );
                        fd.setApproveButtonMnemonic( 'U' );
                        fd.setApproveButtonToolTipText( "Use the selected file for logging" );
                        int val = fd.showDialog( GlobalData.getGlobalData().getMainFrame(), "Use Selected" );
                        if( val == JFileChooser.APPROVE_OPTION)
                        {
                            logFilename = fd.getSelectedFile().getAbsolutePath();
                            try
                            {
                                File f = fd.getSelectedFile();
                                if( f.exists() )
                                    f.delete();
                                logging = true;
                            }
                            catch( Exception exception )
                            {
                                System.out.println( "File could not be deleted.\n" + exception );
                                logFilename = null;
                            }
                        }
                    }
                }

                CommandMenu.setLogging( logging );
                webdavManager.setLogging( logging, logFilename );
            }
            else if (command.equals("View Properties"))
            {
                viewProperties();
            }
            else if (command.equals("View Lock Properties"))
            {
                // Yuzo test to stop repeat
                String s = fileView.getSelected();
                if( s == null )
                {
                    GlobalData.getGlobalData().errorMsg( "No file selected." );
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();
                    requestGenerator.setResource(s, n);

                    requestGenerator.DiscoverLock("display");
                }
            }
            else if (command.equals("Refresh"))
            {
                WebDAVTreeNode n = fileView.getParentNode();
                responseInterpreter.setRefresh( n );
            }
            else if (command.equals("About DAV Explorer..."))
            {
                JOptionPane pane = new JOptionPane( this );
                String message = new String("DAV Explorer Version "+ VERSION + "\n" +
                "Copyright (c) 1999-2001 Regents of the University of California\n" +
                "Authors: Yuzo Kanomata, Joachim Feise\n" +
                "EMail: dav-exp@ics.uci.edu\n\n" +
                "Based on code from the UCI WebDAV Client Group\n" +
                "of the ICS126B class Winter 1998:\n" +
                "Gerair Balian, Mirza Baig, Robert Emmery, Thai Le, Tu Le.\n" +
                "Uses the HTTPClient library (http://www.innovation.ch/java/HTTPClient/).\n" +
                "Uses Microsoft's published XML parser code from June 1997.\n");
                Object [] options = { "OK" };
                pane.showOptionDialog(GlobalData.getGlobalData().getMainFrame(), message, "About DAV Explorer", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            }
        }
    }

    public void addWebDAVCompletionListener( WebDAVCompletionListener l )
    {
        listenerList.add( WebDAVCompletionListener.class, l );
    }

    public void removeWebDAVCompletionListener( WebDAVCompletionListener l )
    {
        listenerList.remove(WebDAVCompletionListener.class, l );
    }

    protected void fireWebDAVCompletion( Object source, boolean success )
    {
        Object[] listeners = listenerList.getListenerList();
        WebDAVCompletionEvent e = null;
        for (int i = listeners.length-2; i>=0; i-=2)
        {
            if (listeners[i]==WebDAVCompletionListener.class)
            {
                if (e == null)
                    e = new WebDAVCompletionEvent( source, success );
                ((WebDAVCompletionListener)listeners[i+1]).completion(e);
            }
        }
    }

    protected void viewDocument()
    {
        if( requestGenerator.GenerateGet("view") ){
            requestGenerator.execute();
    }
    }

    protected void saveAsDocument()
    {
        String s = fileView.getSelected();
        if( s == null )
        {
            GlobalData.getGlobalData().errorMsg( "No file selected." );
        }
        else
        {
            WebDAVTreeNode n = fileView.getParentNode();
            requestGenerator.setResource(s, n);
            if( requestGenerator.GenerateGet("saveas") ){
                requestGenerator.execute();
        }
        }
    }

    protected void deleteDocument( boolean collection )
    {
        String s = fileView.getSelected();
        if( !fileView.hasSelected() )
        {
            GlobalData.getGlobalData().errorMsg( "No file selected." );
        }
        else
        {
            JOptionPane pane = new JOptionPane();
            String str = null;
            String title = null;
            if( collection )
            {
                title = "Delete Collection";
                str = "Delete the collection and all its contents:\nAre you sure?";
            }
            else
            {
                title = "Delete File";
                str = "Delete: Are you sure?";
            }
            int opt = pane.showConfirmDialog( GlobalData.getGlobalData().getMainFrame(), str, title, JOptionPane.YES_NO_OPTION );
            if (opt == JOptionPane.YES_OPTION)
            {
                if( treeView.isRemote( s ) )
                {
                    WebDAVTreeNode n = fileView.getParentNode();
                    requestGenerator.setResource(s, n);
                    //requestGenerator.DiscoverLock("delete");
                    requestGenerator.setExtraInfo("delete");
                    boolean retval = false;
            if( fileView.isSelectedLocked() ){
            retval = requestGenerator.GenerateDelete(fileView.getSelectedLockToken());
            } else {
                        retval = requestGenerator.GenerateDelete(null);
                    }
                    if( retval ){
                        requestGenerator.execute();
            }
                }
                else
                {
                    WebDAVTreeNode n = fileView.getParentNode();
                    File f = new File( s );
                    if( !deleteLocal( f ) )
                        GlobalData.getGlobalData().errorMsg( "Delete Error on local filesystem." );
                    treeView.refreshLocal( n );
                }
            }
        }
    }

    protected void lockDocument()
    {
        requestGenerator.DiscoverLock("lock");
    }

    protected void unlockDocument()
    {
        requestGenerator.DiscoverLock("unlock");
    }

    protected void viewProperties()
    {
        String s = fileView.getSelected();
        if( s == null )
        {
            GlobalData.getGlobalData().errorMsg( "No file selected." );
        }
        else
        {
            requestGenerator.setResource(s, null);
            requestGenerator.setExtraInfo("properties");
            if( requestGenerator.GeneratePropFind(null,"allprop","zero",null,null,false) ){
                requestGenerator.execute();
        }
        }
    }


    private String selectName( String title, String prompt )
    {
        return selectName( title, prompt, null );
    }

    private String selectName( String title, String prompt, String defaultValue )
    {
        JOptionPane pane = new JOptionPane();
        String ret = (String)pane.showInputDialog( GlobalData.getGlobalData().getMainFrame(), prompt, title, JOptionPane.QUESTION_MESSAGE, null, null, defaultValue );
        return ret;
    }

    private boolean deleteLocal( File f )
    {
        try
        {
            if( f.isDirectory() )
            {
                String[] flist = f.list();
                for( int i=0; i<flist.length; i++ )
                {
                    if( !deleteLocal( new File(flist[i]) ) )
                        return false;
                }
                return f.delete();
            }
            else
                return f.delete();
        }
        catch( Exception e )
        {
        }
        return false;
    }

    protected WebDAVFileView fileView;
    protected WebDAVTreeView treeView;
    protected WebDAVRequestGenerator requestGenerator;
    protected WebDAVResponseInterpreter responseInterpreter;
    protected WebDAVManager webdavManager;
    protected WebDAVMenu CommandMenu;
    protected Hashtable authTable;
    protected String authHost;
    protected String writeToDir;
    protected EventListenerList listenerList = new EventListenerList();
}
