from pathlib import Path
import runpy

runpy.run_path('build_v114.py', run_name='__main__')

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text()

old = '''            if(db<80){
                current=0;
                message=db<45?("Tark "+vowels[vowel].repeat(5)+"…"):"Garsiau – pakilimui reikia 80 dB";
                engine+=(0.16f-engine)*Math.min(1,3.0f*dt);
                airspeed+=(0-airspeed)*Math.min(1,2.4f*dt);
                if(airborne){
                    float err=groundY-planeY;
                    float accel=err*1.45f-vy*2.25f+h*.0055f;
                    vy+=accel*dt;
                    vy=Math.max(-h*.18f,Math.min(h*.18f,vy));
                    planeY+=vy*dt;
                    float nose=0.055f+Math.min(.035f,Math.max(0,vy/h*2.0f));
                    pitch+=(nose-pitch)*Math.min(1,2.8f*dt);
                    if(planeY>=groundY-h*.002f){planeY=groundY;vy=0;pitch=0;airborne=false;}
                }else{planeY=groundY;vy=0;pitch=0;}
                return;
            }
'''
new = '''            if(target<80 || db<80){
                boolean hadFlight=airborne || meters>0 || current>0;
                current=0;
                meters=0;
                engine=0;
                airspeed=0;
                vy=0;
                pitch=0;
                shake=0;
                planeY=groundY;
                airborne=false;
                message=hadFlight?"Skrydis baigtas – pradėk iš naujo":(db<45?("Tark "+vowels[vowel].repeat(5)+"…"):"Pakilimui reikia 80 dB");
                return;
            }
'''
if old not in s:
    raise SystemExit('v1.14 below-80 block not found')
s = s.replace(old, new)

s = s.replace('float groundY=h*.49f;', 'float groundY=h*.575f;')
s = s.replace('float groundY = h * .555f;', 'float groundY = h * .575f;')
s = s.replace('planeY = getHeight() * .555f;', 'planeY = getHeight() * .575f;')
s = s.replace('drawPlane(c,l+w*.42f+planeShakeX,planeY+planeShakeY,w*.38f,pitch);', 'drawPlane(c,l+w*.42f+planeShakeX,planeY+planeShakeY,w*.35f,pitch);')

