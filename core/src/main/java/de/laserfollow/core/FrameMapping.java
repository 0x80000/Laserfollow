package de.laserfollow.core;

import de.laserfollow.core.Geometry.Point;
import de.laserfollow.core.Geometry.Region;

/** Immutable mapping between normalized cropped analysis coordinates and preview coordinates. */
public final class FrameMapping {
    private final double a,b,c,d,x,y,det;

    public FrameMapping(double a,double b,double c,double d,double x,double y) {
        if(!Double.isFinite(a+b+c+d+x+y)) throw new IllegalArgumentException("Ungültige Bildzuordnung");
        this.a=a; this.b=b; this.c=c; this.d=d; this.x=x; this.y=y;
        det=a*d-b*c;
        if(!Double.isFinite(det) || Math.abs(det)<1e-10) throw new IllegalArgumentException("Nicht umkehrbare Bildzuordnung");
    }

    /** Android Matrix values, with source coordinates relative to the unrotated crop origin. */
    public static FrameMapping pixelMatrix(float[] m,int sourceWidth,int sourceHeight,int viewWidth,int viewHeight) {
        for(float value:m) if(!Float.isFinite(value)) throw new IllegalArgumentException("Ungültige Bildmatrix");
        if(m.length!=9 || sourceWidth<=0 || sourceHeight<=0 || viewWidth<=0 || viewHeight<=0 ||
            Math.abs(m[6])+Math.abs(m[7])>1e-8 || Math.abs(m[8]-1)>1e-8)
            throw new IllegalArgumentException("Ungültige Bildgeometrie");
        return new FrameMapping(m[0]*sourceWidth/(double)viewWidth,m[1]*sourceHeight/(double)viewWidth,
            m[3]*sourceWidth/(double)viewHeight,m[4]*sourceHeight/(double)viewHeight,
            m[2]/(double)viewWidth,m[5]/(double)viewHeight);
    }

    public Point toPreview(Point p) { return new Point(a*p.x()+b*p.y()+x,c*p.x()+d*p.y()+y); }
    public Point toSource(Point p) {
        double px=p.x()-x,py=p.y()-y;
        return new Point((d*px-b*py)/det,(-c*px+a*py)/det);
    }

    public Region sourceRegion(Region r) {
        Point[] corners={toSource(new Point(r.left(),r.top())),toSource(new Point(r.right(),r.top())),
            toSource(new Point(r.right(),r.bottom())),toSource(new Point(r.left(),r.bottom()))};
        double left=1,top=1,right=0,bottom=0;
        for(Point p:corners) {
            if(!Double.isFinite(p.x()+p.y()) || p.x()<-.01 || p.x()>1.01 || p.y()<-.01 || p.y()>1.01)
                throw new IllegalArgumentException("Bereich außerhalb des Kamerabilds");
            left=Math.min(left,Geometry.clamp(p.x())); top=Math.min(top,Geometry.clamp(p.y()));
            right=Math.max(right,Geometry.clamp(p.x())); bottom=Math.max(bottom,Geometry.clamp(p.y()));
        }
        return new Region(left,top,right,bottom);
    }
}
