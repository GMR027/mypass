<?php

namespace App\Actions\Fortify;

use App\Models\User;
use App\Rules\AdultBirthDate;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;
use Laravel\Fortify\Contracts\CreatesNewUsers;

class CreateNewUser implements CreatesNewUsers
{
    use PasswordValidationRules;

    /**
     * Validate and create a newly registered user.
     *
     * @param  array<string, string>  $input
     *
     * @throws ValidationException
     */
    public function create(array $input): User
    {
        Validator::make($input, [
            'name' => ['required', 'string', 'max:255'],
            'email' => [
                'required',
                'string',
                'email',
                'max:255',
                Rule::unique(User::class),
            ],
            'password' => $this->passwordRules(),
            'birth_date' => ['required', 'date_format:Y-m-d', new AdultBirthDate],
        ])->validate();

        $isSenior = Carbon::parse($input['birth_date'])->age >= User::SENIOR_AGE;

        return User::create([
            'name' => $input['name'],
            'email' => $input['email'],
            'password' => Hash::make($input['password']),
            'birth_date' => $input['birth_date'],
            'locale' => session('locale') === 'en' ? 'en' : 'es-MX',
            'simple_mode' => $isSenior,
            'easy_mode' => null,
        ]);
    }
}
