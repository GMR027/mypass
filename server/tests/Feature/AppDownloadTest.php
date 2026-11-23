<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class AppDownloadTest extends TestCase
{
    public function test_android_apk_download_is_public(): void
    {
        Storage::fake('local');
        Storage::disk('local')->put('downloads/mypass.apk', 'apk-content');

        $response = $this->get(route('app.download'));

        $response->assertOk();
        $response->assertHeader('content-type', 'application/vnd.android.package-archive');
        $response->assertDownload('mypass.apk');
    }

    public function test_android_apk_download_returns_404_when_missing(): void
    {
        Storage::fake('local');

        $this->get(route('app.download'))->assertNotFound();
    }
}
