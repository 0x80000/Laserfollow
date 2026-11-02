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
    private final Path path=new Path();
    private Region cachedRegion;
    private Shape cachedShape;
    private double cachedSize=-1;
    private int cachedWidth=-1,cachedHeight=-1;
    private boolean cachedDemo;
    private record Sample(Point point,boolean start) {}
    final ArrayDeque<Sample> trail=new ArrayDeque<>();
    private boolean gap=true;
    Region region=new Region(.12,.12,.88,.88);
    Shape shape=Shape.CIRCLE;
    double size=.7,phase;
    Point spot,first;
    boolean demo=true;
    Tap tap;
    ExerciseView(Context c) { super(c); setMinimumHeight(260); setContentDescription("Kamerabild mit Übungsbahn; zum Kalibrieren zwei gegenüberliegende Ecken antippen"); }
    void clearFrame() { spot=null; trail.clear(); gap=true; invalidate(); }
    double imageAspect() { return demo?4.0/3.0:getWidth()/(double)Math.max(1,getHeight()); }
    void observation(Point p) { spot=p; if(p==null) gap=true; else { trail.add(new Sample(p,gap)); gap=false; while(trail.size()>3000) trail.remove(); } invalidate(); }
    private float px(double x) { return image.left+(float)x*image.width(); }
    private float py(double y) { return image.top+(float)y*image.height(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if(demo) {
            c.drawColor(Color.rgb(12,25,35));
            float w=Math.min(getWidth(),getHeight()*4f/3),h=w*3/4;
            image.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
            paint.setColor(0xff20333f); paint.setStrokeWidth(1);
            float grid=32*getResources().getDisplayMetrics().density;
            for(float x=0;x<getWidth();x+=grid)c.drawLine(x,0,x,getHeight(),paint);
            for(float y=0;y<getHeight();y+=grid)c.drawLine(0,y,getWidth(),y,paint);
        } else image.set(0,0,getWidth(),getHeight());
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(0xff8a9bb4);
        if(region!=null) {
            c.drawRect(px(region.left()),py(region.top()),px(region.right()),py(region.bottom()),paint);
            if(!region.equals(cachedRegion) || shape!=cachedShape || size!=cachedSize ||
                getWidth()!=cachedWidth || getHeight()!=cachedHeight || demo!=cachedDemo) {
                path.reset();
                for(int i=0;i<=240;i++) { Point p=Geometry.point(shape,i/240.0,region,size,imageAspect()); if(i==0) path.moveTo(px(p.x()),py(p.y())); else path.lineTo(px(p.x()),py(p.y())); }
                cachedRegion=region; cachedShape=shape; cachedSize=size;
                cachedWidth=getWidth(); cachedHeight=getHeight(); cachedDemo=demo;
            }
            paint.setColor(0xff68e0c5); paint.setStrokeWidth(4); c.drawPath(path,paint);
            Point target=Geometry.point(shape,phase,region,size,imageAspect()); paint.setStyle(Paint.Style.FILL); paint.setColor(0xffffd381); c.drawCircle(px(target.x()),py(target.y()),10,paint);
        }
        paint.setStyle(Paint.Style.STROKE); paint.setColor(0xff84bfff);
        paint.setStrokeWidth(2.5f*getResources().getDisplayMetrics().density);
        Point previous=null;
        for(Sample sample:trail) {
            Point p=sample.point();
            if(previous!=null && !sample.start()) c.drawLine(px(previous.x()),py(previous.y()),px(p.x()),py(p.y()),paint);
            else c.drawPoint(px(p.x()),py(p.y()),paint);
            previous=p;
        }
        paint.setStyle(Paint.Style.FILL);
        if(spot!=null) { paint.setColor(Color.WHITE); c.drawCircle(px(spot.x()),py(spot.y()),7,paint); }
        if(first!=null) { paint.setColor(0xffffd381); c.drawCircle(px(first.x()),py(first.y()),8,paint); }
        paint.setColor(Color.WHITE); paint.setTextSize(30*getResources().getDisplayMetrics().scaledDensity/2);
        c.drawText(demo?"Demo – simulierte Daten":"Kamera · fester Bildbereich",image.left+12,image.top+24,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_UP && tap!=null && image.contains(e.getX(),e.getY())) {
            performClick(); tap.at(new Point((e.getX()-image.left)/image.width(),(e.getY()-image.top)/image.height())); return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
