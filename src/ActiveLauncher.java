// -------------------------------------------------------------------------------------------------
// Copyright (c) 2004 Lawrence Dol.
// -------------------------------------------------------------------------------------------------

// UNPACKAGED TO ALLOW CLASS TO BE RUN DIRECTLY FROM DISK

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Window;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

public class ActiveLauncher
extends Window
{

// *************************************************************************************************
// INSTANCE PROPERTIES
// *************************************************************************************************

private Insets                          insets;                                                     // window insets
private Label                           progressLabel1;                                             // progress label component 1
private Label                           progressLabel2;                                             // progress label component 2
private Image                           dbImage;                                                    // double-buffering image
private Dimension                       dbSize;                                                     // double-buffering size
private List<URL>                       classpath=new ArrayList<URL>();                             // class path elements

// *************************************************************************************************
// INSTANCE CONSTRUCTION/INITIALIZATON/FINALIZATION, OPEN/CLOSE
// *************************************************************************************************

private ActiveLauncher() {
    super(new Frame("Launcher"));

    Label                               ttl=new Label(properties.getProperty("window.title","Active Launcher"),Label.CENTER);

    progressLabel1=new Label("X",Label.CENTER);
    progressLabel2=new Label("X",Label.CENTER);

    dbImage=null;
    dbSize=new Dimension(0,0);

    if(properties.getProperty("window.show","true").equalsIgnoreCase("true")) {
        Dimension                       ss;                                                         // screen size
        Panel                           pp;                                                         // progress panel

        ss=getToolkit().getScreenSize();
        insets=new Insets(4,4,4,4);
        setBackground(new Color(255,255,245));

        ttl           .setFont(new Font("SansSerif",Font.BOLD ,20));
        progressLabel1.setFont(new Font("SansSerif",Font.PLAIN,12));
        progressLabel2.setFont(new Font("SansSerif",Font.PLAIN,10));
        pp=new Panel(new BorderLayout());
        pp.add(ttl,BorderLayout.NORTH);
        pp.add(progressLabel1,BorderLayout.CENTER);
        pp.add(progressLabel2,BorderLayout.SOUTH);
        add(pp,BorderLayout.NORTH);

        addNotify(); // do not pack!
        setSize((ss.width<320 ? ss.width : 320),getPreferredSize().height);
        setLocation(((ss.width-getSize().width)/2),((ss.height-getSize().height)/2));
        setVisible(true);
        toFront();
        }

    progress("Preparing ActiveLauncher",null);
    }

// *************************************************************************************************
// INSTANCE METHODS - GUI
// *************************************************************************************************

public void paint(Graphics ctx) {
    Dimension                           cs=getSize();                                               // component size
    Graphics                            dc;                                                         // drawing context

    if(dbImage==null || dbSize.width!=cs.width || dbSize.height!=cs.height) {
        dbImage=createImage(cs.width,cs.height);
        dbSize.width=cs.width;
        dbSize.height=cs.height;
        }
    if(dbImage!=null) { dc=dbImage.getGraphics(); }
    else              { dc=ctx;                   }
    try {
        dc.setColor(Color.black);
        dc.drawRect(0,0,(cs.width-1),(cs.height-1));
        dc.drawRect(1,1,(cs.width-3),(cs.height-3));

        super.paint(dc);

        if(dc!=ctx) { ctx.drawImage(dbImage,0,0,null); }
        }
    finally {
        if(dc!=ctx) { dc.dispose(); }
        }
    }

public Insets getInsets() {
    return insets;
    }

private void progress(String p1, String p2) {
    detailedProgress(p1,p2);
    log("  - "+p1+(p2!=null ? (": "+p2) : ""));
    }

private void detailedProgress(String p1, String p2) {
    if(p1!=null) { progressLabel1.setText(p1);         }
    if(p2!=null) { progressLabel2.setText("("+p2+")"); }
    }

// *************************************************************************************************
// INSTANCE METHODS - UPDATE
// *************************************************************************************************

private void update() throws Exception {
    String                              flu;                                                        // file list url
    File                                flf;                                                        // file list file
    String                              uou;                                                        // update object url
    String[]                            fl;                                                         // file list

    log("Updating Files.");

    flu=properties.getProperty("file.list.url",null).replace('\\','/');
    flf=updateFile(flu,launcherPath,"ActiveLauncherList.txt",true);
    if(flf!=null) {
        fl=readList(flf);
        uou=(flu.substring(0,(flu.lastIndexOf('/')+1)));
        for(int xa=0; xa<fl.length; xa++) {
            String fnm=fl[xa];

            if(fnm.length()>="*DELETE ".length() && fnm.substring(0,"*DELETE ".length()).equalsIgnoreCase("*DELETE ")) {
                fnm=fnm.substring("*DELETE ".length());
                File fil=new File(launcherPath,fnm);
                if(fil.exists()) {
                    progress("Deleting Old File",fnm);
                    delete(fil);
                    }
                fl[xa]=null;
                }
            }
        for(int xa=0; xa<fl.length; xa++) {
            String fnm=fl[xa];

            if(fnm!=null) {
                File to=updateFile((uou+fnm),launcherPath,fnm,false);
                if(to.getCanonicalPath().endsWith(".jar")) { classpath.add(to.getCanonicalFile().toURI().toURL()); }
                }
            }

        loadProperties();
        }
    }

private File updateFile(String url, String tgtdir, String tgtsub, boolean dldlst) {
    URL                                 uo;                                                         // url object
    URLConnection                       uc;                                                         // url connection
    File                                ito;                                                        // intermediate target object
    File                                fto;                                                        // final target object
    File                                oto;                                                        // old target object

    progress("Checking",tgtsub);
    fto=new File(tgtdir,tgtsub);
    ito=new File(fto.getAbsolutePath()+".new");
    oto=new File(fto.getAbsolutePath()+".old");
    if(ito.exists()) { delete(ito); }
    if(oto.exists()) { delete(oto); }
    try {
        uo=new URL(url);
        uc=uo.openConnection();
        if(fto.exists()) { uc.setIfModifiedSince(fto.lastModified()); }
        uc.setDoInput(true);
        uc.setDoOutput(false);
        uc.setAllowUserInteraction(true);
        uc.setUseCaches(false);
        if(dldlst) { try { uc.setConnectTimeout(5000); } catch(Throwable thr) {;} }                 // ignore; Java 5 API
        if(uo.toExternalForm().startsWith("file:")) {
            try { uc.connect(); } catch(FileNotFoundException thr) { uc=null; }
            if(uc!=null && fto.lastModified()!=uc.getLastModified()) {
                downloadFile(uc,ito,fto,oto,tgtsub);
                }
            }
        else if(uc instanceof HttpURLConnection) {                                                  // includes HttpsUrlConnection
            ((HttpURLConnection)uc).setInstanceFollowRedirects(true);
            uc.connect();
            if(((HttpURLConnection)uc).getResponseCode()==200) {
                downloadFile(uc,ito,fto,oto,tgtsub);
                }
            }
        }
    catch(java.net.SocketException thr) {
        log("  - ERROR: "+thr.toString());
        if(dldlst) { fto=null; }
        }
    catch(java.io.IOException thr) {
        log("  - ERROR: "+thr.toString());
        if(dldlst) { fto=null; }
        }
    catch(Throwable thr) {
        stackTrace(thr);
        if(dldlst) { fto=null; }
        }
    return fto;
    }

private void downloadFile(URLConnection uc, File ito, File fto, File oto, String tgtsub) {
    InputStream                         is=null;                                                    // input stream
    OutputStream                        os=null;                                                    // output stream
    int                                 cl,cd;                                                      // content length, content downloaded
    long                                ms;

    try {
        byte[]                          rb;
        int                             rc;

        log("  - Downloading: "+uc.getURL().toExternalForm());

        cl=uc.getContentLength();
        is=uc.getInputStream();
        os=new FileOutputStream(ito);
        progress("Updating",tgtsub);
        rb=new byte[10240]; cd=0; ms=System.currentTimeMillis();
        while((rc=is.read(rb))!=-1) {
            os.write(rb,0,rc);
            cd+=rc;
            if((System.currentTimeMillis()-ms)>1000) {
                if(cl>0) { detailedProgress("Updating",(tgtsub+" - "+percent(cd,cl))); }
                else     { detailedProgress("Updating",(tgtsub+" - "+cd+" bytes"   )); }
                ms=System.currentTimeMillis();
                }
            }
        os.close();
        is.close();

        //CHGME: VALIDATE DIGITAL SIGNATURE HERE

        renameTo(fto,oto);
        renameTo(ito,fto);
        setLastModified(fto,uc.getLastModified());
        delete(oto);
        }
    catch(Throwable thr) {
        stackTrace(thr);
        }
    finally { close(is); close(os); }
    }

private String percent(int cur, int tot) {
    return (((cur*100L)/tot)+"%");
    }

// *************************************************************************************************
// INSTANCE METHODS - LAUNCH
// *************************************************************************************************

private void launch(String[] args) throws Exception {
    String                              tcn = properties.getProperty("target.class",null);                              // target class name

    if(args.length>0 && args[0].equals("--cmd")) {
        args=Arrays.copyOfRange(args,1,args.length);
        tcn=null;
        }

    if(tcn!=null) {
        String                              tpf;                                                                        // target params file

        progress("Launching by Application Class",tcn);

        if(tcn.endsWith(".jar")) {
            File jar=new File(launcherPath,tcn).getCanonicalFile();
            URL  url=jar.toURI().toURL();
            classpath.remove(url);
            classpath.add(0,url);
            tcn=getJarMainClass(jar);
            }
        if((tpf=properties.getProperty("target.parm.file",null))!=null && tpf.length()!=0) {
            String[] rpl=new String[args.length+1];
            System.arraycopy(args,0,rpl,1,args.length);
            rpl[0]=('@'+tpf);
            args=rpl;
            }

        StringBuilder tmp=new StringBuilder();
        for(int xa=0; xa<args.length; xa++) {
            tmp.append(' ').append(args[xa]);
            }

        setVisible(false);

        progress("Launching Application",(tcn+tmp));
        Class<?> cls=new URLClassLoader(classpath.toArray(new URL[classpath.size()])).loadClass(tcn);
        Method   mth=cls.getMethod("main",String[].class);
        mth.invoke(null,new Object[] { args });
        }
    else if(args.length!=0) {
        StringBuilder tmp=new StringBuilder();
        for(int xa=0; xa<args.length; xa++) {
            tmp.append(' ').append(args[xa]);
            if(args[xa].startsWith("\"") || args[xa].endsWith("\"")) { args[xa] = args[xa].substring(1,args[xa].length()-1); }
            }
        progress("Launching by Arguments",tmp.toString().trim());
        Runtime.getRuntime().exec(args);
        System.exit(0);
        }
    else {
        log("Neither target class (target.class) nor arguments specified; exiting.");
        System.exit(0);
        }
    }

// *************************************************************************************************
// INSTANCE INNER CLASSES
// *************************************************************************************************

// *************************************************************************************************
// STATIC NESTED CLASSES
// *************************************************************************************************

// *************************************************************************************************
// STATIC PROPERTIES
// *************************************************************************************************

static private final String             UTF8="UTF-8";                                               // the UTF-8 encoding string (different in different VMs!!!)

static private Properties               properties;
static private String                   launcherPath;
static private String                   launcherLog;

// *************************************************************************************************
// STATIC METHODS -UTILITY
// *************************************************************************************************

static private void loadProperties() {
    String                              prpfil;                                                     // property file

    properties=new Properties(System.getProperties());
    if((prpfil=System.getProperty("activelauncher.props",null))==null) {
        prpfil=(launcherPath+"/ActiveLauncherProps.txt");
        }
    log("  - Loading properties from \""+prpfil+"\".");
    FileInputStream fis=null;
    try {
        fis=new FileInputStream(prpfil);
        properties.load(fis);
        }
    catch(Throwable thr) {
        log("ERROR: Could not load properties from '"+prpfil+"': "+thr);
        }
    finally { close(fis); }
    }

static private void log(String txt) {
    PrintWriter                         lw=null;                                                    // log writer
    String                              lt;                                                         // log text

    System.out.println(txt);
    try {
        lw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(launcherLog,true),UTF8));
        lw.println(txt);
        }
    catch(Throwable thr) {;}
    finally { close(lw); }
    }

