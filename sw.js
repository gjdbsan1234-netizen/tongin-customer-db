const CACHE="tongin-db-v10";
self.addEventListener("install",e=>{self.skipWaiting()});
self.addEventListener("activate",e=>e.waitUntil(
  caches.keys().then(keys=>Promise.all(keys.map(k=>caches.delete(k)))).then(()=>self.clients.claim())
));
self.addEventListener("fetch",e=>{
  const r=e.request;
  if(r.mode==="navigate" || new URL(r.url).pathname.endsWith("/index.html")){
    e.respondWith(fetch(r,{cache:"no-store"}));
    return;
  }
  e.respondWith(fetch(r));
});
