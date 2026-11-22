<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->uuid('public_id')->nullable()->unique()->after('id');
            $table->string('locale', 10)->default('es-MX')->after('password');
            $table->boolean('simple_mode')->default(true)->after('locale');
            $table->boolean('is_admin')->default(false)->after('simple_mode');
            $table->json('settings')->nullable()->after('is_admin');
        });

        Schema::create('devices', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('name');
            $table->string('platform', 20)->default('android');
            $table->text('public_key')->nullable();
            $table->longText('wrapped_master_key')->nullable();
            $table->text('pin_salt')->nullable();
            $table->json('pin_params')->nullable();
            $table->longText('biometric_wrapped_master_key')->nullable();
            $table->string('app_version', 40)->nullable();
            $table->timestamp('last_seen_at')->nullable();
            $table->timestamp('revoked_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'revoked_at']);
        });

        Schema::create('user_keys', function (Blueprint $table) {
            $table->foreignId('user_id')->primary()->constrained()->cascadeOnDelete();
            $table->longText('pc_wrapped_master_key')->nullable();
            $table->text('pc_kdf_salt')->nullable();
            $table->json('pc_kdf_params')->nullable();
            $table->longText('recovery_wrapped_master_key')->nullable();
            $table->text('recovery_nonce')->nullable();
            $table->unsignedInteger('recovery_version')->default(1);
            $table->text('identity_public_encryption_key')->nullable();
            $table->text('identity_public_signing_key')->nullable();
            $table->longText('identity_private_keys_encrypted')->nullable();
            $table->timestamps();
        });

        Schema::create('blobs', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('path')->unique();
            $table->unsignedBigInteger('size');
            $table->string('sha256', 64);
            $table->unsignedInteger('ref_count')->default(1);
            $table->timestamps();

            $table->index(['user_id', 'created_at']);
        });

        Schema::create('items', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('type', 32);
            $table->string('state', 32)->default('active');
            $table->longText('encrypted_metadata');
            $table->uuid('blob_id')->nullable();
            $table->unsignedInteger('version')->default(1);
            $table->timestamp('expires_at')->nullable();
            $table->uuid('source_share_id')->nullable();
            $table->foreignId('transferred_to_user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->timestamp('transferred_at')->nullable();
            $table->softDeletes();
            $table->timestamps();

            $table->foreign('blob_id')->references('id')->on('blobs')->nullOnDelete();
            $table->index(['user_id', 'state']);
            $table->index(['user_id', 'updated_at']);
            $table->index('source_share_id');
        });

        Schema::create('contacts', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('contact_user_id')->constrained('users')->cascadeOnDelete();
            $table->string('status', 20)->default('pending');
            $table->json('permissions')->nullable();
            $table->string('safety_code', 40)->nullable();
            $table->timestamp('verified_at')->nullable();
            $table->timestamps();

            $table->unique(['user_id', 'contact_user_id']);
            $table->index(['user_id', 'status']);
        });

        Schema::create('invites', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('email');
            $table->string('token_hash', 64)->unique();
            $table->timestamp('expires_at');
            $table->timestamp('accepted_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'email']);
        });

        Schema::create('pairing_sessions', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->uuid('device_id')->nullable();
            $table->string('purpose', 30)->default('pair_device');
            $table->string('code_hash', 64);
            $table->longText('payload')->nullable();
            $table->timestamp('expires_at');
            $table->timestamp('used_at')->nullable();
            $table->timestamps();

            $table->foreign('device_id')->references('id')->on('devices')->nullOnDelete();
            $table->index(['user_id', 'purpose', 'expires_at']);
        });

        Schema::create('shares', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('sender_id')->constrained('users')->cascadeOnDelete();
            $table->foreignId('recipient_id')->constrained('users')->cascadeOnDelete();
            $table->uuid('item_id')->nullable();
            $table->uuid('recipient_item_id')->nullable();
            $table->string('mode', 20)->default('copy');
            $table->string('status', 20)->default('pending');
            $table->longText('encrypted_package');
            $table->boolean('download_allowed')->default(true);
            $table->timestamp('expires_at')->nullable();
            $table->timestamp('accepted_at')->nullable();
            $table->timestamp('revoked_at')->nullable();
            $table->timestamps();

            $table->foreign('item_id')->references('id')->on('items')->nullOnDelete();
            $table->foreign('recipient_item_id')->references('id')->on('items')->nullOnDelete();
            $table->index(['sender_id', 'status']);
            $table->index(['recipient_id', 'status']);
            $table->index('expires_at');
        });

        Schema::create('share_requests', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('requester_id')->constrained('users')->cascadeOnDelete();
            $table->foreignId('target_id')->constrained('users')->cascadeOnDelete();
            $table->string('document_type', 60);
            $table->string('purpose', 255);
            $table->string('status', 20)->default('pending');
            $table->timestamp('expires_at')->nullable();
            $table->timestamp('responded_at')->nullable();
            $table->timestamps();

            $table->index(['target_id', 'status']);
            $table->index(['requester_id', 'status']);
        });

        Schema::create('user_notifications', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('type', 60);
            $table->json('data')->nullable();
            $table->timestamp('read_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'read_at']);
        });

        Schema::create('audit_events', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->foreignId('user_id')->nullable()->constrained()->nullOnDelete();
            $table->uuid('device_id')->nullable();
            $table->string('event', 80);
            $table->json('metadata')->nullable();
            $table->string('ip_hash', 64)->nullable();
            $table->timestamp('created_at')->useCurrent();

            $table->index(['user_id', 'created_at']);
            $table->index(['device_id', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('audit_events');
        Schema::dropIfExists('user_notifications');
        Schema::dropIfExists('share_requests');
        Schema::dropIfExists('shares');
        Schema::dropIfExists('pairing_sessions');
        Schema::dropIfExists('invites');
        Schema::dropIfExists('contacts');
        Schema::dropIfExists('items');
        Schema::dropIfExists('blobs');
        Schema::dropIfExists('user_keys');
        Schema::dropIfExists('devices');

        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['public_id', 'locale', 'simple_mode', 'is_admin', 'settings']);
        });
    }
};
