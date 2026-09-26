const tests = [
  ['seongnam','정자동-1339'],
  ['gwangjin','자양동-6060'],
  ['songpa','잠실동-6188'],
  ['gangnam','역삼동-6035'],
  ['seocho','서초동-6128'],
  ['gangdong','천호동-6044'],
];

for (const [name, region] of tests) {
  const u = new URL('https://www.daangn.com/kr/buy-sell/all/');
  u.searchParams.set('in', region);
  u.searchParams.set('search', '노트북');
  u.searchParams.set('_data', 'routes/kr.buy-sell._index');
  try {
    const r = await fetch(u, {headers:{'User-Agent':'Mozilla/5.0','Accept':'application/json,text/html;q=0.9,*/*;q=0.8','Accept-Language':'ko-KR,ko;q=0.9'}});
    const t = await r.text();
    console.log('TEST',name,region,'HTTP',r.status,'LEN',t.length,'HEAD',JSON.stringify(t.slice(0,500)));
    try {
      const j=JSON.parse(t);
      console.log('KEYS',name,JSON.stringify(Object.keys(j)));
      console.log('REGION',name,JSON.stringify(j.region ?? null));
      const arr=j?.allPage?.fleamarketArticles;
      console.log('COUNT',name,Array.isArray(arr)?arr.length:null);
      if(Array.isArray(arr)&&arr[0]) console.log('ITEM',name,JSON.stringify(arr[0]));
    } catch(e) { console.log('JSONERR',name,String(e)); }
  } catch(e) { console.log('FETCHERR',name,String(e)); }
}