/*
 * Copyright (c) 1999 Regents of the University of California.
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

// This is where all of the requests are formed. The class contains
// static information needed to form all WebDAV requests. When GUI
// sends an event indicating that another resource has been
// selected it is properly handled by either
// tableSelectionChanged() or treeSelectionChanged()
//
// Version: 0.3
// Author:  Robert Emmery  <memmery@earthlink.net>
// Date:    4/2/98
//////////////////////////////////////////////////////////////////


package WebDAV;

import WebDAV.WebDAVManager;
import HTTPClient.*;
import java.util.*;
import java.io.*;
import com.sun.java.swing.*;
import com.ms.xml.om.*;
import com.ms.xml.parser.*;
import com.ms.xml.util.*;

public class WebDAVRequestGenerator implements Runnable {

  private static String WebDAVPrefix = "http://";
  private static final int DEFAULT_PORT = 80;
  private static String HostName = "";
  private static int Port = 0;
  private static String Method;
  private static String Path = "";
  private static String ResourceName = "";
  private static String tableResource = "";
  private static String StrippedResource = "";
  private static NVPair[] Headers = null;
  private static byte[] Body = null;
  private static String Extra = "";
  private static String User = "";
  private static String Password = "";
  private static JFrame mainFrame;

  private static Vector listeners = new Vector();

  public WebDAVRequestGenerator() { }
 
  public WebDAVRequestGenerator(JFrame mainFrame)
  {
    super();
    this.mainFrame = mainFrame;
  }

  public void setUser(String username)
  {
    User = username;
  }
  
  public void setPass(String pass)
  {
    Password = pass;
  }
  
  public void tableSelectionChanged(ViewSelectionEvent e)
  {
    if (e.getNode() != null) {
      return;
    }
    else {
      tableResource = e.getPath();
      if (Path.length() == 0) {
        ResourceName = tableResource;
      }
      ResourceName = Path + tableResource;
    }
    
    System.out.println("RequestGenerator: ");
    System.out.println(" * ResourceName = " + ResourceName);
  }

  public void treeSelectionChanged(ViewSelectionEvent e)
  {
    String Item;
    Path = e.getPath();
    ResourceName = Path;

    if (Path.startsWith(WebDAVPrefix)) {    
    }
    System.out.println("RequestGenerator: ");
    System.out.println(" * ResourceName = " + ResourceName);
  }

  public boolean parseResourceName()
  {
    if (ResourceName.equals("")) {
      System.out.println("ResourceName empty");
        errorMsg("No resource selected!");
      return false;
    }
    if (!ResourceName.startsWith(WebDAVPrefix)) {
      System.out.println("ResourceName is local");
        errorMsg("This operation cannot be executed\non a local resource.");
      return false;
    }
    String stripped = ResourceName.substring(WebDAVPrefix.length());
    return parseStripped(stripped);
  }

  public boolean parseStripped(String stripped)
  {
    StringTokenizer str = new StringTokenizer(stripped, "/");
    boolean isColl = false;

    if (!str.hasMoreTokens()) {
      errorMsg("Invalid host name.");
      return false;
    }
    if (stripped.endsWith("/"))
      isColl = true;

    String host = str.nextToken();

    int pos = host.indexOf(":");
    if (pos < 0) {
      HostName = host;
      Port = 0;
    }
    else {
      HostName = host.substring(0,pos);
      String port = host.substring(pos+1);
      try {
        Port = Integer.parseInt(port);
      }
      catch (Exception e) {
        errorMsg("Invalid port number.");
        Port = 0;
        return false;
      }

    }
    String newRes = "";
    while (str.hasMoreTokens())
      newRes = newRes + "/" + str.nextToken();
//    if ( (!newRes.endsWith("/")) && (isColl) )
//      newRes += "/";
    if (newRes.length() == 0)
      newRes = "/";
   
    StrippedResource = newRes;
    return true;
     
  }

  public void execute()
  {
    Thread th = new Thread(this);
    th.start();
  }
  
  public void run()
  {
    if (Headers == null) {
      System.out.println("Invalid Request");
        errorMsg("Invalid Request.");
      return;
    }
    Vector ls;
    synchronized (this) {
      ls = (Vector) listeners.clone();
    }
 
    WebDAVRequestEvent e = new WebDAVRequestEvent(this, Method,HostName,Port,StrippedResource,
                          Headers, Body, Extra, User, Password);
    for (int i=0;i<ls.size();i++) {
      WebDAVRequestListener l = (WebDAVRequestListener) ls.elementAt(i);
      l.requestFormed(e); 
    }
  }
  
  public synchronized void addRequestListener(WebDAVRequestListener l)
  {
    listeners.addElement(l);  
  }
  
  public synchronized void removeRequestListener(WebDAVRequestListener l)
  {
    listeners.removeElement(l);
  }

  public synchronized void DiscoverLock(String method)
  {
    Extra = new String(method);
    String[] prop = new String[1];
    String[] schema = new String[1];

    prop[0] = new String("lockdiscovery");
    schema[0] = new String(WebDAVProp.DAV_SCHEMA);
    GeneratePropFind(null,"prop","zero",prop,schema);
    execute();
  }

  public synchronized void GeneratePropFind(String FullPath, String command, String Depth, String[] props, String[] schemas)
  {
    Headers = null;
    Body = null;

    boolean ok;
    if (FullPath != null)
      ok = parseStripped(FullPath);
    else {
      if (Extra.equals("refresh")) {
    ResourceName = Path;
      }
      ok = parseResourceName();
    }

    if (!ok) {

      System.out.println("Error Generating PROPFIND Method for " + StrippedResource);
      return;
    }
    System.out.println("Generating Propfind PROPFIND Method for: " + StrippedResource);
    String com = "allprop";
    String dep = "infinity";
    if ( command.equalsIgnoreCase("prop") || command.equalsIgnoreCase("propname"))
      com = command.toLowerCase();
    if ( Depth.equalsIgnoreCase("zero") || Depth.equalsIgnoreCase("one"))
      dep = Depth;
    if (dep.equalsIgnoreCase("zero"))
      dep = "0";
    else  if (dep.equalsIgnoreCase("one"))
        dep = "1";

    Method = "PROPFIND";
    Document miniDoc = new Document();
    miniDoc.setVersion("1.0");
    miniDoc.addChild(WebDAVXML.elemNewline,null);

    AsGen asgen = new AsGen();
    WebDAVXML.createNamespace( asgen, null );
    Element propFind = WebDAVXML.createElement( WebDAVXML.ELEM_PROPFIND, Element.ELEMENT, null, asgen );
//    propFind.setAttribute(Name.create("D","xmlns"),"DAV:");
    propFind.addChild(WebDAVXML.elemNewline,null);
    if (com.equals("allprop"))
    {
        Element allpropElem = WebDAVXML.createElement( WebDAVXML.ELEM_ALLPROP, Element.ELEMENT, propFind, asgen );
        addChild( propFind, allpropElem, 1 );
    }
    else if (com.equals("propname"))
    {
        Element propnameElem = WebDAVXML.createElement( WebDAVXML.ELEM_PROPNAME,Element.ELEMENT, propFind, asgen );
        addChild( propFind, propnameElem, 1 );
    }
    else
    {
        Element propElem = WebDAVXML.createElement( WebDAVXML.ELEM_PROP, Element.ELEMENT, propFind, asgen );
        propFind.addChild(WebDAVXML.elemNewline,null);
        for (int i=0;i<props.length;i++)
        {
            Element prop = WebDAVXML.createElement( props[i], Element.ELEMENT, propElem, asgen );
            addChild( propElem, prop, 2 );
        }
        addChild( propFind, propElem, 1 );
    }
    miniDoc.addChild(propFind,null);
    miniDoc.addChild(WebDAVXML.elemNewline, null);

    ByteArrayOutputStream byte_str = new ByteArrayOutputStream();
    XMLOutputStream xml_out = new XMLOutputStream(byte_str);

    try {
      miniDoc.save(xml_out);
      Body = byte_str.toByteArray();
      
      Headers = new NVPair[4];
      if (Port == 0 || Port == DEFAULT_PORT) {
        Headers[0] = new NVPair("Host", HostName);
      }
      else
        Headers[0] = new NVPair("Host", HostName + ":" + Port);
      Headers[1] = new NVPair("Depth", dep);
      Headers[2] = new NVPair("Content-Type", "text/xml");
      Headers[3] = new NVPair("Content-Length", new Long(Body.length).toString());

      System.out.println("generated xml: " );
      XMLOutputStream out = new XMLOutputStream(System.out);
      miniDoc.save(out);
    }
    catch (Exception e) {
      errorMsg("XML generation error: \n" + e);
      System.out.println("XML Generator Error: " + e);
    }
  }

  private void addChild( Element parent, Element child, int tabcount )
  {
    if( parent != null )
    {
        for( int i=0; i<tabcount; i++ )
            parent.addChild( WebDAVXML.elemTab, null );
        parent.addChild( child, null );
        parent.addChild( WebDAVXML.elemNewline, null );
    }
  }
  
  private static boolean docContains(Document doc, Element e)
  {
    Enumeration docEnum = doc.getElements();
    while (docEnum.hasMoreElements()) {
      Element propEl = (Element) docEnum.nextElement();
      Name propTag = propEl.getTagName();
      if (propTag == null)
        continue;
      if (!propTag.getName().equals(WebDAVXML.ELEM_PROP))
        continue;
      Enumeration propEnum = propEl.getElements();
      while (propEnum.hasMoreElements()) {
        Element prop = (Element) propEnum.nextElement();
        Name nameTag = prop.getTagName();
        if (prop.getType() != Element.ELEMENT)
          continue;
        if ( (nameTag.getName().equals(e.getTagName().getName())) &&
           (nameTag.getNameSpace().equals(e.getTagName().getNameSpace())) )
          return true;
      }     
    }
    return false;
  }
  
  public synchronized void GeneratePropPatch(String Host, int port, String Res, Document old_xml, Document new_xml)
  {
    // need to determine here the patches (if any)

    Headers = null;
    Body = null;
    boolean setUsed = false;
    boolean removeUsed = false;
    Method = "PROPPATCH";
    Document miniDoc = new Document();
    miniDoc.setVersion("1.0");
    miniDoc.addChild(WebDAVXML.elemNewline,null);

    AsGen asgen = new AsGen();
    WebDAVXML.createNamespace( asgen, null );
    Element propUpdate = WebDAVXML.createElement( WebDAVXML.ELEM_PROPERTY_UPDATE, Element.ELEMENT, null, asgen );
    propUpdate.addChild(WebDAVXML.elemNewline,null);

    Element setEl = WebDAVXML.createElement( WebDAVXML.ELEM_SET, Element.ELEMENT, propUpdate, asgen );
    setEl.addChild(WebDAVXML.elemNewline,null);
    Element removeEl = WebDAVXML.createElement( WebDAVXML.ELEM_REMOVE, Element.ELEMENT, propUpdate, asgen );
    Element setProp = WebDAVXML.createElement( WebDAVXML.ELEM_PROP, Element.ELEMENT, setEl, asgen );
    Element removeProp = WebDAVXML.createElement( WebDAVXML.ELEM_PROP, Element.ELEMENT, removeEl, asgen );
    addChild( setEl, setProp, 2 );
    addChild( removeEl, removeProp, 2 );

    Enumeration namesEnum = new_xml.getElements();
    while (namesEnum.hasMoreElements()) {
      Element nameEl = (Element) namesEnum.nextElement();
      if (nameEl.getType() != Element.NAMESPACE)
        continue;
      miniDoc.addChild(nameEl,null);
      miniDoc.addChild(WebDAVXML.elemNewline,null);
    }

   // if any of the properties were added, insert them into set elem

    miniDoc.addChild(propUpdate,null);
    miniDoc.addChild(WebDAVXML.elemNewline,null);

    Enumeration newEnum = new_xml.getElements();
    while (newEnum.hasMoreElements()) {
      Element propElem = (Element) newEnum.nextElement();
      Name propTag = propElem.getTagName();
      if (propTag == null)
        continue;
      if (!propTag.getName().equals(WebDAVXML.ELEM_PROP))
        continue;
      Enumeration propEnum = propElem.getElements();
      while (propEnum.hasMoreElements()) {
        Element prop = (Element) propEnum.nextElement();
        if (prop.getType() != Element.ELEMENT)
          continue;
        if (!docContains(old_xml,prop)) {
          setUsed = true;
          setProp.addChild(WebDAVXML.elemTab,null);
          setProp.addChild(prop,null);
          setProp.addChild(WebDAVXML.elemNewline,null);
          setProp.addChild(WebDAVXML.elemTab,null);
          setProp.addChild(WebDAVXML.elemTab,null);
        }
      }
    }

    Enumeration OldEnum = old_xml.getElements();
    while (OldEnum.hasMoreElements()) {
      Element propElem = (Element) OldEnum.nextElement();
      Name propTag = propElem.getTagName();
      if (propTag == null)
        continue;
      if (!propTag.getName().equals(WebDAVXML.ELEM_PROP))
        continue;
      Enumeration propEnum = propElem.getElements();
      while (propEnum.hasMoreElements()) {
        Element prop = (Element) propEnum.nextElement();
        if (prop.getType() != Element.ELEMENT)
          continue;
        if (!docContains(new_xml,prop)) {
          removeUsed = true;
          removeProp.addChild(WebDAVXML.elemTab,null);
          Element remEl = new ElementImpl(prop.getTagName(),Element.ELEMENT);
          removeProp.addChild(remEl,null);
          removeProp.addChild(WebDAVXML.elemNewline,null);
          removeProp.addChild(WebDAVXML.elemTab,null);
          removeProp.addChild(WebDAVXML.elemTab,null);
        }
      }
    }
    if ( (!setUsed) && (!removeUsed)) {
      System.out.println("No changes.");      
      return;
    }

    if (setUsed) {
      propUpdate.addChild(WebDAVXML.elemTab,null);
      propUpdate.addChild(setEl,null);
      propUpdate.addChild(WebDAVXML.elemNewline,null);
    }
    if (removeUsed) {
      propUpdate.addChild(WebDAVXML.elemTab,null);
      propUpdate.addChild(removeEl,null);
      propUpdate.addChild(WebDAVXML.elemNewline,null);
    }

    StrippedResource = Res;
    HostName = Host;
    Port = port;

    ByteArrayOutputStream byte_str = new ByteArrayOutputStream();
    XMLOutputStream xml_out = new XMLOutputStream(byte_str);

    try {
      miniDoc.save(xml_out);
      Body = byte_str.toByteArray();

      Headers = new NVPair[3];
      if (Port == 0 || Port == DEFAULT_PORT)
        Headers[0] = new NVPair("Host", HostName);
      else
        Headers[0] = new NVPair("Host", HostName + ":" + Port);
      Headers[1] = new NVPair("Content-Type", "text/xml");
      Headers[2] = new NVPair("Content-Length", new Long(Body.length).toString());

      System.out.println("generated xml: " );
      XMLOutputStream out = new XMLOutputStream(System.out);
      miniDoc.save(out);
    }
    catch (Exception e) {
      System.out.println("XML Generator Error: " + e);
        errorMsg("XML Generator Error: \n" + e);
    }

    execute();
  }
  
  public synchronized void GenerateMkCol()
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("Error Generating MKCOL Method...");
      return;
    }
    System.out.println("Generating MKCOL Method...");

    Method = "MKCOL";
    Headers = new NVPair[1];
    if (Port == 0 || Port == DEFAULT_PORT)
      Headers[0] = new NVPair("Host",HostName);
    else
      Headers[0] = new NVPair("Host",HostName + ":" + Port);


  }
  
  public synchronized void GenerateGet(String localName)
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("Error Generating GET Method...");
      return;
    }
    System.out.println("Generating GET Method...");

    Extra = localName;
    Method = "GET";
    Body = null;
    Headers = new NVPair[1];
    if (Port == 0 || Port == DEFAULT_PORT)
      Headers[0] = new NVPair("Host",HostName);
    else
      Headers[0] = new NVPair("Host",HostName + ":" + Port);

  }
  
  public synchronized void GenerateDelete(String lockToken)
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("Error Generating DELETE Method...");
      return;
    }
    System.out.println("Generating DELETE Method...");

    Method = "DELETE";
    Body = null;
    if (lockToken != null) {
      Headers = new NVPair[2];
      Headers[1] = new NVPair("If","(<" + lockToken + ">)");
    }
    else
      Headers = new NVPair[1];

    if (Port == 0 || Port == DEFAULT_PORT)
      Headers[0] = new NVPair("Host",HostName);
    else
      Headers[0] = new NVPair("Host",HostName + ":" + Port);

  }
  
  public synchronized void GeneratePut(String fileName, String lockToken)
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("File is not local");
      return;
    }
    System.out.println("Generating PUT Method...");
    if ( (fileName == null) || (fileName.equals("")) ) {
      errorMsg("WebDAV Generator:\nFile not found!\n");
      return;
    }
    System.out.println("filename: " + fileName);
    File file = new File(fileName);
    if (!file.exists()) {
      System.out.println("invalid file");
         errorMsg("Invalid File.");
      return;
    }
    try {
      FileInputStream file_in = new FileInputStream(file);
      DataInputStream in = new DataInputStream(file_in);
      Method = "PUT";

      int off = 0;
      int fileSize = (int) file.length(); // scary stuff
      Body = new byte[fileSize];
      int rcvd = 0;
      do {
        off += rcvd;
        rcvd = file_in.read(Body, off, fileSize-off);
      } while (rcvd != -1 && off+rcvd < fileSize);


    if (lockToken != null) {
      Headers = new NVPair[3];
      Headers[2] = new NVPair("If","(<" + lockToken + ">)");
    }
    else
      Headers = new NVPair[2];

    if (Port == 0 || Port == DEFAULT_PORT)
      Headers[0] = new NVPair("Host",HostName);
    else
      Headers[0] = new NVPair("Host",HostName + ":" + Port);

      Headers[1] = new NVPair("Content-Length",new Integer(fileSize).toString());
    } catch (Exception e) {
        System.out.println("Error generating PUT");
        System.out.println(e);
        return;
     }
  }
  
  public synchronized void GenerateCopy(String Dest, boolean Overwrite, boolean KeepAlive)
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("Error Generating COPY Method...");
      return;
    }  
    System.out.println("Generating COPY Method :");
    System.out.println("Source: " + StrippedResource);
    System.out.println("Destination: " + Dest);
    String ow = (Overwrite) ? "T" : "F";
    if (Dest == null)
      Dest = StrippedResource + "_copy";

    Method = "COPY";
    Body = null;
    if (KeepAlive)
    {
      Document miniDoc = new Document();
      miniDoc.setVersion("1.0");
      miniDoc.addChild(WebDAVXML.elemNewline,null);

      AsGen asgen = new AsGen();
      WebDAVXML.createNamespace( asgen, null );
      Element propBehavior = WebDAVXML.createElement( WebDAVXML.ELEM_PROPERTY_BEHAVIOR, Element.ELEMENT, null, asgen );
      propBehavior.addChild( WebDAVXML.elemNewline, null );

      Element keepAlv = WebDAVXML.createElement( WebDAVXML.ELEM_KEEP_ALIVE, Element.ELEMENT, propBehavior, asgen );
      Element val = WebDAVXML.createElement( null, Element.PCDATA, keepAlv, asgen );
      val.setText("*");
      addChild( keepAlv, val, 1 );
      addChild( propBehavior, keepAlv, 0 );

      miniDoc.addChild(propBehavior, null);
      miniDoc.addChild(WebDAVXML.elemNewline, null);

      ByteArrayOutputStream byte_str = new ByteArrayOutputStream();
      XMLOutputStream xml_out = new XMLOutputStream(byte_str);
 
     try {
        miniDoc.save(xml_out);
        Body = byte_str.toByteArray();

        Headers = new NVPair[5];
        if (Port == 0 || Port == DEFAULT_PORT)
          Headers[0] = new NVPair("Host", HostName);
        else
          Headers[0] = new NVPair("Host", HostName + ":" + Port);
        Headers[1] = new NVPair("Destination", Dest);
        Headers[2] = new NVPair("Content-Type", "text/xml");
        Headers[3] = new NVPair("Content-Length", new Long(Body.length).toString());
        Headers[4] = new NVPair("Overwrite", ow);
        System.out.println("generated xml: " );
        XMLOutputStream out = new XMLOutputStream(System.out);
        miniDoc.save(out);
      } catch (Exception e) {
          System.out.println("XML Generator Eror...");
            errorMsg("XML Generator Error: \n" + e);
        }
    }
    else {
      Headers = new NVPair[3];
      if (Port == 0 || Port == DEFAULT_PORT)
        Headers[0] = new NVPair("Host", HostName);
      else
        Headers[0] = new NVPair("Host", HostName + ":" + Port);
      Headers[1] = new NVPair("Destination", Dest);
      Headers[2] = new NVPair("Overwrite", ow);
    }
  }
  
  public synchronized void GenerateRename(String Dest)
  {
    Extra = new String(tableResource);
    DiscoverLock("rename:" + Dest);
  }
  
  public synchronized void GenerateMove(String Dest, boolean Overwrite, boolean KeepAlive, String lockToken)
  {
    Headers = null;
    Body = null;
    if (!parseResourceName()) {
      System.out.println("Error Generating MOVE Method...");
      return;
    }  
    System.out.println("Generating MOVE Method :");
    System.out.println("Source: " + StrippedResource);
    System.out.println("Destination: " + Dest);
    String ow = (Overwrite) ? "T" : "F";
    if (Dest == null) {
      System.out.println("Invalid Destination ");
      return;
    }
    Method = "MOVE";
    Body = null;
    if (KeepAlive) {

      Document miniDoc = new Document();
      miniDoc.setVersion("1.0");
      miniDoc.addChild(WebDAVXML.elemNewline,null);

      AsGen asgen = new AsGen();
      WebDAVXML.createNamespace( asgen, null );
      Element propBehavior = WebDAVXML.createElement( WebDAVXML.ELEM_PROPERTY_BEHAVIOR, Element.ELEMENT, null, asgen );
      propBehavior.addChild( WebDAVXML.elemNewline, null );
      Element keepAlv = WebDAVXML.createElement( WebDAVXML.ELEM_KEEP_ALIVE, Element.ELEMENT, propBehavior, asgen );
      Element val = WebDAVXML.createElement( null, Element.PCDATA, keepAlv, asgen );
      val.setText("*");
      addChild( keepAlv, val, 1 );
      addChild( propBehavior, keepAlv, 0 );

      miniDoc.addChild(propBehavior, null);
      miniDoc.addChild(WebDAVXML.elemNewline, null);

      ByteArrayOutputStream byte_str = new ByteArrayOutputStream();
      XMLOutputStream xml_out = new XMLOutputStream(byte_str);
 
     try {
        miniDoc.save(xml_out);
        Body = byte_str.toByteArray();
        if (lockToken != null) {
          Headers = new NVPair[6];
          Headers[5] = new NVPair("If","(<" + lockToken + ">)");
        }
        else
          Headers = new NVPair[5];

        if (Port == 0 || Port == DEFAULT_PORT)
          Headers[0] = new NVPair("Host", HostName);
        else
          Headers[0] = new NVPair("Host", HostName + ":" + Port);
        Headers[1] = new NVPair("Destination", Dest);
        Headers[2] = new NVPair("Content-Type", "text/xml");
        Headers[3] = new NVPair("Content-Length", new Long(Body.length).toString());
        Headers[4] = new NVPair("Overwrite", ow);
        System.out.println("generated xml: " );
        XMLOutputStream out = new XMLOutputStream(System.out);
        miniDoc.save(out);
      } catch (Exception e) {
          System.out.println("XML Generator Eror...");
            errorMsg("XML Generator Error: \n" + e);
        }
    }
    else {
      Headers = new NVPair[3];
      if (Port == 0 || Port == DEFAULT_PORT)
        Headers[0] = new NVPair("Host", HostName);
      else
        Headers[0] = new NVPair("Host", HostName + ":" + Port);
      Headers[1] = new NVPair("Destination", Dest);
      Headers[2] = new NVPair("Overwrite", ow);
    }
  }
  
  public synchronized void GenerateLock(String OwnerInfo, String lockToken)
  {
    Headers = null;
    Body = null;
    // Only exclusive write lock is supported at the time

    if (!parseResourceName()) {
      System.out.println("Error Generating LOCK Method for " + StrippedResource);
      return;
    }  
    System.out.println("Generating LOCK Method for: " + StrippedResource);

    Method = "LOCK";
    Body = null;

    if (lockToken == null) {
    // new lock
      Document miniDoc = new Document();
      miniDoc.setVersion("1.0");
      miniDoc.addChild(WebDAVXML.elemNewline,null);

      AsGen asgen = new AsGen();
      WebDAVXML.createNamespace( asgen, null );
      Element lockInfoElem = WebDAVXML.createElement( WebDAVXML.ELEM_LOCK_INFO, Element.ELEMENT, null, asgen );
      lockInfoElem.addChild(WebDAVXML.elemNewline,null);
      
      Element lockTypeElem = WebDAVXML.createElement( WebDAVXML.ELEM_LOCK_TYPE, Element.ELEMENT, lockInfoElem, asgen );
      Element scopeElem = WebDAVXML.createElement( WebDAVXML.ELEM_LOCK_SCOPE, Element.ELEMENT, lockInfoElem, asgen );
      Element ownerElem = WebDAVXML.createElement( WebDAVXML.ELEM_OWNER, Element.ELEMENT, lockInfoElem, asgen );
      
      Element typeValue = WebDAVXML.createElement( WebDAVXML.ELEM_WRITE, Element.ELEMENT, lockTypeElem, asgen );
      Element scopeVal = WebDAVXML.createElement( WebDAVXML.ELEM_EXCLUSIVE, Element.ELEMENT, scopeElem, asgen );
      Element ownerHref = WebDAVXML.createElement( WebDAVXML.ELEM_HREF, Element.ELEMENT, ownerElem, asgen );
      Element ownerVal = WebDAVXML.createElement( null, Element.PCDATA, ownerElem, asgen );
      ownerVal.setText(OwnerInfo);
      addChild( ownerHref, ownerVal, 3 );
      addChild( ownerElem, ownerHref, 2 );
      addChild( lockTypeElem, typeValue, 2 );
      addChild( scopeElem, scopeVal, 2 );
      addChild( lockInfoElem, lockTypeElem, 1 );
      addChild( lockInfoElem, scopeElem, 1 );
      addChild( lockInfoElem, ownerElem, 1 );

      miniDoc.addChild(lockInfoElem,null);
      miniDoc.addChild(WebDAVXML.elemNewline, null);
 
      ByteArrayOutputStream byte_str = new ByteArrayOutputStream();
      XMLOutputStream xml_out = new XMLOutputStream(byte_str);

      try {
        miniDoc.save(xml_out);
        Body = byte_str.toByteArray();

        Headers = new NVPair[3];
        if (Port == 0 || Port == DEFAULT_PORT)
          Headers[0] = new NVPair("Host", HostName);
        else
          Headers[0] = new NVPair("Host", HostName + ":" + Port);
        Headers[1] = new NVPair("Content-Type", "text/xml");
        Headers[2] = new NVPair("Content-Length", new Long(Body.length).toString());

        System.out.println("generated xml: " );
        XMLOutputStream out = new XMLOutputStream(System.out);
        miniDoc.save(out);
      }
      catch (Exception e) {
        System.out.println("XML Generator Error: " + e);
          errorMsg("XML Generator Error: \n" + e);
      }

    }
    else {
      // refresh the lock
      try {
        String token = "(<" + lockToken + ">)";

        Headers = new NVPair[3];
        if (Port == 0 || Port == DEFAULT_PORT)
          Headers[0] = new NVPair("Host", HostName);
        else
          Headers[0] = new NVPair("Host", HostName + ":" + Port);

        Headers[1] = new NVPair("Timeout", "Infinite");
        Headers[2] = new NVPair("If", token);

      } catch (Exception e) {
          System.out.println(e);
            errorMsg(e.toString());
          return;
        }
    }
  }
  
  public synchronized void GenerateUnlock(String lockToken)
  {
    Headers = null;
    Body = null;

    if (!parseResourceName()) {
      System.out.println("Error Generating UNLOCK Method...");
      return;
    }

    try {
 
      Method = "UNLOCK";
      Body = null;
      Headers = new NVPair[2];
      if (Port == 0 || Port == DEFAULT_PORT)
        Headers[0] = new NVPair("Host",HostName);
      else
        Headers[0] = new NVPair("Host",HostName + ":" + Port);
      Headers[1] = new NVPair("Lock-Token", "(<" + lockToken + ">)");

    } catch (Exception e) {
      System.out.println("Error Generating UNLOCK..");
      }
  }
  
  public synchronized void setExtraInfo(String info)
  {
    Extra = info;
  }

  public void handlePropPatch(PropDialogEvent e)
  {

    String hostname = e.getHost();
    String host = null;
    String res = e.getResource();
    byte[] old_bytes = e.getInitialData().getBytes();
    byte[] new_bytes = e.getData().getBytes();

    int pos = hostname.indexOf(":");
    int port = 0;
    if (pos > 0) {
      try {
        host = hostname.substring(0,pos);
        port = Integer.parseInt(hostname.substring(pos+1));
      } catch (Exception exception) { return; }
    }

    // Generate XML

    Document oldProp = null;
    Document newProp = null;
    try {
      ByteArrayInputStream old_b = new ByteArrayInputStream(old_bytes);    
      XMLInputStream old_in = new XMLInputStream(old_b);
      oldProp = new Document();
      oldProp.load(old_in);

      ByteArrayInputStream new_b = new ByteArrayInputStream(new_bytes);
      XMLInputStream new_in = new XMLInputStream(new_b);
      newProp = new Document();
      newProp.load(new_in);
   
    }
    catch (Exception ex) {
      System.out.println("Error Reading XML Document");
      System.out.println("PROPPATCH failed");
      ex.printStackTrace();
        errorMsg("PROPPATCH Failed! \nXML Parsing Error: \n\n" + e);
      return;
    }
    GeneratePropPatch(host,port,res,oldProp,newProp);
  }
  
  public void errorMsg(String str)
  {
    JOptionPane pane = new JOptionPane();
    Object [] options = { "OK" };
    pane.showOptionDialog(mainFrame,str, "Error Message", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,options, options[0]);
  }
}
