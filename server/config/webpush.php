<?php

return [
    'enabled' => filter_var(env('WEBPUSH_ENABLED', false), FILTER_VALIDATE_BOOL),

    'vapid' => [
        'subject' => env('VAPID_SUBJECT', env('APP_URL', 'https://mypass.iguzman.com.mx')),
        'public_key' => env('VAPID_PUBLIC_KEY'),
        'private_key' => env('VAPID_PRIVATE_KEY'),
    ],

    'ttl' => (int) env('WEBPUSH_TTL', 86400),
];
