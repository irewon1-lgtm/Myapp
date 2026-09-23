import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {engineState} from '../../scripts/engine-info.mjs';
import {createSpec} from '../../scripts/current-spec.mjs';
const engine=await engineState('public');
const policy=JSON.parse(fs.readFileSync('public/engine/learning-policy.json','utf8'));
const release={id:'same-tested-source',sourceDigest:'a'.repeat(64),sourceSha:'b'.repeat(40)};
test('Netlify mirror commit changes cannot change the current authoring bundle',()=>{
  assert.deepEqual(createSpec(engine,policy,release),createSpec(engine,policy,{...release,sourceSha:'c'.repeat(40)}));
});
test('a different actual source fingerprint remains detectable',()=>{
  assert.notDeepEqual(createSpec(engine,policy,release),createSpec(engine,policy,{...release,sourceDigest:'d'.repeat(64)}));
});
