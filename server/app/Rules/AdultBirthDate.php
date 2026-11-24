<?php

namespace App\Rules;

use App\Models\User;
use Closure;
use DateTimeImmutable;
use Illuminate\Contracts\Validation\ValidationRule;
use Throwable;

class AdultBirthDate implements ValidationRule
{
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if (! is_string($value)) {
            $fail(__('La fecha de nacimiento no es válida.'));

            return;
        }

        try {
            $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
        } catch (Throwable) {
            $date = false;
        }

        if (! $date instanceof DateTimeImmutable || $date->format('Y-m-d') !== $value) {
            $fail(__('La fecha de nacimiento no es válida.'));

            return;
        }

        $today = new DateTimeImmutable('today');

        if ($date > $today) {
            $fail(__('La fecha de nacimiento no puede estar en el futuro.'));

            return;
        }

        $age = $date->diff($today)->y;

        if ($age < User::MINIMUM_AGE) {
            $fail(__('Debes tener al menos 18 años para crear una cuenta.'));

            return;
        }

        if ($age > User::MAXIMUM_AGE) {
            $fail(__('Revisa la fecha de nacimiento: el año no parece correcto.'));
        }
    }
}