static private void stackTrace(Throwable thr) {
    PrintWriter                         lw=null;                                                    // log writer
    String                              lt;                                                         // log text

    thr.printStackTrace(System.out);
    try {
        lw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(launcherLog,true),UTF8));
        thr.printStackTrace(lw);
        }
    catch(Throwable thr2) {;}
    finally { close(lw); }
    }

static public String[] readList(File file) {
    BufferedReader                      isr=null;                                                   // input reader
    String                              txt;                                                        // text line
    ArrayList<String>                   al;                                                         // array list

    al=new ArrayList<String>();
    try {
        isr=new BufferedReader(new InputStreamReader(new FileInputStream(file),UTF8));
        while((txt=isr.readLine())!=null) {
            if(txt.length()>1 && txt.charAt(0)=='/' && txt.charAt(1)=='/') { continue; }

            txt=txt.replace('\\','/');
            for(int xa; txt.length()>0 && (xa=txt.indexOf("../" ))!=-1; ) {
                txt=(txt.substring(0,xa)+txt.substring(xa+3));
                }
            while(txt.length()>0 && txt.charAt(0)=='/') {
                txt=txt.substring(1);
                }

            if(txt.length()==0) { continue; }
            al.add(txt);
            }
        }
    catch(IOException thr) {
        log("  - Error reading file list from "+file+": "+thr);
        }
    finally {
        close(isr);
        }
    return al.toArray(new String[al.size()]);
    }

