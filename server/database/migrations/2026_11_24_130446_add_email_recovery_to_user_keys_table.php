<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('user_keys', function (Blueprint $table) {
            $table->text('email_recovery_secret')->nullable()->after('recovery_version');
            $table->longText('email_recovery_wrapped_master_key')->nullable()->after('email_recovery_secret');
            $table->unsignedInteger('email_recovery_version')->default(1)->after('email_recovery_wrapped_master_key');
            $table->timestamp('email_recovery_enabled_at')->nullable()->after('email_recovery_version');
        });
    }

    public function down(): void
    {
        Schema::table('user_keys', function (Blueprint $table) {
            $table->dropColumn([
                'email_recovery_secret',
                'email_recovery_wrapped_master_key',
                'email_recovery_version',
                'email_recovery_enabled_at',
            ]);
        });
    }
};