ds = s.index('        void drawPlane(Canvas c,float x,float y,float size,float ang){')
de = s.index('        @Override public boolean onTouchEvent', ds)
draw = '''        void drawPlane(Canvas c,float x,float y,float size,float ang){
            c.save();
            c.translate(x,y);
            c.rotate((float)Math.toDegrees(ang));
            float z=size/340f;

            p.setShader(null);
            p.setColor(Color.argb(38,35,50,65));
            c.drawOval(new RectF(-96*z,38*z,102*z,56*z),p);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.0f*z);
            stroke.setColor(Color.rgb(78,98,116));

            Path body=new Path();
            body.moveTo(-142*z,0*z);
            body.quadTo(-130*z,-18*z,-98*z,-21*z);
            body.lineTo(86*z,-21*z);
            body.quadTo(130*z,-20*z,151*z,-4*z);
            body.quadTo(145*z,17*z,107*z,22*z);
            body.lineTo(-108*z,22*z);
            body.quadTo(-140*z,18*z,-142*z,0*z);
            body.close();
            p.setShader(new LinearGradient(-120*z,-24*z,120*z,28*z,Color.rgb(255,255,255),Color.rgb(226,232,238),Shader.TileMode.CLAMP));
            c.drawPath(body,p);
            p.setShader(null);
            c.drawPath(body,stroke);

            Path belly=new Path();
            belly.moveTo(-136*z,12*z);
            belly.quadTo(-40*z,25*z,105*z,18*z);
            belly.quadTo(140*z,16*z,148*z,8*z);
            belly.quadTo(139*z,28*z,103*z,30*z);
            belly.lineTo(-122*z,30*z);
            belly.close();
            p.setColor(Color.rgb(22,109,205));
            c.drawPath(belly,p);

            Path stripe=new Path();
            stripe.moveTo(-128*z,8*z);
            stripe.quadTo(-5*z,12*z,138*z,10*z);
            stripe.lineTo(136*z,14*z);
            stripe.quadTo(-6*z,15*z,-128*z,11*z);
            stripe.close();
            p.setColor(Color.rgb(243,193,50));
            c.drawPath(stripe,p);

            Path fin=new Path();
            fin.moveTo(-108*z,1*z);
            fin.lineTo(-90*z,-66*z);
            fin.lineTo(-44*z,1*z);
            fin.close();
            p.setColor(Color.rgb(234,70,56));
            c.drawPath(fin,p);
            p.setColor(Color.WHITE);
            c.drawRect(-87*z,-29*z,-52*z,-23*z,p);
            p.setColor(Color.rgb(243,193,50));
            c.drawRect(-89*z,-19*z,-48*z,-14*z,p);

            rf.set(-136*z,-5*z,-84*z,19*z);
            p.setShader(new LinearGradient(-136*z,-5*z,-84*z,19*z,Color.rgb(243,246,249),Color.rgb(196,205,214),Shader.TileMode.CLAMP));
            c.drawRoundRect(rf,12*z,12*z,p);
            p.setShader(null);
            c.drawRoundRect(rf,12*z,12*z,stroke);
            p.setColor(Color.rgb(28,36,44));
            c.drawCircle(-85*z,7*z,8.3f*z,p);
            p.setColor(Color.rgb(122,136,147));
            c.drawCircle(-87*z,7*z,3.8f*z,p);

            Path wing=new Path();
            wing.moveTo(-8*z,14*z);
            wing.lineTo(-72*z,52*z);
            wing.lineTo(18*z,46*z);
            wing.lineTo(63*z,17*z);
            wing.close();
            p.setShader(new LinearGradient(-10*z,10*z,48*z,48*z,Color.rgb(247,249,252),Color.rgb(210,218,225),Shader.TileMode.CLAMP));
            c.drawPath(wing,p);
            p.setShader(null);
            c.drawPath(wing,stroke);
            Path wingAccent=new Path();
            wingAccent.moveTo(-70*z,52*z);
            wingAccent.lineTo(-40*z,51*z);
            wingAccent.lineTo(-28*z,44*z);
            wingAccent.lineTo(-55*z,45*z);
            wingAccent.close();
            p.setColor(Color.rgb(22,109,205));
            c.drawPath(wingAccent,p);

            Path tailWing=new Path();
            tailWing.moveTo(-114*z,12*z);
            tailWing.lineTo(-144*z,26*z);
            tailWing.lineTo(-90*z,24*z);
            tailWing.lineTo(-66*z,14*z);
            tailWing.close();
            p.setColor(Color.rgb(236,240,244));
            c.drawPath(tailWing,p);
            c.drawPath(tailWing,stroke);

            Path cockpit=new Path();
            cockpit.moveTo(92*z,-16*z);
            cockpit.lineTo(121*z,-15*z);
            cockpit.lineTo(135*z,-4*z);
            cockpit.lineTo(98*z,-4*z);
            cockpit.close();
            p.setColor(Color.rgb(48,94,128));
            c.drawPath(cockpit,p);
            c.drawPath(cockpit,stroke);

            p.setColor(Color.rgb(23,104,166));
            for(int i=0;i<7;i++){
                float wx=(-43+i*20)*z;
                c.drawOval(new RectF(wx,-10*z,wx+7*z,0*z),p);
            }
            p.setColor(Color.argb(75,255,255,255));
            c.drawOval(new RectF(-28*z,-16*z,54*z,-4*z),p);

            stroke.setStrokeWidth(3.0f*z);
            stroke.setColor(Color.rgb(96,111,123));
            for(float wx:new float[]{-16f,80f}){
                c.drawLine(wx*z,22*z,wx*z,38*z,stroke);
                p.setColor(Color.rgb(27,29,31));
                c.drawCircle(wx*z,43*z,10*z,p);
                p.setColor(Color.rgb(191,201,208));
                c.drawCircle(wx*z,43*z,4.7f*z,p);
            }
            stroke.setStyle(Paint.Style.FILL);
            c.restore();
        }
'''
s = s[:ds] + draw + s[de:]
s = s.replace('meters=current=best=0;airspeed=engine=vy=pitch=shake=0;airborne=false;', 'meters=current=best=0;airspeed=engine=vy=pitch=shake=0;airborne=false;planeY=getHeight()*.575f;')
p.write_text(s)

g = Path('app/build.gradle')
gs = g.read_text().replace('versionCode 14','versionCode 16').replace('versionName "1.14"','versionName "1.16"')
g.write_text(gs)
