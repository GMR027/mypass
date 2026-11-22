<script nonce="{{ $cspNonce }}">
    window.mypassTranslations = @json($jsTranslations ?? []);
    window.mypassLocale = @json(app()->getLocale());
    window.mypassT = function (key) {
        return Object.prototype.hasOwnProperty.call(window.mypassTranslations, key)
            ? window.mypassTranslations[key]
            : key;
    };
    window.mypassTF = function (key, values) {
        values = values || {};
        return window.mypassT(key).replace(/:(\w+)/g, function (_, name) {
            return Object.prototype.hasOwnProperty.call(values, name) ? values[name] : ':' + name;
        });
    };
</script>
