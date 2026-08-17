from pathlib import Path
import re

# Reconstruct the complete v1.18 source first.
exec(compile(Path('build_v118.py').read_text(encoding='utf-8'), 'build_v118.py', 'exec'))

# v1.19: the aircraft stops immediately when it touches down.
p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
text = p.read_text(encoding='utf-8')
old = 'if (planeY >= groundY - 2f && Math.abs(vy) < h * .008f) {'
new = 'if (planeY >= groundY - 2f) {'
if old not in text:
    raise RuntimeError('Landing stop condition was not found')
text = text.replace(old, new, 1)
p.write_text(text, encoding='utf-8')

# Bump Android app version.
g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 19', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.19"', gs)
g.write_text(gs, encoding='utf-8')
