<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('contacts', function (Blueprint $table) {
            $table->foreignId('initiated_by_user_id')
                ->nullable()
                ->after('contact_user_id')
                ->constrained('users')
                ->nullOnDelete();
        });

        // Las solicitudes existentes guardan en la notificación el ID de la
        // fila del contacto del remitente. Con ese dato reconstruimos quién
        // inició la relación para que la interfaz no muestre "Aceptar" a quien
        // envió la invitación.
        $notifications = DB::table('user_notifications')
            ->where('type', 'contact_request')
            ->orderBy('created_at')
            ->get();

        foreach ($notifications as $notification) {
            $data = json_decode((string) $notification->data, true);
            $requesterContactId = $data['contact_id'] ?? null;

            if (! is_string($requesterContactId) || $requesterContactId === '') {
                continue;
            }

            $requesterContact = DB::table('contacts')->where('id', $requesterContactId)->first();

            if (! $requesterContact) {
                continue;
            }

            DB::table('contacts')
                ->where(function ($query) use ($requesterContact) {
                    $query->where(function ($pair) use ($requesterContact) {
                        $pair->where('user_id', $requesterContact->user_id)
                            ->where('contact_user_id', $requesterContact->contact_user_id);
                    })->orWhere(function ($pair) use ($requesterContact) {
                        $pair->where('user_id', $requesterContact->contact_user_id)
                            ->where('contact_user_id', $requesterContact->user_id);
                    });
                })
                ->update(['initiated_by_user_id' => $requesterContact->user_id]);
        }
    }

    public function down(): void
    {
        Schema::table('contacts', function (Blueprint $table) {
            $table->dropConstrainedForeignId('initiated_by_user_id');
        });
    }
};
