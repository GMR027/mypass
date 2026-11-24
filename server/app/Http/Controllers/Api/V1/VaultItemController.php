<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Blob;
use App\Models\VaultItem;
use App\Services\AuditLogger;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;

class VaultItemController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $data = $request->validate([
            'state' => ['sometimes', Rule::in(['active', 'transferred', 'received', 'trashed'])],
            'trashed' => ['sometimes', 'boolean'],
        ]);

        $onlyTrashed = (bool) ($data['trashed'] ?? false)
            || ($data['state'] ?? null) === 'trashed';

        if ($onlyTrashed) {
            $this->purgeExpiredTrash($request);
        }

        $query = $request->user()->items();

        if ($onlyTrashed) {
            $query->onlyTrashed();
        } elseif (isset($data['state'])) {
            $query->where('state', $data['state']);
        }

        $items = $query
            ->orderByDesc($onlyTrashed ? 'deleted_at' : 'updated_at')
            ->get()
            ->map(fn (VaultItem $item) => $this->payload($item));

        return response()->json(['data' => $items]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'id' => ['sometimes', 'uuid'],
            'type' => ['required', Rule::in(VaultItem::TYPES)],
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
            'type' => ['sometimes', Rule::in(VaultItem::TYPES)],
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

    public function destroy(Request $request, string $item, AuditLogger $audit): JsonResponse
    {
        $item = $request->user()->items()->findOrFail($item);
        $audit->log($request->user(), 'item_deleted', [
            'item_id' => $item->id,
            'type' => $item->type,
        ]);
        $item->delete();

        return response()->json([
            'message' => __('Documento movido a la papelera.'),
            'data' => $this->payload($item->fresh() ?? $item),
        ]);
    }

    public function restore(Request $request, string $item, AuditLogger $audit): JsonResponse
    {
        $item = $request->user()->items()->onlyTrashed()->findOrFail($item);

        $item->restore();
        $item->forceFill([
            'version' => $item->version + 1,
            'updated_at' => now(),
        ])->save();

        $audit->log($request->user(), 'item_restored', [
            'item_id' => $item->id,
            'type' => $item->type,
        ]);

        return response()->json(['data' => $this->payload($item->fresh() ?? $item)]);
    }

    public function forceDestroy(Request $request, string $item, AuditLogger $audit): JsonResponse
    {
        $item = $request->user()->items()->onlyTrashed()->findOrFail($item);
        $blobId = $item->blob_id;
        $type = $item->type;
        $id = $item->id;

        $item->forceDelete();
        $this->deleteBlobIfUnused($request->user()->id, $blobId);

        $audit->log($request->user(), 'item_purged', [
            'item_id' => $id,
            'type' => $type,
        ]);

        return response()->json(['message' => __('Documento eliminado definitivamente.')]);
    }

    private function purgeExpiredTrash(Request $request): void
    {
        $expired = $request->user()->items()
            ->onlyTrashed()
            ->where('deleted_at', '<=', now()->subDays(30))
            ->get();

        foreach ($expired as $item) {
            $blobId = $item->blob_id;
            $item->forceDelete();
            $this->deleteBlobIfUnused($request->user()->id, $blobId);
        }
    }

    private function deleteBlobIfUnused(int $userId, ?string $blobId): void
    {
        if ($blobId === null) {
            return;
        }

        $referenced = VaultItem::withTrashed()
            ->where('user_id', $userId)
            ->where('blob_id', $blobId)
            ->exists();

        if ($referenced) {
            return;
        }

        $blob = Blob::where('user_id', $userId)->find($blobId);
        if ($blob === null) {
            return;
        }

        Storage::disk('local')->delete($blob->path);
        $blob->delete();
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
            'state' => $item->trashed() ? 'trashed' : $item->state,
            'encrypted_metadata' => $item->encrypted_metadata,
            'blob_id' => $item->blob_id,
            'version' => $item->version,
            'expires_at' => $item->expires_at?->toIso8601String(),
            'deleted_at' => $item->deleted_at?->toIso8601String(),
            'transferred_to' => $item->transferredTo?->public_id,
            'transferred_at' => $item->transferred_at?->toIso8601String(),
            'updated_at' => $item->updated_at?->toIso8601String(),
        ];
    }
}
