"""Reproducible additive APK patch; leaves old reader code and all user data untouched."""
import struct,zipfile,pathlib,sys,hashlib
base,dex,html,out=map(pathlib.Path,sys.argv[1:])
assert hashlib.sha256(base.read_bytes()).hexdigest()=='92a673a4c99d752a32f200d6774251d865fe4ee7239496eb04f316efd22f6f24', 'Wrong baseline APK'
u16=lambda b,p:struct.unpack_from('<H',b,p)[0]
u32=lambda b,p:struct.unpack_from('<I',b,p)[0]
z=zipfile.ZipFile(base);xml=z.read('AndroidManifest.xml');chunks=[];o=8
while o<len(xml):
 t,h,s=struct.unpack_from('<HHI',xml,o);chunks.append(bytearray(xml[o:o+s]));o+=s
pool=next(c for c in chunks if u16(c,0)==1);n=u32(pool,8);assert u32(pool,12)==0,'Styled XML unsupported';flags=u32(pool,16);start=u32(pool,20);h=u16(pool,2);strings=[]
def length(b,p):
 v=b[p];return (((v&127)<<8|b[p+1]),p+2) if v&128 else (v,p+1)
for i in range(n):
 pos=start+u32(pool,h+4*i)
 if flags&256:
  _,pos=length(pool,pos);l,pos=length(pool,pos);strings.append(pool[pos:pos+l].decode('utf8'))
 else:
  l=u16(pool,pos);assert l<32768;strings.append(pool[pos+2:pos+2+l*2].decode('utf-16-le'))
def idx(s):
 if s not in strings:strings.append(s)
 return strings.index(s)
new_name=idx('com.codingroadmap.live.LiveActivity');internet=idx('android.permission.INTERNET');ver=idx('0.9.0')
def tag(c):return strings[u32(c,20)] if u16(c,0) in (0x102,0x103) else ''
def attr(c,name,value,typ=None):
 st,sz,cnt=struct.unpack_from('<HHH',c,24)
 for j in range(cnt):
  p=16+st+j*sz
  if strings[u32(c,p+4)]==name:
   existing_type=c[p+15];typ=existing_type if typ is None else typ
   struct.pack_into('<I',c,p+8,value if typ==3 else 0xffffffff);c[p+15]=typ;struct.pack_into('<I',c,p+16,value);return
 raise ValueError(name)
manifest=next(c for c in chunks if tag(c)=='manifest' and u16(c,0)==0x102);attr(manifest,'versionCode',100,16);attr(manifest,'versionName',ver,3)
old_i=next(i for i,c in enumerate(chunks) if tag(c)=='activity' and u16(c,0)==0x102)
old_end=next(i for i in range(old_i+1,len(chunks)) if tag(chunks[i])=='activity' and u16(chunks[i],0)==0x103)
new_start=bytearray(chunks[old_i]);attr(new_start,'name',new_name,3)
launcher=[bytearray(c) for c in chunks[old_i+1:old_end]]
new_end=bytearray(chunks[old_end]);attr(chunks[old_i],'exported',0,18)
chunks[old_i+1:old_end]=[]
app_end=next(i for i,c in enumerate(chunks) if tag(c)=='application' and u16(c,0)==0x103)
chunks[app_end:app_end]=[new_start,*launcher,new_end]
perm_i=next(i for i,c in enumerate(chunks) if tag(c)=='uses-permission' and u16(c,0)==0x102)
new_perm=bytearray(chunks[perm_i]);attr(new_perm,'name',internet,3)
chunks[perm_i:perm_i]=[new_perm,bytearray(chunks[perm_i+1])]
# Rebuild pool; append-only indices preserve every existing resource reference.
data=bytearray();offsets=[]
def enc_len(l):return bytes([0x80|(l>>8),l&255]) if l>127 else bytes([l])
for s in strings:
 offsets.append(len(data))
 if flags&256:
  b=s.encode('utf8');data+=enc_len(len(s.encode('utf-16-le'))//2)+enc_len(len(b))+b+b'\0'
 else:
  b=s.encode('utf-16-le');data+=struct.pack('<H',len(b)//2)+b+b'\0\0'
while len(data)%4:data+=b'\0'
header=bytearray(pool[:h]);new_start=h+4*len(strings);struct.pack_into('<I',header,4,new_start+len(data));struct.pack_into('<I',header,8,len(strings));struct.pack_into('<I',header,20,new_start)
new_pool=header+b''.join(struct.pack('<I',x) for x in offsets)+data
chunks[next(i for i,c in enumerate(chunks) if u16(c,0)==1)]=new_pool
body=b''.join(chunks);new_xml=struct.pack('<HHI',3,8,len(body)+8)+body
with zipfile.ZipFile(out,'w') as w:
 for item in z.infolist():
  if item.filename.startswith('META-INF/') and item.filename.rsplit('.',1)[-1] in ('RSA','DSA','EC','SF','MF'):continue
  w.writestr(item,new_xml if item.filename=='AndroidManifest.xml' else z.read(item.filename))
 w.writestr('classes3.dex',dex.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
 w.writestr('assets/live/reader.html',html.read_bytes(),compress_type=zipfile.ZIP_DEFLATED)
print('Added live reader; original dex and content entries preserved. APK unsigned:',out)
