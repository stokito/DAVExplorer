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
 * Title:       WebDAV Treeview
 * Description: This class is part of the GUI module for the WebDAV
 *              Client. It provides the user with a Windows Explorer
 *              like interface.
 * Copyright:   Copyright (c) 1998-2003 Regents of the University of California. All rights reserved.
 * @author      Robert Emmery
 * @date        2 April 1998
 * @author      Yuzo Kanomata, Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 1999
 * Changes:     method fireSelectionEvent() has been renames to initTree()
 *              This more acurately describes its purpose and function.
 *              The MouseListener section of code which includes the methods
 *              handleSingleClick and handleDoubleClick have been commented out.
 *              This code is redundent to the functionality of tree selection,
 *              and caused a side effect tree selection event to be caused
 *              when the X,Y portion of the cursor was repositioned on a
 *              repaint by the display.
 *              In class SelectionChangeListener, in method
 *              valueChanged(TreeSelectionEvent e), the Cursor is changed
 *              to the WAIT_CURSOR while the the Event of a Tree Selection
 *              are being processed by the various Listeners.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        12 January 2001
 * Changes:     Added support for https (SSL)
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        1 October 2001
 * Changes:     Change of package name
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        2 April 2002
 * Changes:     Updated for JDK 1.4
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 2003
 * Changes:     Integrated Brian Johnson's applet changes.
 *              Added better error reporting.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        23 September 2003
 * Changes:     Integrated the DeltaV code from the Spring 2003 ICS125 team.
 */

package edu.uci.ics.DAVExplorer;

import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.Dimension;
import java.awt.Cursor;
import java.io.File;
import java.util.Vector;
import java.util.Enumeration;

// Yuzo: This should be a Model
public class WebDAVTreeView implements ViewSelectionListener, CopyResponseListener, PutListener
{
    JTree tree;
    final static String WebDAVRoot = "DAV Explorer";

    DefaultMutableTreeNode root = new WebDAVTreeNode( WebDAVRoot, true, "" );
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    DefaultMutableTreeNode currNode = root;
    Vector rootElements = new Vector();
    TreePath currPath;
    TreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    Vector selListeners = new Vector();
    public String homeDirName;
    private String startDirName = null;
    private String userAgent;
    JScrollPane sp;

    // Yuzo: Changing from this to an new Selection Listener which does no
    SelectionChangeListener treeSelectionListener = new SelectionChangeListener();
    // Adding an Expansion Event Listener so as to expand a node
    // without having to select it.
    treeExpansionListener treeExpListener = new treeExpansionListener();

    // Constructor
    public WebDAVTreeView()
    {
        GlobalData.getGlobalData().setTree(this);
        tree = new JTree(treeModel);

        tree.putClientProperty("JTree.lineStyle", "Angled");

        tree.setSelectionModel(selectionModel);
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(treeSelectionListener);

        tree.setRowHeight(-1);

        sp = new JScrollPane(tree);
        sp.setPreferredSize(new Dimension(240,400));

        String showLocal = System.getProperty( "local", "yes" );
        if( !showLocal.equalsIgnoreCase("no") )
        {
            String os = (System.getProperty( "os.name" )).toLowerCase();
            if( os.indexOf( "windows" ) == -1 )
                startDirName = System.getProperty("user.home");
            else
            {
                // On W2K, user.home is something like C:\\Documents and Settings\\user
                // On NT4, it is C:\\WinNT\\Profiles\\user
                // On Win ME, it is C:\\Windows
                // This code extracts the root directory, i.e., C:\\ or whatever the
                // system partition is
                startDirName = System.getProperty("user.home");
                int pos = startDirName.indexOf(new Character(File.separatorChar).toString() );
                if( pos > -1 )
                    startDirName = startDirName.substring(0, pos+1) + File.separatorChar;
            }
            if( startDirName == null )
            {
                startDirName = new Character(File.separatorChar).toString();
            }
        }

        // Listen for expansion Events
        // Yuzo Adding Expansion Event listner for testing purposes:
        tree.addTreeExpansionListener( treeExpListener);


        treeModel.addTreeModelListener( new TreeModelListener()
        {
            public void treeNodesChanged(TreeModelEvent e)
            {
            }

            public void treeNodesInserted(TreeModelEvent e)
            {
            }

            public void treeNodesRemoved(TreeModelEvent e)
            {
            }

            public void treeStructureChanged(TreeModelEvent e)
            {
            }
        });
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }


