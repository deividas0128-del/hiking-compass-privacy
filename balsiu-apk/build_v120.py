from pathlib import Path
import runpy, re

# Start from complete v1.18 source.
runpy.run_path('build_v118.py', run_name='__main__')

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')

# Use the real runway top minus the wheel-bottom offset as the plane reference touchdown Y.
contact_expr = 'h * .600625f - (getWidth() * .35f / 360f) * 54f'
s = s.replace('float groundY = h * .575f;', f'float groundY = {contact_expr};')
s = s.replace('float groundY = getHeight() * .575f;', 'float groundY = getHeight() * .600625f - (getWidth() * .35f / 360f) * 54f;')
s = s.replace('planeY = getHeight() * .575f;', 'planeY = getHeight() * .600625f - (getWidth() * .35f / 360f) * 54f;')

start = s.index('            if (landing) {')
end = s.index('            if (db < 80) {', start)
landing = '''            if (landing) {
                message = "Nusileidimas...";

                // Reduce thrust and speed during approach.
                engine += (.10f - engine) * Math.min(1, 2.2f * dt);
                float targetSpeed = 24f;
                airspeed += (targetSpeed - airspeed) * Math.min(1, 1.35f * dt);

                // Positive vy means downward. The sink rate gently decreases near the runway.
                float distance = Math.max(0f, groundY - planeY);
                float desiredSink = Math.max(h * .010f, Math.min(h * .055f, distance * .75f));
                if (distance < h * .035f) desiredSink = Math.max(h * .006f, distance * 1.25f);
                vy += (desiredSink - vy) * Math.min(1, 2.6f * dt);
                planeY += vy * dt;

                // Small flare just before wheel contact.
                float nearGround = 1f - Math.max(0f, Math.min(1f, distance / (h * .075f)));
                float desiredPitch = .012f + nearGround * .050f;
                pitch += (desiredPitch - pitch) * Math.min(1, 3.0f * dt);

                meters += airspeed * dt * .07f;
                world += airspeed * dt;

                // HARD touchdown: the instant the wheels reach the runway, freeze every motion.
                if (planeY >= groundY - 1.5f) {
                    planeY = groundY;
                    vy = 0f;
                    pitch = 0f;
                    engine = 0f;
                    airspeed = 0f;
                    shake = 0f;
                    landing = false;
                    showResults = true;
                    flightStarted = false;
                    lastResultMeters = meters;
                    lastResultTime = current;
                    message = "Skrydis baigtas";
                }
                return;
            }

'''
s = s[:start] + landing + s[end:]

# Version bump.
p.write_text(s, encoding='utf-8')
g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 20', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.20"', gs)
g.write_text(gs, encoding='utf-8')
