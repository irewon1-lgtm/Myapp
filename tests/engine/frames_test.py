import unittest, tempfile, pathlib, subprocess, sys, json, importlib.util
spec=importlib.util.spec_from_file_location('frames',pathlib.Path(__file__).parents[2]/'engine/frames.py');frames=importlib.util.module_from_spec(spec);spec.loader.exec_module(frames)
class Frames(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=pathlib.Path(self.t.name);self.video=self.root/'control.mp4'
  subprocess.run(['ffmpeg','-loglevel','error','-f','lavfi','-i','color=c=red:s=1280x720:r=10:d=2','-f','lavfi','-i','color=c=green:s=1280x720:r=10:d=2','-f','lavfi','-i','color=c=blue:s=1280x720:r=10:d=2','-filter_complex','[0:v][1:v][2:v]concat=n=3:v=1:a=0[v]','-map','[v]','-c:v','libx264','-pix_fmt','yuv420p','-y',str(self.video)],check=True)
 def tearDown(self):self.t.cleanup()
 def test_real_decoding_and_time_selection(self):
  r=frames.extract(self.video,{'candidates':[{'seconds':1},{'seconds':3},{'seconds':5}]},self.root/'out','0SGfDKMLdaI')
  self.assertEqual(len(r['frames']),3);self.assertEqual(r['status'],'AWAITING_VISUAL_REVIEW')
  for i,f in enumerate(r['frames']):
   im=frames.Image.open(self.root/'out'/f['file']);pixel=im.getpixel((400,300));self.assertGreater(pixel[i],max(pixel[j] for j in range(3) if j!=i));self.assertIsNone(f['review']);self.assertEqual(f['sha256'],frames.sha(self.root/'out'/f['file']));self.assertEqual(f['capture']['videoSha256'],r['videoSha256'])
  self.assertTrue((self.root/'out/contact-01.jpg').exists())
 def test_out_of_bounds(self):
  with self.assertRaises(ValueError):frames.extract(self.video,{'candidates':[{'seconds':99}]},self.root/'out','0SGfDKMLdaI')
 def test_no_thumbnail_or_remote_url_fallback(self):
  with self.assertRaises(ValueError):frames.extract(pathlib.Path('https://youtu.be/0SGfDKMLdaI'),{'candidates':[]},self.root/'out','0SGfDKMLdaI')
if __name__=='__main__':unittest.main()
