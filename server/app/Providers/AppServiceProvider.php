<?php

namespace App\Providers;

use Illuminate\Support\Facades\File;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\View;
use Illuminate\Support\ServiceProvider;
use Illuminate\Validation\Rules\Password;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        Password::defaults(fn () => Password::min(12)->letters()->numbers());

        View::composer('*', function ($view): void {
            $translations = [];

            if (app()->getLocale() === 'en') {
                $path = lang_path('en.json');

                if (File::exists($path)) {
                    $translations = json_decode(File::get($path), true) ?: [];
                }
            }

            $androidVersionCode = 0;
            $versionPath = 'downloads/version.json';

            if (Storage::disk('local')->exists($versionPath)) {
                $version = json_decode(Storage::disk('local')->get($versionPath), true);
                $androidVersionCode = (int) ($version['versionCode'] ?? 0);
            }

            $view->with('jsTranslations', $translations);
            $view->with('androidVersionCode', $androidVersionCode);
        });
    }
}
