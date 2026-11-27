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


self.addEventListener('push', (event) => {
    if (!event.data) {
        return;
    }

    let payload = {};

    try {
        payload = event.data.json();
    } catch {
        payload = { body: event.data.text() };
    }

    const title = payload.title || 'My Pass';
    const options = {
        body: payload.body || '',
        icon: payload.icon || '/images/favicon-180.png',
        badge: payload.badge || '/images/favicon-32.png',
        tag: payload.tag || 'mypass',
        renotify: true,
        data: {
            url: payload.url || '/notifications',
            type: payload.type || 'activity',
        },
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();

    const target = new URL(event.notification.data?.url || '/notifications', self.location.origin).href;

    event.waitUntil((async () => {
        const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });

        for (const client of clients) {
            if (client.url.startsWith(self.location.origin)) {
                await client.focus();
                client.postMessage({ type: 'mypass-open-url', url: target });
                return;
            }
        }

        await self.clients.openWindow(target);
    })());
});
