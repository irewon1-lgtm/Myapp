"""Build one portable HTML. Fonts are NOT bundled; all artwork and scripts are."""
from pathlib import Path
import base64,re,json
root=Path(__file__).resolve().parents[1];p=root/'public'
html=(p/'index.html').read_text()
html=re.sub(r'<link rel="manifest"[^>]+>','',html)
icon='data:image/png;base64,'+base64.b64encode((p/'icon-192.png').read_bytes()).decode()
html=html.replace('./icon-192.png',icon)
html=re.sub(r'<link rel="stylesheet" href="\./style\.css(?:\?[^"]*)?"/>',lambda m:'<style>'+(p/'style.css').read_text()+'</style>',html)
art={f.stem:'data:image/webp;base64,'+base64.b64encode(f.read_bytes()).decode() for f in (p/'art').glob('*.webp')}
html=re.sub(r'<script src="\./library\.js(?:\?[^"]*)?"></script>',lambda m:'<script>window.CHATBOOK_PORTABLE=true;window.CHATBOOK_ART='+json.dumps(art)+';</script><script src="./library.js"></script>',html)
def inline(m):
 code=(p/m.group(1).split('?')[0]).read_text().replace('</script','<\\/script')
 return '<script>'+code+'</script>'
html=re.sub(r'<script src="\./([^"]+)"></script>',inline,html)
path=Path('/mnt/data/챗북_바로실행.html');path.write_text(html)
print(path, path.stat().st_size)
