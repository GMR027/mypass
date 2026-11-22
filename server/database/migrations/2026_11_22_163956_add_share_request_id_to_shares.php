<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('shares', function (Blueprint $table) {
            $table->uuid('share_request_id')->nullable()->after('recipient_id');
            $table->foreign('share_request_id')->references('id')->on('share_requests')->nullOnDelete();
            $table->index('share_request_id');
        });
    }

    public function down(): void
    {
        Schema::table('shares', function (Blueprint $table) {
            $table->dropForeign(['share_request_id']);
            $table->dropColumn('share_request_id');
        });
    }
};
