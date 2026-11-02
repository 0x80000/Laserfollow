package de.laserfollow.core;

/** Seconds use a monotonic clock. Only consecutive valid observations within 0.35 s contribute duration. */
public final class SessionMetrics {
    private double last=Double.NaN, active,tracked,sum,targetSum; private boolean previousValid; private long samples;
    public void reset() { last=Double.NaN; active=tracked=sum=targetSum=0; previousValid=false; samples=0; }
    public void breakContinuity() { last=Double.NaN; previousValid=false; }
    public double observe(double now,boolean valid,double normalizedError) {
        return observe(now,valid,normalizedError,normalizedError);
    }
    public double observe(double now,boolean valid,double normalizedError,double targetError) {
        if(!Double.isFinite(now)) throw new IllegalArgumentException("Zeit");
        if(!Double.isNaN(last) && now<last) return 0; // Ignore late frames, never move the clock backwards.
        valid=valid && Double.isFinite(normalizedError) && normalizedError>=0 && Double.isFinite(targetError) && targetError>=0;
        double dt=Double.isNaN(last)?0:Math.max(0,now-last);
        boolean continuous=dt<=.35;
        active+=dt; // Unpaused camera outages lower coverage, even when no frames arrive.
        double measured=continuous && valid && previousValid?dt:0;
        tracked+=measured;
        if(valid) { sum+=normalizedError; targetSum+=targetError; samples++; }
        else valid=false;
        last=now; previousValid=valid;
        return measured;
    }
    public long samples() { return samples; }
    public double meanErrorPercent() { return samples==0?Double.NaN:100*sum/samples; }
    public double meanTargetErrorPercent() { return samples==0?Double.NaN:100*targetSum/samples; }
    public boolean sufficientData() { return samples>=5 && tracked>=.5; }
    public double trackedSeconds() { return tracked; }
    public double coveragePercent() { return active>0?100*tracked/active:0; }
}
