/*
 * Copyright (c) 2005 Regents of the University of California.
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
 * Title:       ACL OwnerProperty Dialog
 * Description: Dialog for viewing/modifying ACL owner and group properties
 * Copyright:   Copyright (c) 2005 Regents of the University of California. All rights reserved.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        18 January 2005
 */

package edu.uci.ics.DAVExplorer;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.ms.xml.om.Element;


/**
 * 
 */
public class ACLOwnerDialog extends JDialog
    implements ActionListener, ChangeListener, ListSelectionListener, WebDAVCompletionListener
{
    /**
     * Constructor
     * @param properties
     * @param resource
     * @param hostname
     * @param locktoken
     * @param changeable
     */
    public ACLOwnerDialog( Element properties, String resource, String hostname, boolean owner, boolean changeable )
    {
        super( GlobalData.getGlobalData().getMainFrame() );
        this.changeable = changeable;
        this.owner = owner;
        String title;
        if( changeable )
            title = "View/Modify ACL ";
        else
            title = "View ACL ";
        if( owner )
            title += "Owner";
        else
            title += "Group";
        setTitle( title );
        this.resource = hostname + resource;
        JLabel label = new JLabel( this.resource, JLabel.CENTER );
        label.setForeground(Color.black);
        getContentPane().add( "North", label );

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        closeButton  = new JButton("Close");
        closeButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        getRootPane().setDefaultButton( closeButton );
        closeButton.grabFocus();
        saveButton.setEnabled( false );

        getContentPane().add( "South", buttonPanel );
        setBackground(Color.lightGray);

        model = new ACLOwnerModel( properties );
        model.addChangeListener(this);
        treeTable = new JTreeTable( model );
        treeTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane scrollpane = new JScrollPane();
        scrollpane.setViewportView( treeTable );
        getContentPane().add( "Center", scrollpane );

        ((Main)GlobalData.getGlobalData().getMainFrame()).addWebDAVCompletionListener(this);
        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent we_Event)
                {
                    cancel();
                }
            });

        pack();
        setSize( getPreferredSize() );
        center();
        show();
    }


    /**
     * 
     * @param e
     */
    public void stateChanged( ChangeEvent e )
    {
        setChanged( true );
    }


    /**
     * 
     * @param enable
     */
    public void setChanged( boolean enable )
    {
        if( changeable )
        {
            changed = enable;
            saveButton.setEnabled( changed );
            if( !changed )
                model.clear();
        }
    }


    /**
     * 
     * @param e
     */
    public void actionPerformed(ActionEvent e)
    {
        if( e.getActionCommand().equals("Save") )
        {
            save();
        }
        else if( e.getActionCommand().equals("Close") )
        {
            cancel();
        }
    }


    /**
     * 
     * @param e
     */
    public void valueChanged(ListSelectionEvent e)
    {
    }


    /**
     * 
     * @param e
     */
    public void completion( WebDAVCompletionEvent e )
    {
        if( waiting && e.isSuccessful() )
            setChanged( false );  // disable save button
        waiting = false;
    }


    /**
     * 
     */
    public void save()
    {
        Element modified = model.getModified(false);
        ACLRequestGenerator generator = (ACLRequestGenerator)WebDAVResponseInterpreter.getGenerator();
        //generator.setResource( resource, null );
        if( owner )
            generator.SetOwner( modified, resource );
        else
            generator.SetGroup( modified, resource );
        waiting = true;
        generator.execute();
    }


    /**
     *
     */
    public void cancel()
    {
        setVisible(false);
        dispose();
    }


    /**
     *
     */
    protected void center()
    {
        Rectangle recthDimensions = getParent().getBounds();
        Rectangle bounds = getBounds();
        setBounds(recthDimensions.x + (recthDimensions.width-bounds.width)/2,
             recthDimensions.y + (recthDimensions.height - bounds.height)/2, bounds.width, bounds.height );
    }


    /**
     * 
     * @param title
     * @param text
     * @return
     */
    protected boolean ConfirmationDialog( String title, String text )
    {
        int opt = JOptionPane.showConfirmDialog( GlobalData.getGlobalData().getMainFrame(), text, title, JOptionPane.YES_NO_OPTION );
        if (opt == JOptionPane.YES_OPTION)
            return true;
        return false;
    }


    private JTreeTable treeTable;
    private PropModel model;
    private JButton saveButton;
    private JButton closeButton;
    private boolean changeable;
    private boolean changed = false;
    private String resource;
    private boolean waiting;
    private boolean owner;
}
