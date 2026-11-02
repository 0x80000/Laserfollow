package de.laserfollow.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import de.laserfollow.core.Geometry.*;

public final class Detector {
    public enum Color { RED, GREEN }
    public enum State { DETECTED, MISSING, AMBIGUOUS }
    public record Result(State state, Point point) {
        public static Result missing() { return new Result(State.MISSING,null); }
    }
    public static boolean matches(int argb, Color color) {
        int r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
        int c=color==Color.RED?r:g, other=color==Color.RED?g:r;
        return c>=150 && c-other>=65 && c-b>=65 && c>=other*1.5;
    }
    public Result detect(int[] pixels,int width,int height,Color color,Region roi) {
        if(width<=0 || height<=0 || pixels.length!=width*height) throw new IllegalArgumentException("Bildgröße");
        boolean[] seen=new boolean[pixels.length]; int[] queue=new int[pixels.length];
        List<double[]> candidates=new ArrayList<>();
        int max=Math.max(12,(int)(width*height*.0005));
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int index=y*width+x;
            if(seen[index] || !inside(x,y,width,height,roi) || !matches(pixels[index],color)) continue;
            int head=0,tail=0; queue[tail++]=index; seen[index]=true; double sx=0,sy=0,strength=0; boolean fullyInside=true;
            int minX=x,maxX=x,minY=y,maxY=y;
            while(head<tail) {
                int at=queue[head++],ax=at%width,ay=at/width;
                if(!inside(ax,ay,width,height,roi)) fullyInside=false;
                minX=Math.min(minX,ax); maxX=Math.max(maxX,ax); minY=Math.min(minY,ay); maxY=Math.max(maxY,ay);
                sx+=ax+.5; sy+=ay+.5; strength+= color==Color.RED?(pixels[at]>>16)&255:(pixels[at]>>8)&255;
                for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++) {
                    int nx=ax+dx,ny=ay+dy;
                    if(nx<0||ny<0||nx>=width||ny>=height) continue;
                    int n=ny*width+nx;
                    if(!seen[n] && matches(pixels[n],color)) { seen[n]=true; queue[tail++]=n; }
                }
            }
            int spanX=maxX-minX+1,spanY=maxY-minY+1;
            boolean compact=tail/(double)(spanX*spanY)>=.35 && Math.max(spanX,spanY)<=3*Math.min(spanX,spanY);
            if(fullyInside && compact && tail>=2 && tail<=max) candidates.add(new double[]{strength/tail,sx/tail/width,sy/tail/height});
        }
        if(candidates.isEmpty()) return Result.missing();
        candidates.sort(Comparator.comparingDouble((double[] a)->a[0]).reversed());
        if(candidates.size()>1 && candidates.get(1)[0]>=candidates.get(0)[0]*.65) return new Result(State.AMBIGUOUS,null);
        double[] best=candidates.get(0); return new Result(State.DETECTED,new Point(best[1],best[2]));
    }
    private boolean inside(int x,int y,int w,int h,Region r) { return r==null || r.contains((x+.5)/w,(y+.5)/h); }
}
