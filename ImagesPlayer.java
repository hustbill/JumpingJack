
// ImagesPlayer.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* ImagesPLayer is aimed at displaying the sequence of images
   making up a 'n', 's', or 'g' image file, as loaded by
   ImagesLoader.

   The ImagesPlayer constructor is supplied with the
   intended duration for showing the entire sequence
   (seqDuration). This is used to calculate showPeriod,
   the amount of time each image should be shown before
   the next image is displayed.

   The animation period (animPeriod) input argument states
   how often the ImagesPlayer's updateTick() method will be
   called. The intention is that updateTick() will be called periodically
   from the update() method in the top-level animation framework.

   The current animation time is calculated when updateTick()
   is called, and used to calculate imPosition, imPosition
   specifies which image should be returned when getCurrentImage() 
   is called.

   The ImagesPlayer can be set to cycle, stop, resume, or restart
   at a given image position.

   When the sequence finishes, a callback, sequenceEnded(), can
   be invoked in a specified object implementing the 
   ImagesPlayerWatcher interface.

*/

import java.awt.image.*;


public class ImagesPlayer
{
  private String imName;
  private boolean isRepeating, ticksIgnored;
  private ImagesLoader imsLoader;

  private int animPeriod;
         // period used by animation loop (in ms)
  private long animTotalTime;

  private int showPeriod;     
         // period the current image is shown (in ms)
  private double seqDuration;   
         // total duration of the entire image sequence (in secs)

  private int numImages;
  private int imPosition;     // position of current displayable image

  private ImagesPlayerWatcher watcher = null;


  public ImagesPlayer(String nm, int ap, double d, 
                                boolean isr, ImagesLoader il) 
  {
    imName = nm;
    animPeriod = ap; 
    seqDuration = d;
    isRepeating = isr;
    imsLoader = il;

    animTotalTime = 0L;

    if (seqDuration < 0.5) {
      System.out.println("Warning: minimum sequence duration is 0.5 sec.");
      seqDuration = 0.5;
    }

    if (!imsLoader.isLoaded(imName)) {
      System.out.println(imName + " is not known by the ImagesLoader");
      numImages = 0;
      imPosition = -1;
      ticksIgnored = true;
    }
    else {
      numImages = imsLoader.numImages(imName);
      imPosition = 0;
      ticksIgnored = false;
      showPeriod = (int) (1000 * seqDuration / numImages);
    }
  } // end of ImagesPlayer()



  public void updateTick()
  /* We assume that this method is called every animPeriod ms */
  {
    if (!ticksIgnored) {
      // update total animation time, modulo the animation sequence duration
      animTotalTime = (animTotalTime + animPeriod) % (long)(1000 * seqDuration);

      // calculate current displayable image position
      imPosition = (int) (animTotalTime / showPeriod);   // in range 0 to num-1
      if ((imPosition == numImages-1) && (!isRepeating)) {  // at end of sequence
        ticksIgnored = true;   // stop at this image
        if (watcher != null)
          watcher.sequenceEnded(imName);   // call callback
      }
    }
  }  // end of updateTick()



  public BufferedImage getCurrentImage()
  { if (numImages != 0)
      return imsLoader.getImage(imName, imPosition); 
    else
      return null; 
  } // end of getCurrentImage()


  public int getCurrentPosition()
  {  return imPosition;  }



  public void setWatcher(ImagesPlayerWatcher w)
  {  watcher = w;  }
   

  public void stop()
  /* updateTick() calls will no longer update the
     total animation time or imPosition. */
  {  ticksIgnored = true;  }


  public boolean isStopped()
  {  return ticksIgnored;  }


  public boolean atSequenceEnd()
  // are we at the last image and not cycling through them?
  {  return ((imPosition == numImages-1) && (!isRepeating));  }



  public void restartAt(int imPosn)
  /* Start showing the images again, starting with image number
     imPosn. This requires a resetting of the animation time as 
     well. */
  {
    if (numImages != 0) {
      if ((imPosn < 0) || (imPosn > numImages-1)) {
        System.out.println("Out of range restart, starting at 0");
        imPosn = 0;
      }

      imPosition = imPosn;
      // calculate a suitable animation time
      animTotalTime = (long) imPosition * showPeriod;
      ticksIgnored = false;
    }
  }  // end of restartAt()


  public void resume()
  // start at previous image position
  { 
    if (numImages != 0)
      ticksIgnored = false;
  } 


} // end of ImagesPlayer class
