package de.laserfollow.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import de.laserfollow.core.Geometry.*;
import de.laserfollow.core.Detector.*;

public class CoreTest {
    private final Detector detector=new Detector();
    private int[] image() { return new int[100*100]; }
    private void blob(int[] p,int x,int y,int size,int color) { for(int j=y;j<y+size;j++)for(int i=x;i<x+size;i++)p[j*100+i]=color; }
    private Result detect(int[] p,Color c) { return detector.detect(p,100,100,c,null); }
    @Test public void blackMissing() { Result r=detect(image(),Color.RED); assertEquals(State.MISSING,r.state()); assertNull(r.point()); }
    @Test public void redAndGreenDots() {
        for(Color c:Color.values()) { int[] p=image(); blob(p,30,40,3,c==Color.RED?0xffff2020:0xff20ff20);
            Result r=detect(p,c); assertEquals(State.DETECTED,r.state()); assertEquals(.315,r.point().x(),1e-9); assertEquals(.415,r.point().y(),1e-9); }
    }
    @Test public void wrongColorAndWhiteGlare() { int[] p=image(); blob(p,20,20,3,0xffff0000); assertEquals(State.MISSING,detect(p,Color.GREEN).state()); blob(p,20,20,3,0xffffffff); assertEquals(State.MISSING,detect(p,Color.RED).state()); }
    @Test public void equalBlobsAmbiguous() { int[] p=image(); blob(p,20,20,3,0xffff0000); blob(p,70,70,3,0xffff0000); Result r=detect(p,Color.RED); assertEquals(State.AMBIGUOUS,r.state()); assertNull(r.point()); }
    @Test public void unequalAreaSameBrightnessIsStillAmbiguous() { int[] p=image(); blob(p,20,20,2,0xffff0000); blob(p,70,70,3,0xffff0000); assertEquals(State.AMBIGUOUS,detect(p,Color.RED).state()); }
    @Test public void longColoredLineIsNotADot() { int[] p=image(); for(int x=20;x<30;x++) p[2020+x-20]=0xffff0000; assertEquals(State.MISSING,detect(p,Color.RED).state()); }
    @Test public void largeBlobRejected() { int[] p=image(); blob(p,20,20,20,0xffff0000); assertEquals(State.MISSING,detect(p,Color.RED).state()); }
    @Test public void roiExcludesOutsideDot() { int[] p=image(); blob(p,20,20,3,0xffff0000); assertEquals(State.MISSING,detector.detect(p,100,100,Color.RED,new Region(.5,.5,.9,.9)).state()); }
    @Test public void singleNoisePixelRejected() { int[] p=image(); p[3030]=0xffff0000; assertEquals(State.MISSING,detect(p,Color.RED).state()); }
    @Test public void clippedLargeObjectIsNotASmallLaser() {
        int[] p=image(); blob(p,30,30,20,0xffff0000);
        assertEquals(State.MISSING,detector.detect(p,100,100,Color.RED,new Region(.48,.48,.9,.9)).state());
    }
    @Test public void saturatedWhiteCenterStillDetectsColoredHalo() {
        int[] p=image();
        blob(p,30,40,3,0xffd06864);
        blob(p,31,41,1,0xffffffff);
        Result result=detect(p,Color.RED);
        assertEquals(State.DETECTED,result.state());
        assertEquals(.315,result.point().x(),1e-9);
        assertEquals(.415,result.point().y(),1e-9);
    }
    @Test public void paleRedLaserRemainsVisible() {
        int[] p=image();
        blob(p,30,40,3,0xffaa7873);
        assertEquals(State.DETECTED,detect(p,Color.RED).state());
    }
    @Test public void faintRedHaloNeedsStrongColorSeed() {
        int[] p=image();
        blob(p,30,40,3,0xff9d8380);
        blob(p,30,40,1,0xffee6b6b);
        blob(p,31,41,1,0xffffffff);
        assertEquals(State.DETECTED,detect(p,Color.RED).state());
        blob(p,30,40,3,0xff9d8380);
        assertEquals(State.MISSING,detect(p,Color.RED).state());
    }
    @Test public void neutralWhiteAndPinkishGlareAreNotLaser() {
        int[] p=image();
        blob(p,30,40,5,0xffffffff);
        assertEquals(State.MISSING,detect(p,Color.RED).state());
        blob(p,30,40,5,0xfff2eee8);
        assertEquals(State.MISSING,detect(p,Color.RED).state());
    }
    @Test public void ambiguousSaturatedHalosRemainAmbiguous() {
        int[] p=image();
        blob(p,20,20,3,0xffe05040);
        blob(p,21,21,1,0xffffffff);
        blob(p,65,65,3,0xffe05040);
        blob(p,66,66,1,0xffffffff);
        assertEquals(State.AMBIGUOUS,detect(p,Color.RED).state());
    }
    @Test public void regionConversionPreservesImageCoordinatesAndGuardBand() {
        ByteBuffer y=ByteBuffer.allocate(100),u=ByteBuffer.allocate(25),v=ByteBuffer.allocate(25);
        for(int i=0;i<100;i++)y.put(i,(byte)80);
        for(int i=0;i<25;i++) {u.put(i,(byte)128);v.put(i,(byte)128);}
        Yuv.Plane yy=new Yuv.Plane(y,10,1),uu=new Yuv.Plane(u,5,1),vv=new Yuv.Plane(v,5,1);
        Yuv.Frame all=Yuv.bounded(yy,uu,vv,0,0,10,10,10,Yuv.Range.FULL);
        Yuv.Frame cropped=Yuv.bounded(yy,uu,vv,0,0,10,10,10,Yuv.Range.FULL,new Region(.2,.2,.8,.8));
        assertEquals(all.width(),cropped.width());
        assertEquals(all.height(),cropped.height());
        assertEquals(all.pixels()[5*10+5],cropped.pixels()[5*10+5]);
        assertEquals(0,cropped.pixels()[0]);
        assertEquals(all.pixels()[1*10+5],cropped.pixels()[1*10+5]); // border guard
    }
    @Test public void circleRemainsCircularInWideAndTallImages() {
        Region r=new Region(.1,.2,.9,.8);
        for(double aspect:new double[]{.75,4.0/3,2}) {
            Point right=Geometry.point(Shape.CIRCLE,0,r,1,aspect);
            Point bottom=Geometry.point(Shape.CIRCLE,.25,r,1,aspect);
            assertEquals((right.x()-.5)*aspect,bottom.y()-.5,1e-9);
            assertEquals(0,Geometry.pathDistance(right,Shape.CIRCLE,r,1,aspect),1e-9);
        }
    }
    @Test public void pathAccuracyUsesOnlyValidDetectionsAndTolerance() {
        SessionMetrics m=new SessionMetrics();
        for(int i=0;i<10;i++) {
            m.observe(i*.1,true,i<6?.019:.025,.02);
        }
        m.observe(1.0,false,Double.NaN);
        assertTrue(m.sufficientData());
        assertEquals(60,m.pathAccuracyPercent(),1e-9);
        assertEquals(10,m.samples());
        m.reset();
        assertTrue(Double.isNaN(m.pathAccuracyPercent()));
        assertEquals(0,m.samples());
    }
    @Test public void pathAccuracyCountsBoundaryOfTwoPercent() {
        SessionMetrics m=new SessionMetrics();
        m.observe(0,true,.02,.0);
        m.observe(.1,true,.020001,.0);
        assertEquals(50,m.pathAccuracyPercent(),1e-9);
    }
    @Test public void longCameraOutageLowersCoverage() {
        SessionMetrics m=new SessionMetrics(); m.observe(0,true,.1); m.observe(.1,true,.1);
        m.observe(5,false,Double.NaN); m.observe(5.1,true,.1);
        assertEquals(.1,m.trackedSeconds(),1e-9);
        assertEquals(100*.1/5.1,m.coveragePercent(),1e-9);
    }
    @Test public void lateFrameCannotRewindClock() {
        SessionMetrics m=new SessionMetrics(); m.observe(1,true,.1); m.observe(1.1,true,.1);
        m.observe(.9,true,.8); m.observe(1.2,true,.1);
        assertEquals(.2,m.trackedSeconds(),1e-9); assertEquals(3,m.samples());
        assertEquals(10,m.meanErrorPercent(),1e-9);
    }
    @Test public void stationarySpotOnPathDoesNotFollowPacer() {
        Region r=new Region(.1,.1,.9,.9); Point fixed=Geometry.point(Shape.CIRCLE,0,r,1);
        SessionMetrics m=new SessionMetrics();
        for(int i=0;i<=20;i++) {
            Point target=Geometry.point(Shape.CIRCLE,i/20.0,r,1);
            m.observe(i*.1,true,Geometry.pathDistance(fixed,Shape.CIRCLE,r,1)/r.diagonal(),fixed.distance(target)/r.diagonal());
        }
        assertTrue(m.sufficientData()); assertEquals(0,m.meanErrorPercent(),1e-9);
        assertTrue(m.meanTargetErrorPercent()>30);
    }
    @Test public void singleFrameIsInsufficient() { SessionMetrics m=new SessionMetrics(); m.observe(0,true,0); assertFalse(m.sufficientData()); }
    @Test public void finiteOrderedPaths() { Region r=new Region(.1,.1,.9,.9); for(Shape s:Shape.values())for(int i=0;i<1000;i++) { Point p=Geometry.point(s,i/999.0,r,.8); assertTrue(Double.isFinite(p.x())); assertTrue(Double.isFinite(p.y())); assertTrue(r.contains(p.x(),p.y())); } assertEquals(Geometry.point(Shape.LINE,0,r,1),Geometry.point(Shape.LINE,1,r,1)); assertEquals(.9,Geometry.point(Shape.LINE,.5,r,1).x(),1e-9); }
    @Test public void projectionAndDistance() { Point p=Geometry.project(new Point(.5,.8),new Point(0,0),new Point(1,0)); assertEquals(.5,p.x(),0); assertEquals(0,p.y(),0); assertEquals(new Point(1,0),Geometry.project(new Point(2,2),new Point(0,0),new Point(1,0))); assertEquals(new Point(0,0),Geometry.project(new Point(2,2),new Point(0,0),new Point(0,0))); assertEquals(.2,Geometry.pathDistance(new Point(.5,.7),Shape.LINE,new Region(0,0,1,1),1),1e-9); }
    @Test public void calibrationNormalizesAndClamps() { Region r=Region.corners(new Point(1.2,.9),new Point(.2,-.1)); assertEquals(new Region(.2,0,1,.9),r); }
    @Test(expected=IllegalArgumentException.class) public void degenerateCalibration() { Region.corners(new Point(.5,.5),new Point(.51,.52)); }
    @Test(expected=IllegalArgumentException.class) public void nonFiniteCalibration() { Region.corners(new Point(Double.NaN,0),new Point(1,1)); }
    @Test public void noSamplesIsInsufficient() { SessionMetrics m=new SessionMetrics(); m.observe(0,false,Double.NaN); m.observe(.1,false,Double.NaN); assertTrue(Double.isNaN(m.meanErrorPercent())); assertEquals(0,m.samples()); assertEquals(0,m.trackedSeconds(),0); }
    @Test public void missingAndAmbiguousExcludedWithoutBridge() { SessionMetrics m=new SessionMetrics(); m.observe(0,true,.1); m.observe(.1,true,.2); m.observe(.2,false,Double.NaN); m.observe(.3,false,Double.NaN); m.observe(.4,true,.3); assertEquals(20,m.meanErrorPercent(),1e-9); assertEquals(.1,m.trackedSeconds(),1e-9); assertEquals(25,m.coveragePercent(),1e-9); assertEquals(0,m.observe(.5,false,0),0); }
    @Test public void pauseResumeAndTimestampGaps() { SessionMetrics m=new SessionMetrics(); m.observe(0,true,.1); m.observe(.1,true,.1); m.breakContinuity(); assertEquals(0,m.observe(10,true,.1),0); assertEquals(.1,m.observe(10.1,true,.1),1e-9); assertEquals(0,m.observe(20,true,.1),0); assertEquals(.2,m.trackedSeconds(),1e-9); }
    @Test public void invalidErrorCannotCountAsTracking() { SessionMetrics m=new SessionMetrics(); m.observe(0,true,.1); m.observe(.1,true,Double.NaN); assertEquals(0,m.trackedSeconds(),0); assertEquals(1,m.samples()); }
    @Test public void yuvPositionCropAndStrides() {
        ByteBuffer yy=ByteBuffer.allocate(50),u=ByteBuffer.allocate(30),v=ByteBuffer.allocate(30);
        yy.position(3); u.position(2); v.position(4);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)yy.put(3+y*10+x*2,(byte)(16+x*30+y*5));
        for(int y=0;y<2;y++)for(int x=0;x<2;x++) { u.put(2+y*8+x*2,(byte)128); v.put(4+y*8+x*2,(byte)128); }
        int[] out=Yuv.rgb(new Yuv.Plane(yy,10,2),new Yuv.Plane(u,8,2),new Yuv.Plane(v,8,2),1,1,2,2);
        assertEquals(4,out.length); assertEquals(Yuv.color(51,128,128),out[0]); assertEquals(Yuv.color(86,128,128),out[3]); assertEquals(3,yy.position()); assertEquals(2,u.position());
        assertEquals(0xff000000,Yuv.color(0,128,128)); assertEquals(0xffffffff,Yuv.color(255,128,128));
        assertEquals(0xff101010,Yuv.color(16,128,128));
        assertEquals(0xff000000,Yuv.color(16,128,128,Yuv.Range.LIMITED));
        assertEquals(0xffffffff,Yuv.color(235,128,128,Yuv.Range.LIMITED));
    }
    @Test public void planarChromaAndBoundedConversion() {
        ByteBuffer y=ByteBuffer.allocate(24),u=ByteBuffer.allocate(6),v=ByteBuffer.allocate(6);
        for(int i=0;i<24;i++) y.put(i,(byte)i);
        for(int i=0;i<6;i++) { u.put(i,(byte)128); v.put(i,(byte)128); }
        Yuv.Frame f=Yuv.bounded(new Yuv.Plane(y,6,1),new Yuv.Plane(u,3,1),new Yuv.Plane(v,3,1),0,0,6,4,3,Yuv.Range.FULL);
        assertEquals(3,f.width()); assertEquals(2,f.height()); assertEquals(6,f.pixels().length);
        assertEquals(Yuv.color(16,128,128),f.pixels()[5]);
    }
}
