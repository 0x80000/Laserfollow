package de.laserfollow.core;

public final class Geometry {
    private Geometry() {}
    public record Point(double x, double y) {
        public double distance(Point p) { return Math.hypot(x-p.x, y-p.y); }
    }
    public record Region(double left, double top, double right, double bottom) {
        public Region {
            if (!Double.isFinite(left+top+right+bottom) || left<0 || top<0 || right>1 || bottom>1 || right-left<.1 || bottom-top<.1)
                throw new IllegalArgumentException("Bereich zu klein oder ungültig");
        }
        public static Region corners(Point a, Point b) {
            return new Region(clamp(Math.min(a.x,b.x)), clamp(Math.min(a.y,b.y)), clamp(Math.max(a.x,b.x)), clamp(Math.max(a.y,b.y)));
        }
        public double diagonal() { return Math.hypot(right-left,bottom-top); }
        public double diagonal(double aspect) { return Math.hypot((right-left)*aspect,bottom-top); }
        public boolean contains(double x,double y) { return x>=left && x<=right && y>=top && y<=bottom; }
    }
    public enum Shape { CIRCLE, LINE, EIGHT }
    public static double clamp(double v) { return Math.max(0,Math.min(1,v)); }
    public static Point point(Shape shape, double phase, Region r, double size) {
        return point(shape,phase,r,size,1);
    }
    public static Point point(Shape shape, double phase, Region r, double size, double aspect) {
        if(!Double.isFinite(aspect) || aspect<=0) throw new IllegalArgumentException("Bildverhältnis");
        double t=2*Math.PI*(phase-Math.floor(phase)), x=0,y=0;
        switch(shape) {
            case CIRCLE -> { x=Math.cos(t); y=Math.sin(t); }
            case LINE -> x=1-4*Math.abs((phase-Math.floor(phase))-.5);
            case EIGHT -> { x=Math.sin(t); y=Math.sin(2*t); }
        }
        double rx=(r.right-r.left)*size/2,ry=(r.bottom-r.top)*size/2;
        if(shape==Shape.CIRCLE) { double radius=Math.min(rx*aspect,ry); rx=radius/aspect; ry=radius; }
        return new Point((r.left+r.right)/2+x*rx,(r.top+r.bottom)/2+y*ry);
    }
    public static Point project(Point p, Point a, Point b) {
        double dx=b.x-a.x,dy=b.y-a.y,d=dx*dx+dy*dy;
        double t=d==0?0:clamp(((p.x-a.x)*dx+(p.y-a.y)*dy)/d);
        return new Point(a.x+t*dx,a.y+t*dy);
    }
    public static double pathDistance(Point p, Shape s, Region r,double size) {
        return pathDistance(p,s,r,size,1);
    }
    public static double pathDistance(Point p, Shape s, Region r,double size,double aspect) {
        double best=Double.POSITIVE_INFINITY;
        Point scaled=new Point(p.x*aspect,p.y);
        Point start=point(s,0,r,size,aspect),a=new Point(start.x*aspect,start.y);
        for(int i=1;i<=360;i++) { Point next=point(s,i/360.0,r,size,aspect),b=new Point(next.x*aspect,next.y); best=Math.min(best,scaled.distance(project(scaled,a,b))); a=b; }
        return best;
    }
}
