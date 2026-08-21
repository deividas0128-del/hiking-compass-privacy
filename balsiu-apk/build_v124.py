from pathlib import Path
import runpy, re

# Start from v1.22 so full-screen HUD, leaderboard and improved aircraft are preserved.
runpy.run_path('build_v122.py', run_name='__main__')

p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')

# 1) Sensitivity state + slider hit area.
s = s.replace(
    'float calibrationOffset;\n        volatile boolean calibrating = false;',
    'float calibrationOffset;\n        float micSensitivity = .50f;\n        volatile boolean calibrating = false;',
    1
)
s = s.replace(
    'RectF closeBtn = new RectF(), calBtn = new RectF(), micBtn = new RectF();',
    'RectF closeBtn = new RectF(), sensitivityTrack = new RectF(), micBtn = new RectF();',
    1
)

# 2) Stop using old calibration value as the control. Default center equals previous 105 dB offset.
s = s.replace(
    'calibrationOffset = prefs.getFloat("calibration_offset", 105f);\n            vowel = prefs.getInt("vowel", 0);',
    'calibrationOffset = 105f;\n            micSensitivity = prefs.getFloat("mic_sensitivity", .50f);\n            vowel = prefs.getInt("vowel", 0);',
    1
)

# 3) Live microphone mapping. Slider gives +/-12 dB around the old default.
s = s.replace(
    'float game = Math.max(0f, Math.min(110f, rdb + calibrationOffset));',
    'float sensitivityBoost = (micSensitivity - .50f) * 24f;\n                    float game = Math.max(0f, Math.min(110f, rdb + calibrationOffset + sensitivityBoost));',
    1
)

# 4) Replace calibration block in settings with a draggable sensitivity slider.
old_settings = '''            float y2=micBtn.bottom+h*.045f;
            text(c,"Kalibravimas",l+w*.07f,y2,w*.034f,Color.rgb(67,83,99),Paint.Align.LEFT,true);
            calBtn.set(l+w*.07f,y2+h*.025f,l+pw-w*.07f,y2+h*.10f);
            p.setColor(Color.rgb(244,183,54)); c.drawRoundRect(calBtn,h*.018f,h*.018f,p);
            text(c,calibrating?"Kalibruojama...":"Kalibruoti mikrofoną iki 85 dB",calBtn.centerX(),calBtn.top+calBtn.height()*.66f,w*.033f,Color.rgb(77,55,12),Paint.Align.CENTER,true);
            text(c,"Kalbėk įprastu balsu apie 2 sek.",l+w*.07f,calBtn.bottom+h*.035f,w*.027f,Color.rgb(103,117,129),Paint.Align.LEFT,false);
'''
new_settings = '''            float y2=micBtn.bottom+h*.045f;
            text(c,"Mikrofono jautrumas",l+w*.07f,y2,w*.034f,Color.rgb(67,83,99),Paint.Align.LEFT,true);
            text(c,Math.round(micSensitivity*100f)+" %",l+pw-w*.07f,y2,w*.033f,Color.rgb(18,73,129),Paint.Align.RIGHT,true);

            float trackL=l+w*.10f, trackR=l+pw-w*.10f, trackY=y2+h*.075f;
            sensitivityTrack.set(trackL,trackY-h*.035f,trackR,trackY+h*.035f);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeWidth(h*.012f);
            stroke.setColor(Color.rgb(208,216,224));
            c.drawLine(trackL,trackY,trackR,trackY,stroke);
            float knobX=trackL+(trackR-trackL)*micSensitivity;
            stroke.setColor(Color.rgb(52,194,82));
            c.drawLine(trackL,trackY,knobX,trackY,stroke);
            stroke.setStyle(Paint.Style.FILL);
            p.setShadowLayer(h*.010f,0,h*.004f,Color.argb(60,0,0,0));
            p.setColor(Color.WHITE); c.drawCircle(knobX,trackY,h*.022f,p); p.clearShadowLayer();
            p.setColor(Color.rgb(52,194,82)); c.drawCircle(knobX,trackY,h*.014f,p);
            text(c,"Mažiau",trackL,trackY+h*.055f,w*.027f,Color.rgb(103,117,129),Paint.Align.LEFT,false);
            text(c,"Jautriau",trackR,trackY+h*.055f,w*.027f,Color.rgb(103,117,129),Paint.Align.RIGHT,false);
            text(c,"Keičia, kaip stipriai programėlė reaguoja į balsą.",l+w*.07f,trackY+h*.100f,w*.026f,Color.rgb(103,117,129),Paint.Align.LEFT,false);
'''
if old_settings not in s:
    raise SystemExit('settings calibration block not found')
s = s.replace(old_settings, new_settings, 1)

# 5) Helper: update and persist slider value.
insert_before = '        @Override public boolean onTouchEvent(MotionEvent e){'
helper = '''        void setMicSensitivity(float x){
            float width=sensitivityTrack.width();
            if(width<=1f) return;
            micSensitivity=clamp((x-sensitivityTrack.left)/width,0f,1f);
            prefs.edit().putFloat("mic_sensitivity",micSensitivity).apply();
            postInvalidateOnAnimation();
        }

'''
if insert_before not in s:
    raise SystemExit('onTouchEvent signature not found')
s = s.replace(insert_before, helper + insert_before, 1)

# 6) Allow real dragging (DOWN + MOVE + UP), then preserve existing tap handling.
old_touch_head = '''        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX(), y=e.getY();
'''
new_touch_head = '''        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(), y=e.getY();
            int action=e.getAction();
            if(panel==1 && sensitivityTrack.width()>0f &&
                    y>=sensitivityTrack.top && y<=sensitivityTrack.bottom &&
                    (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP)){
                setMicSensitivity(x);
                return true;
            }
            if(action!=MotionEvent.ACTION_UP) return true;
'''
if old_touch_head not in s:
    raise SystemExit('touch method head not found')
s = s.replace(old_touch_head, new_touch_head, 1)

# Remove the obsolete calibration-button touch action.
s = s.replace('                if(calBtn.contains(x,y)){calibrate();return true;}\n', '', 1)

p.write_text(s, encoding='utf-8')

g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 24', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.24"', gs)
g.write_text(gs, encoding='utf-8')
