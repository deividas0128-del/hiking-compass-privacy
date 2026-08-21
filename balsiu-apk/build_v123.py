from pathlib import Path
import runpy, re

runpy.run_path('build_v122.py', run_name='__main__')
p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')
lines = s.splitlines()
keep = []
for i, line in enumerate(lines, 1):
    low = line.lower()
    if any(k in low for k in ['kalib', 'settings', 'nustat', 'calbtn', 'ontouchevent', 'rectf', 'offset']):
        a = max(1, i-5); b = min(len(lines), i+12)
        keep.append(f'--- around line {i} ---\n' + '\n'.join(f'{j}: {lines[j-1]}' for j in range(a,b+1)))
Path('inspect_v122.txt').write_text('\n\n'.join(keep), encoding='utf-8')

g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 23', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.23"', gs)
g.write_text(gs, encoding='utf-8')
