"""Extract genuine local-video frames and an inspection sheet. No URL downloader,
no thumbnail substitution, no OCR, no automatic claim that a frame is educational.
Usage: python engine/frames.py VIDEO.mp4 PLAN.json OUTPUT_DIR VIDEO_ID
Requires ffmpeg, ffprobe and Pillow. These are NOT required by the text engine.
"""
from __future__ import annotations
import hashlib, json, pathlib, subprocess, sys, math, re
from PIL import Image, ImageDraw, ImageStat, ImageFilter

def run(argv: list[str], timeout: int = 45) -> bytes:
    p = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    if p.returncode:
        raise RuntimeError(p.stderr.decode('utf8', 'replace')[-1200:])
    return p.stdout

def sha(path: pathlib.Path) -> str:
    h=hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()

def dhash(image: Image.Image) -> str:
    px=list(image.convert('L').resize((9,8)).getdata())
    bits=''.join('1' if px[y*9+x]>px[y*9+x+1] else '0' for y in range(8) for x in range(8))
    return f'{int(bits,2):016x}'

def extract(video: pathlib.Path, plan: dict, out: pathlib.Path, video_id: str) -> dict:
    if not video.is_file() or not re.fullmatch(r'[\w-]{11}',video_id):raise ValueError('A local video and verified video ID are required')
    out.mkdir(parents=True,exist_ok=True)
    info=json.loads(run(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_type,width,height','-of','json',str(video)]))
    duration=float(info['format']['duration']);v=next(s for s in info['streams'] if s['codec_type']=='video')
    video_hash=sha(video); frames=[]; last_exact={}
    candidates=plan.get('candidates',[])
    if len(candidates)>240:raise ValueError('At most 240 candidates per extraction batch; schedule another batch explicitly')
    # One bounded FFmpeg seek per candidate; ffmpeg preserves the actual screen.
    for i, candidate in enumerate(candidates):
        t=float(candidate['seconds'])
        if not math.isfinite(t) or t<0 or t>=duration:raise ValueError('Timestamp outside the local video')
        filename=out/f'frame-{i:04d}-{t:010.3f}.jpg'
        run(['ffmpeg','-hide_banner','-loglevel','error','-ss',str(t),'-i',str(video),'-frames:v','1','-vf',"scale='min(1600,iw)':-2",'-q:v','2','-y',str(filename)])
        im=Image.open(filename).convert('RGB');h=sha(filename)
        item={'id':f'f{i:04d}','file':filename.name,'seconds':t,'width':im.width,'height':im.height,'sha256':h,'dHash':dhash(im),'kind':'screenshot-candidate','capture':{'videoId':video_id,'videoSha256':video_hash,'method':'ffmpeg-local-frame','seconds':t},'review':None,'eligibleResolution':im.width>=640,'reasons':candidate.get('reasons',[])}
        # Exact duplicate only. A near-duplicate code screen can contain a critical change.
        if h in last_exact:item['exactDuplicateOf']=last_exact[h]
        else:last_exact[h]=item['id']
        frames.append(item)
    for k in range(0,len(frames),20):
        group=frames[k:k+20];sheet=Image.new('RGB',(4*320,math.ceil(len(group)/4)*210),'#f0f0ed');draw=ImageDraw.Draw(sheet)
        for j,f in enumerate(group):
            im=Image.open(out/f['file']).convert('RGB');im.thumbnail((310,175));x=(j%4)*320;y=(j//4)*210;sheet.paste(im,(x+(320-im.width)//2,y));draw.text((x+8,y+179),f"{f['id']} / {f['seconds']:.2f}s / candidate",fill='#161616')
        sheet.save(out/f'contact-{k//20+1:02d}.jpg',quality=88)
    result={'schema':1,'videoId':video_id,'videoSha256':video_hash,'localDurationSeconds':duration,'sourceWidth':v['width'],'sourceHeight':v['height'],'frames':frames,'status':'AWAITING_VISUAL_REVIEW','notes':['Candidate extraction does not establish educational value or complete coverage.','Inspect diagrams, code changes, text readability and their match to the paragraph.','Do not use facial closeups or thumbnails as instructional figures.','For small text, use the original frame or a reviewed crop; never hallucinate letters with generative upscaling.']}
    (out/'frames.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    return result
if __name__=='__main__':
    try:
        video,plan,out,vid=sys.argv[1:];r=extract(pathlib.Path(video),json.loads(pathlib.Path(plan).read_text()),pathlib.Path(out),vid);print(json.dumps({'candidates':len(r['frames']),'status':r['status']}))
    except Exception as exc:
        print(json.dumps({'error':str(exc)},ensure_ascii=False),file=sys.stderr);sys.exit(1)
