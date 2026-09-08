const CACHE="tongin-db-v8";
const ASSETS=["./","./index.html","./manifest.json","./sw.js"];
self.addEventListener("install",e=>{self.skipWaiting();e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)))});
self.addEventListener("activate",e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener("fetch",e=>{
  const r=e.request;
  if(r.mode==="navigate" || new URL(r.url).pathname.endsWith("/index.html")){
    e.respondWith(fetch(r).then(resp=>{const copy=resp.clone();caches.open(CACHE).then(c=>c.put(r,copy));return resp}).catch(()=>caches.match(r).then(x=>x||caches.match("./index.html"))));
    return;
  }
  e.respondWith(caches.match(r).then(cached=>cached||fetch(r).then(resp=>{const copy=resp.clone();caches.open(CACHE).then(c=>c.put(r,copy));return resp})));
});
