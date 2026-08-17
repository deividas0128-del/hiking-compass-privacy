from pathlib import Path
import zlib, base64, re

b64 = ''.join(Path(f'v118_part{i}.txt').read_text().strip() for i in range(1, 8))
data = base64.b64decode(b64)
text = zlib.decompress(data).decode('utf-8')
Path('app/src/main/java/lt/deikai/balsiulektuvas/MainActivity.java').write_text(text, encoding='utf-8')

g = Path('app/build.gradle')
gs = g.read_text(encoding='utf-8')
gs = re.sub(r'versionCode\s+\d+', 'versionCode 18', gs)
gs = re.sub(r'versionName\s+"[^"]+"', 'versionName "1.18"', gs)
g.write_text(gs, encoding='utf-8')
