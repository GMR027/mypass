<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Contact;
use App\Models\User;
use App\Models\UserNotification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ContactController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $contacts = $request->user()->contacts()
            ->with('contactUser')
            ->orderByDesc('created_at')
            ->get()
            ->map(fn (Contact $contact) => $this->payload($contact));

        return response()->json(['data' => $contacts]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'email' => ['required', 'string', 'lowercase', 'email', 'max:255'],
        ]);

        $contactUser = User::where('email', $data['email'])->first();

        if (! $contactUser) {
            return response()->json([
                'message' => __('No existe una cuenta con ese correo. La otra persona debe crear su cuenta primero.'),
            ], 422);
        }

        if ($contactUser->id === $request->user()->id) {
            return response()->json(['message' => __('No puedes agregarte a ti mismo.')], 422);
        }

        $existing = $request->user()->contacts()
            ->where('contact_user_id', $contactUser->id)
            ->first();

        if ($existing && $existing->status === 'accepted') {
            return response()->json(['data' => $this->payload($existing)]);
        }

        $contact = Contact::updateOrCreate(
            [
                'user_id' => $request->user()->id,
                'contact_user_id' => $contactUser->id,
            ],
            ['status' => 'pending'],
        );

        Contact::updateOrCreate(
            [
                'user_id' => $contactUser->id,
                'contact_user_id' => $request->user()->id,
            ],
            ['status' => 'pending'],
        );

        UserNotification::create([
            'user_id' => $contactUser->id,
            'type' => 'contact_request',
            'data' => [
                'contact_id' => $contact->id,
                'from' => $request->user()->name,
            ],
        ]);

        return response()->json(['data' => $this->payload($contact)], 201);
    }

    public function accept(Request $request, string $contact): JsonResponse
    {
        $contact = $request->user()->contacts()->findOrFail($contact);

        if ($contact->status !== 'pending') {
            return response()->json(['message' => __('Esta relación ya fue procesada.')], 422);
        }

        $contact->forceFill(['status' => 'accepted', 'verified_at' => now()])->save();

        Contact::where('user_id', $contact->contact_user_id)
            ->where('contact_user_id', $request->user()->id)
            ->update(['status' => 'accepted', 'verified_at' => now()]);

        UserNotification::create([
            'user_id' => $contact->contact_user_id,
            'type' => 'contact_accepted',
            'data' => [
                'contact_id' => $contact->id,
                'from' => $request->user()->name,
            ],
        ]);

        return response()->json(['data' => $this->payload($contact)]);
    }

    public function publicKey(Request $request, string $contact): JsonResponse
    {
        $contact = $request->user()->contacts()
            ->where('status', 'accepted')
            ->with('contactUser.keys')
            ->findOrFail($contact);

        $publicKey = $contact->contactUser->keys?->identity_public_encryption_key;

        if (! $publicKey) {
            return response()->json([
                'message' => __('Esa cuenta todavía no tiene su bóveda configurada.'),
            ], 422);
        }

        return response()->json([
            'data' => [
                'public_key' => $publicKey,
            ],
        ]);
    }

    public function block(Request $request, string $contact): JsonResponse
    {
        $contact = $request->user()->contacts()->findOrFail($contact);
        $contact->forceFill(['status' => 'blocked'])->save();
        Contact::where('user_id', $contact->contact_user_id)
            ->where('contact_user_id', $request->user()->id)
            ->update(['status' => 'blocked']);

        return response()->json(['message' => __('Contacto bloqueado.')]);
    }

    private function payload(Contact $contact): array
    {
        return [
            'id' => $contact->id,
            'status' => $contact->status,
            'name' => $contact->contactUser->name,
            'email' => $contact->contactUser->email,
            'public_id' => $contact->contactUser->public_id,
            'verified_at' => $contact->verified_at?->toIso8601String(),
        ];
    }
}
