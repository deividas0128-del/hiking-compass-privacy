package lt.deikai.balsiulektuvas;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_MIC = 7;
    private GameView game;
    private MicEngine mic;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(244,248,251));
        getWindow().setNavigationBarColor(Color.rgb(11,134,235));
        game = new GameView(this);
        mic = new MicEngine();
        setContentView(game);
    }

    void toggleMic() {
        if (mic.running) { mic.stop(); game.micOn=false; return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED) mic.start();
        else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);
    }
    void calibrate() {
        if (!mic.running) { game.message="Pirma įjunk mikrofoną"; game.invalidate(); return; }
        mic.calibrating=true; mic.calStart=SystemClock.elapsedRealtime(); mic.sum=0; mic.count=0;
        game.message="Tark balsę tolygiai ~2 s";
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==REQ_MIC){
            if(g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED) mic.start();
            else { game.message="Mikrofono leidimas nesuteiktas"; game.invalidate(); }
        }
    }
    @Override protected void onPause(){super.onPause();mic.stop();}
    @Override protected void onDestroy(){mic.stop();super.onDestroy();}

    final class MicEngine {
        volatile boolean running=false, calibrating=false;
        Thread t; float offset=112f; long calStart; double sum; int count;
        void start(){
            if(running)return; running=true; game.micOn=true; game.message="Mikrofonas veikia ✓";
            t=new Thread(this::loop,"mic");t.start();
        }
        void stop(){running=false; if(t!=null)t.interrupt(); t=null; game.micOn=false;}
        AudioRecord recorder(int src,int bs){try{AudioRecord r=new AudioRecord(src,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bs);if(r.getState()==AudioRecord.STATE_INITIALIZED)return r;r.release();}catch(Throwable ignored){}return null;}
        void loop(){
            int min=AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT); if(min<1)min=4096; int bs=Math.max(8192,min*2);
            AudioRecord r=recorder(MediaRecorder.AudioSource.UNPROCESSED,bs); if(r==null)r=recorder(MediaRecorder.AudioSource.VOICE_RECOGNITION,bs); if(r==null)r=recorder(MediaRecorder.AudioSource.MIC,bs);
            if(r==null){running=false;game.message="Nepavyko paleisti mikrofono";game.invalidate();return;}
            short[] buf=new short[2048];
            try{
                r.startRecording();
                while(running && !Thread.currentThread().isInterrupted()){
                    int n=r.read(buf,0,buf.length); if(n<=0)continue;
                    double ss=0; for(int i=0;i<n;i++){double v=buf[i]/32768.0;ss+=v*v;}
                    double rms=Math.sqrt(ss/Math.max(1,n)); double raw=20*Math.log10(Math.max(rms,1e-7));
                    if(calibrating){ if(raw>-60){sum+=raw;count++;} if(SystemClock.elapsedRealtime()-calStart>1800){ if(count>3){offset=(float)(75-(sum/count));game.message="Sukalibruota ✓";} else game.message="Kalibravimas nepavyko"; calibrating=false; }}
                    float db=(float)(raw<-72?0:raw+offset); db=Math.max(0,Math.min(105,db)); game.setMicDb(db);
                }
            }catch(Throwable e){game.message="Mikrofono klaida";}finally{try{r.stop();}catch(Throwable ignored){}r.release();running=false;game.micOn=false;}
        }
    }

    final class GameView extends View {
        Paint p=new Paint(3); Paint stroke=new Paint(3); RectF rf=new RectF();
        String[] vowels={"A","E","Ė","I","O","U"}; int vowel=0;
        volatile float micDb=0; float db=0, filtered=0; boolean micOn=false;
        float meters=0,current=0,best=0; float planeY=0,vy=0,pitch=0,airspeed=0,engine=0,world=0;
        long last=System.nanoTime(); String message="Tark AAAAA…";
        RectF micBtn=new RectF(),resetBtn=new RectF(),calBtn=new RectF(); RectF[] vbtn=new RectF[6];
        public GameView(Context c){super(c); p.setTypeface(Typeface.create("sans",Typeface.NORMAL)); for(int i=0;i<6;i++)vbtn[i]=new RectF(); setBackgroundColor(Color.rgb(20,145,235));}
        void setMicDb(float v){micDb=v;postInvalidateOnAnimation();}
        float dp(float v){return v*getResources().getDisplayMetrics().density;}
        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align a,boolean bold){p.setColor(color);p.setTextSize(size);p.setTextAlign(a);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
        void rr(Canvas c,float l,float t,float r,float b,float rad,int color){p.setColor(color);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();if(planeY==0)planeY=h*.49f;
            long now=System.nanoTime();float dt=Math.min(.05f,(now-last)/1_000_000_000f);last=now;update(dt,h);
            c.drawColor(Color.rgb(18,146,235));
            float pad=w*.025f;
            text(c,"Balsių lėktuvas",w/2,h*.052f,w*.068f,Color.WHITE,Paint.Align.CENTER,true);
            // meter card
            rr(c,pad,h*.072f,w-pad,h*.245f,w*.045f,Color.rgb(248,251,253));
            text(c,String.format(Locale.US,"%.0f",db),w*.47f,h*.135f,w*.095f,Color.rgb(13,81,158),Paint.Align.CENTER,true);
            text(c,"dB",w*.59f,h*.135f,w*.045f,Color.rgb(13,81,158),Paint.Align.LEFT,true);
            float bx=w*.09f,by=h*.165f,bw=w*.027f,g=w*.008f; for(int i=0;i<24;i++){float level=45+i*2.1f;int col=Color.rgb(221,227,233);if(db>=level) col=level<70?Color.rgb(255,171,49):(level<=80?Color.rgb(47,197,70):Color.rgb(241,75,67));rr(c,bx+i*(bw+g),by,bx+i*(bw+g)+bw,by+h*.026f,bw/2,col);} rr(c,w*.29f,h*.207f,w*.71f,h*.238f,h*.015f,Color.rgb(30,175,62));text(c,"Tikslas 70–80 dB",w/2,h*.231f,w*.043f,Color.WHITE,Paint.Align.CENTER,true);
            // scene
            float st=h*.265f,sb=h*.64f; rr(c,pad,st,w-pad,sb,w*.04f,Color.rgb(111,203,244)); drawScene(c,pad,st,w-pad,sb);
            rr(c,w*.34f,st+h*.018f,w*.67f,st+h*.065f,h*.02f,Color.WHITE);text(c,message,w*.505f,st+h*.052f,w*.037f,Color.rgb(20,91,165),Paint.Align.CENTER,true);
            // stats
            float sy=h*.655f,sh=h*.075f; float gap=w*.018f,sw=(w-pad*2-gap*2)/3;String[] labs={"🛣 Nuskrista","⏱ Dabartinis","★ Geriausias"};String[] vals={(int)meters+" m",String.format(Locale.US,"%.1f s",current),String.format(Locale.US,"%.1f s",best)};for(int i=0;i<3;i++){float l=pad+i*(sw+gap);rr(c,l,sy,l+sw,sy+sh,h*.02f,Color.rgb(248,252,253));text(c,labs[i],l+sw/2,sy+sh*.34f,w*.027f,Color.rgb(57,91,125),Paint.Align.CENTER,false);text(c,vals[i],l+sw/2,sy+sh*.78f,w*.045f,Color.rgb(10,76,145),Paint.Align.CENTER,true);}
            // vowels
            float vy0=h*.742f,vh=h*.068f;float vg=w*.012f,vw=(w-pad*2-vg*5)/6;for(int i=0;i<6;i++){float l=pad+i*(vw+vg);vbtn[i].set(l,vy0,l+vw,vy0+vh);rr(c,l,vy0,l+vw,vy0+vh,h*.018f,i==vowel?Color.rgb(97,219,28):Color.WHITE);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(w*.006f);stroke.setColor(i==vowel?Color.rgb(159,241,74):Color.rgb(23,102,194));c.drawRoundRect(vbtn[i],h*.018f,h*.018f,stroke);stroke.setStyle(Paint.Style.FILL);text(c,vowels[i],l+vw/2,vy0+vh*.7f,w*.052f,i==vowel?Color.WHITE:Color.rgb(21,86,168),Paint.Align.CENTER,true);}
            // buttons
            float bt=h*.825f,bh=h*.055f;micBtn.set(pad,bt,w*.49f,bt+bh);resetBtn.set(w*.51f,bt,w-pad,bt+bh);rr(c,micBtn.left,bt,micBtn.right,bt+bh,h*.018f,Color.rgb(52,202,72));rr(c,resetBtn.left,bt,resetBtn.right,bt+bh,h*.018f,Color.WHITE);text(c,micOn?"🎤 Mikrofonas ✓":"🎤 Mikrofonas",micBtn.centerX(),bt+bh*.68f,w*.033f,Color.WHITE,Paint.Align.CENTER,true);text(c,"↻ Iš naujo",resetBtn.centerX(),bt+bh*.68f,w*.033f,Color.rgb(19,91,173),Paint.Align.CENTER,true);
            calBtn.set(pad,h*.895f,w-pad,h*.95f);rr(c,calBtn.left,calBtn.top,calBtn.right,calBtn.bottom,h*.018f,Color.rgb(255,211,67));text(c,"Kalibruoti 75 dB",w/2,calBtn.top+(calBtn.height()*.68f),w*.034f,Color.rgb(95,72,22),Paint.Align.CENTER,true);
            postInvalidateOnAnimation();
        }
        void update(float dt,float h){float target=micOn?micDb:0; float k=target>filtered?8f:4f; filtered+= (target-filtered)*Math.min(1,k*dt); db=filtered;
            if(db<45){engine+=(0-engine)*Math.min(1,8*dt);airspeed+=(0-airspeed)*Math.min(1,8*dt);vy+=(0-vy)*Math.min(1,8*dt);pitch+=(0-pitch)*Math.min(1,6*dt);current=0;message="Tark "+vowels[vowel].repeat(5)+"…";return;}
            float desired;if(db<70){desired=.25f+(db-45)/25f*.45f;message="Dar truputį garsiau";}else if(db<=80){desired=.72f+(db-70)/10f*.12f;message="Puiku! Skrendi teisingai!";}else{desired=Math.min(1f,.85f+(db-80)/25f*.15f);message="Per garsiai – tyliau";}
            engine+=(desired-engine)*Math.min(1,2.8f*dt);float targetSpeed=35+engine*105;airspeed+=(targetSpeed-airspeed)*Math.min(1,1.8f*dt);
            float targetY=h*.49f;if(db<70)targetY=h*.56f-(db-45)*h*.003f;else if(db<=80)targetY=h*.43f;else targetY=Math.max(h*.31f,h*.43f-(db-80)*h*.005f);
            float err=targetY-planeY;float accel=err*1.2f-vy*1.8f;vy+=accel*dt;vy=Math.max(-h*.22f,Math.min(h*.22f,vy));planeY+=vy*dt;pitch+=(Math.max(-.12f,Math.min(.09f,-vy/h*2.4f))-pitch)*Math.min(1,3*dt);
            meters+=airspeed*dt*.11f;world+=airspeed*dt;if(db>=70&&db<=80){current+=dt;best=Math.max(best,current);}else current=0;
        }
        void drawScene(Canvas c,float l,float t,float r,float b){c.save();c.clipRect(l,t,r,b);float w=r-l,h=b-t;p.setColor(Color.rgb(105,203,245));c.drawRect(l,t,r,b,p);
            // moving clouds
            for(int i=0;i<5;i++){float x=l+((i*210-world*.12f)%(w+240)+w+240)%(w+240)-80;float y=t+h*(.12f+.1f*(i%3));cloud(c,x,y,w*.07f);}
            p.setColor(Color.rgb(122,190,104));Path hill=new Path();hill.moveTo(l,t+h*.62f);for(int i=0;i<=8;i++){float x=l+i*w/8;float y=t+h*(.58f+.05f*(float)Math.sin((i+world*.002f)));hill.lineTo(x,y);}hill.lineTo(r,b);hill.lineTo(l,b);hill.close();c.drawPath(hill,p);
            p.setColor(Color.rgb(139,205,105));c.drawRect(l,t+h*.67f,r,b,p);
            // scenery
            for(int i=0;i<14;i++){float x=l+((i*95-world*.55f)%(w+120)+w+120)%(w+120)-60;float gy=b-h*.12f;if(i%5==0)house(c,x,gy,w*.035f);else if(i%4==0)cow(c,x,gy,w*.028f);else tree(c,x,gy,w*.035f);}
            p.setColor(Color.rgb(73,82,95));c.drawRect(l,b-h*.105f,r,b,p);p.setColor(Color.WHITE);for(float x=l-((world*1.3f)%120);x<r;x+=120)c.drawRect(x,b-h*.055f,x+55,b-h*.045f,p);
            drawPlane(c,l+w*.42f,planeY,w*.32f,pitch);
            if(db>=45){p.setColor(Color.argb(120,255,255,255));for(int i=0;i<5;i++){float sx=l+w*.42f-w*.16f-i*w*.027f;float sy=planeY+w*.002f+(float)Math.sin(world*.03+i)*3;c.drawCircle(sx,sy,w*(.008f+i*.002f),p);}}
            c.restore();
        }
        void cloud(Canvas c,float x,float y,float s){p.setColor(Color.argb(235,255,255,255));c.drawCircle(x,y,s*.55f,p);c.drawCircle(x+s*.5f,y-s*.15f,s*.7f,p);c.drawCircle(x+s,y,s*.55f,p);}
        void tree(Canvas c,float x,float y,float s){p.setColor(Color.rgb(119,84,50));c.drawRect(x-s*.08f,y-s*.5f,x+s*.08f,y,p);p.setColor(Color.rgb(43,139,66));c.drawCircle(x,y-s*.65f,s*.45f,p);c.drawCircle(x-s*.25f,y-s*.5f,s*.35f,p);c.drawCircle(x+s*.25f,y-s*.5f,s*.35f,p);}
        void house(Canvas c,float x,float y,float s){p.setColor(Color.rgb(247,224,187));c.drawRect(x-s*.5f,y-s*.5f,x+s*.5f,y,p);p.setColor(Color.rgb(205,78,60));Path q=new Path();q.moveTo(x-s*.65f,y-s*.5f);q.lineTo(x,y-s);q.lineTo(x+s*.65f,y-s*.5f);q.close();c.drawPath(q,p);p.setColor(Color.rgb(83,123,165));c.drawRect(x-s*.25f,y-s*.35f,x-s*.05f,y-s*.1f,p);}
        void cow(Canvas c,float x,float y,float s){p.setColor(Color.WHITE);c.drawOval(x-s*.6f,y-s*.45f,x+s*.35f,y,p);p.setColor(Color.DKGRAY);c.drawCircle(x-s*.25f,y-s*.28f,s*.12f,p);c.drawCircle(x+s*.25f,y-s*.18f,s*.1f,p);c.drawCircle(x+s*.48f,y-s*.3f,s*.18f,p);}
        void drawPlane(Canvas c,float x,float y,float size,float ang){c.save();c.translate(x,y);c.rotate((float)Math.toDegrees(ang));float s=size/300f;stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(2*s);stroke.setColor(Color.rgb(83,105,125));p.setColor(Color.rgb(249,250,252));Path body=new Path();body.moveTo(-135*s,0);body.quadTo(-118*s,-24*s,-85*s,-25*s);body.lineTo(72*s,-24*s);body.quadTo(130*s,-22*s,147*s,0);body.quadTo(137*s,25*s,88*s,29*s);body.lineTo(-95*s,29*s);body.quadTo(-130*s,24*s,-135*s,0);body.close();c.drawPath(body,p);c.drawPath(body,stroke);
            p.setColor(Color.rgb(21,111,202));Path belly=new Path();belly.moveTo(-125*s,15*s);belly.quadTo(-30*s,32*s,90*s,24*s);belly.quadTo(130*s,20*s,145*s,9*s);belly.quadTo(132*s,31*s,90*s,34*s);belly.lineTo(-108*s,34*s);belly.close();c.drawPath(belly,p);
            p.setColor(Color.rgb(241,194,49));c.drawRect(-115*s,9*s,128*s,14*s,p);
            p.setColor(Color.rgb(232,69,55));Path tail=new Path();tail.moveTo(-105*s,2*s);tail.lineTo(-92*s,-72*s);tail.lineTo(-42*s,2*s);tail.close();c.drawPath(tail,p);p.setColor(Color.WHITE);c.drawRect(-90*s,-30*s,-52*s,-23*s,p);p.setColor(Color.rgb(241,194,49));c.drawRect(-92*s,-20*s,-46*s,-13*s,p);
            p.setColor(Color.rgb(238,241,245));rf.set(-130*s,-5*s,-73*s,23*s);c.drawRoundRect(rf,14*s,14*s,p);c.drawRoundRect(rf,14*s,14*s,stroke);p.setColor(Color.rgb(25,31,38));c.drawCircle(-75*s,9*s,9*s,p);
            p.setColor(Color.rgb(240,243,247));Path wing=new Path();wing.moveTo(-20*s,15*s);wing.lineTo(-75*s,57*s);wing.lineTo(25*s,50*s);wing.lineTo(66*s,18*s);wing.close();c.drawPath(wing,p);c.drawPath(wing,stroke);
            p.setColor(Color.rgb(30,102,157));for(int i=0;i<7;i++)c.drawOval((-35+i*21)*s,-11*s,(-27+i*21)*s,0,p);p.setColor(Color.rgb(47,101,143));Path cp=new Path();cp.moveTo(78*s,-18*s);cp.lineTo(111*s,-16*s);cp.lineTo(128*s,-3*s);cp.lineTo(91*s,-3*s);cp.close();c.drawPath(cp,p);
            stroke.setStrokeWidth(3*s);stroke.setColor(Color.rgb(104,120,132));for(float wx:new float[]{-12,88}){c.drawLine(wx*s,27*s,wx*s,43*s,stroke);p.setColor(Color.rgb(30,30,30));c.drawCircle(wx*s,49*s,11*s,p);p.setColor(Color.rgb(190,201,209));c.drawCircle(wx*s,49*s,5*s,p);}stroke.setStyle(Paint.Style.FILL);c.restore();}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();for(int i=0;i<6;i++)if(vbtn[i].contains(x,y)){vowel=i;message="Tark "+vowels[i].repeat(5)+"…";return true;}if(micBtn.contains(x,y)){toggleMic();return true;}if(resetBtn.contains(x,y)){meters=current=best=0;airspeed=engine=vy=0;message="Tark "+vowels[vowel].repeat(5)+"…";return true;}if(calBtn.contains(x,y)){calibrate();return true;}return true;}
    }
}
