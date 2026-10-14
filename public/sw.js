// Service Worker for kv-suno PWA Media Player — v6.4
const CACHE_NAME = 'kv-suno-pwa-v6.4';

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  // Never intercept range audio streams or dynamic assets; let browser stream natively for background audio continuity
  return;
});