static void close(InputStream is) {
    try { if(is!=null) { is.close(); } } catch(Throwable thr) {;}
    }

static void close(OutputStream os) {
    try { if(os!=null) { os.close(); } } catch(Throwable thr) {;}
    }

static void close(Reader ir) {
    try { if(ir!=null) { ir.close(); } } catch(Throwable thr) {;}
    }

static void close(Writer ow) {
    try { if(ow!=null) { ow.close(); } } catch(Throwable thr) {;}
    }

static public String getJarMainClass(File jarfil) throws IOException {
    JarFile                             jar=new JarFile(jarfil);

    try {
        Manifest   mnf=jar.getManifest();
        Attributes atr=(mnf!= null ? mnf.getMainAttributes() : null);
        return (atr!=null ? atr.getValue(Attributes.Name.MAIN_CLASS) : null);
        }
    finally {
        try { jar.close(); } catch(Throwable thr) { System.out.println("Error closing application JAR file: "+thr); }
        }
    }

// *************************************************************************************************
// STATIC MAIN
// *************************************************************************************************

static public void main(String[] args) {
    try {
        System.out.println("Starting ActiveLauncher");

        launcherPath=new File(ClassLoader.getSystemResource("ActiveLauncher.class").toURI()).getPath().replace('\\','/');
        launcherPath=launcherPath.substring(0,launcherPath.lastIndexOf('/'));
        launcherLog=(launcherPath+File.separatorChar+"ActiveLauncherLog.txt");
        delete(new File(launcherLog));

        log("ActiveLauncher (Version "+getVersion() + ")");
        loadProperties();

        ActiveLauncher lch=new ActiveLauncher();                                                                        // launcher
        lch.update();
        lch.launch(args);
        }
    catch(Throwable thr) {
        stackTrace(thr);
        System.exit(1);
        }
    }

// *************************************************************************************************
// STATIC METHODS -UTILITY
// *************************************************************************************************

private static void delete(File fil) {
    if(!fil.delete() && fil.exists()) { log("Could not delete file: "+fil); }
    }

private static void renameTo(File fil, File tgt) {
    delete(tgt);
    if(!fil.renameTo(tgt) && fil.exists()) { log("Could not rename file: Source="+fil+", Target="+tgt); }
    }

private static void setLastModified(File fil, long mod) {
    if(!fil.setLastModified(mod)) { log("Could not set modified timestamp on file: "+fil); }
    }

// *************************************************************************************************
// STATIC METHODS -VERSION
// *************************************************************************************************

/// Get the version string.
static public String getVersion() {
    final String                        VERSION="1.00"; /*2025.0222.0038*/                                              // version (the commented datestamp is when the version was last updated)
    return VERSION;
    }

} // END CLASS
