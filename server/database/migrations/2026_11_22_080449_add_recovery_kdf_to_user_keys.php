<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('user_keys', function (Blueprint $table) {
            $table->text('recovery_kdf_salt')->nullable()->after('recovery_nonce');
            $table->json('recovery_kdf_params')->nullable()->after('recovery_kdf_salt');
        });
    }

    public function down(): void
    {
        Schema::table('user_keys', function (Blueprint $table) {
            $table->dropColumn(['recovery_kdf_salt', 'recovery_kdf_params']);
        });
    }
};
