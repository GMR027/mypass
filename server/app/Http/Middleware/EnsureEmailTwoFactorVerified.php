<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Symfony\Component\HttpFoundation\Response;

class EnsureEmailTwoFactorVerified
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (
            $user !== null &&
            $user->email_two_factor_enabled &&
            $request->session()->get('email_two_factor_verified') !== $user->id
        ) {
            $request->session()->put('email_two_factor_user_id', $user->id);
            Auth::guard('web')->logout();
            $request->session()->regenerateToken();

            if ($request->expectsJson()) {
                return response()->json([
                    'message' => 'Verifica tu correo para continuar.',
                    'email_two_factor_required' => true,
                ], 403);
            }

            return redirect()->route('email-two-factor.challenge');
        }

        return $next($request);
    }
}
