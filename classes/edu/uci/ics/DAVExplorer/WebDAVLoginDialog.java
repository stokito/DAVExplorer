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

/////////////////////////////////////////////////////////////////////////
//This is the class that will display a dialog box prompting the user
//for authentication information.
//
//Written by: Gerair D. Balian (Elite 5)
//On: 3/3/98
//For ICS126B (WebDAV Project)
/////////////////////////////////////////////////////////////////////////
// This class causes a login dialog box to
// appear.  The purpose of this Login box is
// to authenticate users when they attempt to
// connect to a DAV site through the action of
// connecting to it.
//
// This class DOES NOT authenticate users at this
// time.  It is in place as a UI component which
// may be fully integrated in an authetication scheme
// at some future point.
////////////////////////////////////////////////////////////////
// The code has been modified to include povisions for the final
// WebDAV xml namespaces.  A small number of program errors have
// been corrected.
//
// Please use the following contact:
//
// dav-exp@ics.uci.edu
//
// Version: 0.4
// Changes by: Yuzo Kanomata and Joe Feise
// Date: 3/17/99
//
// Change List:
// Note: No authenication check is executed in the Action Listener
// when "okay" is clicked.
//
// Version: 0.5
// Changes by: Joe Feise
// Date: 12/3/99
//
// Change List:
// Now invoked from Authentication handler in HTTPClient
// when "okay" is clicked.

package DAVExplorer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JDialog.*;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.util.*;
import java.awt.*;
import java.awt.event.*;


public class WebDAVLoginDialog extends JDialog implements ActionListener
{
/*-----------------------------------------------------------------------
Public methods and attributes section
-----------------------------------------------------------------------*/

    Vector listeners = new Vector();

    //Construction
    public WebDAVLoginDialog( String strCaption, String realm, String scheme, boolean isModal )
    {
        super( GlobalData.getGlobalData().getMainFrame(), strCaption, isModal );

        GridBagLayout gridbag = new GridBagLayout();
        JPanel groupPanel = new JPanel( gridbag );
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        JLabel l = new JLabel( "Realm: " + realm, JLabel.CENTER );
        l.setForeground(Color.black);
        gridbag.setConstraints( l, constraints );
        groupPanel.add( l );
        l = new JLabel( "Scheme: " + scheme, JLabel.CENTER );
        l.setForeground(Color.black);
        gridbag.setConstraints( l, constraints );
        groupPanel.add( l );
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        l = new JLabel( "Login name:", JLabel.LEFT );
        l.setForeground(Color.black);
        gridbag.setConstraints( l, constraints );
        groupPanel.add( l );
        constraints.weightx = 3.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        txtUsername = new JTextField(30);
        txtUsername.addActionListener( this );
        gridbag.setConstraints( txtUsername, constraints );
        groupPanel.add( txtUsername );
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        l = new JLabel( "Password:", JLabel.LEFT );
        l.setForeground(Color.black);
        gridbag.setConstraints( l, constraints );
        groupPanel.add( l );
        constraints.weightx = 3.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        txtPassword = new JPasswordField( "", 30 );
        txtPassword.addActionListener( this );
        gridbag.setConstraints( txtPassword, constraints );
        groupPanel.add( txtPassword );
        JPanel p = new JPanel();
        p.add( OkButton = new JButton( "OK" ) );
        OkButton.addActionListener( this );
        p.add( CancelButton = new JButton( "Cancel" ) );
        CancelButton.addActionListener( this );
        gridbag.setConstraints( p, constraints );
        groupPanel.add( p );
        getRootPane().setDefaultButton( OkButton );
        txtUsername.requestFocus();

        getContentPane().add( groupPanel );
        pack();
        center();
        setResizable( false );
        setVisible( true );
    }


    public synchronized void addListener( ActionListener l )
    {
        listeners.addElement(l);
    }


    public synchronized void removeListener( ActionListener l )
    {
        listeners.removeElement(l);
    }


    public void actionPerformed( ActionEvent e )
    {
        if( e.getActionCommand().equals("OK") )
        {
            m_strUsername = txtUsername.getText();
            m_strUserPassword = String.valueOf( txtPassword.getPassword() );
            if ( (m_strUsername.length()>0) && (m_strUserPassword.length()>0) )
            {
                setVisible( false );
                dispose();
            }
        }
        else if( e.getActionCommand().equals( "Cancel" ) )
        {
            m_strUsername = "";
            m_strUserPassword = "";
            setVisible( false );
            dispose();
        }
        else
        {
            /*
             * Simulate click on default button
             * JTextFields intercept the return button
             * Ideally, this would be modified by code like this:
             * static {
             *   JTextField f = new JTextField();
             *   KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
             *   Keymap map = f.getKeymap();
             *   map.removeKeyStrokeBinding(enter);
             * }
             * However, this changes the keymap for *all* JTextFields, and we
             * need the original mapping for the URI box
             */
            m_strUsername = txtUsername.getText();
            m_strUserPassword = String.valueOf( txtPassword.getPassword() );
            if ( (m_strUsername.length()>0) && (m_strUserPassword.length()>0) )
                OkButton.doClick();
        }
    }


    //Get the user name to be sent through the wire
    public String getUsername()
    {
        return m_strUsername;
    }

    //Get the user password to be sent through the wire.
    public String getUserPassword()
    {
        return m_strUserPassword;
    }


/*-----------------------------------------------------------------------
Protected methods and attributes section
-----------------------------------------------------------------------*/
    protected void center()
    {
        Rectangle recthDimensions = getParent().getBounds();
        Rectangle bounds = getBounds();
        setBounds(recthDimensions.x + (recthDimensions.width-bounds.width)/2,
             recthDimensions.y + (recthDimensions.height - bounds.height)/2, bounds.width, bounds.height );
    }


    protected String m_strUsername;
    protected String m_strUserPassword;
    protected JTextField txtUsername;
    protected JPasswordField txtPassword;
    protected JButton OkButton;
    protected JButton CancelButton;
}

