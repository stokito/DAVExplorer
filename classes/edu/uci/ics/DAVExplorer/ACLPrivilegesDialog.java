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

import com.ms.xml.om.Element;

/**
 * Title:       
 * Description: 
 * Copyright:   Copyright (c) 2005 Regents of the University of California. All rights reserved.
 * @author      Joachim Feise (dav-exp@ics.uci.edu)
 * @date        19 January 2005
 */
public class ACLPrivilegesDialog extends PropDialog
{

    /**
     * @param properties
     * @param resource
     * @param hostname
     * @param supported
     */
    public ACLPrivilegesDialog( Element properties, String resource,
            String hostname, boolean supported )
    {
        init( new ACLPropModel(properties), properties, resource, hostname, null, false );
        if( supported )
            setTitle( "View supported Privileges" );
        else
            setTitle( "View User Privileges" );
        buttonPanel.remove(addButton);
        buttonPanel.remove(deleteButton);
        buttonPanel.remove(saveButton);
        pack();
        setSize( getPreferredSize() );
        GlobalData.getGlobalData().center( this );
        show();
    }
}
