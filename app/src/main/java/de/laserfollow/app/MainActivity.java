package de.laserfollow.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import de.laserfollow.core.*;
import de.laserfollow.core.Geometry.*;
import de.laserfollow.core.Geometry.Point;
import de.laserfollow.core.Geometry.Region;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends ComponentActivity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final SessionMetrics metrics=new SessionMetrics();
    private final Detector detector=new Detector();
    private final AtomicBoolean deliveryPending=new AtomicBoolean();
    private volatile long generation;
    private volatile long configuration;
    private volatile Region detectionRegion;
    private volatile Detector.Color color=Detector.Color.RED;
    private ProcessCameraProvider provider;
    private ImageAnalysis analysis;
    private androidx.lifecycle.LiveData<CameraState> cameraState;
    private OrientationEventListener orientationListener;
    private int orientationRotation=Surface.ROTATION_0;
    private boolean permissionDenied,permissionPending;
    private ExerciseView view;
    private TextView status,result,badge;
    private Button start,pause,stop,calibrate;
    private boolean camera,running,paused,calibrating,foreground;
    private boolean cameraReady;
    private double phase,period=12,size=.7;
    private Shape shape=Shape.CIRCLE;
    private long lastFrameTime,lastAccepted;
    private int frameWidth,frameHeight,frameRotation=-1;
    private ActivityResultLauncher<String> permission;
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if(!foreground) return;
            long now=SystemClock.elapsedRealtime();
            if(!camera && running && !paused) {
                Point p=Geometry.point(shape,phase+.012,view.region,size,view.imageAspect());
                p=new Point(p.x()+.012*Math.sin(now/380.0),p.y()+.009*Math.cos(now/530.0));
                accept(new Detector.Result(Detector.State.DETECTED,p),now);
            } else if(camera && now-lastFrameTime>350) {
                view.observation(null);
                cameraReady=false; updateControls();
                if(running && !paused) {
                    metrics.observe(now/1000.0,false,Double.NaN); renderMetrics();
                    setStatus("Kein aktuelles Kamerabild – Ziel wartet");
                }
            }
            main.postDelayed(this,100);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
            permissionPending=false; permissionDenied=!granted;
            if(camera && foreground) { if(granted) openCamera(); else fail("Kamerazugriff abgelehnt. Demo ist weiterhin verfügbar."); }
        });
        orientationRotation=getWindowManager().getDefaultDisplay().getRotation();
        orientationListener=new OrientationEventListener(this) {
            @Override public void onOrientationChanged(int degrees) {
                if(degrees==ORIENTATION_UNKNOWN) return;
                int next=degrees>=45 && degrees<135?Surface.ROTATION_270:
                    degrees>=135 && degrees<225?Surface.ROTATION_180:
                    degrees>=225 && degrees<315?Surface.ROTATION_90:Surface.ROTATION_0;
                if(next==orientationRotation) return;
                orientationRotation=next;
                if(analysis!=null) {
                    analysis.setTargetRotation(next); view.region=null; detectionRegion=null;
                    view.first=null; calibrating=false; reset("Telefon gedreht. Bitte neu kalibrieren.");
                }
            }
        };
        buildUi();
        setStatus("Bereit · Start beginnt die Simulation");
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView label(String text,int sp) { TextView t=new TextView(this); t.setText(text); t.setTextColor(0xffe4edf9); t.setTextSize(sp); t.setPadding(0,dp(6),0,dp(6)); return t; }
    private void buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xff0b1425);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(12),dp(18),dp(24)); scroll.addView(root); setContentView(scroll);
        // Keep controls inside system bars, including Android 15 edge-to-edge enforcement.
        scroll.setOnApplyWindowInsetsListener((v,insets)->{ v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        root.addView(label("LASERFOLLOW",26));
        badge=label("Demo – simulierte Daten",17); badge.setTextColor(0xff54d6c7); root.addView(badge);
        root.addView(label("Ziele erscheinen nur auf dem Telefon. Still sitzen, Telefon fest aufstellen, Rückkamera zur matten Wand.",14));
        view=new ExerciseView(this); root.addView(view,new LinearLayout.LayoutParams(-1,dp(290)));
        status=label("",17); root.addView(status);
        LinearLayout controls=new LinearLayout(this); root.addView(controls);
        start=button("Start",controls,()->startSession()); pause=button("Pause",controls,()->togglePause()); stop=button("Stopp",controls,()->stopSession("Beendet"));
        result=label("Noch keine Messdaten",16); root.addView(result);
        root.addView(label("Einstellungen · Änderungen setzen Messwerte zurück",16));
        selector(root,new String[]{"Demo – simulierte Daten","Kamera – Live-Erkennung"},i->switchMode(i==1));
        selector(root,new String[]{"Kreis","Linie · hin und zurück","Acht"},i->{ shape=Shape.values()[i]; view.shape=shape; reset("Bahn geändert"); });
        selector(root,new String[]{"Roter Punkt","Grüner Punkt"},i->{ color=i==0?Detector.Color.RED:Detector.Color.GREEN; reset("Farbe geändert"); });
        TextView sizeLabel=label("Bahngröße: 70 % des Bereichs",14); root.addView(sizeLabel);
        SeekBar sizeBar=new SeekBar(this); sizeBar.setMax(65); sizeBar.setProgress(45); root.addView(sizeBar);
        sizeBar.setOnSeekBarChangeListener(seek((v)->{ size=(25+v)/100.0; view.size=size; sizeLabel.setText("Bahngröße: "+(25+v)+" % des Bereichs"); reset("Bahngröße geändert"); }));
        TextView periodLabel=label("Umlaufzeit: 12 s · frei wählbar",14); root.addView(periodLabel);
        SeekBar periodBar=new SeekBar(this); periodBar.setMax(26); periodBar.setProgress(8); root.addView(periodBar);
        periodBar.setOnSeekBarChangeListener(seek(v->{ period=v+4; periodLabel.setText("Umlaufzeit: "+(v+4)+" s · frei wählbar"); reset("Umlaufzeit geändert"); }));
        calibrate=new Button(this); calibrate.setText("Wandbereich kalibrieren"); root.addView(calibrate);
        calibrate.setOnClickListener(v->{ if(camera && !cameraReady) { setStatus("Zuerst ein Kamerabild abwarten"); return; } reset("Zwei gegenüberliegende Ecken im Bild antippen"); calibrating=true; view.first=null; view.region=null; detectionRegion=null; updateControls(); });
        view.tap=p->{
            if(!calibrating) return;
            if(view.first==null) { view.first=p; setStatus("Jetzt die gegenüberliegende Ecke antippen"); }
            else { try { Region r=Region.corners(view.first,p); view.region=r; detectionRegion=r; calibrating=false; view.first=null; reset("Bereich gesetzt. Bei Telefonbewegung neu kalibrieren."); }
                catch(IllegalArgumentException e) { view.first=null; setStatus("Bereich zu klein. Bitte beide Ecken erneut antippen."); } }
            view.invalidate(); updateControls();
        };
        root.addView(label("Türkis: Bahn · Gelb: Taktpunkt · Weiß: beobachteter Punkt. Bei fehlendem oder mehrdeutigem Punkt wartet das Ziel. Stillhalten auf der Bahn bedeutet nicht, dass die Form nachgefahren wurde.",14));
        root.addView(label("Selbst gewählte Darstellung, keine Bewegungsvorgabe. Nach jeder Bewegung des Telefons neu kalibrieren. Nur Bildkoordinaten, keine Zentimeter oder Halswinkel.",14));
        root.addView(label("Physio-Forschungsprototyp, nicht klinisch validiert. Nur mit professioneller Begleitung. Bei Schmerzen oder Schwindel stoppen. Laser niemals auf Augen, Menschen oder reflektierende Flächen richten.",14));
        root.addView(label("Erkennungsgrenzen: weiße überbelichtete Zentren, Sonne, Reflexionen und farbige Gegenstände können die Messung stören. Keine Gesundheits- oder Erholungsbewertung. Keine Speicherung von Bildern oder Sitzungen.",14));
        updateControls();
    }
    private interface Choice { void select(int i); }
    private void selector(LinearLayout root,String[] entries,Choice choice) {
        Spinner s=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,entries); s.setAdapter(adapter); root.addView(s);
        s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() { int previous=0;
            public void onItemSelected(AdapterView<?> p,View v,int i,long id) { if(i!=previous) { previous=i; choice.select(i); } }
            public void onNothingSelected(AdapterView<?> p) {} });
    }
    private SeekBar.OnSeekBarChangeListener seek(Choice c) { return new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar s,int p,boolean user) { if(user)c.select(p); }
        public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {} }; }
    private Button button(String text,LinearLayout parent,Runnable action) { Button b=new Button(this); b.setText(text); b.setMinHeight(dp(56)); parent.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->action.run()); return b; }
    private void setStatus(String s) { status.setText((camera?"Kamera · ":"Demo – simulierte Daten · ")+s); }
    private void updateControls() {
        start.setEnabled(!running && !calibrating && (!camera || cameraReady && view.region!=null));
        pause.setEnabled(running); stop.setEnabled(running); pause.setText(paused?"Weiter":"Pause");
        if(calibrate!=null) calibrate.setEnabled(!camera || cameraReady);
    }
    private void renderMetrics() {
        String prefix=camera?"Kamera-Ergebnis":"Demo – simulierte Daten";
        if(!metrics.sufficientData()) result.setText(prefix+"\nUnzureichende Daten · mindestens 0,5 s gültig erfassen");
        else result.setText(String.format(Locale.GERMAN,"%s\nNächster Bahnabstand: %.2f %%\nAbstand zum Taktpunkt: %.2f %%\nJeweils Mittelwert in %% der Bereichsdiagonale\nErfasst: %.1f s · Erkennungsabdeckung: %.1f %%",prefix,metrics.meanErrorPercent(),metrics.meanTargetErrorPercent(),metrics.trackedSeconds(),metrics.coveragePercent()));
    }
    private void reset(String message) { configuration++; running=false; paused=false; phase=0; metrics.reset(); view.phase=0; view.observation(null); renderMetrics(); setStatus(message); updateControls(); }
    private void startSession() { if(camera && (!cameraReady || view.region==null) || calibrating) return; configuration++; metrics.reset(); phase=0; running=true; paused=false; view.phase=0; view.observation(null); setStatus("Läuft · Punkt suchen"); renderMetrics(); updateControls(); }
    private void togglePause() { if(!running)return; configuration++; paused=!paused; metrics.breakContinuity(); view.observation(null); setStatus(paused?"Pausiert":"Fortgesetzt · Punkt suchen"); updateControls(); }
    private void stopSession(String message) { configuration++; running=false; paused=false; metrics.breakContinuity(); view.observation(null); renderMetrics(); setStatus(message); updateControls(); }
    private void accept(Detector.Result observation,long now) {
        if(!running || paused) { view.observation(null); return; }
        boolean valid=observation.state()==Detector.State.DETECTED;
        double error=valid?Geometry.pathDistance(observation.point(),shape,view.region,size,view.imageAspect())/view.region.diagonal(view.imageAspect()):Double.NaN;
        Point target=Geometry.point(shape,phase,view.region,size,view.imageAspect());
        double targetError=valid?Math.hypot((observation.point().x()-target.x())*view.imageAspect(),observation.point().y()-target.y())/view.region.diagonal(view.imageAspect()):Double.NaN;
        double dt=metrics.observe(now/1000.0,valid,error,targetError);
        phase+=dt/period; view.phase=phase; view.observation(valid?observation.point():null);
        setStatus(switch(observation.state()) { case DETECTED->"Punkt erkannt · läuft"; case MISSING->"Punkt fehlt – Ziel wartet"; case AMBIGUOUS->"Mehrere Punkte / mehrdeutig – Ziel wartet"; });
        renderMetrics();
    }
    private void switchMode(boolean useCamera) {
        closeCamera(); camera=useCamera; view.demo=!camera; view.clearFrame(); view.first=null; calibrating=false;
        if(camera) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        permissionDenied=false;
        detectionRegion=null; view.region=camera?null:new Region(.12,.12,.88,.88);
        badge.setText(camera?"Kamera – Live-Erkennung":"Demo – simulierte Daten"); reset(camera?"Kamera wird vorbereitet; danach kalibrieren":"Simulation bereit");
        if(camera && foreground) ensurePermission();
    }
    private void ensurePermission() {
        if(permissionPending) return;
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) openCamera();
        else if(permissionDenied) fail("Kamerazugriff abgelehnt. Demo ist weiterhin verfügbar.");
        else { permissionPending=true; permission.launch(Manifest.permission.CAMERA); }
    }
    private void fail(String message) { closeCamera(); view.clearFrame(); view.region=null; detectionRegion=null; calibrating=false; stopSession(message); }
    private void openCamera() {
        final long token=++generation;
        setStatus("Kamera startet …");
        var future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            if(token!=generation || !foreground || !camera)return;
            try {
                provider=future.get();
                if(!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) { fail("Keine Rückkamera verfügbar. Bitte Demo wählen."); return; }
                analysis=new ImageAnalysis.Builder().setResolutionSelector(new ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(new ResolutionStrategy(new Size(640,480),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
                    .setTargetRotation(orientationRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(worker,image->analyze(image,token));
                var bound=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,analysis);
                cameraState=bound.getCameraInfo().getCameraState();
                cameraState.observe(this,state->{
                    if(token==generation && state.getError()!=null) fail("Kamerafehler. Demo wählen oder Kamera erneut öffnen.");
                });
                // A bound camera is not ready until an actual analyzed frame arrives.
                lastFrameTime=SystemClock.elapsedRealtime();
                setStatus("Warte auf Kamerabild …");
                main.postDelayed(()->{ if(token==generation && !cameraReady) fail("Kamerastart ohne Bild. Bitte Demo wählen oder Kamera neu öffnen."); },5000);
            } catch(Exception e) { fail("Kamera konnte nicht gestartet werden. Bitte Demo wählen."); }
        },main::post);
    }
    @androidx.annotation.OptIn(markerClass=androidx.camera.core.ExperimentalGetImage.class)
    private void analyze(ImageProxy image,long token) {
        Bitmap upright=null,raw=null;
        try {
            long now=SystemClock.elapsedRealtime();
            if(token!=generation || now-lastAccepted<100 || !deliveryPending.compareAndSet(false,true)) return;
            lastAccepted=now;
            Rect crop=image.getCropRect(); ImageProxy.PlaneProxy[] planes=image.getPlanes();
            Yuv.Plane y=new Yuv.Plane(planes[0].getBuffer(),planes[0].getRowStride(),planes[0].getPixelStride());
            Yuv.Plane u=new Yuv.Plane(planes[1].getBuffer(),planes[1].getRowStride(),planes[1].getPixelStride());
            Yuv.Plane v=new Yuv.Plane(planes[2].getBuffer(),planes[2].getRowStride(),planes[2].getPixelStride());
            Yuv.Range range=Yuv.Range.FULL;
            if(Build.VERSION.SDK_INT>=33 && image.getImage()!=null) {
                int space=image.getImage().getDataSpace();
                if(space==android.hardware.DataSpace.DATASPACE_BT601_625 ||
                    space==android.hardware.DataSpace.DATASPACE_BT601_525 ||
                    space==android.hardware.DataSpace.DATASPACE_BT709 ||
                    space==android.hardware.DataSpace.DATASPACE_BT2020) range=Yuv.Range.LIMITED;
            }
            Yuv.Frame rgb=Yuv.bounded(y,u,v,crop.left,crop.top,crop.width(),crop.height(),640,range);
            raw=Bitmap.createBitmap(rgb.pixels(),rgb.width(),rgb.height(),Bitmap.Config.ARGB_8888);
            int rotation=image.getImageInfo().getRotationDegrees();
            if(rotation!=0) { Matrix matrix=new Matrix(); matrix.postRotate(rotation); upright=Bitmap.createBitmap(raw,0,0,raw.getWidth(),raw.getHeight(),matrix,false); if(upright!=raw)raw.recycle(); } else upright=raw;
            raw=null;
            int w=upright.getWidth(),h=upright.getHeight();
            int[] pixels=new int[w*h]; upright.getPixels(pixels,0,w,0,0,w,h);
            long config=configuration;
            Region roi=detectionRegion;
            Detector.Result detected=roi==null?Detector.Result.missing():detector.detect(pixels,w,h,color,roi);
            Bitmap frame=upright; upright=null;
            main.post(()->{
                try {
                    if(token!=generation || !foreground || !camera) { frame.recycle(); return; }
                    boolean firstFrame=frameRotation==-1;
                    boolean changed=w!=frameWidth || h!=frameHeight || rotation!=frameRotation;
                    frameWidth=w; frameHeight=h; frameRotation=rotation;
                    if(changed) { view.region=null; detectionRegion=null; view.first=null; calibrating=false; reset(firstFrame?"Kamerabild bereit. Wandbereich kalibrieren.":"Bildgeometrie geändert. Bitte neu kalibrieren."); }
                    long delivered=SystemClock.elapsedRealtime();
                    lastFrameTime=now; cameraReady=delivered-now<=350;
                    view.frame(frame); updateControls();
                    if(!changed && config==configuration && cameraReady) accept(detected,now);
                    else {
                        view.observation(null);
                        if(running && !paused) { metrics.observe(delivered/1000.0,false,Double.NaN); renderMetrics(); }
                        if(!cameraReady) setStatus("Bildverarbeitung zu langsam – Ziel wartet");
                    }
                } finally { deliveryPending.set(false); }
            });
        } catch(Exception e) {
            if(raw!=null && !raw.isRecycled()) raw.recycle();
            if(upright!=null) upright.recycle(); deliveryPending.set(false);
            main.post(()->{ if(token==generation) fail("Bildverarbeitung fehlgeschlagen. Kamera neu öffnen oder Demo wählen."); });
        } finally { image.close(); }
    }
    private void closeCamera() {
        generation++; cameraReady=false; frameWidth=frameHeight=0; frameRotation=-1;
        if(cameraState!=null) { cameraState.removeObservers(this); cameraState=null; }
        if(analysis!=null) { analysis.clearAnalyzer(); analysis=null; }
        if(provider!=null) { provider.unbindAll(); provider=null; }
    }
    @Override protected void onStart() { super.onStart(); foreground=true; orientationListener.enable(); main.post(ticker); if(camera)ensurePermission(); }
    @Override protected void onStop() {
        foreground=false; orientationListener.disable(); main.removeCallbacks(ticker); closeCamera();
        if(camera) { detectionRegion=null; view.region=null; view.first=null; calibrating=false; view.clearFrame(); }
        stopSession("Unterbrochen; bewusst neu starten" ); super.onStop();
    }
    @Override protected void onDestroy() { generation++; view.clearFrame(); worker.shutdownNow(); super.onDestroy(); }
}
