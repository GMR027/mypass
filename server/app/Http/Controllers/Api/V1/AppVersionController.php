<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Storage;

class AppVersionController extends Controller
{
    public function show(): JsonResponse
    {
        $path = 'downloads/version.json';

        abort_unless(Storage::disk('local')->exists($path), 404);

        $version = json_decode(Storage::disk('local')->get($path), true);

        abort_unless(is_array($version), 500);

        return response()
            ->json($version)
            ->header('Cache-Control', 'no-store, max-age=0');
    }
}
