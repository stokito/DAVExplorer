/*
 * Copyright (C) 2005 Regents of the University of California.
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
package edu.uci.ics.DAVExplorer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * Title:       
 * Description: 
 * Copyright:   Copyright (c) 2005 Regents of the University of California. All rights reserved.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        
 */
public class ACLReportChangeSearchPropertiesDialog extends
        ACLReportPropertiesDialog implements DocumentListener
{

    /**
     * @param resource
     */
    public ACLReportChangeSearchPropertiesDialog( String resource )
    {
        super( resource, "Select Search Criteria" );
    }


    protected void changePanel( JPanel panel )
    {
        JLabel sepLabel = new JLabel( " " );
        sepLabel.setHorizontalAlignment( JLabel.RIGHT );
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        GridBagLayout gridbag = (GridBagLayout)panel.getLayout();
        gridbag.setConstraints( sepLabel, c );
        panel.add( sepLabel );
        JLabel matchLabel = new JLabel( "Match: " );
        matchLabel.setHorizontalAlignment( JLabel.RIGHT );
        c.gridwidth = 1;
        gridbag.setConstraints( matchLabel, c );
        panel.add( matchLabel );
        match = new JTextField();
        match.getDocument().addDocumentListener( this );
        match.setActionCommand( "match" );
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints( match, c );
        panel.add( match );
        super.changePanel( panel );
    }


    /**
     * DocumentListener interface
     * @param e
     */
    public void insertUpdate( DocumentEvent e )
    {
        okButton.setEnabled( match.getText().length() > 0 );
    }


    /**
     * DocumentListener interface
     * @param e
     */
    public void removeUpdate( DocumentEvent e )
    {
        okButton.setEnabled( match.getText().length() > 0 );
    }


    /**
     * DocumentListener interface
     * @param e
     */
    public void changedUpdate( DocumentEvent e )
    {
        okButton.setEnabled( match.getText().length() > 0 );
    }


    /**
     * 
     * @param enable
     */
    public void setChanged( boolean enable )
    {
        changed = enable;
        okButton.setEnabled( (match.getText().length()>0) && (selected.size()>0) && changed );
    }


    public String getMatch()
    {
        return match.getText();
    }


    protected JTextField match;
}
