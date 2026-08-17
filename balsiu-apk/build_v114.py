from pathlib import Path

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text()

s = s.replace('offset=(float)(75-(sum/count))', 'offset=(float)(85-(sum/count))')
s = s.replace('offset=(float)(85-(sum/count))', 'offset=(float)(85-(sum/count))')
s = s.replace('Kalibruoti 75 dB', 'Kalibruoti 85 dB')
s = s.replace('Tikslas 70–80 dB', 'Tikslas 80–90 dB')
s = s.replace('float level=45+i*2.1f;', 'float level=45+i*2.8f;')
s = s.replace('level<70?Color.rgb(255,171,49):(level<=80?Color.rgb(47,197,70):Color.rgb(241,75,67))', 'level<80?Color.rgb(255,171,49):(level<=90?Color.rgb(47,197,70):(level<=100?Color.rgb(255,151,50):Color.rgb(241,75,67)))')
s = s.replace('float meters=0,current=0,best=0; float planeY=0,vy=0,pitch=0,airspeed=0,engine=0,world=0;', 'float meters=0,current=0,best=0; float planeY=0,vy=0,pitch=0,airspeed=0,engine=0,world=0,shake=0; boolean airborne=false;')
s = s.replace('float meters=0,current=0,best=0; float planeY=0,vy=0,pitch=0,airspeed=0,engine=0,world=0; boolean airborne=false;', 'float meters=0,current=0,best=0; float planeY=0,vy=0,pitch=0,airspeed=0,engine=0,world=0,shake=0; boolean airborne=false;')

start = s.index('        void update(float dt,float h){')
end = s.index('        void drawScene(Canvas c,float l,float t,float r,float b){', start)
update = '''        void update(float dt,float h){
            float groundY=h*.49f;
            float target=micOn?micDb:0;
            float k=target>filtered?8.5f:4.2f;
            filtered+=(target-filtered)*Math.min(1,k*dt);
            db=filtered;
            shake+=(0-shake)*Math.min(1,5.5f*dt);

            if(db<80){
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

            airborne=true;
            boolean perfect=db<=90;
            boolean overload=db>100;
            float desired;
            float targetY;
            if(db<=90){desired=.78f+(db-80f)/10f*.09f;targetY=h*.425f;message="Puiku! Tobulas skrydis!";}
            else if(db<=100){desired=.88f+(db-90f)/10f*.07f;targetY=Math.max(h*.335f,h*.425f-(db-90f)*h*.0048f);message="Šiek tiek tyliau";}
            else{float over=Math.min(1f,(db-100f)/10f);desired=.72f-over*.18f;targetY=groundY;shake=Math.min(1f,shake+dt*(4.2f+over*3.5f));message="Per garsiai! Lėktuvas dreba ir leidžiasi";}

            engine+=(desired-engine)*Math.min(1,2.45f*dt);
            float targetSpeed=(overload?48f:58f)+engine*(overload?82f:102f);
            airspeed+=(targetSpeed-airspeed)*Math.min(1,1.55f*dt);
            float err=targetY-planeY;
            float accel=err*(overload?1.75f:1.32f)-vy*(overload?2.05f:2.35f);
            if(overload){float over=Math.min(1f,(db-100f)/10f);accel+=h*(.008f+.012f*over);accel+=(float)Math.sin(world*.19f)*h*.006f*shake;}
            vy+=accel*dt;
            vy=Math.max(-h*.22f,Math.min(h*.24f,vy));
            planeY+=vy*dt;
            if(planeY>groundY){planeY=groundY;if(vy>0)vy=0;}
            if(planeY<h*.27f){planeY=h*.27f;if(vy<0)vy*=.35f;}
            float desiredPitch=Math.max(-.14f,Math.min(.10f,-vy/h*2.9f));
            if(overload)desiredPitch+=(float)Math.sin(world*.23f)*.055f*shake;
            pitch+=(desiredPitch-pitch)*Math.min(1,3.0f*dt);
            meters+=airspeed*dt*.11f;
            world+=airspeed*dt;
            if(perfect){current+=dt;best=Math.max(best,current);}else current=0;
        }
'''
s = s[:start] + update + s[end:]

s = s.replace('drawPlane(c,l+w*.42f,planeY,w*.32f,pitch);', 'float sx=(float)Math.sin(world*.21f+1.1f)*w*.009f*shake; float sy=(float)Math.cos(world*.27f)*h*.012f*shake; drawPlane(c,l+w*.42f+sx,planeY+sy,w*.38f,pitch);')

