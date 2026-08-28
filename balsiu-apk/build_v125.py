from pathlib import Path
import runpy, re

# Preserve v1.24 full-screen UI, sensitivity slider and v1.22 aircraft.
runpy.run_path('build_v124.py', run_name='__main__')

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')

# Faster flight: raise requested airspeed while keeping the existing aerodynamic smoothing.
if re.search(r'float targetSpeed\s*=\s*[^;]+;', s):
    s = re.sub(r'float targetSpeed\s*=\s*([^;]+);', r'float targetSpeed = (\1) * 1.38f;', s, count=1)
elif re.search(r'float thrust\s*=\s*[^;]+;', s):
    s = re.sub(r'float thrust\s*=\s*([^;]+);', r'float thrust = (\1) * 1.28f;', s, count=1)

# Permit the faster model to keep accelerating instead of hitting the older cap too early.
s = s.replace('Math.min(152f, airspeed)', 'Math.min(205f, airspeed)', 1)
s = s.replace('Math.min(160f, airspeed)', 'Math.min(205f, airspeed)', 1)

# Draw distance-dependent scenery and more moving sky objects immediately before the aircraft.
plane_call = '            drawPlane(c, px, py, ps, pitch, w, h);'
if plane_call not in s:
    raise SystemExit('drawPlane call not found')
s = s.replace(plane_call,
'''            drawDistanceEnvironment(c, w, h);\n            drawExtraSkyObjects(c, w, h);\n''' + plane_call, 1)

