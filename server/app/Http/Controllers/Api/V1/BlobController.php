<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Blob;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Symfony\Component\HttpFoundation\StreamedResponse;

class BlobController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'file' => ['required', 'file', 'max:102400'],
        ]);

        $user = $request->user();
        $id = (string) Str::uuid();
        $directory = 'vault/'.$user->id;
        $filename = $id.'.bin';
        $path = $data['file']->storeAs($directory, $filename, 'local');

        $size = Storage::disk('local')->size($path);
        $sha256 = hash_file('sha256', Storage::disk('local')->path($path));

        $blob = Blob::create([
            'id' => $id,
            'user_id' => $user->id,
            'path' => $path,
            'size' => $size,
            'sha256' => $sha256,
            'ref_count' => 0,
        ]);

        return response()->json([
            'data' => [
                'id' => $blob->id,
                'size' => $blob->size,
                'sha256' => $blob->sha256,
            ],
        ], 201);
    }

    public function show(Request $request, string $blob): StreamedResponse
    {
        $blob = $request->user()->items()
            ->whereNotNull('blob_id')
            ->where('blob_id', $blob)
            ->firstOrFail()
            ->blob;

        abort_unless($blob && Storage::disk('local')->exists($blob->path), 404);

        return Storage::disk('local')->download($blob->path, 'document.bin', [
            'Content-Type' => 'application/octet-stream',
            'Cache-Control' => 'no-store, private',
        ]);
    }
}
