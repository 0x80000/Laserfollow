package de.laserfollow.app;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import de.laserfollow.core.Geometry;
import de.laserfollow.core.Geometry.*;
import de.laserfollow.core.Geometry.Point;
import de.laserfollow.core.Geometry.Region;
import java.util.ArrayDeque;

final class ExerciseView extends View {
    interface Tap { void at(Point point); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF image=new RectF();
    private Bitmap bitmap;
    final ArrayDeque<Point> trail=new ArrayDeque<>();
    Region region=new Region(.12,.12,.88,.88);
    Shape shape=Shape.CIRCLE;
    double size=.7,phase;
    Point spot,first;
    boolean demo=true;
    Tap tap;
    ExerciseView(Context c) { super(c); setMinimumHeight(260); setContentDescription("Kamerabild mit Übungsbahn; zum Kalibrieren zwei gegenüberliegende Ecken antippen"); }
    void frame(Bitmap next) { Bitmap old=bitmap; bitmap=next; if(old!=null && old!=next) old.recycle(); invalidate(); }
    void clearFrame() { frame(null); spot=null; trail.clear(); }
    double imageAspect() { return bitmap==null?4.0/3.0:bitmap.getWidth()/(double)bitmap.getHeight(); }
    void observation(Point p) { spot=p; if(p==null) trail.clear(); else { trail.add(p); while(trail.size()>16) trail.remove(); } invalidate(); }
    private float px(double x) { return image.left+(float)x*image.width(); }
    private float py(double y) { return image.top+(float)y*image.height(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(Color.rgb(17,30,49));
        float ratio=bitmap==null?4f/3:bitmap.getWidth()/(float)bitmap.getHeight();
        float w=Math.min(getWidth(),getHeight()*ratio),h=w/ratio;
        image.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
        if(bitmap!=null) c.drawBitmap(bitmap,null,image,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(0xff8a9bb4);
        if(region!=null) {
            c.drawRect(px(region.left()),py(region.top()),px(region.right()),py(region.bottom()),paint);
            Path path=new Path();
            for(int i=0;i<=240;i++) { Point p=Geometry.point(shape,i/240.0,region,size,imageAspect()); if(i==0) path.moveTo(px(p.x()),py(p.y())); else path.lineTo(px(p.x()),py(p.y())); }
            paint.setColor(0xff54d6c7); paint.setStrokeWidth(4); c.drawPath(path,paint);
            Point target=Geometry.point(shape,phase,region,size,imageAspect()); paint.setStyle(Paint.Style.FILL); paint.setColor(0xffffd381); c.drawCircle(px(target.x()),py(target.y()),10,paint);
        }
        paint.setStyle(Paint.Style.FILL); paint.setColor(0x9987c9ff);
        for(Point p:trail) c.drawCircle(px(p.x()),py(p.y()),4,paint);
        if(spot!=null) { paint.setColor(Color.WHITE); c.drawCircle(px(spot.x()),py(spot.y()),7,paint); }
        if(first!=null) { paint.setColor(0xffffd381); c.drawCircle(px(first.x()),py(first.y()),8,paint); }
        paint.setColor(Color.WHITE); paint.setTextSize(30*getResources().getDisplayMetrics().scaledDensity/2);
        c.drawText(demo?"Demo – simulierte Daten":"Kamera · nur live",image.left+12,image.top+24,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_UP && tap!=null && image.contains(e.getX(),e.getY())) {
            performClick(); tap.at(new Point((e.getX()-image.left)/image.width(),(e.getY()-image.top)/image.height())); return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
