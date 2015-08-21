
// ClipsLoader.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* ClipsLoader  stores a collection of ClipInfo objects
   in a HashMap whose keys are their names. 

   The name and filename for a clip is obtained from a sounds
   information file which is loaded when ClipsLoader is created.
   The information file is assumed to be in Sounds/.

   ClipsLoader allows a specified clip to be played, stopped, 
   resumed, looped. A SoundsWatcher can be attached to a clip.
   All of this functionality is handled in the ClipInfo object; 
   ClipsLoader simply redirects the method call to the right ClipInfo.

   It is possible for many clips to play at the same time, since
   each ClipInfo object is responsible for playing its clip.
*/


import java.awt.*;
import java.util.*;
import java.io.*;


public class ClipsLoader
{
  private final static String SOUND_DIR = "Sounds/";

  private HashMap clipsMap; 
    /* The key is the clip 'name', the object (value) 
       is a ClipInfo object */


  public ClipsLoader(String soundsFnm)
  { clipsMap = new HashMap();
    loadSoundsFile(soundsFnm);
  }

  public ClipsLoader()
  {  clipsMap = new HashMap();  } 



  private void loadSoundsFile(String soundsFnm)
  /* The file format are lines of:
        <name> <filename>         // a single sound file
     and blank lines and comment lines.
  */
  { 
    String sndsFNm = SOUND_DIR + soundsFnm;
    System.out.println("Reading file: " + sndsFNm);
    try {
      InputStream in = this.getClass().getResourceAsStream(sndsFNm);
      BufferedReader br = new BufferedReader( new InputStreamReader(in));
      // BufferedReader br = new BufferedReader( new FileReader(sndsFNm));
      StringTokenizer tokens;
      String line, name, fnm;
      while((line = br.readLine()) != null) {
        if (line.length() == 0)  // blank line
          continue;
        if (line.startsWith("//"))   // comment
          continue;

        tokens = new StringTokenizer(line);
        if (tokens.countTokens() != 2)
          System.out.println("Wrong no. of arguments for " + line);
        else {
          name = tokens.nextToken();
          fnm = tokens.nextToken();
          load(name, fnm);
        }
      }
      br.close();
    } 
    catch (IOException e) 
    { System.out.println("Error reading file: " + sndsFNm);
      System.exit(1);
    }
  }  // end of loadSoundsFile()



  // ----------- manipulate a particular clip --------


  public void load(String name, String fnm)
  // create a ClipInfo object for name and store it
  {
    if (clipsMap.containsKey(name))
      System.out.println( "Error: " + name + "already stored");
    else {
      clipsMap.put(name, new ClipInfo(name, fnm) );
      System.out.println("-- " + name + "/" + fnm);
    }
  }  // end of load()


  public void close(String name)
  // close the specified clip
  {  ClipInfo ci = (ClipInfo) clipsMap.get(name);
     if (ci == null)
       System.out.println( "Error: " + name + "not stored");
     else
      ci.close();
  } // end of close()

 

  public void play(String name, boolean toLoop)
  // play (perhaps loop) the specified clip
  {  ClipInfo ci = (ClipInfo) clipsMap.get(name);
     if (ci == null)
       System.out.println( "Error: " + name + "not stored");
     else
      ci.play(toLoop);
  } // end of play()


  public void stop(String name)
  // stop the clip, resetting it to the beginning
  { ClipInfo ci = (ClipInfo) clipsMap.get(name);
    if (ci == null)
      System.out.println( "Error: " + name + "not stored");
    else
      ci.stop();
  } // end of stop()


  public void pause(String name)
  { ClipInfo ci = (ClipInfo) clipsMap.get(name);
    if (ci == null)
      System.out.println( "Error: " + name + "not stored");
    else
      ci.pause();
  } // end of pause()


  public void resume(String name)
  { ClipInfo ci = (ClipInfo) clipsMap.get(name);
    if (ci == null)
      System.out.println( "Error: " + name + "not stored");
    else
      ci.resume();
  } // end of resume()


  // -------------------------------------------------------


  public void setWatcher(String name, SoundsWatcher sw)
  /* Set up a watcher for the clip. It will be notified when
     the clip loops or stops. */
  { ClipInfo ci = (ClipInfo) clipsMap.get(name);
    if (ci == null)
      System.out.println( "Error: " + name + "not stored");
    else
      ci.setWatcher(sw);
  } // end of setWatcher()

}  // end of ClipsLoader class