ds = s.index('        void drawPlane(Canvas c,float x,float y,float size,float ang){')
de = s.index('        @Override public boolean onTouchEvent', ds)
draw = '''        void drawPlane(Canvas c,float x,float y,float size,float ang){
            c.save();c.translate(x,y);c.rotate((float)Math.toDegrees(ang));float z=size/340f;
            p.setColor(Color.argb(42,30,50,70));c.drawOval(new RectF(-105*z,43*z,112*z,61*z),p);
            stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(2.0f*z);stroke.setColor(Color.rgb(74,96,115));
            Path body=new Path();body.moveTo(-150*z,2*z);body.quadTo(-136*z,-18*z,-104*z,-22*z);body.lineTo(86*z,-22*z);body.quadTo(137*z,-20*z,157*z,-2*z);body.quadTo(146*z,23*z,104*z,28*z);body.lineTo(-111*z,28*z);body.quadTo(-146*z,23*z,-150*z,2*z);body.close();
            p.setColor(Color.rgb(248,250,252));c.drawPath(body,p);c.drawPath(body,stroke);
            Path belly=new Path();belly.moveTo(-140*z,14*z);belly.quadTo(-45*z,31*z,104*z,22*z);belly.quadTo(143*z,19*z,154*z,9*z);belly.quadTo(145*z,31*z,102*z,35*z);belly.lineTo(-121*z,35*z);belly.close();p.setColor(Color.rgb(18,108,202));c.drawPath(belly,p);
            p.setColor(Color.rgb(244,194,43));c.drawRect(-132*z,9*z,138*z,15*z,p);
            Path fin=new Path();fin.moveTo(-112*z,2*z);fin.lineTo(-94*z,-69*z);fin.lineTo(-43*z,1*z);fin.close();p.setColor(Color.rgb(232,63,52));c.drawPath(fin,p);p.setColor(Color.WHITE);c.drawRect(-91*z,-31*z,-52*z,-24*z,p);p.setColor(Color.rgb(244,194,43));c.drawRect(-93*z,-20*z,-47*z,-14*z,p);
            rf.set(-140*z,-6*z,-80*z,22*z);p.setColor(Color.rgb(225,231,236));c.drawRoundRect(rf,14*z,14*z,p);c.drawRoundRect(rf,14*z,14*z,stroke);p.setColor(Color.rgb(26,34,42));c.drawCircle(-82*z,8*z,9.5f*z,p);p.setColor(Color.rgb(112,128,140));c.drawCircle(-84*z,8*z,4.5f*z,p);
            Path wing=new Path();wing.moveTo(-18*z,15*z);wing.lineTo(-84*z,59*z);wing.lineTo(25*z,51*z);wing.lineTo(72*z,18*z);wing.close();p.setColor(Color.rgb(235,240,244));c.drawPath(wing,p);c.drawPath(wing,stroke);
            Path tailWing=new Path();tailWing.moveTo(-115*z,12*z);tailWing.lineTo(-149*z,28*z);tailWing.lineTo(-91*z,26*z);tailWing.lineTo(-65*z,14*z);tailWing.close();p.setColor(Color.rgb(235,239,243));c.drawPath(tailWing,p);c.drawPath(tailWing,stroke);
            Path cockpit=new Path();cockpit.moveTo(93*z,-17*z);cockpit.lineTo(123*z,-15*z);cockpit.lineTo(139*z,-3*z);cockpit.lineTo(101*z,-4*z);cockpit.close();p.setColor(Color.rgb(44,91,126));c.drawPath(cockpit,p);
            p.setColor(Color.rgb(22,104,164));for(int i=0;i<8;i++){float wx=(-48+i*20)*z;c.drawOval(new RectF(wx,-11*z,wx+8*z,1*z),p);}
            stroke.setStrokeWidth(3*z);stroke.setColor(Color.rgb(93,110,122));for(float wx:new float[]{-20f,92f}){c.drawLine(wx*z,28*z,wx*z,44*z,stroke);p.setColor(Color.rgb(27,29,31));c.drawCircle(wx*z,50*z,11*z,p);p.setColor(Color.rgb(189,199,207));c.drawCircle(wx*z,50*z,5*z,p);}stroke.setStyle(Paint.Style.FILL);c.restore();
        }
'''
s = s[:ds] + draw + s[de:]
s = s.replace('meters=current=best=0;airspeed=engine=vy=0;airborne=false;', 'meters=current=best=0;airspeed=engine=vy=pitch=shake=0;airborne=false;')
p.write_text(s)

g = Path('app/build.gradle')
gs = g.read_text().replace('versionCode 13','versionCode 14').replace('versionName "1.13"','versionName "1.14"')
g.write_text(gs)
