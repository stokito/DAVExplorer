/*
 * Copyright (c) 1998-2002 Regents of the University of California.
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
 * Title:       WebDAVFileView
 * Description: This class is part of the client's GUI.
 * Copyright:   Copyright (c) 1998-2002 Regents of the University of California. All rights reserved.
 * @author      Robert Emmery (dav-exp@ics.uci.edu)
 * @date        2 April 1998
 * @author      Yuzo Kanomata, Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 1999
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        12 January 2001
 * Changes:     Added support for https (SSL)
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        1 October 2001
 * Changes:     Change of package name
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        2 April 2002
 * Changes:     Updated for JDK 1.4
 */

package edu.uci.ics.DAVExplorer;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.border.BevelBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class WebDAVFileView implements ViewSelectionListener, ActionListener
{
    private static String jarPath = null;
    private final static String jarExtension =".jar";
    final static String WebDAVClassName = "edu/uci/ics/DAVExplorer";
    final static String WebDAVPrefix = "http://";
    final static String WebDAVPrefixSSL = "https://";
    final static String IconDir = "icons";

    final String[] colNames = { " ",
                                "  ",
                                "Name",
                                "Display",
                                "Type",
                                "Size",
                                "Last Modified" };

    private Vector data = new Vector();
    JTable table;
    JScrollPane scrPane;
    TableModel dataModel;
    ListSelectionModel selectionModel = new DefaultListSelectionModel();
    TableSorter sorter;
    Color headerColor;
    Vector selListeners = new Vector();
    Vector renameListeners = new Vector();
    Vector displayLockListeners = new Vector();
    ImageIcon FILE_ICON;
    ImageIcon LOCK_ICON;
    ImageIcon UNLOCK_ICON;
    ImageIcon FOLDER_ICON;
    WebDAVTreeNode parentNode;
    String parentPath = new String();
    String selectedResource;
    int selectedRow;
    int pressRow, releaseRow;

    public WebDAVFileView()
    {
        String iconPath = getIconPath();

        if (iconPath == null)
        {
            System.exit(0);
        }

        String folderIconPath =  iconPath + File.separatorChar + "TreeClosed.gif";
        String resPath = iconPath + File.separatorChar + "resource.gif";
        String lckPath = iconPath + File.separatorChar + "lck.gif";
        String unlckPath = iconPath + File.separatorChar + "unlck.gif";

        if( jarPath == null )
        {
            FOLDER_ICON = new ImageIcon(folderIconPath);
            FILE_ICON = new ImageIcon(resPath);
            LOCK_ICON = new ImageIcon(lckPath);
            UNLOCK_ICON = new ImageIcon(unlckPath);
        }
        else
        {
            try
            {
                folderIconPath = iconPath + "TreeClosed.gif";
                resPath = iconPath + "resource.gif";
                lckPath = iconPath + "lck.gif";
                unlckPath = iconPath + "unlck.gif";
                ZipFile file = new ZipFile( jarPath );
                ZipEntry entry = file.getEntry( folderIconPath );
                InputStream is = null;
                if( entry != null )
                {
                    is = file.getInputStream( entry );
                    int len = (int)entry.getSize();
                    if( len != -1 )
                    {
                        byte[] ba = new byte[len];
                        is.read( ba, 0, len );
                        FOLDER_ICON = new ImageIcon( ba );
                    }
                }
                entry = file.getEntry( resPath );
                if( entry != null )
                {
                    is = file.getInputStream( entry );
                    int len = (int)entry.getSize();
                    if( len != -1 )
                    {
                        byte[] ba = new byte[len];
                        is.read( ba, 0, len );
                        FILE_ICON = new ImageIcon( ba );
                    }
                }
                entry = file.getEntry( lckPath );
                if( entry != null )
                {
                    is = file.getInputStream( entry );
                    int len = (int)entry.getSize();
                    if( len != -1 )
                    {
                        byte[] ba = new byte[len];
                        is.read( ba, 0, len );
                        LOCK_ICON = new ImageIcon( ba );
                    }
                }
                entry = file.getEntry( unlckPath );
                if( entry != null )
                {
                    is = file.getInputStream( entry );
                    int len = (int)entry.getSize();
                    if( len != -1 )
                    {
                        byte[] ba = new byte[len];
                        is.read( ba, 0, len );
                        UNLOCK_ICON = new ImageIcon( ba );
                    }
                }
            }
            catch( IOException e )
            {
                errorMsg("Icon load failure: " + e );
            }
        }


        dataModel = new AbstractTableModel()
        {
            public int getColumnCount()
            {
                return colNames.length;
            }

            public int getRowCount()
            {
                return data.size();
            }

            public Object getValueAt(int row, int col)
            {
                if (data.size() == 0)
                    return null;
                return ((Vector)data.elementAt(row)).elementAt(col);
            }

            public String getColumnName(int column)
            {
                return colNames[column];
            }

            public Class getColumnClass(int c)
            {
                if (data.size() == 0)
                    return null;
                return getValueAt(0,c).getClass();
            }

            public  boolean isCellEditable(int row, int col)
            {
                // only allow edit of name
                return (col == 2);
            }

            public void setValueAt(Object value, int row, int column)
            {
                if( GlobalData.getGlobalData().getDebugFileView() )
                {
                    System.err.println( "WebDAVFileView::AbstractTableModel::setValueAt" );
                }

                if (column == 2)
                {
                    String val = null;
                    try
                    {
                        val = (String) table.getValueAt(selectedRow,2);
                    }
                    catch (Exception e)
                    {
                    }

                    if ( val != null)
                    {
                        if( !parentPath.startsWith(WebDAVPrefix) && !parentPath.startsWith(WebDAVPrefixSSL) )
                            return;

                        ((Vector)data.elementAt(row)).setElementAt(value,column);

                        if (value.equals(selectedResource))
                        {
                            return;
                        }
                        Vector ls;
                        synchronized (this)
                        {
                            ls = (Vector) renameListeners.clone();
                        }
                        ActionEvent e = new ActionEvent(this,0,value.toString());
                        for (int i=0; i<ls.size();i++)
                        {
                            ActionListener l = (ActionListener) ls.elementAt(i);
                            l.actionPerformed(e);
                        }
                        return;
                    }
                }
                try
                {
                    ((Vector)data.elementAt(row)).setElementAt(value,column);
                }
                catch (Exception exc)
                {
                    System.out.println(exc);
                }
            }
        };

        sorter = new TableSorter(dataModel);
        table = new JTable(sorter);
        scrPane = new JScrollPane( table );
        scrPane.setPreferredSize(new Dimension(750,400));

        scrPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        MouseListener ml = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() == 2)
                {
                    handleDoubleClick(e);
                }
            }

            public void mousePressed(MouseEvent e)
            {
                handlePress(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                handleRelease(e);
            }
        };
        table.addMouseListener(ml);
        setupTable(table);
        table.updateUI();
    }


    // Returns the path to the parentNode
    public String getParentPath(){
    return parentPath;
    }

    ////////////
    // Implements the Action Listener.
    // Purpose: to listen for a reset to the old selected name
    // for the case of a failure of Rename.
    // Need to do this because Response Interpreter can't access the
    // resetName method because there teh FileView is not accessible
    public void actionPerformed( ActionEvent e )
    {
    resetName();
    }


    ////////////
    // This implements the View Selection Listener Interface
    // The purpose of this listners is to respond to the
    // Selection of a node on the TreeView.  This means
    // that the Table should become populated with the directories
    // and files in the Selected Node.
    public synchronized void selectionChanged(ViewSelectionEvent e)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::selectionChanged" );
        }

        table.clearSelection();
        clearTable();
        selectedRow = -1;

        parentNode = (WebDAVTreeNode)e.getNode();

        // set Parent Path

        TreePath tp = e.getPath();

        Object pathString[] = tp.getPath();
        parentPath = "";
        if( pathString.length > 0 ){
            for (int i = 1; i < pathString.length; i++){
                parentPath += pathString[i].toString() + "/";
            }
        }

        if (table.getRowCount() != 0)
        {
            return;
        }

        TreePath path = e.getPath();
        WebDAVTreeNode tn = (WebDAVTreeNode)path.getLastPathComponent();


        WebDAVTreeNode pn = (WebDAVTreeNode)tn.getParent();

        GlobalData.getGlobalData().setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

        addDirToTable(tn);

        DataNode dn = tn.getDataNode();
        if (dn == null)
        {
            table.updateUI();
            GlobalData.getGlobalData().resetCursor(); //reset to original cursor
            return;
        }

        Vector sub = dn.getSubNodes();

        if (sub == null)
        {
            //System.out.println(" No  resource");
        }
        else
        {
            for (int i=0; i < sub.size(); i++)
            {
                DataNode d_node = (DataNode)sub.elementAt(i);

        }
            addFileToTable(sub);

        }

        //table.updateUI();
        GlobalData.getGlobalData().resetCursor(); //reset to original cursor

    }

    protected void addDirToTable(WebDAVTreeNode n)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::addDirToTable" );
        }

        // Add the directories to the Table

        int count = n.getChildCount();
        for (int i=0; i < count; i++)
        {
            WebDAVTreeNode child = (WebDAVTreeNode) n.getChildAt(i);
            DataNode d_node = child.getDataNode();
            if (d_node != null)
            {
                Object[] rowObj = new Object[7];
                rowObj[0] = "true";
                rowObj[1] = new Boolean(d_node.isLocked());
                rowObj[2] = d_node.getName();
                rowObj[3] = d_node.getDisplay();
                rowObj[4] = d_node.getType();
                rowObj[5] = (new Long(d_node.getSize())).toString();
                rowObj[6] = d_node.getDate();

                addRow(rowObj);
            }
        }
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();
    }

    protected void addFileToTable(Vector v)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::addFileToTable" );
        }

        for (int i=0; i < v.size(); i++)
        {
            Object[] rowObj = new Object[7];
            DataNode d_node = (DataNode)v.elementAt(i);

            rowObj[0] = "false";
            rowObj[1] = new Boolean(d_node.isLocked());
            rowObj[2] = d_node.getName();
            rowObj[3] = d_node.getDisplay();
            rowObj[4] = d_node.getType();
            rowObj[5] = (new Long(d_node.getSize())).toString();
            rowObj[6] = d_node.getDate();

            addRow(rowObj);
        }
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();
    }

   public WebDAVTreeNode getParentNode(){
    return parentNode;
   }

   protected String getParentPathString(){
    String s = "";
    TreePath tp = new TreePath(parentNode.getPath());

    if (tp.getPathCount() > 1) {
        for ( int i = 1; i < tp.getPathCount(); i++ )
        {
            s = s + tp.getPathComponent(i);
            if( s.startsWith( WebDAVPrefix ) || s.startsWith( WebDAVPrefixSSL ) )
                s += "/";
            else if( !s.endsWith( String.valueOf(File.separatorChar) ) )
                s += File.separatorChar;
        }
    }
    return s;

   }


   /* Yuzo Added: purpose, to get Selected Resource which is the
      old full name of a renamed item. */

    public String getOldSelectedResource(){
        return getParentPathString() + selectedResource;
    }


    // Added to return the currently selected collection, null if none of resource
    // null if no collection selected
    public WebDAVTreeNode getSelectedCollection()
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::getSelectedCollection" );
        }

        try {
            if (selectedRow < 0)
            {
                return null;
            }
            boolean isCollection =
                new Boolean(table.getValueAt(selectedRow,0).toString()).booleanValue();

            if (isCollection) {
        //Get the TreeNode and return it
        //Integer val = (Integer)table.getValueAt(selectedRow,7);
        //Vector row = (Vector)data.elementAt(selectedRow);
        //Integer val = (Integer)row.elementAt(7);
        //int i = val.intValue();

        boolean found = false;
        WebDAVTreeNode node = null;
        Enumeration enum = parentNode.children();

        while(!found && enum.hasMoreElements()){

            node = (WebDAVTreeNode)enum.nextElement();

            String s = (String) node.getUserObject();
            if (s.equals(table.getValueAt(selectedRow,2))){
            found = true;
            }
        }
        if (found){
            return node;
        }else{
        return null;
        }
            } else { // a resource so send back null
        return null;
        }
        }catch (Exception exc) {
        System.out.println("Exception getSelectedCollection");
                    exc.printStackTrace();
                    return null;
        }
    }

   public boolean isSelectedLocked(){
    boolean b = new
            Boolean( table.getValueAt(selectedRow,1).toString()).booleanValue();
    return b;

   }

    // Attempt to get at the selected item's dataNode
    public String getSelectedLockToken()
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::getSelectedLockToken" );
        }

        if (selectedRow < 0){
            return null;
        }

        //Get the TreeNode and return dataNode's lockTocken
        WebDAVTreeNode n = getSelectedCollection();

        if (n != null) {
            // return lockToken from the Node's dataNode
            DataNode dn = n.getDataNode();
            // Get the lockToken
            return dn.getLockToken();
        }
        // Must be resource

        // 1. Find the resource's data Node
        DataNode dn = parentNode.getDataNode();

        // Do a search of the subNodes
        boolean found = false;
        Vector sub = dn.getSubNodes();
        String token = null;
        DataNode node;

        for( int i = 0; i < sub.size() && !found; i++){
            node = (DataNode)sub.elementAt(i);
            String s = node.getName();
            if(selectedResource.equals( s )){
                found = true;
                token = node.getLockToken();
            }
        }


        // 2. Get the token
        if (token == null)
        {
            System.out.println("Error: getSelectedCollection, dataNode not found for selected item");
            return null;
        }
        else
            return token;
    }


    public boolean hasSelected(){
    if (selectedRow >= 0)
        return true;
    else
        return false;
    }


    public String getSelected()
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::getSelected" );
        }

        String s = "";
        if ( selectedRow >= 0 )
        {
            TreePath tp = new TreePath(parentNode.getPath());

            s =  getParentPathString() + (String)table.getValueAt( selectedRow , 2);

            boolean isCollection =
                new Boolean(table.getValueAt(selectedRow,0).toString()).booleanValue();

            if(isCollection)
                return s + "/";
            else
                return s;
        } else{
            // Return the parent node
            return getParentPathString();
        }
    }

    public void resetName()
    {
        table.setValueAt(selectedResource, selectedRow,2);
        update();
    }

    public String getName()
    {
        return selectedResource;
    }

    public synchronized void setLock()
    {
        int row = selectedRow;
        try
        {
            table.setValueAt(new Boolean("true"),row,1);
        }
        catch (Exception exc)
        {
        };
        update();
    }

    public synchronized void resetLock()
    {
        int row = selectedRow;
        try
        {
            table.setValueAt(new Boolean("false"),row,1);
        }
        catch (Exception exc)
        {
        };
        update();
    }

    public synchronized void update()
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::update" );
        }

        table.clearSelection();
        updateTable(data);
    }

    private static String getIconPath()
    {
        String icons = WebDAVClassName;
        if( File.separatorChar != '/' )
        {
			int pos = icons.indexOf("/");
			while( pos >=0 )
			{
				icons = icons.substring( 0, pos) + File.separatorChar + icons.substring( pos+1 );
				pos = icons.indexOf( "/" );
			}
		}
        icons += File.separatorChar + IconDir;

        String classPath = System.getProperty("java.class.path");
        if (classPath == null)
        {
            errorMsg("Fileview:\nNo Classpath set." );
            return null;
        }
        StringTokenizer paths = new StringTokenizer(classPath,":;");

        while (paths.hasMoreTokens())
        {
            String nextPath = paths.nextToken();
            String lowerPath = nextPath.toLowerCase();
            if( lowerPath.endsWith( jarExtension ) )
            {
                jarPath = nextPath;
                int pos = lowerPath.indexOf( jarExtension );
                nextPath = nextPath.substring( 0, pos );
            }
            if (!nextPath.endsWith(new Character(File.separatorChar).toString()))
                nextPath += File.separatorChar;
            nextPath += icons;
            File iconDirFile = new File(nextPath);
            if (iconDirFile.exists())
                return nextPath;
            if( jarPath != null )
            {
                try
                {
                    ZipFile zfile = new ZipFile( jarPath );
                    icons = WebDAVClassName + "/" + IconDir + "/";
                    ZipEntry entry = zfile.getEntry( icons + "connect.gif" );
                    if( entry != null )
                    {
                        return icons;
                    }
                    else
                        jarPath = null;
                }
                catch( IOException e )
                {
                }
            }
        }
        errorMsg("Fileview:\nPath to icons not found." );
        return null;
    }

    public void setupTable(JTable table)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::setupTable" );
        }

        table.clearSelection();
        table.setSelectionModel(selectionModel);
        selectionModel.addListSelectionListener(new SelectionChangeListener());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setShowGrid( false );
        DefaultTableCellRenderer ren;

        TableColumn resizeCol = table.getColumn(colNames[0]);
        resizeCol.setMaxWidth(25);
        resizeCol.setMinWidth(25);

        DefaultTableCellRenderer resIconRenderer = new DefaultTableCellRenderer()
        {
            public void setValue(Object value)
            {
                try
                {
                    boolean isColl = new Boolean(value.toString()).booleanValue();
                    if (isColl)
                        setIcon(FOLDER_ICON);
                    else
                        setIcon(FILE_ICON);
                }
                catch (Exception e)
                {
                }
            }
        };
        resizeCol.setCellRenderer(resIconRenderer);

        resizeCol = table.getColumn(colNames[1]);
        resizeCol.setMaxWidth(25);
        resizeCol.setMinWidth(25);
        DefaultTableCellRenderer lockIconRenderer = new DefaultTableCellRenderer()
        {
            public void setValue(Object value)
            {
                try
                {
                    boolean isLocked = new Boolean(value.toString()).booleanValue();
                    if (isLocked)
                        setIcon(LOCK_ICON);
                    else
                        setIcon(UNLOCK_ICON);
                }
                catch (Exception e)
                {
                }
            }
        };
        resizeCol.setCellRenderer(lockIconRenderer);

        resizeCol = table.getColumn(colNames[2]);
        resizeCol.setMinWidth(100);

        resizeCol = table.getColumn(colNames[3]);
        resizeCol.setMinWidth(100);

        resizeCol = table.getColumn(colNames[4]);
        resizeCol.setMinWidth(100);
        ren = new DefaultTableCellRenderer();
        ren.setHorizontalAlignment(JLabel.CENTER);
        resizeCol.setCellRenderer(ren);

        resizeCol = table.getColumn(colNames[5]);
        resizeCol.setMinWidth(50);
        ren = new DefaultTableCellRenderer();
        ren.setHorizontalAlignment(JLabel.CENTER);
        resizeCol.setCellRenderer(ren);

        resizeCol = table.getColumn(colNames[6]);
        resizeCol.setMinWidth(210);
        ren = new DefaultTableCellRenderer();
        ren.setHorizontalAlignment(JLabel.CENTER);
        resizeCol.setCellRenderer(ren);
    }

    public JScrollPane getScrollPane()
    {
        return scrPane;
    }

    public void fireTableModuleEvent()
    {
        TableModelEvent e = new TableModelEvent(dataModel);
        synchronized( this )
        {
            if( sorter != null )
                sorter.tableChanged(e);
        }
    }

    public void updateTable(Vector newdata)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::selectionChanged" );
        }

        this.data = newdata;
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();
    }

    private void addRow(Object[] rowData)
    {
        Vector newRow = new Vector();

        int numOfCols = table.getColumnCount();
        for (int i=0;i<numOfCols;i++)
            newRow.addElement(rowData[i]);

        data.addElement(newRow);
    }

    private void removeRow(int row)
    {
        data.removeElementAt(row);
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();
    }

    public void clearTable()
    {
        updateTable(new Vector());

        selectedRow = -1;
    }

    public void treeSelectionChanged(ViewSelectionEvent e)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::treeSelectionChanged" );
        }

        table.clearSelection();
        clearTable();
        WebDAVTreeNode t_node = (WebDAVTreeNode) e.getNode();
        parentNode = t_node;
        parentPath = e.getPath().toString();
        int cnt = t_node.getChildCount();
        if (table.getRowCount() != 0)
            return;
        for (int i=0;i<cnt;i++)
        {
            WebDAVTreeNode child = (WebDAVTreeNode) t_node.getChildAt(i);
            DataNode d_node = child.getDataNode();
            if (d_node == null)
                return;
            //Object[] rowObj = new Object[7];
            Object[] rowObj = new Object[8];
            rowObj[0] = "true";
            rowObj[1] = new Boolean(d_node.isLocked());
            rowObj[2] = d_node.getName();
            rowObj[3] = d_node.getDisplay();
            rowObj[4] = d_node.getType();
            rowObj[5] = (new Long(d_node.getSize())).toString();
            rowObj[6] = d_node.getDate();
            rowObj[7] = new Integer(i);
            addRow(rowObj);
        }
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();

        DataNode this_data_node = t_node.getDataNode();
        if (this_data_node == null)
            return;
        Vector subs = this_data_node.getSubNodes();
        if (subs == null)
            return;
        for (int i=0;i<subs.size();i++)
        {
            //Object[] rowObj = new Object[7];
            Object[] rowObj = new Object[8];
            DataNode d_node = (DataNode) subs.elementAt(i);

            rowObj[0] = "false";
            rowObj[1] = new Boolean(d_node.isLocked());
            rowObj[2] = d_node.getName();
            rowObj[3] = d_node.getDisplay();
            rowObj[4] = d_node.getType();
            rowObj[5] = (new Long(d_node.getSize())).toString();
            rowObj[6] = d_node.getDate();
            rowObj[7] = new Integer(-1);
            addRow(rowObj);
        }
        fireTableModuleEvent();
        synchronized( this )
        {
            if( sorter != null )
                sorter.sortByColumn(2,true);
        }
        //table.updateUI();
    }

    public synchronized void addViewSelectionListener(ViewSelectionListener l)
    {
        selListeners.addElement(l);
    }

    public synchronized void removeViewSelectionListener(ViewSelectionListener l)
    {
        selListeners.removeElement(l);
    }

    public synchronized void addRenameListener(ActionListener l)
    {
        renameListeners.addElement(l);
    }

    public synchronized void removeRenameListener(ActionListener l)
    {
        renameListeners.removeElement(l);
    }

    public synchronized void addDisplayLockListener(ActionListener l)
    {
        displayLockListeners.addElement(l);
    }

    public synchronized void removeDisplayLockListener(ActionListener l)
    {
        displayLockListeners.removeElement(l);
    }

    public void handlePress(MouseEvent e)
    {
        Point cursorPoint = new Point(e.getX(),e.getY());
        pressRow = table.rowAtPoint(cursorPoint);
    selectedRow = pressRow;
    }

    public void handleRelease(MouseEvent e)
    {
        Point cursorPoint = new Point(e.getX(),e.getY());
        releaseRow = table.rowAtPoint(cursorPoint);
    selectedRow = releaseRow;

        if (pressRow != -1)
        {
            if (releaseRow != -1)
            {
                if (pressRow != releaseRow){
                    //System.out.println("WebDAVFileView: Got Drag");
        }
            }
            else
            {
                //System.out.println("dragged outside");
            }
        }
    }

    public void displayLock()
    {
        Vector ls;
        synchronized (this)
        {
            ls = (Vector) displayLockListeners.clone();
        }
        ActionEvent e = new ActionEvent(this,0,null);
        for (int i=0; i<ls.size();i++)
        {
            ActionListener l = (ActionListener) ls.elementAt(i);
            l.actionPerformed(e);
        }
    }

    public void handleDoubleClick(MouseEvent e)
    {
        if( GlobalData.getGlobalData().getDebugFileView() )
        {
            System.err.println( "WebDAVFileView::handleDoubleClick" );
        }

        Point pt = e.getPoint();
        int col = table.columnAtPoint(pt);
        int row = table.rowAtPoint(pt);

        if ( (col == 1) && (row != -1) )
        {
            Boolean locked = null;
            try
            {
                locked = (Boolean) table.getValueAt(row,col);
            }
            catch (Exception exc)
            {
                System.out.println(exc);
                return;
            }
            if ( (locked != null) && (locked.booleanValue()) )
            {
                displayLock();
                return;
            }
        }

        Vector ls;
        if (selListeners == null)
        {
            return;
        }
        synchronized (this)
        {
            ls = (Vector) selListeners.clone();
        }

        int selRow = selectionModel.getMaxSelectionIndex();
        if (selRow != -1)
        {
            if( sorter == null )
                return;
            int origRow = sorter.getTrueRow(selRow);
            if (origRow == -1)
            {
                return;
            }

            if (origRow > parentNode.getChildCount()-1)
                return;

            WebDAVTreeNode tempNode = (WebDAVTreeNode)parentNode.getChildAt(origRow);
            TreePath path = new TreePath(tempNode.getPath());

            ViewSelectionEvent selEvent = new ViewSelectionEvent(this, tempNode, path );
            for (int i=0; i<ls.size();i++)
            {
                ViewSelectionListener l = (ViewSelectionListener) ls.elementAt(i);
                l.selectionChanged(selEvent);
            }

            //selectionChanged( selEvent );
        }
    }

    class SelectionChangeListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if( GlobalData.getGlobalData().getDebugFileView() )
            {
                System.err.println( "WebDAVFileView::SelectionChangeListener::valueChanged" );
            }

            Vector ls;
            synchronized (this)
            {
                ls = (Vector) selListeners.clone();
            }
            int selRow = selectionModel.getMaxSelectionIndex();
            if ((selRow >= 0) && (data.size() > 0) )
            {
                selectedResource = (String) table.getValueAt(selRow,2);
                String selResource = new String(selectedResource);
            selectedRow = selRow; // set the clas golbal variable to
                  // be used later for Select Node
                try
                {
                    boolean isColl = new Boolean(table.getValueAt(selRow,0).toString()).booleanValue();
                    if (isColl)
                    {
                        if( parentPath.startsWith(WebDAVPrefix) || parentPath.startsWith(WebDAVPrefixSSL) ||
                            selResource.startsWith(WebDAVPrefix) || selResource.startsWith(WebDAVPrefixSSL) )
                        {
                            if( !selResource.endsWith( "/" ) )
                                selResource += "/";
                        }
                        else
                            selResource += new Character(File.separatorChar).toString();

                    }
                }
                catch (Exception exc)
                {
                    exc.printStackTrace();
                    return;
                }



                ViewSelectionEvent selEvent = new ViewSelectionEvent(this,null,null);
                for (int i=0; i<ls.size();i++)
                {
                    ViewSelectionListener l = (ViewSelectionListener) ls.elementAt(i);
                }
            }
        }
    }

    private static void errorMsg(String str)
    {
        JOptionPane pane = new JOptionPane();
        Object[] options = { "OK" };
        pane.showOptionDialog( null, str,"Error Message", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    }
}
