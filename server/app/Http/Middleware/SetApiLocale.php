<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class SetApiLocale
{
    public function handle(Request $request, Closure $next): Response
    {
        $acceptLanguage = strtolower((string) $request->header('Accept-Language'));

        app()->setLocale(str_starts_with($acceptLanguage, 'en') ? 'en' : 'es-MX');

        return $next($request);
    }
}
