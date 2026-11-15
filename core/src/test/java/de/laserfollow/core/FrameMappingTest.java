package de.laserfollow.core;

import org.junit.Test;
import static org.junit.Assert.*;
import de.laserfollow.core.Geometry.Point;
import de.laserfollow.core.Geometry.Region;

public class FrameMappingTest {
    private void point(Point p,double x,double y) { assertEquals(x,p.x(),1e-7); assertEquals(y,p.y(),1e-7); }
    @Test public void differentResolutionsMapToSamePreviewPosition() {
        FrameMapping m=FrameMapping.pixelMatrix(new float[]{2,0,0,0,2,0,0,0,1},640,480,1280,960);
        point(m.toPreview(new Point(.25,.75)),.25,.75);
        point(m.toSource(new Point(.25,.75)),.25,.75);
    }
    @Test public void allQuarterTurnsRoundTripAndKeepCalibration() {
        FrameMapping[] rotations={new FrameMapping(1,0,0,1,0,0),new FrameMapping(0,-1,1,0,1,0),
            new FrameMapping(-1,0,0,-1,1,1),new FrameMapping(0,1,-1,0,0,1)};
        Region area=new Region(.2,.3,.8,.9);
        for(FrameMapping m:rotations) {
            point(m.toSource(m.toPreview(new Point(.23,.67))),.23,.67);
            Region raw=m.sourceRegion(area);
            Point a=m.toPreview(new Point(raw.left(),raw.top()));
            Point b=m.toPreview(new Point(raw.right(),raw.bottom()));
            assertEquals(area.left(),Math.min(a.x(),b.x()),1e-7);
            assertEquals(area.top(),Math.min(a.y(),b.y()),1e-7);
            assertEquals(area.right(),Math.max(a.x(),b.x()),1e-7);
            assertEquals(area.bottom(),Math.max(a.y(),b.y()),1e-7);
        }
    }
    @Test public void rotatedPixelBufferUsesUnrotatedCropDimensions() {
        FrameMapping m=FrameMapping.pixelMatrix(new float[]{0,-2,960,2,0,0,0,0,1},640,480,960,1280);
        point(m.toPreview(new Point(.2,.3)),.7,.2);
    }
    @Test public void translatedAndScaledCropIsInvertible() {
        FrameMapping m=new FrameMapping(2,0,0,2,-.5,-.5);
        Region raw=m.sourceRegion(new Region(.1,.2,.9,.8));
        assertEquals(new Region(.3,.35,.7,.65),raw);
        point(m.toPreview(new Point(.5,.5)),.5,.5);
    }
    @Test(expected=IllegalArgumentException.class) public void refusesSingularMapping() { new FrameMapping(1,2,2,4,0,0); }
    @Test(expected=IllegalArgumentException.class) public void refusesUnknownImageSize() { FrameMapping.pixelMatrix(new float[]{1,0,0,0,1,0,0,0,1},0,480,640,480); }
    @Test(expected=IllegalArgumentException.class) public void refusesNonFiniteMatrix() { FrameMapping.pixelMatrix(new float[]{1,0,0,0,1,0,0,0,Float.NaN},640,480,640,480); }
    @Test(expected=IllegalArgumentException.class) public void refusesPerspectiveMatrix() { FrameMapping.pixelMatrix(new float[]{1,0,0,0,1,0,.1f,0,1},640,480,640,480); }
    @Test(expected=IllegalArgumentException.class) public void refusesCalibrationOutsideImage() { new FrameMapping(.5,0,0,.5,.25,.25).sourceRegion(new Region(0,0,1,1)); }
}
