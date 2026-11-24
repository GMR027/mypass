<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class AppVersionTest extends TestCase
{
    public function test_app_version_is_public(): void
    {
        Storage::fake('local');
        Storage::disk('local')->put('downloads/version.json', json_encode([
            'versionCode' => 4,
            'versionName' => '0.4.0',
            'apkUrl' => 'https://mypass.iguzman.com.mx/downloads/mypass.apk?v=4',
            'sha256' => str_repeat('a', 64),
            'sizeBytes' => 1024,
            'minSupportedVersionCode' => 0,
            'publishedAt' => '2026-11-24T00:00:00Z',
            'notes' => ['es' => ['Nueva versión'], 'en' => ['New version']],
        ]));

        $response = $this->getJson(route('api.app.version'));

        $response->assertOk();
        $response->assertJsonPath('versionCode', 4);
        $response->assertJsonPath('versionName', '0.4.0');
        $response->assertHeader('cache-control', 'max-age=0, no-store, private');
    }

    public function test_app_version_returns_404_when_missing(): void
    {
        Storage::fake('local');

        $this->getJson(route('api.app.version'))->assertNotFound();
    }
}
