<?php

namespace App\Services;

use App\Models\AuditEvent;
use App\Models\User;
use Illuminate\Http\Request;

class AuditLogger
{
    /**
     * @param  array<string, mixed>  $metadata
     */
    public function log(User $user, string $event, array $metadata = []): void
    {
        $request = request();

        AuditEvent::create([
            'user_id' => $user->id,
            'event' => $event,
            'metadata' => $metadata,
            'ip_hash' => $this->ipHash($request),
            'created_at' => now(),
        ]);
    }

    private function ipHash(?Request $request): ?string
    {
        if (! $request instanceof Request || $request->ip() === null) {
            return null;
        }

        return hash_hmac('sha256', $request->ip(), (string) config('app.key'));
    }
}
