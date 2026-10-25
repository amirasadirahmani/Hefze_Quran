import importlib.util,json,shutil,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('assets',ROOT/'tools/validate_assets.py');assets=importlib.util.module_from_spec(spec);spec.loader.exec_module(assets)
class ContentTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        target=self.root/'app/src/main/assets';shutil.copytree(ROOT/'app/src/main/assets',target)
        for path in (target/'audio/036').glob('*.mp3'):path.unlink()
        for path in (target/'timings/036').glob('*.json'):
            data=json.loads(path.read_text());data['reviewed']=False;data['durationMs']=None;data['audioSha256']=None
            for word in data['words']:word['startMs']=None;word['endMs']=None
            path.write_text(json.dumps(data))
    def test_development_valid(self):
        errors,pending=assets.validate(self.root);self.assertEqual(errors,[]);self.assertEqual(len(pending),166)
    def test_release_refuses_missing_audio_and_timings(self):
        errors,_=assets.validate(self.root,release=True);self.assertEqual(len(errors),166)
    def test_corruption_detected(self):
        path=self.root/'app/src/main/assets/content/yasin.json';data=json.loads(path.read_text());data['pages'][0]['lines'][0][0]['text']='wrong';path.write_text(json.dumps(data))
        errors,_=assets.validate(self.root);self.assertTrue(any('missing, duplicated' in e for e in errors))
    def test_fake_review_rejected(self):
        path=self.root/'app/src/main/assets/timings/036/036-001.json';data=json.loads(path.read_text());data['reviewed']=True;path.write_text(json.dumps(data))
        errors,_=assets.validate(self.root);self.assertTrue(any('Bad duration' in e for e in errors))
    def test_stale_audio_hash_rejected(self):
        path=self.root/'app/src/main/assets/timings/036/036-001.json';data=json.loads(path.read_text());data.update(reviewed=True,durationMs=1000,audioSha256='stale');data['words'][0].update(startMs=0,endMs=1000);path.write_text(json.dumps(data))
        (self.root/'app/src/main/assets/audio/036/036-001.mp3').write_bytes(b'fixture only')
        errors,_=assets.validate(self.root);self.assertTrue(any('different audio' in e for e in errors))
    def test_catalog_must_not_advertise_unavailable_packs(self):
        path=self.root/'app/src/main/assets/content/surahs.json';data=json.loads(path.read_text());data['surahs'][0]['available']=True;path.write_text(json.dumps(data))
        errors,_=assets.validate(self.root);self.assertIn('Only Yasin has a learning pack',errors)
    def test_yasin_juz_boundary_is_preserved(self):
        path=self.root/'app/src/main/assets/content/surahs.json';data=json.loads(path.read_text());data['surahs'][35]['lastJuz']=22;path.write_text(json.dumps(data))
        errors,_=assets.validate(self.root);self.assertIn('Yasin must span juz 22 and 23',errors)
if __name__=='__main__':unittest.main()
