<?php

namespace App\Providers;

use Illuminate\Support\Facades\File;
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

            $view->with('jsTranslations', $translations);
        });
    }
}
