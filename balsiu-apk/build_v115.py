from pathlib import Path
import runpy

# First apply the complete v1.14 graphics, microphone and flight-model changes.
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

new = '''            // Immediate fail/restart rule: a single microphone reading below 80 dB
            // ends the current flight at once. Best time is preserved.
            if(target<80 || db<80){
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
p.write_text(s)

g = Path('app/build.gradle')
gs = g.read_text().replace('versionCode 14', 'versionCode 15').replace('versionName "1.14"', 'versionName "1.15"')
g.write_text(gs)
