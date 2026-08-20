from pathlib import Path
import runpy, re

# Start from the full-screen v1.21 version so settings, leaderboard and HUD are preserved.
runpy.run_path('build_v121.py', run_name='__main__')

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')

old_smoke = '''            if (engine > .15f && !showResults) {
                for (int i = 0; i < 6; i++) {
                    float ex = px - ps * (.34f + i * .055f);
                    float ey = py + ps * .012f + (float)Math.sin(world * .03f + i) * ps * .01f;
                    p.setColor(Color.argb(55 - i * 6, 255, 255, 255));
                    c.drawCircle(ex, ey, ps * (.015f + i * .006f), p);
                }
            }
'''
new_smoke = '''            // Animated rear-engine smoke / exhaust. It grows and fades with thrust and airspeed.
            if (engine > .12f && flightStarted && !showResults) {
                float exhaustX = px - ps * .385f;
                float exhaustY = py + ps * .018f;
                float smokePower = clamp(.30f + engine * .78f + airspeed / 220f, .35f, 1.18f);
                for (int i = 0; i < 11; i++) {
                    float smokeT = i / 10f;
                    float ex = exhaustX - ps * (.018f + smokeT * .36f);
                    float ey = exhaustY
                            + (float)Math.sin(world * .035f + i * .72f) * ps * (.006f + smokeT * .016f)
                            + smokeT * ps * .012f;
                    float rx = ps * (.018f + smokeT * .045f) * smokePower;
                    float ry = ps * (.013f + smokeT * .030f) * smokePower;
                    int alpha = (int)(118f * (1f - smokeT) * smokePower);
                    alpha = Math.max(0, Math.min(135, alpha));
                    int shade = 248 - Math.min(30, i * 3);
                    p.setShader(null);
                    p.setColor(Color.argb(alpha, shade, shade, shade));
                    c.drawOval(new RectF(ex-rx, ey-ry, ex+rx*1.65f, ey+ry), p);
                }
                // Dense exhaust core immediately behind the single rear jet engine.
                for (int i = 0; i < 4; i++) {
                    float ex = exhaustX - ps * (.010f + i * .026f);
                    float ey = exhaustY + (float)Math.cos(world * .055f + i) * ps * .005f;
                    int alpha = (int)(92f * smokePower);
                    p.setColor(Color.argb(Math.min(120, alpha), 245, 245, 245));
                    c.drawCircle(ex, ey, ps * (.011f + i * .0035f), p);
                }
            }
'''
if old_smoke not in s:
    raise SystemExit('v1.21 smoke block not found')
s = s.replace(old_smoke, new_smoke, 1)

signature = '        void drawPlane(Canvas c, float x, float y, float size, float ang, float w, float h) {'
start = s.index(signature)
# Find the matching closing brace of drawPlane.
brace = 0
in_string = False
escape = False
end = None
for i in range(start, len(s)):
    ch = s[i]
    if in_string:
        if escape:
            escape = False
        elif ch == '\\':
            escape = True
        elif ch == '"':
            in_string = False
        continue
    if ch == '"':
        in_string = True
    elif ch == '{':
        brace += 1
    elif ch == '}':
        brace -= 1
        if brace == 0:
            end = i + 1
            break
if end is None:
    raise SystemExit('drawPlane end not found')

