<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Blob;
use App\Models\VaultItem;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class VaultItemController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $data = $request->validate([
            'state' => ['sometimes', Rule::in(['active', 'transferred', 'received', 'trashed'])],
        ]);

        $items = $request->user()->items()
            ->when($data['state'] ?? null, fn ($query, $state) => $query->where('state', $state))
            ->orderByDesc('updated_at')
            ->get()
            ->map(fn (VaultItem $item) => $this->payload($item));

        return response()->json(['data' => $items]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'id' => ['sometimes', 'uuid'],
            'type' => ['required', Rule::in(['ticket', 'document', 'card', 'other'])],
            'encrypted_metadata' => ['required', 'string'],
            'blob_id' => ['nullable', 'uuid'],
            'expires_at' => ['nullable', 'date'],
        ]);

        $this->assertBlobBelongsToUser($request, $data['blob_id'] ?? null);

        $item = $request->user()->items()->create([
            'id' => $data['id'] ?? null,
            'type' => $data['type'],
            'state' => 'active',
            'encrypted_metadata' => $data['encrypted_metadata'],
            'blob_id' => $data['blob_id'] ?? null,
            'version' => 1,
            'expires_at' => $data['expires_at'] ?? null,
        ]);

        return response()->json(['data' => $this->payload($item)], 201);
    }

    public function show(Request $request, string $item): JsonResponse
    {
        $item = $request->user()->items()->findOrFail($item);

        return response()->json(['data' => $this->payload($item)]);
    }

    public function update(Request $request, string $item): JsonResponse
    {
        $item = $request->user()->items()->findOrFail($item);

        $data = $request->validate([
            'type' => ['sometimes', Rule::in(['ticket', 'document', 'card', 'other'])],
            'encrypted_metadata' => ['sometimes', 'string'],
            'blob_id' => ['sometimes', 'nullable', 'uuid'],
            'expires_at' => ['sometimes', 'nullable', 'date'],
            'version' => ['required', 'integer', 'min:1'],
        ]);

        if ((int) $data['version'] !== $item->version) {
            return response()->json([
                'message' => __('El documento cambió en otro dispositivo. Vuelve a cargarlo.'),
                'current_version' => $item->version,
            ], 409);
        }

        $this->assertBlobBelongsToUser($request, $data['blob_id'] ?? null);

        $item->fill([
            'type' => $data['type'] ?? $item->type,
            'encrypted_metadata' => $data['encrypted_metadata'] ?? $item->encrypted_metadata,
            'blob_id' => array_key_exists('blob_id', $data) ? $data['blob_id'] : $item->blob_id,
            'expires_at' => array_key_exists('expires_at', $data) ? $data['expires_at'] : $item->expires_at,
            'version' => $item->version + 1,
        ])->save();

        return response()->json(['data' => $this->payload($item)]);
    }

    public function destroy(Request $request, string $item): JsonResponse
    {
        $item = $request->user()->items()->findOrFail($item);
        $item->delete();

        return response()->json(['message' => __('Documento movido a la papelera.')]);
    }

    private function assertBlobBelongsToUser(Request $request, ?string $blobId): void
    {
        if ($blobId === null) {
            return;
        }

        abort_unless(
            $request->user()->items()->where('blob_id', $blobId)->exists()
                || Blob::where('user_id', $request->user()->id)->whereKey($blobId)->exists(),
            422,
            __('El archivo no pertenece a esta cuenta.')
        );
    }

    private function payload(VaultItem $item): array
    {
        return [
            'id' => $item->id,
            'type' => $item->type,
            'state' => $item->state,
            'encrypted_metadata' => $item->encrypted_metadata,
            'blob_id' => $item->blob_id,
            'version' => $item->version,
            'expires_at' => $item->expires_at?->toIso8601String(),
            'transferred_to' => $item->transferredTo?->public_id,
            'transferred_at' => $item->transferred_at?->toIso8601String(),
            'updated_at' => $item->updated_at?->toIso8601String(),
        ];
    }
}
