from pathlib import Path
import runpy

runpy.run_path('build_v122.py', run_name='__main__')
p = Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java')
s = p.read_text(encoding='utf-8')
for i, line in enumerate(s.splitlines(), 1):
    low = line.lower()
    if any(k in low for k in ['kalib', 'settings', 'nustat', 'calbtn', 'ontouchevent', 'rectf', 'offset']):
        print(f'{i}: {line}')
raise SystemExit('INSPECT_V122')
