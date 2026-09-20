const VERSION='0.5';
self.addEventListener('install',event=>event.waitUntil(self.skipWaiting()));
self.addEventListener('activate',event=>event.waitUntil((async()=>{
  const keys=await caches.keys();
  await Promise.all(keys.filter(k=>k.startsWith('blendweb-')).map(k=>caches.delete(k)));
  await self.clients.claim();
  const windows=await self.clients.matchAll({type:'window',includeUncontrolled:true});
  for(const client of windows){
    try{
      const url=new URL(client.url);
      if(url.origin!==self.location.origin) continue;
      if(url.searchParams.get('_bw')===VERSION) continue;
      url.searchParams.set('_bw',VERSION);
      await client.navigate(url.href);
    }catch{}
  }
  await self.registration.unregister();
})()));
