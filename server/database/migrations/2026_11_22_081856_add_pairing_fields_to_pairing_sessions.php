<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('pairing_sessions', function (Blueprint $table) {
            $table->text('public_key')->nullable()->after('device_id');
            $table->string('device_name', 120)->nullable()->after('public_key');
            $table->string('status', 20)->default('pending')->after('code_hash');
            $table->timestamp('joined_at')->nullable()->after('status');
            $table->timestamp('approved_at')->nullable()->after('joined_at');
        });
    }

    public function down(): void
    {
        Schema::table('pairing_sessions', function (Blueprint $table) {
            $table->dropColumn([
                'public_key',
                'device_name',
                'status',
                'joined_at',
                'approved_at',
            ]);
        });
    }
};
