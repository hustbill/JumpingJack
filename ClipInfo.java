
// ClipInfo.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* Load a clip, which can be played, stopped, resumed, looped. 

   An object implementing the SoundsWatcher interface 
   can be notified when the clip loops or stops.

   Changes 9th August 2004
     - closed the input stream in loadClip()

   Changes 15th September 2004
     - bug: if the supplied WAV file is less than a second long then 
            no sound is played. See the bug report at 
            http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5085008
            and SoundPlayer/PlayClipBF.java

     Solution: make all sounds > 1 sec.
               added clip.setFramePosition(0) to update()

     added checkDuration() and DecimalFormat df
*/

import java.io.*;
import javax.sound.sampled.*;
import java.text.DecimalFormat;


public class ClipInfo implements LineListener
{
  private final static String SOUND_DIR = "Sounds/";

  private String name, filename;
  private Clip clip = null;
  private boolean isLooping = false;
  private SoundsWatcher watcher = null;
  private DecimalFormat df; 


  public ClipInfo(String nm, String fnm)
  { name = nm;
    filename = SOUND_DIR + fnm;
    df = new DecimalFormat("0.#");  // 1 dp

    loadClip(filename);
  } // end of ClipInfo()


  private void loadClip(String fnm)
  {
    try {
      // link an audio stream to the sound clip's file
      AudioInputStream stream = AudioSystem.getAudioInputStream(
                          getClass().getResource(fnm) );

      AudioFormat format = stream.getFormat();

      // convert ULAW/ALAW formats to PCM format
      if ( (format.getEncoding() == AudioFormat.Encoding.ULAW) ||
           (format.getEncoding() == AudioFormat.Encoding.ALAW) ) {
        AudioFormat newFormat = 
           new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                format.getSampleRate(),
                                format.getSampleSizeInBits()*2,
                                format.getChannels(),
                                format.getFrameSize()*2,
                                format.getFrameRate(), true);  // big endian
        // update stream and format details
        stream = AudioSystem.getAudioInputStream(newFormat, stream);
        System.out.println("Converted Audio format: " + newFormat);
        format = newFormat;
      }

      DataLine.Info info = new DataLine.Info(Clip.class, format);

      // make sure sound system supports data line
      if (!AudioSystem.isLineSupported(info)) {
        System.out.println("Unsupported Clip File: " + fnm);
        return;
      }

      // get clip line resource
      clip = (Clip) AudioSystem.getLine(info);

      // listen to clip for events
      clip.addLineListener(this);

      clip.open(stream);    // open the sound file as a clip
      stream.close(); // we're done with the input stream
     
      checkDuration();
    } // end of try block

    catch (UnsupportedAudioFileException audioException) {
      System.out.println("Unsupported audio file: " + fnm);
    }
    catch (LineUnavailableException noLineException) {
      System.out.println("No audio line available for : " + fnm);
    }
    catch (IOException ioException) {
      System.out.println("Could not read: " + fnm);
    }
    catch (Exception e) {
      System.out.println("Problem with " + fnm);
    }
  } // end of loadClip()


  private void checkDuration()
  {
    // duration (in secs) of the clip
	double duration = clip.getMicrosecondLength()/1000000.0;  // new
    if (duration <= 1.0) {
      System.out.println("WARNING. Duration <= 1 sec : " + df.format(duration) + " secs");
      System.out.println("         The clip in " + filename + 
                         " may not play in J2SE 1.5 -- make it longer");
    }
    else
      System.out.println(filename + ": Duration: " + df.format(duration) + " secs");
  }  // end of checkDuration()



  public void update(LineEvent lineEvent)
  /* Called when the clip's line detects open, close, start, or
     stop events. The watcher (if one exists) is notified.
  */
  {
    // when clip is stopped / reaches its end
    if (lineEvent.getType() == LineEvent.Type.STOP) {
      // System.out.println("update() STOP for " + name);
      clip.stop();
      clip.setFramePosition(0);  // NEW
      if (!isLooping) {  // it isn't looping
        if (watcher != null)
          watcher.atSequenceEnd(name, SoundsWatcher.STOPPED);
      }
      else {      // else play it again
        clip.start();
        if (watcher != null)
          watcher.atSequenceEnd(name, SoundsWatcher.REPLAYED);
      }
    }
  } // end of update()


  public void close()
  { if (clip != null) {
      clip.stop();
      clip.close();
    }
  }


  public void play(boolean toLoop)
  { if (clip != null) {
      isLooping = toLoop;
      clip.start(); // start playing
    }
  }

  public void stop()
  // stop and reset clip to its start
  { if (clip != null) {
      isLooping = false;
      clip.stop();
      clip.setFramePosition(0);
    }
  }

  public void pause()
  // stop the clip at its current playing position
  { if (clip != null)
      clip.stop();
  }

  public void resume()
  { if (clip != null)
      clip.start();
  }


  public void setWatcher(SoundsWatcher sw)
  {  watcher = sw;  }


  // -------------- other access methods -------------------

  public String getName()
  {  return name;  }

}  // end of ClipInfo class