    public void setUserAgent( String ua )
    {
        userAgent = ua;
        ((WebDAVTreeNode)root).setUserAgent( userAgent );
    }

    class treeExpansionListener implements TreeExpansionListener
    {
        public void treeExpanded( TreeExpansionEvent evt )
        {
            if( GlobalData.getGlobalData().getDebugTreeView() )
            {
                System.err.println( "WebDAVTreeView::treeExpansionListener::treeExpanded" );
            }

            TreePath selectedPath = selectionModel.getSelectionPath();
            GlobalData.getGlobalData().setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

            TreePath expansionPath = evt.getPath();
            currPath = expansionPath;
            WebDAVTreeNode tn = (WebDAVTreeNode)expansionPath.getLastPathComponent();
            if(!tn.hasLoadedChildren())
            {
                tn.loadChildren( false );
            }
            else
            {
            }
            GlobalData.getGlobalData().resetCursor();
        }

        public void treeCollapsed( TreeExpansionEvent evt )
        {
        }
    }



    // Yuzo: added PutResponse stuff to handle put to a selected collection
    // in the file view

    // Goal here is to load the node without changing the selection
    public void PutEventResponse(PutEvent e)
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::PutEventResponse" );
        }

        WebDAVTreeNode tn = e.getNode();

        TreeNode path[] = tn.getPath();

        tree.removeTreeExpansionListener(treeExpListener);
        tree.removeTreeSelectionListener(treeSelectionListener);

        tn.removeChildren();
        tn.loadChildren( false );

        tree.addTreeExpansionListener(treeExpListener);
        tree.addTreeSelectionListener(treeSelectionListener);

    }

    //Yuzo: Added Copy ResponseListner stuff
    public synchronized void CopyEventResponse(CopyResponseEvent e)
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::CopyEventResponse" );
        }

        WebDAVTreeNode tn = e.getNode();

        if( tn == null )
            return;

        TreeNode path[] = tn.getPath();

        // Now then reload the Tree from this node
        // This means that we have to unload this node
        // then reload load it with the updated info making
        // a call to the server
        tn.removeAllChildren();

        // Load all the Children of this Node.
        tn.loadChildren( true );
    }


    public JScrollPane getScrollPane()
    {
        // We package the whole TreeView inside a Scroll Pane, returned
        // by this function.
        return(sp);
    }

    //Yuzo Added: Get the Selection Event from TableView
    // This means that a folder(dir) was double clicked.
    // Now handle this event as an open of that partictular dir
    public void selectionChanged(ViewSelectionEvent e)
    {
        tableSelectionChanged(e);
    }

    //  This is where selection event from the TableView are routed to.
    //  The Event includes the information needed to expand/select
    //  the particular row.
    protected void tableSelectionChanged(ViewSelectionEvent e)
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::tableSelectionChanged" );
        }

        WebDAVTreeNode tn = (WebDAVTreeNode)e.getNode();
        TreePath tp = (TreePath)e.getPath();


        tree.removeTreeExpansionListener(treeExpListener);
        tree.removeTreeSelectionListener(treeSelectionListener);

        if(!tn.hasLoadedChildren())
        {
            Object obj = tp.getPathComponent(1);
            String indicator = obj.toString();
            if ( indicator.startsWith(GlobalData.WebDAVPrefix) || indicator.startsWith(GlobalData.WebDAVPrefixSSL) )
            {
                tree.addTreeExpansionListener(treeExpListener);
                tree.addTreeSelectionListener(treeSelectionListener);
                tn.loadChildren( true );
                return;
            }
            else
            {
                tn.loadChildren( true );
            }
        }

        GlobalData.getGlobalData().setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

        if (!tree.isExpanded(tp))
        {
            tree.expandPath(tp);
        }

        tree.setSelectionPath(tp);
        //tree.revalidate();            // Attempt to validate the new tree
                                        // This does not work because the vaildate
                                        // is invoked later.  This causes
                                        // the scrollPathToVisible not to work
        tree.makeVisible(tp);
        tree.scrollPathToVisible(tp);
        //tree.scrollPathToVisible(tp); // Sun Bug Id 4180658  -- does not work
                                        // but this second call is a "fix"
                                        // according to the bug report
        tree.treeDidChange();

        ViewSelectionEvent event =
            new ViewSelectionEvent(this, tn, tp);
        for (int i=0; i<selListeners.size();i++)
        {
            ViewSelectionListener l =
                (ViewSelectionListener)selListeners.elementAt(i);
            l.selectionChanged(event);
        }

        currPath = tp;

        GlobalData.getGlobalData().resetCursor();

        tree.addTreeExpansionListener(treeExpListener);
        tree.addTreeSelectionListener(treeSelectionListener);
    }

    public void refresh()
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::refresh" );
        }

        // Make sure the directory structure is current.
        int row = tree.getRowForPath(currPath);
        tree.clearSelection();
        tree.setSelectionRow(row);
    }

    public synchronized void addViewSelectionListener(ViewSelectionListener l)
    {
        // Register a listener
        selListeners.addElement(l);
    }

    public synchronized void removeViewSelectionListener(ViewSelectionListener l)
    {
        selListeners.removeElement(l);
    }


    // This method sets the selected TreeNode to tn and sends the proper
    // notifications out.
    public void setSelectedNode( WebDAVTreeNode tn )
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::setSelectedNode" );
        }


        // Could make sure of currPath by geeting it fro tn
        TreePath tp = new TreePath(tn.getPath());

        tree.removeTreeExpansionListener(treeExpListener);
        tree.removeTreeSelectionListener(treeSelectionListener);

        if (!tree.isExpanded(tp))
        {
            tree.expandPath(tp);
        }

        treeModel.nodeStructureChanged(tn);

        tree.setSelectionPath(tp);
        tree.makeVisible(tp);
        tree.scrollPathToVisible(tp);

        tree.addTreeExpansionListener(treeExpListener);
        tree.addTreeSelectionListener(treeSelectionListener);

        ViewSelectionEvent event =
        new ViewSelectionEvent(this, tn, tp); // check currPath
        for (int i=0; i<selListeners.size();i++)
        {
            ViewSelectionListener l =
                (ViewSelectionListener)selListeners.elementAt(i);
            l.selectionChanged(event);
        }
    }

    //Yuzo New Selection Listener for tree selection
    class SelectionChangeListener implements TreeSelectionListener
    {
        // This is where we handle the tree selection event.
        public void valueChanged(TreeSelectionEvent e)
        {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::SelectionChangeListener::valueChanged" );
        }

            //Need to make sure that the newly selected node (dir)
            //has its children's children loaded.  This is needed to
            // ensure that handles on the files are correct.
            TreePath tp = e.getPath();

            currPath = tp;

            // Get the last node, then check if all the Children are
            // loaded.
            WebDAVTreeNode tn = (WebDAVTreeNode)currPath.getLastPathComponent();


            if (!tn.hasLoadedChildren())
            {
                Object obj = tp.getPathComponent(1);
                if (obj != null)
                {
                    String s = obj.toString();

                    if( s.startsWith(GlobalData.WebDAVPrefix) || s.startsWith(GlobalData.WebDAVPrefixSSL) )
                    {
                        tn.loadChildren(true);
                        return;
                    }
                    else
                    {
                        GlobalData.getGlobalData().setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
                        refreshLocal(tn);
                        tn.setHasLoadedChildren(true);
                        GlobalData.getGlobalData().resetCursor();
                    }
                }
            }

            treeModel.nodeStructureChanged(tn);

            // alert the Selection Listeners (FileView)
            ViewSelectionEvent event = new ViewSelectionEvent(this, tn, tp);
            for (int i=0; i<selListeners.size();i++)
            {
                ViewSelectionListener l = (ViewSelectionListener)selListeners.elementAt(i);
                l.selectionChanged(event);
            }
        }
    }


    public void initTree()
    {
        // For initialization purposes.
        // This function is called when the client starts.
        if (startDirName != null && GlobalData.getGlobalData().doAddStartDir())
            addRowToRoot( startDirName, true, false );

        if( GlobalData.getGlobalData().isAppletMode() )
        {
            // as applet, we navigate to the specified sites automatically
            String[][] initialSites = GlobalData.getGlobalData().getInitialSites();
            for (int i = 0; i < initialSites.length; i++)
            {
                String is[] = initialSites[i];
                if( is.length == 0 )
                    continue;
                String initialSite = initialSites[i][0];
                if (initialSite.startsWith(GlobalData.WebDAVPrefixSSL))
                    initialSite = initialSite.substring(GlobalData.WebDAVPrefixSSL.length());
                if (initialSite.startsWith(GlobalData.WebDAVPrefix))
                    initialSite = initialSite.substring(GlobalData.WebDAVPrefix.length());
    
                // simulate user input
                URIBox uriBox = GlobalData.getGlobalData().getURIBox();
                uriBox.setText(initialSite);
                uriBox.notifyListener();
            }
        }
    }


    public String constructPath(TreePath the_path)
    {
        // This will iterate through the path array, and construct
        // the appropriate path for method generation purposes.
        String newPath = "";
        if( the_path == null )
            return newPath;
        Object[] path = the_path.getPath();
        if (path.length == 1)
            return newPath;

        String firstComp = path[1].toString();

        if( !firstComp.startsWith(GlobalData.WebDAVPrefix) && !firstComp.startsWith(GlobalData.WebDAVPrefixSSL) )
        {
            // We're constructing local filename path
            newPath += startDirName;
            if (newPath.endsWith(new Character(File.separatorChar).toString()))
                newPath = newPath.substring(0,newPath.length() - 1);
            for (int i=2;i<path.length;i++)
            {
                newPath += File.separator + path[i].toString();
            }
            if (!newPath.endsWith(new Character(File.separatorChar).toString()) )
                newPath += new Character(File.separatorChar).toString();
        }
        else
        {
            // Construct WebDAV path
            for (int i=1;i<path.length;i++)
            {
                newPath += path[i].toString();
                if (!newPath.endsWith("/"))
                    newPath += "/";
            }
            if (!newPath.endsWith("/"))
                newPath += "/";
        }
        return (newPath);
    }

    public boolean addRowToRoot(String name, boolean local, boolean deltaV )
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::addRowToRoot" );
        }

        // Add item to the tree. If local == true, the item is
        // considered to be a file on a local file system.
        String newName = "";

        if ( (name == null) || (name.equals("")) )
            return false;

        if (local)
        {
            if (name.endsWith(new Character(File.separatorChar).toString()))
                name = name.substring(0,name.length() - 1);
            if (name.length() == 0)
                name = new Character(File.separatorChar).toString();
            newName = name;
            File file = null;
            file = new File(newName);
            if (file == null)
                return false;
            if (!file.isDirectory())
            {
                GlobalData.getGlobalData().errorMsg("TreeView Error:\n\nFile is not a directory.");
                return false;
            }
        }
        else
        {
            if (name.endsWith("/"))
                name = name.substring(0,name.length() - 1);
            if( GlobalData.getGlobalData().getSSL() && !name.startsWith(GlobalData.WebDAVPrefixSSL))
                newName = GlobalData.WebDAVPrefixSSL + name;
            else if ( !name.startsWith(GlobalData.WebDAVPrefix) )
                newName = GlobalData.WebDAVPrefix + name;
            else
                newName = name;
        }

        if (rootElements.contains(newName))
        {
            // be quiet in applet mode
            if (!GlobalData.getGlobalData().isAppletMode())
                GlobalData.getGlobalData().errorMsg("TreeView Error:\n\nNode already exists!");
            return false;
        }

        rootElements.addElement(newName);
        WebDAVTreeNode newNode = new WebDAVTreeNode( newName, userAgent );
        treeModel.insertNodeInto(newNode,root,0);

        if (local)
        {
            tree.clearSelection();
            tree.setSelectionRow(1);
            tree.expandPath(tree.getPathForRow(0));
        }
        else
        {
            // Now finish the Processing for the root.
            newNode.finishLoadChildren();
            newNode.setHasLoadedChildren(true);
            treeModel.nodeStructureChanged(newNode);
            tree.setSelectionRow(1);
            newNode.setDeltaV( deltaV );
        }

        return true;
    }

    public String getCurrentPath()
    {
        return constructPath( currPath );
    }

    // Yuzo: This is to allow Main to add fileView as a Tree Listener
    public void addTreeSelectionListener(TreeSelectionListener tsl)
    {
        tree.addTreeSelectionListener(tsl);
    }

    public String getLockToken( String curFile )
    {
        if( currPath == null )
            return null;
        currNode = (WebDAVTreeNode) currPath.getLastPathComponent();
        DataNode node = ((WebDAVTreeNode)currNode).getDataNode();
        return getLockToken( node, curFile );
    }

    public void setLock( String curFile, String token )
    {
        currNode = (WebDAVTreeNode) currPath.getLastPathComponent();
        DataNode node = ((WebDAVTreeNode)currNode).getDataNode();
        node = getCurrentDataNode( node, curFile );
        if( node != null )
        {
            node.lock( token );
        }
    }

    public void resetLock( String curFile )
    {
        currNode = (WebDAVTreeNode) currPath.getLastPathComponent();
        DataNode node = ((WebDAVTreeNode)currNode).getDataNode();
        node = getCurrentDataNode( node, curFile );
        if( node != null )
        {
            node.unlock();
        }
    }


    public boolean isCollection( String curFile )
    {
        int pos;
        if( isRemote( curFile ) )
            pos = curFile.lastIndexOf( "/" );
        else
            pos = curFile.lastIndexOf( String.valueOf(File.separatorChar) );
        if( pos >= 0 )
            curFile = curFile.substring( pos+1 );
        currNode = (WebDAVTreeNode) currPath.getLastPathComponent();
        DataNode node = ((WebDAVTreeNode)currNode).getDataNode();
        node = getCurrentDataNode( node, curFile );
        if( node != null )
        {
            if( node.isCollection() )
                return true;
        }
        Enumeration enum = ((WebDAVTreeNode)currNode).children();
        while( enum.hasMoreElements() )
        {
            WebDAVTreeNode treeNode = (WebDAVTreeNode)enum.nextElement();
            node = ((WebDAVTreeNode)treeNode).getDataNode();
            node = getCurrentDataNode( node, curFile );
            if( node != null )
            {
                if( node.isCollection() )
                    return true;
            }
        }
        return false;
    }


    public boolean isRemote( String curFile )
    {
        if( curFile.startsWith(GlobalData.WebDAVPrefix) || curFile.startsWith(GlobalData.WebDAVPrefixSSL) )
            return true;
        else
            return false;
    }


    public boolean isDeltaV()
    {
        return getDeltaV();
    }
    
    
    public boolean getDeltaV()
    {
        if( currPath==null || currPath.getPathCount()<1 )
            return false;

        // get the first entry below the root (which refers to a
        // server) and check if it supports DeltaV
        WebDAVTreeNode node = (WebDAVTreeNode)currPath.getPathComponent(1);
        return node.getDeltaV();
    }
    
    
    public boolean getDeltaVReports()
    {
        if( currPath==null || currPath.getPathCount()<1 )
            return false;

        // get the first entry below the root (which refers to a
        // server) and check if it supports DeltaV
        WebDAVTreeNode node = (WebDAVTreeNode)currPath.getPathComponent(1);
        return node.getDeltaVReports();
    }
    
    
    public void setVersions( String curFile, Vector versions )
    {
        currNode = (WebDAVTreeNode) currPath.getLastPathComponent();
        DataNode node = ((WebDAVTreeNode)currNode).getDataNode();
        node = getCurrentDataNode( node, curFile );
        if( node != null && node instanceof DeltaVDataNode )
        {
            ((DeltaVDataNode)node).setVersions( versions );
        }
    }


    public void refreshLocalNoSelection( WebDAVTreeNode n )
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::refreshLocalNoSelection" );
        }

        tree.removeTreeExpansionListener(treeExpListener);
        tree.removeTreeSelectionListener(treeSelectionListener);

        n.removeAllChildren();
        n.setHasLoadedChildren( false );
        n.loadChildren( true );
        treeModel.nodeStructureChanged(n);
        n.setHasLoadedChildren(true);

        tree.addTreeExpansionListener(treeExpListener);
        tree.addTreeSelectionListener(treeSelectionListener);
    }

    public void refreshLocal( WebDAVTreeNode n )
    {
        if( GlobalData.getGlobalData().getDebugTreeView() )
        {
            System.err.println( "WebDAVTreeView::refreshLocal" );
        }

        if( n == null )
            return;

        TreeNode path[] = n.getPath();

        String s = "";
        for (int i = 1; i < path.length; i++)
        {
            s = s + path[i];
            if( !s.endsWith( String.valueOf(File.separatorChar) ) )
                s += File.separatorChar;
        }
        // Now then reload the Tree from this node
        // This means that we have to unload this node
        // then reload it

        tree.removeTreeExpansionListener(treeExpListener);
        tree.removeTreeSelectionListener(treeSelectionListener);

        n.removeAllChildren();
        n.setHasLoadedChildren( false );

        TreePath tp = new TreePath(path);

        // Load all the Children of this Node.
        n.loadChildren( true );

        treeModel.nodeStructureChanged(n);
        n.setHasLoadedChildren(true);

        tree.addTreeExpansionListener(treeExpListener);
        tree.addTreeSelectionListener(treeSelectionListener);

        ViewSelectionEvent event = new ViewSelectionEvent(this, n, tp);
        for (int i=0; i<selListeners.size();i++)
        {
            ViewSelectionListener l = (ViewSelectionListener)selListeners.elementAt(i);
            l.selectionChanged(event);
        }
    }


    private String getLockToken( DataNode node, String curFile )
    {
        if( node == null )
            return null;

        if( node.getName().equals( curFile ) )
            return node.getLockToken();
        if( node.isCollection() )
        {
            if( node.getSubNodes() == null )
                return null;
            for( Enumeration e = node.getSubNodes().elements(); e.hasMoreElements(); )
            {
                DataNode current = (DataNode)e.nextElement();
                String token = getLockToken( current, curFile );
                if( token != null )
                    return token;
            }
        }
        return null;
    }

    private DataNode getCurrentDataNode( DataNode node, String curFile )
    {
        if( node == null )
            return null;

        if( node.getName().equals( curFile ) )
            return node;
        if( node.isCollection() )
        {
            if( node.getSubNodes() == null )
                return null;
            for( Enumeration e = node.getSubNodes().elements(); e.hasMoreElements(); )
            {
                DataNode current = (DataNode)e.nextElement();
                DataNode curNode = getCurrentDataNode( current, curFile );
                if( curNode != null )
                    return curNode;
            }
        }
        return null;
    }
}
