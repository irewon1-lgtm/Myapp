import fs from 'node:fs';
import { createHash } from 'node:crypto';
import { DEFAULTS, POLICY } from '../public/engine/learning.mjs';
const expected={
 'public/engine/core.mjs':'d037c8aedd70be27bfceb4b28d3e911d3c2a2e71',
 'public/engine/index.html':'13c5a1c8cd77274bb6ac444b452eeabb475c2258',
 'public/engine/workbench.mjs':'5abf442aa56830ee12fe7ead1286217e59db483a',
 'engine/cli.mjs':'26d3dc08633aa99ca5d531cc2efaa81f72b8a2e5',
 'engine/runner.mjs':'04fd261efda408acfa53b20211f8ad7ed895860f',
 'public/engine/defaults.json':'ab40f0dbaf10f602ebfacea42792e5e4ce268982'
};
const blob=b=>createHash('sha1').update(Buffer.concat([Buffer.from(`blob ${b.length}\0`),b])).digest('hex');
for(const [p,sha]of Object.entries(expected))if(blob(fs.readFileSync(p))!==sha)throw Error(`Concurrent source change: ${p}; stop without overwriting.`);
const edits=new Map();
for(const p of ['engine/cli.mjs','engine/runner.mjs'])edits.set(p,fs.readFileSync(p,'utf8').replace('../public/engine/core.mjs','../public/engine/learning.mjs'));
let w=fs.readFileSync('public/engine/workbench.mjs','utf8').replace("from './core.mjs'","from './learning.mjs?v=1.1.0'");
w=w.replace('if(job.source.warnings.length)',"if(job.plan?.warnings?.length)r.append(text('p','긴 원문은 호출·재시도 한도를 먼저 점검합니다. 한도에 맞추려고 내용을 줄이지 않습니다.','muted'));\n if(job.source.warnings.length)");edits.set('public/engine/workbench.mjs',w);
let h=fs.readFileSync('public/engine/index.html','utf8');
h=h.replace('./workbench.mjs','./workbench.mjs?v=1.1.0').replace('./workbench.css','./workbench.css?v=1.1.0').replace('<span id="version">1.0</span>','<span id="version">1.1</span>');
h=h.replace('원문 기준본과 초보자 해설을 따로 만들고, 대조가 끝난 내용만 서재로 보냅니다.','원문은 지키고, 어려운 부분은 차근차근. 한국어·영어 강의 모두 뜻을 보존하는 기준본과 초보자 해설을 분리합니다.');
h=h.replace('<strong>원문 보존 → 쉬운 해설</strong><span>정의뿐 아니라 예시·비유·연결 설명·주의점도 보존합니다.</span>','<strong>원문 → 한국어 기준본 → 필요한 부분만 쉬운 해설</strong><span>영어 책과 직역본을 여러 번 만들지 않습니다. 충실한 한국어 번역을 기준본으로 삼고, 이미 쉬운 문장은 그대로 보존합니다.</span>');
h=h.replace('<strong>4분 안팎 · 동시에 2구간</strong><span>텍스트가 많으면 더 짧게 나눕니다. 완료한 구간은 다시 만들지 않습니다.</span>','<strong>분량을 줄여 속도를 맞추지 않습니다</strong><span>예시·비유·조건·주의사항도 학습 내용입니다. 원문 내부의 대응 범위와 의미 단위별 검수를 요구합니다. 짧아진 문단과 같은 설명의 반복은 따로 점검합니다.</span></div><div class="rule"><strong>4분 안팎 · 동시에 2구간부터</strong><span>자료 밀도와 한도를 보고 더 작게 나누며, 변경 없는 문단은 재생성하지 않습니다. 실제 번역 속도를 보증하는 수치는 아닙니다.</span>');
h=h.replace('<strong>검수 전에는 게시하지 않음</strong><span>근거 없는 문장, 불확실한 용어, 필요한 화면이 빠지면 검토 상태로 멈춥니다.</span>','<strong>원문과 양방향으로 확인합니다</strong><span>원문에서 빠진 설명과 최종 한국어에 새로 생긴 주장을 모두 확인합니다. 부정·조건·숫자와 단위·코드·의견을 지키고, 검토되지 않은 내용은 게시하지 않습니다.</span>');
h=h.replace('<footer>원문 전체 구간의 참조가 있어도 의미의 정확성까지 자동으로 증명되지는 않습니다.<br>실제 영상 확인과 독립 검수를 통과한 뒤에만 게시합니다.</footer>','<footer>출처 연결과 검수 기록은 의미 정확성을 자동으로 증명하지 않습니다.<br>집필·의미 검수는 작성자 또는 연결된 모델이 수행합니다. 검사 역할 분리는 독립 인간 검수를 뜻하지 않습니다.</footer>');
edits.set('public/engine/index.html',h);edits.set('public/engine/defaults.json',JSON.stringify(DEFAULTS,null,2)+'\n');
edits.set('public/engine/learning-policy.json',JSON.stringify({version:'1.1.0',entrypoint:'learning.mjs',...POLICY,validationScope:'Reference coverage and declared semantic review contracts; not proof of translation correctness.'},null,2)+'\n');
for(const [p,s]of edits)fs.writeFileSync(p,s);
console.log('Installed reader-first defaults; no catalog, book, or reader files changed.');
