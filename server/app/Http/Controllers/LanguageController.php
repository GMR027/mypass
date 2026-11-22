<?php

namespace App\Http\Controllers;

use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;

class LanguageController extends Controller
{
    public function switch(Request $request, string $locale): RedirectResponse
    {
        abort_unless(in_array($locale, ['es-MX', 'en'], true), 404);

        $request->session()->put('locale', $locale);
        app()->setLocale($locale);

        if ($request->user() !== null) {
            $request->user()->forceFill(['locale' => $locale])->save();
        }

        return redirect()->back();
    }
}
