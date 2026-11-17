// Service Worker for KV Suno PWA Media Player — v6.6
const CACHE_NAME = 'kv-suno-pwa-v6.6';
const STATIC_ASSETS = [
  '/',
  '/site.webmanifest',
  '/manifest.json',
  '/icon.svg',
  '/logo.svg',
  '/favicon.svg',
  '/icon-maskable.svg',
  '/pwa-192.png',
  '/pwa-512.png',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  '/apple-touch-icon.png',
  '/favicon.ico',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  // Never intercept byte-range audio requests or API streams so background playback stays uninterrupted
  if (req.headers.get('range') || req.url.includes('/api/')) {
    return;
  }

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) {
    return;
  }

  event.respondWith(
    fetch(req)
      .then((res) => {
        if (res && res.ok && res.type === 'basic') {
          const clone = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(req, clone)).catch(() => {});
        }
        return res;
      })
      .catch(() => caches.match(req).then((cached) => cached || caches.match('/')))
  );
});

self.addEventListener('message', (event) => {
  if (!event.data) return;
  if (event.data.type === 'BACKGROUND_AUDIO' || event.data.type === 'MEDIA_SESSION_ACTION') {
    self.clients.matchAll().then((clients) => {
      clients.forEach((client) => client.postMessage(event.data));
    });
  }
});
