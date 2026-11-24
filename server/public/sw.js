const SHARE_PATH = '/share-target';
let pendingShare = null;

self.addEventListener('install', () => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('message', (event) => {
    if (event.data?.type !== 'mypass-share-request') {
        return;
    }

    const port = event.ports?.[0];
    const files = pendingShare?.files ?? [];
    pendingShare = null;
    port?.postMessage({ type: 'mypass-share-files', files });
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    if (event.request.method !== 'POST' || url.pathname !== SHARE_PATH) {
        return;
    }

    event.respondWith((async () => {
        try {
            const form = await event.request.formData();
            const files = form.getAll('files').filter((value) => value instanceof File);
            pendingShare = { files, at: Date.now() };

            const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
            for (const client of clients) {
                client.postMessage({ type: 'mypass-share-ready', count: files.length });
            }

            return Response.redirect('/vault?share=1', 303);
        } catch {
            return Response.redirect('/vault?share=error', 303);
        }
    })());
});