insert_at = s.index('        void drawPlane(Canvas c, float x, float y, float size, float ang, float w, float h) {')
helpers = r'''        void drawDistanceEnvironment(Canvas c,float w,float h){
            // The world changes as distance grows: countryside -> forest/lake -> mountains -> city/airport.
            int zone=((int)(meters/450f))%4;
            float base=h*.70f;
            p.setShader(null);

            if(zone==0){
                // Countryside: distant fields, wind turbines and farm silhouettes.
                p.setColor(Color.argb(115,112,181,91));
                for(int i=0;i<7;i++){
                    float x=((i*w*.22f-world*.18f)%(w*1.25f)+w*1.25f)%(w*1.25f)-w*.12f;
                    c.drawOval(new RectF(x,base-h*.025f,x+w*.24f,base+h*.035f),p);
                }
                stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(w*.004f); stroke.setColor(Color.argb(150,238,240,236));
                for(int i=0;i<3;i++){
                    float x=((w*(.24f+i*.38f)-world*.30f)%(w*1.3f)+w*1.3f)%(w*1.3f)-w*.1f;
                    float y=base-h*.075f;
                    c.drawLine(x,y,x,base,stroke);
                    c.drawLine(x,y,x-w*.035f,y-h*.018f,stroke); c.drawLine(x,y,x+w*.036f,y-h*.018f,stroke); c.drawLine(x,y,x,y+h*.04f,stroke);
                }
                stroke.setStyle(Paint.Style.FILL);
            }else if(zone==1){
                // Forest / lake zone.
                p.setColor(Color.argb(120,46,126,72));
                for(int i=0;i<16;i++){
                    float x=((i*w*.085f-world*.28f)%(w*1.22f)+w*1.22f)%(w*1.22f)-w*.1f;
                    float s0=w*(.018f+(i%3)*.004f);
                    Path pine=new Path(); pine.moveTo(x,base); pine.lineTo(x-s0,base-s0*2.2f); pine.lineTo(x+s0,base-s0*2.2f); pine.close(); c.drawPath(pine,p);
                }
                p.setColor(Color.argb(100,68,164,218));
                c.drawOval(new RectF(-w*.1f,base+h*.018f,w*1.12f,base+h*.085f),p);
            }else if(zone==2){
                // Mountain zone, with snow caps that scroll more slowly for depth.
                p.setColor(Color.argb(145,92,132,146));
                for(int i=0;i<5;i++){
                    float x=((i*w*.34f-world*.10f)%(w*1.45f)+w*1.45f)%(w*1.45f)-w*.22f;
                    float peak=base-h*(.12f+(i%2)*.055f);
                    Path m=new Path(); m.moveTo(x-w*.18f,base); m.lineTo(x,peak); m.lineTo(x+w*.22f,base); m.close(); c.drawPath(m,p);
                    p.setColor(Color.argb(185,245,248,250)); Path snow=new Path(); snow.moveTo(x-w*.05f,peak+h*.045f); snow.lineTo(x,peak); snow.lineTo(x+w*.055f,peak+h*.047f); snow.close(); c.drawPath(snow,p);
                    p.setColor(Color.argb(145,92,132,146));
                }
            }else{
                // City / airport zone.
                for(int i=0;i<13;i++){
                    float x=((i*w*.095f-world*.24f)%(w*1.25f)+w*1.25f)%(w*1.25f)-w*.1f;
                    float bh=h*(.045f+(i%5)*.013f);
                    p.setColor(Color.argb(155,92+(i%3)*12,113+(i%2)*12,132));
                    c.drawRect(x,base-bh,x+w*.055f,base,p);
                    p.setColor(Color.argb(150,244,205,70));
                    for(int row=0;row<3;row++) for(int col=0;col<2;col++) c.drawRect(x+w*(.012f+col*.022f),base-bh+h*(.010f+row*.017f),x+w*(.019f+col*.022f),base-bh+h*(.017f+row*.017f),p);
                }
                // Airport control tower in the distance.
                float tx=((w*.82f-world*.16f)%(w*1.4f)+w*1.4f)%(w*1.4f)-w*.15f;
                p.setColor(Color.argb(170,118,130,143)); c.drawRect(tx,base-h*.10f,tx+w*.025f,base,p);
                p.setColor(Color.argb(190,53,93,124)); c.drawRoundRect(new RectF(tx-w*.018f,base-h*.115f,tx+w*.043f,base-h*.09f),w*.008f,w*.008f,p);
            }
        }

        void drawExtraSkyObjects(Canvas c,float w,float h){
            // Extra sky activity. Positions are tied to world/distance so objects genuinely pass by during flight.
            float skyTop=h*.09f, skyBottom=h*.47f;
            p.setShader(null);

            // Flocks of birds.
            stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(w*.004f); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setColor(Color.argb(175,54,72,82));
            for(int flock=0;flock<3;flock++){
                float bx=((w*(.25f+flock*.43f)-world*(.34f+flock*.05f))%(w*1.35f)+w*1.35f)%(w*1.35f)-w*.12f;
                float by=skyTop+h*(.09f+flock*.07f)+(float)Math.sin(world*.008f+flock)*h*.025f;
                for(int j=0;j<4;j++){
                    float x=bx+j*w*.025f, y=by+(j%2)*h*.012f;
                    c.drawArc(new RectF(x-w*.013f,y-h*.008f,x,y+h*.008f),205,130,false,stroke);
                    c.drawArc(new RectF(x,y-h*.008f,x+w*.013f,y+h*.008f),205,130,false,stroke);
                }
            }
            stroke.setStyle(Paint.Style.FILL);

            // Hot-air balloons appear in countryside / forest sections.
            int zone=((int)(meters/450f))%4;
            if(zone<=1){
                for(int i=0;i<2;i++){
                    float x=((w*(.74f+i*.52f)-world*.12f)%(w*1.55f)+w*1.55f)%(w*1.55f)-w*.18f;
                    float y=skyTop+h*(.11f+i*.10f)+(float)Math.sin(world*.004f+i)*h*.018f;
                    float r=w*(i==0?.028f:.022f);
                    p.setColor(i==0?Color.rgb(235,91,67):Color.rgb(244,183,54)); c.drawOval(new RectF(x-r,y-r*1.25f,x+r,y+r*1.15f),p);
                    p.setColor(Color.argb(210,255,255,255)); c.drawRect(x-r*.16f,y-r*1.1f,x+r*.16f,y+r*.9f,p);
                    p.setColor(Color.rgb(130,91,56)); c.drawRect(x-r*.22f,y+r*1.30f,x+r*.22f,y+r*1.65f,p);
                    stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(w*.002f); stroke.setColor(Color.rgb(105,83,64)); c.drawLine(x-r*.3f,y+r*.9f,x-r*.18f,y+r*1.30f,stroke); c.drawLine(x+r*.3f,y+r*.9f,x+r*.18f,y+r*1.30f,stroke); stroke.setStyle(Paint.Style.FILL);
                }
            }

            // A distant aircraft crosses the opposite side of the sky after some distance.
            if(meters>250f){
                float ax=((w*1.15f-world*.22f)%(w*1.7f)+w*1.7f)%(w*1.7f)-w*.25f;
                float ay=skyTop+h*.10f;
                p.setColor(Color.argb(165,242,245,247)); c.drawOval(new RectF(ax-w*.035f,ay-w*.007f,ax+w*.045f,ay+w*.009f),p);
                p.setColor(Color.argb(170,55,92,132)); Path aw=new Path(); aw.moveTo(ax,ay); aw.lineTo(ax-w*.025f,ay+w*.022f); aw.lineTo(ax+w*.017f,ay+w*.006f); aw.close(); c.drawPath(aw,p);
                p.setColor(Color.argb(140,255,255,255)); c.drawRoundRect(new RectF(ax-w*.13f,ay-w*.003f,ax-w*.045f,ay+w*.003f),w*.002f,w*.002f,p);
            }

            // Small moving cloudlets at different parallax speeds for a richer sky.
            for(int i=0;i<5;i++){
                float x=((i*w*.31f-world*(.06f+i*.018f))%(w*1.45f)+w*1.45f)%(w*1.45f)-w*.16f;
                float y=skyTop+h*(.04f+(i%3)*.09f);
                float rr=w*(.020f+(i%2)*.008f);
                p.setColor(Color.argb(105,255,255,255)); c.drawCircle(x,y,rr,p); c.drawCircle(x+rr*.8f,y-rr*.15f,rr*.75f,p); c.drawCircle(x+rr*1.45f,y,rr*.60f,p);
            }
        }

'''
s = s[:insert_at] + helpers + s[insert_at:]

p.write_text(s, encoding='utf-8')

g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 25', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.25"', gs)
g.write_text(gs, encoding='utf-8')
