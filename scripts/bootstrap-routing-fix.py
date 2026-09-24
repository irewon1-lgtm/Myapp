"""Scoped bootstrap routing correction. No network or deployment here.
Only two gateway files may change in the frozen contract; existing sync stays intact.
"""
from pathlib import Path
import json,hashlib
root=Path('.')
contract_path=root/'live/shell-contract.json';contract=json.loads(contract_path.read_text())
for p,d in contract['files'].items():
    assert hashlib.sha256((root/p).read_bytes()).hexdigest()==d,'Frozen baseline changed: '+p
p=root/'netlify/lib/live-gateway.mjs';s=p.read_text()
marker="   try{\n     if(p==='/'||p==='/index.html'){"
assert s.count(marker)==1
s=s.replace(marker,"""   try{
     // Platform SPA rewrites may forward an original static URL here. Handle
     // the immutable bootstrap explicitly, even on a completely cold CDN.
     if(['/live-guard.js','/live-sw.js','/manifest.webmanifest','/icon-192.png','/icon-512.png'].includes(p)){
       const file=p.slice(1),v=await manifest('bundled'),b=await asset('bundled',v.manifest,file);
       return response(request.method==='HEAD'?null:b,200,file,{'Cache-Control':'no-cache, must-revalidate','Service-Worker-Allowed':'/','X-Chatbook-Bootstrap':'1'});
     }
     if(p==='/'||p==='/index.html'){""",1);p.write_text(s)
p=root/'netlify/functions/live.mts';s=p.read_text();old="path: ['/', '/index.html', '/__cb/*',";assert old in s
s=s.replace(old,"path: ['/', '/index.html', '/live-guard.js', '/live-sw.js', '/manifest.webmanifest', '/icon-192.png', '/icon-512.png', '/__cb/*',",1);p.write_text(s)
p=root/'tests/live/server.mjs';s=p.read_text();start=s.index("   if(['/live-guard.js','/live-sw.js','/manifest.webmanifest','/icon-192.png','/icon-512.png'].includes(u.pathname)){");end=s.index("   if(u.pathname.startsWith('/api/'))",start);s=s[:start]+s[end:];p.write_text(s)
p=root/'tests/live/gateway.test.mjs';s=p.read_text();s+='''
for(const file of ['live-guard.js','live-sw.js','manifest.webmanifest','icon-192.png','icon-512.png'])test('cold gateway serves frozen bootstrap '+file+' without static shortcut',async()=>{
 const v=fixture(),dir=fs.mkdtempSync(path.join(os.tmpdir(),'cb-boot-'));
 try{v.f[file]=Buffer.from(file.endsWith('.js')?'/* bootstrap */':'{}');v.m.assets[file]={sha256:sha(v.f[file]),bytes:v.f[file].length};
  for(const[p,b]of Object.entries(v.f)){fs.mkdirSync(path.dirname(path.join(dir,p)),{recursive:true});fs.writeFileSync(path.join(dir,p),b);}
  const mp=path.join(dir,'manifest.json');fs.writeFileSync(mp,JSON.stringify(v.m));let requests=0;
  const g=createGateway({bundleRoot:dir,manifestFile:mp,fetcher:async()=>{requests++;throw Error('offline');}});
  const r=await g(req('/'+file));assert.equal(r.status,200);assert.equal(Buffer.from(await r.arrayBuffer()).toString(),v.f[file].toString());assert.equal(requests,0);assert.equal(r.headers.get('Service-Worker-Allowed'),'/');
  if(file.endsWith('.js'))assert.match(r.headers.get('Content-Type'),/javascript/);
 }finally{fs.rmSync(dir,{recursive:true,force:true});}
});
''';p.write_text(s)
p=root/'tests/live/browser.py';s=p.read_text();mark="    record('same origin remains unchanged',page.evaluate('location.origin')==base)";assert mark in s
s=s.replace(mark,mark+'''
    for static_path in ['/live-guard.js','/live-sw.js','/manifest.webmanifest']:
        status,headers,body=get(static_path)
        record('cold gateway bootstrap route '+static_path,status==200 and headers.get('x-chatbook-bootstrap')=='1' and len(body)>0)
''',1);p.write_text(s)
for name in ['netlify/lib/live-gateway.mjs','netlify/functions/live.mts']:
    contract['files'][name]=hashlib.sha256((root/name).read_bytes()).hexdigest()
contract_path.write_text(json.dumps(contract,ensure_ascii=False,indent=2)+'\n')
for p,d in contract['files'].items():assert hashlib.sha256((root/p).read_bytes()).hexdigest()==d,p
# A separate receipt records the correction; the original one-call receipt is never erased.
s=(root/'scripts/bootstrap-once.mjs').read_text()
s=s.replace("process.env.CHATBOOK_INITIAL_MIGRATION!=='1'","process.env.CHATBOOK_ROUTING_CORRECTION!=='1'")
s=s.replace("const receiptPath='.deployment/live-bootstrap-receipt.json';", "const prior=await read('.deployment/live-bootstrap-receipt.json');if(prior?.value?.productionCalls!==1)throw Error('Expected original one-call bootstrap receipt');\nconst receiptPath='.deployment/live-routing-correction-receipt.json';")
s=s.replace("kind:'chatbook-live-one-time-bootstrap'","kind:'chatbook-live-routing-correction'")
s=s.replace("kind:'chatbook-live-bootstrap'","kind:'chatbook-live-routing-correction'")
s=s.replace('productionCalls:1,','productionCalls:1,totalTransitionCalls:2,')
(root/'scripts/bootstrap-routing-once.mjs').write_text(s)
print('Scoped cold-route correction prepared; existing sync/storage/guard/worker unchanged; no deployment.')
