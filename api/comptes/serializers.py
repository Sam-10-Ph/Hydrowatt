from rest_framework import serializers
from django.contrib.auth.password_validation import validate_password
from .models import Utilisateur


class UtilisateurSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, required=False, validators=[validate_password])

    class Meta:
        model = Utilisateur
        fields = [
            "id", "username", "email", "first_name", "last_name",
            "is_staff", "is_active", "is_2fa_enabled", "password",
        ]
        read_only_fields = ["id"]

    def create(self, validated_data):
        password = validated_data.pop("password", None)
        user = Utilisateur(**validated_data)
        # Comptes créés uniquement par l'administrateur : mot de passe toujours haché.
        user.set_password(password or Utilisateur.objects.make_random_password())
        user.save()
        return user

    def update(self, instance, validated_data):
        password = validated_data.pop("password", None)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        if password:
            instance.set_password(password)
        instance.save()
        return instance


class LoginSerializer(serializers.Serializer):
    username = serializers.CharField()
    password = serializers.CharField(write_only=True)


class Verify2FASerializer(serializers.Serializer):
    pre_auth_token = serializers.CharField()
    code = serializers.CharField(min_length=6, max_length=6)


class ForgotPasswordSerializer(serializers.Serializer):
    email = serializers.EmailField()


class ResetPasswordSerializer(serializers.Serializer):
    token = serializers.CharField()
    new_password = serializers.CharField(validators=[validate_password])
