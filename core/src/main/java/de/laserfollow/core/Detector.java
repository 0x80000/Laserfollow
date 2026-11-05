package de.laserfollow.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import de.laserfollow.core.Geometry.*;

/** Bright chromatic spots with optional overexposed white centers. */
public final class Detector {
    public enum Color { RED, GREEN }
    public enum State { DETECTED, MISSING, AMBIGUOUS }
    public record Result(State state, Point point) {
        public static Result missing() { return new Result(State.MISSING,null); }
    }

    /** A strongly color-dominant seed. White light by itself must never count as a laser. */
    public static boolean matches(int argb, Color color) {
        int r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
        int c=color==Color.RED?r:g, other=color==Color.RED?g:r;
        return c>=125 && c-other>=38 && c-b>=35 && c>=Math.max(other,b)*1.25;
    }

    /** Less saturated halo pixels can join a seeded component. */
    private static boolean halo(int argb, Color color) {
        int r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
        int c=color==Color.RED?r:g, other=color==Color.RED?g:r;
        return c>=95 && c-other>=22 && c-b>=20 && c>=Math.max(other,b)*1.12;
    }

    private static boolean hotCenter(int argb) {
        int r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
        return Math.min(r,Math.min(g,b))>=220
            && Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=40;
    }

    public Result detect(int[] pixels,int width,int height,Color color,Region roi) {
        if(width<=0 || height<=0 || pixels.length!=(long)width*height) throw new IllegalArgumentException("Bildgröße");
        boolean[] seen=new boolean[pixels.length]; int[] queue=new int[pixels.length];
        List<double[]> candidates=new ArrayList<>();
        int max=Math.max(12,(int)(width*(long)height*.0005));
        int top=roi==null?0:Math.max(0,(int)Math.floor(roi.top()*height));
        int bottom=roi==null?height:Math.min(height,(int)Math.ceil(roi.bottom()*height));
        int left=roi==null?0:Math.max(0,(int)Math.floor(roi.left()*width));
        int right=roi==null?width:Math.min(width,(int)Math.ceil(roi.right()*width));
        for(int y=top;y<bottom;y++) for(int x=left;x<right;x++) {
            int index=y*width+x;
            if(seen[index] || !inside(x,y,width,height,roi) || !halo(pixels[index],color)) continue;
            int head=0,tail=0,seedCount=0;
            queue[tail++]=index; seen[index]=true;
            double sx=0,sy=0,strength=0; boolean fullyInside=true;
            int minX=x,maxX=x,minY=y,maxY=y;
            while(head<tail) {
                int at=queue[head++],ax=at%width,ay=at/width;
                if(!inside(ax,ay,width,height,roi)) fullyInside=false;
                minX=Math.min(minX,ax); maxX=Math.max(maxX,ax); minY=Math.min(minY,ay); maxY=Math.max(maxY,ay);
                sx+=ax+.5; sy+=ay+.5;
                int pixel=pixels[at];
                strength+=color==Color.RED?(pixel>>16)&255:(pixel>>8)&255;
                if(matches(pixel,color)) seedCount++;
                for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++) {
                    int nx=ax+dx,ny=ay+dy;
                    if(nx<0||ny<0||nx>=width||ny>=height) continue;
                    int n=ny*width+nx;
                    if(!seen[n] && halo(pixels[n],color)) { seen[n]=true; queue[tail++]=n; }
                }
            }
            if(!fullyInside || seedCount==0 || tail<2 || tail>max) continue;
            int spanX=maxX-minX+1,spanY=maxY-minY+1;
            if(Math.max(spanX,spanY)>3*Math.min(spanX,spanY)) continue;

            // A saturated laser often has a white center. Its surrounding colored
            // halo establishes that it is a candidate; neutral white alone never does.
            int core=0;
            for(int cy=minY;cy<=maxY;cy++) for(int cx=minX;cx<=maxX;cx++) {
                int at=cy*width+cx;
                if(!seen[at] && hotCenter(pixels[at])) {
                    core++; sx+=cx+.5; sy+=cy+.5;
                }
            }
            int count=tail+core;
            if(count/(double)(spanX*spanY)<.35) continue;
            candidates.add(new double[]{strength/tail,sx/count/width,sy/count/height});
        }
        if(candidates.isEmpty()) return Result.missing();
        candidates.sort(Comparator.comparingDouble((double[] a)->a[0]).reversed());
        if(candidates.size()>1 && candidates.get(1)[0]>=candidates.get(0)[0]*.65)
            return new Result(State.AMBIGUOUS,null);
        double[] best=candidates.get(0);
        return new Result(State.DETECTED,new Point(best[1],best[2]));
    }
    private boolean inside(int x,int y,int w,int h,Region r) {
        return r==null || r.contains((x+.5)/w,(y+.5)/h);
    }
}
