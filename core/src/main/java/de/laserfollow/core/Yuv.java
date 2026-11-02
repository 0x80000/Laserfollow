package de.laserfollow.core;

import java.nio.ByteBuffer;

/** Plane offsets are relative to each buffer's current position; buffers are never mutated. */
public final class Yuv {
    private Yuv() {}
    public enum Range { FULL, LIMITED }
    public record Frame(int[] pixels,int width,int height) {}
    public record Plane(ByteBuffer buffer,int rowStride,int pixelStride) {
        public int value(int x,int y) { return buffer.get(buffer.position()+y*rowStride+x*pixelStride)&255; }
    }
    public static int[] rgb(Plane y,Plane u,Plane v,int left,int top,int width,int height) {
        int[] out=new int[width*height];
        for(int row=0;row<height;row++) for(int col=0;col<width;col++) {
            int x=left+col,z=top+row;
            out[row*width+col]=color(y.value(x,z),u.value(x/2,z/2),v.value(x/2,z/2));
        }
        return out;
    }
    public static Frame bounded(Plane y,Plane u,Plane v,int left,int top,int width,int height,int maxEdge,Range range) {
        if(width<=0 || height<=0 || maxEdge<=0) throw new IllegalArgumentException("Bildgröße");
        int step=Math.max(1,(Math.max(width,height)+maxEdge-1)/maxEdge);
        int w=(width+step-1)/step,h=(height+step-1)/step;
        int[] pixels=new int[w*h];
        for(int row=0;row<h;row++) for(int col=0;col<w;col++) {
            int x=left+col*step,z=top+row*step;
            pixels[row*w+col]=color(y.value(x,z),u.value(x/2,z/2),v.value(x/2,z/2),range);
        }
        return new Frame(pixels,w,h);
    }
    public static int color(int y,int u,int v) {
        return color(y,u,v,Range.FULL);
    }
    public static int color(int y,int u,int v,Range range) {
        if(range==Range.FULL) {
            int d=u-128,e=v-128;
            return 0xff000000 | (clip((int)Math.round(y+1.402*e))<<16)
                | (clip((int)Math.round(y-.344136*d-.714136*e))<<8)
                | clip((int)Math.round(y+1.772*d));
        }
        int c=Math.max(0,y-16),d=u-128,e=v-128;
        int r=clip((298*c+409*e+128)>>8),g=clip((298*c-100*d-208*e+128)>>8),b=clip((298*c+516*d+128)>>8);
        return 0xff000000|(r<<16)|(g<<8)|b;
    }
    private static int clip(int v) { return Math.max(0,Math.min(255,v)); }
}
