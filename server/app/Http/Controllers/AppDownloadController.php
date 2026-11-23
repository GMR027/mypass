<?php

namespace App\Http\Controllers;

use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\BinaryFileResponse;

class AppDownloadController extends Controller
{
    public function android(): BinaryFileResponse
    {
        $path = Storage::disk('local')->path('downloads/mypass.apk');

        abort_unless(is_file($path), 404);

        return response()->download($path, 'mypass.apk', [
            'Content-Type' => 'application/vnd.android.package-archive',
            'Cache-Control' => 'public, max-age=3600',
        ]);
    }
}
