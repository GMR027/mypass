<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class UpdateAvatarRequest extends FormRequest
{
    protected $errorBag = 'updateAvatar';

    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, array<int, string>>
     */
    public function rules(): array
    {
        return [
            'avatar' => ['required', 'image', 'mimes:jpg,jpeg,png,webp', 'dimensions:max_width=4000,max_height=4000', 'max:4096'],
        ];
    }

    /**
     * @return array<string, string>
     */
    public function messages(): array
    {
        return [
            'avatar.required' => __('Selecciona una foto de perfil.'),
            'avatar.image' => __('El archivo debe ser una imagen.'),
            'avatar.mimes' => __('Usa una imagen JPG, PNG o WEBP.'),
            'avatar.dimensions' => __('La foto de perfil es demasiado grande en dimensiones.'),
            'avatar.max' => __('La foto de perfil no debe superar 4 MB.'),
        ];
    }

    /**
     * @return array<string, string>
     */
    public function attributes(): array
    {
        return [
            'avatar' => __('foto de perfil'),
        ];
    }
}