new_plane = r'''        void drawPlane(Canvas c, float x, float y, float size, float ang, float w, float h) {
            c.save();
            c.translate(x, y);
            c.rotate((float)Math.toDegrees(ang));
            float z = size / 360f;
            float groundY = groundPlaneY(w, h);
            boolean gearDown = landing || !flightStarted || planeY > groundY - h * .035f;

            float altitude = Math.max(0f, groundY - planeY);
            int shadowAlpha = (int)clamp(42f - altitude / Math.max(1f, h * .30f) * 25f, 13f, 42f);
            p.setShader(null);
            p.setColor(Color.argb(shadowAlpha, 24, 39, 52));
            float shadowStretch = 1f + clamp(altitude / Math.max(1f, h * .35f), 0f, .65f);
            c.drawOval(new RectF(-98*z*shadowStretch, 42*z, 104*z*shadowStretch, 55*z), p);

            Paint.Style oldStyle = stroke.getStyle();
            Paint.Cap oldCap = stroke.getStrokeCap();
            Paint.Join oldJoin = stroke.getStrokeJoin();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setStrokeWidth(2.0f*z);
            stroke.setColor(Color.rgb(70, 88, 104));

            Path body = new Path();
            body.moveTo(-151*z, 0);
            body.quadTo(-140*z, -18*z, -107*z, -24*z);
            body.lineTo(91*z, -24*z);
            body.quadTo(132*z, -23*z, 158*z, -5*z);
            body.quadTo(153*z, 19*z, 112*z, 27*z);
            body.lineTo(-110*z, 27*z);
            body.quadTo(-144*z, 22*z, -151*z, 0);
            body.close();
            p.setShader(new LinearGradient(-135*z,-29*z,126*z,34*z,
                    Color.rgb(255,255,255), Color.rgb(216,224,232), Shader.TileMode.CLAMP));
            c.drawPath(body,p);
            p.setShader(null);
            c.drawPath(body,stroke);

            Path belly = new Path();
            belly.moveTo(-143*z, 13*z);
            belly.quadTo(-35*z, 29*z, 111*z, 21*z);
            belly.quadTo(146*z, 18*z, 155*z, 11*z);
            belly.quadTo(145*z, 34*z, 102*z, 36*z);
            belly.lineTo(-126*z, 36*z);
            belly.quadTo(-139*z, 28*z, -143*z, 13*z);
            belly.close();
            p.setColor(Color.rgb(7,83,210));
            c.drawPath(belly,p);

            Path stripe = new Path();
            stripe.moveTo(-126*z, 8*z);
            stripe.quadTo(10*z, 9*z, 142*z, 12*z);
            stripe.lineTo(143*z, 17*z);
            stripe.quadTo(10*z, 14*z, -127*z, 13*z);
            stripe.close();
            p.setColor(Color.rgb(248,190,22));
            c.drawPath(stripe,p);

            Path fin = new Path();
            fin.moveTo(-103*z, 2*z);
            fin.lineTo(-82*z, -79*z);
            fin.quadTo(-75*z, -84*z, -67*z, -81*z);
            fin.lineTo(-27*z, -1*z);
            fin.quadTo(-61*z, 1*z, -103*z, 2*z);
            fin.close();
            p.setShader(new LinearGradient(-82*z,-80*z,-34*z,1*z,
                    Color.rgb(255,67,45), Color.rgb(226,38,27), Shader.TileMode.CLAMP));
            c.drawPath(fin,p);
            p.setShader(null);
            c.drawPath(fin,stroke);
            p.setColor(Color.WHITE);
            c.drawRect(-76*z,-42*z,-42*z,-35*z,p);
            p.setColor(Color.rgb(248,190,22));
            c.drawRect(-76*z,-30*z,-36*z,-24*z,p);

            Path enginePod = new Path();
            enginePod.moveTo(-141*z, 2*z);
            enginePod.quadTo(-137*z, -11*z, -124*z, -13*z);
            enginePod.lineTo(-95*z, -13*z);
            enginePod.quadTo(-77*z, -11*z, -67*z, 1*z);
            enginePod.quadTo(-66*z, 20*z, -83*z, 30*z);
            enginePod.lineTo(-119*z, 30*z);
            enginePod.quadTo(-137*z, 25*z, -141*z, 2*z);
            enginePod.close();
            p.setShader(new LinearGradient(-141*z,-13*z,-67*z,30*z,
                    Color.rgb(250,251,252), Color.rgb(198,207,216), Shader.TileMode.CLAMP));
            c.drawPath(enginePod,p);
            p.setShader(null);
            c.drawPath(enginePod,stroke);
            p.setColor(Color.rgb(86,99,110));
            c.drawOval(new RectF(-148*z,5*z,-138*z,20*z),p);
            p.setColor(Color.rgb(35,42,49));
            c.drawCircle(-79*z, 8*z, 12*z,p);
            p.setColor(Color.rgb(10,15,20));
            c.drawCircle(-79*z, 8*z, 7*z,p);
            p.setColor(Color.rgb(146,158,169));
            c.drawCircle(-83*z, 6*z, 3.2f*z,p);

            Path tailWing = new Path();
            tailWing.moveTo(-120*z, 16*z);
            tailWing.lineTo(-151*z, 31*z);
            tailWing.lineTo(-94*z, 29*z);
            tailWing.lineTo(-62*z, 17*z);
            tailWing.close();
            p.setShader(new LinearGradient(-145*z,15*z,-70*z,31*z,
                    Color.rgb(248,250,252), Color.rgb(204,213,222), Shader.TileMode.CLAMP));
            c.drawPath(tailWing,p);
            p.setShader(null);
            c.drawPath(tailWing,stroke);
            p.setColor(Color.rgb(7,83,210));
            Path tailTip = new Path();
            tailTip.moveTo(-149*z,31*z); tailTip.lineTo(-129*z,31*z); tailTip.lineTo(-117*z,25*z); tailTip.lineTo(-139*z,25*z); tailTip.close();
            c.drawPath(tailTip,p);

            Path wing = new Path();
            wing.moveTo(-12*z, 18*z);
            wing.lineTo(-82*z, 54*z);
            wing.lineTo(20*z, 49*z);
            wing.lineTo(76*z, 20*z);
            wing.close();
            p.setShader(new LinearGradient(-20*z,14*z,70*z,53*z,
                    Color.rgb(252,253,254), Color.rgb(205,214,224), Shader.TileMode.CLAMP));
            c.drawPath(wing,p);
            p.setShader(null);
            c.drawPath(wing,stroke);
            p.setColor(Color.argb(42,35,50,70));
            Path wingShade = new Path();
            wingShade.moveTo(-8*z,20*z); wingShade.lineTo(66*z,21*z); wingShade.lineTo(18*z,44*z); wingShade.lineTo(-55*z,48*z); wingShade.close();
            c.drawPath(wingShade,p);
            p.setColor(Color.rgb(7,83,210));
            Path tip = new Path();
            tip.moveTo(-82*z,54*z); tip.lineTo(-54*z,53*z); tip.lineTo(-43*z,47*z); tip.lineTo(-69*z,48*z); tip.close();
            c.drawPath(tip,p);

            Path cockpit = new Path();
            cockpit.moveTo(95*z,-17*z);
            cockpit.lineTo(124*z,-16*z);
            cockpit.lineTo(140*z,-4*z);
            cockpit.lineTo(100*z,-4*z);
            cockpit.close();
            p.setShader(new LinearGradient(95*z,-17*z,140*z,-4*z,
                    Color.rgb(15,85,151), Color.rgb(78,172,231), Shader.TileMode.CLAMP));
            c.drawPath(cockpit,p);
            p.setShader(null);
            c.drawPath(cockpit,stroke);
            stroke.setStrokeWidth(1.6f*z);
            c.drawLine(116*z,-15*z,126*z,-4*z,stroke);

            p.setColor(Color.rgb(7,92,174));
            for(int i=0;i<8;i++){
                float wx=(-48+i*19)*z;
                c.drawRoundRect(new RectF(wx,-10*z,wx+7*z,2*z),3.5f*z,3.5f*z,p);
            }
            p.setColor(Color.argb(88,255,255,255));
            c.drawOval(new RectF(-25*z,-18*z,68*z,-6*z),p);

            p.setColor(Color.rgb(231,63,55)); c.drawCircle(-81*z,54*z,1.7f*z,p);
            p.setColor(Color.rgb(45,216,96)); c.drawCircle(75*z,20*z,1.7f*z,p);

            if (gearDown) {
                stroke.setStrokeWidth(3.0f*z);
                stroke.setColor(Color.rgb(94,108,121));
                for(float gx:new float[]{-18f,82f}){
                    c.drawLine(gx*z,25*z,gx*z,39*z,stroke);
                    p.setColor(Color.rgb(24,27,30));
                    c.drawCircle(gx*z,45*z,9*z,p);
                    p.setColor(Color.rgb(190,201,210));
                    c.drawCircle(gx*z,45*z,4.4f*z,p);
                }
            }

            stroke.setStyle(oldStyle);
            stroke.setStrokeCap(oldCap);
            stroke.setStrokeJoin(oldJoin);
            c.restore();
        }'''

s = s[:start] + new_plane + s[end:]
p.write_text(s, encoding='utf-8')

g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 22', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.22"', gs)
g.write_text(gs, encoding='utf-8')
