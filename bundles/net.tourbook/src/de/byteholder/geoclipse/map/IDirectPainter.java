package de.byteholder.geoclipse.map;

public interface IDirectPainter {

   /**
    * Dispose resources in the {@link IDirectPainter}
    */
   public abstract void dispose();

   /**
    * The paint method is called when the map get's an onPaint event. Therefore this method should
    * be optimized that it takes a short time
    * 
    * @param directMappingPainterContext
    */
   public abstract void paint(DirectPainterContext directPainterContext);
}
