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
 * Title:       URI Box
 * Description: This class creates an extension of JPanel which creates the
 *              URI entry box on the WebDAVExplorer.  This box contains
 *              the text field in which the user enters the dav server's URI.
 * Copyright:   Copyright (c) 1998-2001 Regents of the University of California. All rights reserved.
 * @author      Undergraduate project team ICS 126B 1998
 * @date        1998
 * @author      Yuzo Kanomata, Joachim Feise (dav-exp@ics.uci.edu)
 * @date        17 March 1999
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        12 January 2001
 * Changes:     Added support for https (SSL)
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        1 October 2001
 * Changes:     Change of package name
 * @author      Joachim Feise (dav-exp@ics.uci.edu), Karen Schuchardt
 * @date        2 April 2002
 * Changes:     Incorporated Karen Schuchardt's changes to improve the loading of
 *              images. Thanks!
 */

package edu.uci.ics.DAVExplorer;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

public class URIBox extends JPanel implements ActionListener
{
    private Vector URIBoxListener;
    private static String jarPath = null;
    private final static String jarExtension =".jar";
    private final static String WebDAVClassName = "edu/uci/ics/DAVExplorer";
    private final static String IconDir = "icons";

    public URIBox()
    {
        super();
        //setLayout(new BorderLayout());

        JPanel panel = new JPanel();

        okButton = new JButton(loadImageIcon("connect.gif", "Connect"));

        //okButton.setMargin(new Insets(1,1,1,1));
        okButton.setActionCommand("Connect");
        okButton.addActionListener(this);
        okButton.setToolTipText("Connect");

        panel.add(okButton);

        textField1 = new JTextField(30);
        textField1.addActionListener(new EnterPressedListener());
        label1 = new JLabel();
        if( GlobalData.getGlobalData().doSSL() )
            label1.setText( "https://" );
        else
            label1.setText( "http://" );
        label1.setHorizontalAlignment( SwingConstants.RIGHT );
        label1.setForeground( Color.black );

        panel.add(label1);
        panel.add(textField1);

        add("Center", panel);
        URIBoxListener = new Vector();
    }

    JTextField textField1;
    JLabel label1;
    JButton okButton;


    private ImageIcon loadImageIcon(String filename, String description)
    {
        try {
            return new ImageIcon(getClass().getResource("icons/" + filename),description);
        } catch (Exception ex) {
            errorMsg("Toolbar:\nIcon load error." );
            return null;
        }
    }

    public void actionPerformed(ActionEvent evt)
    {
        notifyListener(evt);
    }

    public String getText()
    {
        return textField1.getText().trim();
    }

    public synchronized void addActionListener(ActionListener l)
    {
        URIBoxListener.addElement(l);
    }

    public synchronized void removeActionListener(ActionListener l)
    {
        URIBoxListener.removeElement(l);
    }

    protected void notifyListener(ActionEvent e)
    {
        ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText());
        Vector v;
        synchronized(this)
        {
            v = (Vector)URIBoxListener.clone();
        }

        for (int i=0; i< v.size(); i++)
        {
            WebDAVURIBoxListener client = (WebDAVURIBoxListener)v.elementAt(i);
            client.actionPerformed(evt);
        }
    }

    private static void errorMsg(String str)
    {
        JOptionPane pane = new JOptionPane();
        Object[] options = { "OK" };
        pane.showOptionDialog( null, str,"Error Message", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    }

    class EnterPressedListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            notifyListener(e);
        }
    }
}
