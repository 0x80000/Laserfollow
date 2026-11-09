package de.laserfollow.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.util.Random;
public class CandidateBufferTest {
 @Test public void skippedChromaNeverPassesDetector() {
  for(Yuv.Range range:Yuv.Range.values()) for(int y=0;y<256;y++)
   for(int u=120;u<=136;u++) for(int v=120;v<=136;v++)
    for(Detector.Color c:Detector.Color.values()) assertFalse(Detector.matches(Yuv.color(y,u,v,range),c));
 }
 @Test public void sparseFramePreservesCandidatesAndClearsReusedBuffer() {
  Random random=new Random(42); byte[] data=new byte[10000]; random.nextBytes(data);
  Yuv.Plane y=new Yuv.Plane(ByteBuffer.wrap(data),100,1);
  Yuv.Plane u=new Yuv.Plane(ByteBuffer.wrap(data),100,2);
  Yuv.Plane v=new Yuv.Plane(ByteBuffer.wrap(data,1,data.length-1),100,2);
  Yuv.CandidateBuffer buffer=new Yuv.CandidateBuffer();
  for(Yuv.Range range:Yuv.Range.values()) for(Detector.Color c:Detector.Color.values()) {
   Yuv.Frame expected=Yuv.bounded(y,u,v,3,5,50,40,640,range);
   Yuv.Frame actual=buffer.read(y,u,v,3,5,50,40,range,c);
   for(int i=0;i<actual.pixels().length;i++) assertEquals(Detector.matches(expected.pixels()[i],c)?expected.pixels()[i]:0,actual.pixels()[i]);
  }
  java.util.Arrays.fill(data,(byte)128);
  for(int pixel:buffer.read(y,u,v,3,5,50,40,Yuv.Range.FULL,Detector.Color.RED).pixels()) assertEquals(0,pixel);
 }
}
