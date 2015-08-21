
// ImagesPlayerWatcher.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* When an ImagesPlayer gets to the end of a sequence, it can
   call sequenceEnded() in a listener.
*/

public interface ImagesPlayerWatcher 
{
    void sequenceEnded(String imageName);
}

