package de.laserfollow.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
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
import androidx.camera.view.PreviewView;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.camera.view.transform.CoordinateTransform;
import de.laserfollow.core.*;
import de.laserfollow.core.Geometry.*;
import de.laserfollow.core.Geometry.Point;
import de.laserfollow.core.Geometry.Region;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@androidx.annotation.OptIn(markerClass=androidx.camera.view.TransformExperimental.class)
public final class MainActivity extends ComponentActivity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final SessionMetrics metrics=new SessionMetrics();
    private final Detector detector=new Detector();
    private final Yuv.CandidateBuffer candidateBuffer=new Yuv.CandidateBuffer();
    private long rateWindow; private int rateFrames;
    private final AtomicBoolean deliveryPending=new AtomicBoolean();
    private volatile long generation;
    private volatile long configuration;
    private volatile Region detectionRegion;
    private volatile Detector.Color color=Detector.Color.RED;
    private ProcessCameraProvider provider;
    private ImageAnalysis analysis;
    private Preview preview;
    private PreviewView previewView;
    private FrameMapping mapping;
    private int boundViewWidth,boundViewHeight;
    private androidx.lifecycle.LiveData<CameraState> cameraState;
    private int orientationRotation=Surface.ROTATION_0;
    private boolean permissionDenied,permissionPending;
    private ExerciseView view;
    private TextView status,result,badge;
    private Button start,pause,stop,calibrate;
    private boolean camera,running,paused,calibrating,foreground;
    private boolean cameraReady;
    private double phase,period=12,size=.7;
    private Shape shape=Shape.CIRCLE;
    private long lastFrameTime,lastMetricsRender;
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
                if(now-lastFrameTime>1500) cameraReady=false; updateControls();
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
            if(camera && foreground) { if(granted) openCamera(); else fail("Kamerazugriff fehlt. In Android-Einstellungen erlauben und Kamera neu öffnen."); }
        });
        orientationRotation=getWindowManager().getDefaultDisplay().getRotation();
        buildUi();
        switchMode(true);
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private android.graphics.drawable.GradientDrawable panel(int color,int radius) {
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout card(LinearLayout parent,String title,String subtitle) {
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18),dp(14),dp(18),dp(18)); c.setBackground(panel(0xff142330,24));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(16); parent.addView(c,lp);
        TextView heading=label(title,20); heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)); c.addView(heading);
        if(!subtitle.isEmpty()) { TextView hint=label(subtitle,13); hint.setTextColor(0xffa9bdc9); c.addView(hint); }
        return c;
    }
    private void styleButton(Button b,boolean primary) {
        b.setAllCaps(false); b.setTextSize(15); b.setMinHeight(dp(52)); b.setMinimumWidth(0);
        b.setPadding(dp(8),dp(4),dp(8),dp(4)); b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        int[][] states={new int[]{-android.R.attr.state_enabled},new int[]{}};
        b.setTextColor(new android.content.res.ColorStateList(states,new int[]{0xff718691,primary?0xff082c29:0xffe4edf3}));
        android.content.res.ColorStateList colors=new android.content.res.ColorStateList(states,new int[]{0xff20303c,primary?0xff68e0c5:0xff29414e});
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x337fffff),panel(primary?0xff68e0c5:0xff29414e,16),null));
        b.setBackgroundTintList(colors);
    }
    private TextView label(String text,int sp) { TextView t=new TextView(this); t.setText(text); t.setTextColor(0xffe4edf9); t.setTextSize(sp); t.setPadding(0,dp(6),0,dp(6)); return t; }
    private void buildUi() {
        LinearLayout screen=new LinearLayout(this); screen.setOrientation(LinearLayout.VERTICAL); screen.setBackgroundColor(0xff0b151e);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(4),dp(12),dp(8)); screen.addView(root); setContentView(screen);
        // Keep controls inside system bars, including Android 15 edge-to-edge enforcement.
        screen.setOnApplyWindowInsetsListener((v,insets)->{ v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets; });
        badge=label("Laserfollow · Live-Kamera",18); badge.setSingleLine(true); badge.setEllipsize(android.text.TextUtils.TruncateAt.END); root.addView(badge);
        LinearLayout session=root;
        FrameLayout cameraLayer=new FrameLayout(this);
        cameraLayer.setBackground(panel(0xff0c1923,20)); cameraLayer.setClipToOutline(true);
        int cameraHeight=Math.max(80,Math.min(320,getResources().getConfiguration().screenHeightDp-348));
        session.addView(cameraLayer,new LinearLayout.LayoutParams(-1,dp(cameraHeight)));
        previewView=new PreviewView(this);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setVisibility(View.GONE);
        cameraLayer.addView(previewView,new FrameLayout.LayoutParams(-1,-1));
        view=new ExerciseView(this); cameraLayer.addView(view,new FrameLayout.LayoutParams(-1,-1));
        previewView.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(camera && foreground && analysis!=null &&
                (r-l!=boundViewWidth || b-t!=boundViewHeight)) {
                closeCamera(); view.region=null; detectionRegion=null; view.first=null; calibrating=false;
                reset("Ansicht geändert. Bitte neu kalibrieren."); openCamera();
            }
        });
        status=label("",14); status.setLines(2); status.setEllipsize(android.text.TextUtils.TruncateAt.END); status.setTextColor(0xffb8cbd5); session.addView(status);
        LinearLayout controls=new LinearLayout(this); session.addView(controls);
        start=button("Start",controls,()->startSession()); pause=button("Pause",controls,()->togglePause()); stop=button("Stopp",controls,()->stopSession("Beendet"));
        LinearLayout quick=new LinearLayout(this); session.addView(quick);
        calibrate=button("Bereich festlegen",quick,()->{});
        button("Kamera neu öffnen",quick,()->{ closeCamera(); view.clearFrame(); view.region=null; detectionRegion=null; reset("Kamera wird vorbereitet"); permissionDenied=false; ensurePermission(); });
        Switch targetToggle=new Switch(this); targetToggle.setText("Gelben Zielpunkt anzeigen"); targetToggle.setTextColor(0xffe4edf3); targetToggle.setMinHeight(dp(48)); session.addView(targetToggle);
        targetToggle.setOnCheckedChangeListener((button,checked)->{ view.showTarget=checked; reset(checked?"Zielpunkt eingeschaltet · neu starten":"Freies Nachfahren · neu starten"); view.invalidate(); });
        ScrollView detailsScroll=new ScrollView(this); screen.addView(detailsScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(12),0,dp(12),dp(16)); detailsScroll.addView(details);
        LinearLayout results=card(details,"Dein Verlauf","Bildbasierte Messwerte · keine medizinische Bewertung");
        result=label("Starte eine Übung, um deinen Verlauf zu sehen.",15); result.setMinLines(6); result.setLineSpacing(dp(4),1); results.addView(result);
        LinearLayout settings=card(details,"Übung gestalten","Änderungen starten die Messung neu.");
        settings.addView(label("FORM",11));
        selector(settings,new String[]{"Kreis","Linie · hin und zurück","Acht"},i->{ shape=Shape.values()[i]; view.shape=shape; reset("Bahn geändert"); });
        settings.addView(label("LASERFARBE",11));
        selector(settings,new String[]{"Roter Punkt","Grüner Punkt"},i->{ color=i==0?Detector.Color.RED:Detector.Color.GREEN; reset("Farbe geändert"); });
        TextView sizeLabel=label("Bahngröße: 70 % des Bereichs",14); settings.addView(sizeLabel);
        SeekBar sizeBar=new SeekBar(this); sizeBar.setMax(65); sizeBar.setProgress(45); settings.addView(sizeBar);
        sizeBar.setOnSeekBarChangeListener(seek((v)->{ size=(25+v)/100.0; view.size=size; sizeLabel.setText("Bahngröße: "+(25+v)+" % des Bereichs"); reset("Bahngröße geändert"); }));
        TextView periodLabel=label("Umlaufzeit: 12 s · frei wählbar",14); settings.addView(periodLabel);
        SeekBar periodBar=new SeekBar(this); periodBar.setMax(26); periodBar.setProgress(8); settings.addView(periodBar);
        periodBar.setOnSeekBarChangeListener(seek(v->{ period=v+4; periodLabel.setText("Umlaufzeit: "+(v+4)+" s · frei wählbar"); reset("Umlaufzeit geändert"); }));
        calibrate.setOnClickListener(v->{ if(camera && !cameraReady) { setStatus("Zuerst ein Kamerabild abwarten"); return; } reset("Zwei gegenüberliegende Ecken im Bild antippen"); calibrating=true; view.first=null; view.region=null; detectionRegion=null; updateControls(); });
        view.tap=p->{
            if(!calibrating) return;
            if(view.first==null) { view.first=p; setStatus("Jetzt die gegenüberliegende Ecke antippen"); }
            else { try {
                Region r=Region.corners(view.first,p);
                Region source=camera?(mapping==null?null:mapping.sourceRegion(r)):r;
                if(source==null) throw new IllegalArgumentException("Bildzuordnung fehlt");
                view.region=r; detectionRegion=source; calibrating=false; view.first=null;
                reset("Bereich fixiert. Telefon auf dem Stativ lassen."); }
                catch(IllegalArgumentException e) { view.first=null; setStatus(e.getMessage()+". Bitte beide Ecken erneut antippen."); } }
            view.invalidate(); updateControls();
        };
        LinearLayout info=card(details,"Gut vorbereitet","Bitte vor der ersten Kameraübung lesen.");
        info.addView(label("Türkis: Bahn · Gelb: Ziel · Weiß: aktueller Punkt · Blau: Bewegungsspur der letzten 3.000 Beobachtungen. Pause und Stopp behalten die Spur. Start und neue Einstellungen löschen sie. Erkennungslücken werden nicht verbunden. Bei fehlendem oder mehrdeutigem Punkt wartet das Ziel. Stillhalten auf der Bahn bedeutet nicht, dass die Form nachgefahren wurde.",14));
        info.addView(label("Fester Bildbereich für Stativbetrieb, keine AR-Verankerung. Bei bewegtem Telefon wandert die Wand hinter der Markierung: dann neu kalibrieren. Nur Bildkoordinaten, keine Zentimeter oder Halswinkel.",14));
        info.addView(label("Physio-Forschungsprototyp, nicht klinisch validiert. Nur mit professioneller Begleitung. Bei Schmerzen oder Schwindel stoppen. Laser niemals auf Augen, Menschen oder reflektierende Flächen richten.",14));
        info.addView(label("Erkennungsgrenzen: weiße überbelichtete Zentren, Sonne, Reflexionen und farbige Gegenstände können die Messung stören. Keine Gesundheits- oder Erholungsbewertung. Keine Speicherung von Bildern oder Sitzungen.",14));
        updateControls();
    }
    private interface Choice { void select(int i); }
    private void selector(LinearLayout root,String[] entries,Choice choice) {
        Spinner s=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,entries); s.setAdapter(adapter); s.setMinimumHeight(dp(52)); s.setPadding(dp(12),0,dp(12),0); s.setBackground(panel(0xff223744,14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52)); lp.bottomMargin=dp(12); root.addView(s,lp);
        s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() { int previous=0;
            public void onItemSelected(AdapterView<?> p,View v,int i,long id) { if(i!=previous) { previous=i; choice.select(i); } }
            public void onNothingSelected(AdapterView<?> p) {} });
    }
    private SeekBar.OnSeekBarChangeListener seek(Choice c) { return new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar s,int p,boolean user) { if(user)c.select(p); }
        public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {} }; }
    private Button button(String text,LinearLayout parent,Runnable action) { Button b=new Button(this); b.setText(text); styleButton(b,text.equals("Start")); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),dp(4),dp(3),dp(4)); parent.addView(b,lp); b.setOnClickListener(v->action.run()); return b; }
    private void setStatus(String s) { String text=(camera?"Kamera · ":"Demo – simulierte Daten · ")+s; if(!text.contentEquals(status.getText()))status.setText(text); }
    private void updateControls() {
        start.setEnabled(!running && !calibrating && (!camera || cameraReady && view.region!=null));
        pause.setEnabled(running); stop.setEnabled(running); pause.setText(paused?"Weiter":"Pause");
        if(calibrate!=null) calibrate.setEnabled(!camera || cameraReady);
    }
    private void renderMetrics() {
        String prefix=camera?"Kamera-Ergebnis":"Demo – simulierte Daten";
        if(!metrics.sufficientData()) result.setText(prefix+"\nUnzureichende Daten · mindestens 0,5 s gültig erfassen");
        else {
            String target=view.showTarget?String.format(Locale.GERMAN,"\nAbstand zum Zielpunkt: %.2f %%",metrics.meanTargetErrorPercent()):"";
            result.setText(String.format(Locale.GERMAN,"%s\nBahngenauigkeit: %.0f %%\nAnteil erkannter Positionen innerhalb der Toleranz: 2 %% der Bereichsdiagonale.\nMittlerer Bahnabstand: %.2f %%%s\nErfasst: %.1f s · Erkennungsabdeckung: %.1f %%\nBewertet Bahnnähe, nicht das vollständige Nachfahren. Erkennungslücken sind nicht im Genauigkeitswert enthalten.",prefix,metrics.pathAccuracyPercent(),metrics.meanErrorPercent(),target,metrics.trackedSeconds(),metrics.coveragePercent()));
        }
    }
    private void reset(String message) { configuration++; running=false; paused=false; phase=0; view.clearFrame(); metrics.reset(); view.phase=0; view.observation(null); renderMetrics(); setStatus(message); updateControls(); }
    private void startSession() { if(running) return; if(camera && (!cameraReady || view.region==null) || calibrating) return; configuration++; view.clearFrame(); metrics.reset(); phase=0; running=true; paused=false; view.phase=0; view.observation(null); setStatus("Läuft · Punkt suchen"); renderMetrics(); updateControls(); }
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
        if(now-lastMetricsRender>=250) { renderMetrics(); lastMetricsRender=now; }
    }
    private void switchMode(boolean useCamera) {
        closeCamera(); camera=useCamera; view.demo=!camera; view.clearFrame(); view.first=null; calibrating=false;
        previewView.setVisibility(camera?View.VISIBLE:View.GONE);
        setRequestedOrientation(camera?ActivityInfo.SCREEN_ORIENTATION_LOCKED:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        orientationRotation=getWindowManager().getDefaultDisplay().getRotation();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        permissionDenied=false;
        detectionRegion=null; view.region=camera?null:new Region(.12,.12,.88,.88);
        badge.setText(camera?"Kamera – Live-Erkennung":"Demo – simulierte Daten"); reset(camera?"Kamera wird vorbereitet; danach kalibrieren":"Simulation bereit");
        if(camera && foreground) ensurePermission();
    }
    private void ensurePermission() {
        if(permissionPending) return;
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) openCamera();
        else if(permissionDenied) fail("Kamerazugriff fehlt. In Android-Einstellungen erlauben und Kamera neu öffnen.");
        else { permissionPending=true; permission.launch(Manifest.permission.CAMERA); }
    }
    private void fail(String message) { closeCamera(); view.clearFrame(); view.region=null; detectionRegion=null; calibrating=false; stopSession(message); }
    private void openCamera() {
        if(previewView.getWidth()==0 || previewView.getHeight()==0) {
            previewView.post(()->{if(camera && foreground)openCamera();}); return;
        }
        final long token=++generation;
        setStatus("Kamera startet …");
        var future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            if(token!=generation || !foreground || !camera)return;
            try {
                provider=future.get();
                if(!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) { fail("Keine Rückkamera verfügbar. Bitte Kamera neu öffnen."); return; }
                boundViewWidth=previewView.getWidth(); boundViewHeight=previewView.getHeight();
                orientationRotation=getWindowManager().getDefaultDisplay().getRotation();
                preview=new Preview.Builder().setTargetRotation(orientationRotation).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                analysis=new ImageAnalysis.Builder().setResolutionSelector(new ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(new ResolutionStrategy(new Size(640,480),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
                    .setTargetRotation(orientationRotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(worker,image->analyze(image,token));
                ViewPort viewport=previewView.getViewPort();
                if(viewport==null) { fail("Kameraansicht noch nicht bereit. Bitte erneut öffnen."); return; }
                UseCaseGroup group=new UseCaseGroup.Builder().setViewPort(viewport).addUseCase(preview).addUseCase(analysis).build();
                var bound=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,group);
                cameraState=bound.getCameraInfo().getCameraState();
                cameraState.observe(this,state->{
                    if(token==generation && state.getError()!=null) fail("Kamerafehler. Kamera erneut öffnen.");
                });
                // A bound camera is not ready until an actual analyzed frame arrives.
                lastFrameTime=SystemClock.elapsedRealtime();
                setStatus("Warte auf Kamerabild …");
                main.postDelayed(()->{ if(token==generation && frameRotation==-1) fail("Kamerastart ohne Bild. Bitte Kamera neu öffnen."); },5000);
            } catch(Exception e) { fail("Kamera konnte nicht gestartet werden. Bitte Kamera neu öffnen."); }
        },main::post);
    }
    @androidx.annotation.OptIn(markerClass=androidx.camera.core.ExperimentalGetImage.class)
    private void analyze(ImageProxy image,long token) {
        try {
            long now=SystemClock.elapsedRealtime();
            if(token!=generation || !deliveryPending.compareAndSet(false,true)) return;

            long config=configuration;
            Region roi=detectionRegion;
            Rect crop=image.getCropRect();
            int w=crop.width(),h=crop.height();
            int rotation=image.getImageInfo().getRotationDegrees();
            ImageProxyTransformFactory factory=new ImageProxyTransformFactory();
            factory.setUsingCropRect(true);
            factory.setUsingRotationDegrees(false); // Detector reads the unrotated cropped buffer.
            OutputTransform source=factory.getOutputTransform(image);
            Detector.Result observation=Detector.Result.missing();
            if(roi!=null) {
                ImageProxy.PlaneProxy[] planes=image.getPlanes();
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
                Detector.Color selected=color;
                Yuv.Frame rgb=candidateBuffer.read(y,u,v,crop.left,crop.top,w,h,range,selected);
                observation=detector.detectMoving(rgb.pixels(),rgb.width(),rgb.height(),selected,roi);
            }
            Detector.Result detected=observation;
            main.post(()->{
                try {
                    if(token!=generation || !foreground || !camera) return;
                    OutputTransform target=previewView.getOutputTransform();
                    if(target==null) { cameraReady=false; mapping=null; view.observation(null); updateControls(); return; }
                    Matrix matrix=new Matrix();
                    new CoordinateTransform(source,target).transform(matrix);
                    float[] values=new float[9]; matrix.getValues(values);
                    mapping=FrameMapping.pixelMatrix(values,w,h,previewView.getWidth(),previewView.getHeight());
                    boolean firstFrame=frameRotation==-1;
                    boolean changed=w!=frameWidth || h!=frameHeight || rotation!=frameRotation;
                    frameWidth=w; frameHeight=h; frameRotation=rotation;
                    if(changed) { view.region=null; detectionRegion=null; view.first=null; calibrating=false; reset(firstFrame?"Kamerabild bereit. Wandbereich kalibrieren.":"Bildgeometrie geändert. Bitte neu kalibrieren."); }
                    if(view.region!=null) detectionRegion=mapping.sourceRegion(view.region);
                    long delivered=SystemClock.elapsedRealtime();
                    rateFrames++;
                    if(delivered-rateWindow>=1000) {
                        if(rateWindow!=0) badge.setText(String.format(Locale.GERMAN,"Laserfollow · %.0f Bilder/s · %d ms Verarbeitung",rateFrames*1000.0/(delivered-rateWindow),delivered-now));
                        rateWindow=delivered; rateFrames=0;
                    }
                    lastFrameTime=now; cameraReady=delivered-now<=1500;
                    updateControls();
                    if(!changed && config==configuration && delivered-now<=350) {
                        Detector.Result mapped=detected;
                        if(detected.state()==Detector.State.DETECTED) {
                            Point point=mapping.toPreview(detected.point());
                            mapped=view.region!=null && view.region.contains(point.x(),point.y())?
                                new Detector.Result(Detector.State.DETECTED,point):Detector.Result.missing();
                        }
                        accept(mapped,now);
                    }
                    else {
                        view.observation(null);
                        if(running && !paused) { metrics.observe(delivered/1000.0,false,Double.NaN); renderMetrics(); }
                        if(delivered-now>350) setStatus("Bildverarbeitung zu langsam – Ziel wartet");
                    }
                } catch(IllegalArgumentException | IllegalStateException e) {
                    mapping=null; cameraReady=false; view.region=null; detectionRegion=null; calibrating=false;
                    stopSession("Bildzuordnung geändert. Bitte neu kalibrieren.");
                } finally { deliveryPending.set(false); }
            });
        } catch(Exception e) {
            deliveryPending.set(false);
            main.post(()->{ if(token==generation) fail("Bildverarbeitung fehlgeschlagen. Kamera neu öffnen."); });
        } finally { image.close(); }
    }
    private void closeCamera() {
        generation++; cameraReady=false; frameWidth=frameHeight=0; frameRotation=-1;
        mapping=null; preview=null;
        if(cameraState!=null) { cameraState.removeObservers(this); cameraState=null; }
        if(analysis!=null) { analysis.clearAnalyzer(); analysis=null; }
        if(provider!=null) { provider.unbindAll(); provider=null; }
    }
    @Override protected void onStart() { super.onStart(); foreground=true; main.post(ticker); if(camera)ensurePermission(); }
    @Override protected void onStop() {
        foreground=false; main.removeCallbacks(ticker); closeCamera();
        if(camera) { detectionRegion=null; view.region=null; view.first=null; calibrating=false; view.clearFrame(); }
        stopSession("Unterbrochen; bewusst neu starten" ); super.onStop();
    }
    @Override protected void onDestroy() { generation++; view.clearFrame(); worker.shutdownNow(); super.onDestroy(); }
}
