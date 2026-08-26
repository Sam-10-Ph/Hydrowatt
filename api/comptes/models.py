import secrets
import pyotp
from django.contrib.auth.models import AbstractUser
from django.db import models
from django.utils import timezone
from datetime import timedelta


class Utilisateur(AbstractUser):
    """
    Utilisateur de la plateforme HydroWatt.
    Aucune auto-inscription publique : les comptes sont créés uniquement
    par un administrateur (is_staff=True) via le module Utilisateurs.
    """
    email = models.EmailField(unique=True)
    otp_secret = models.CharField(max_length=32, blank=True, null=True)
    is_2fa_enabled = models.BooleanField(default=True)

    USERNAME_FIELD = "username"
    REQUIRED_FIELDS = ["email"]

    def get_or_create_otp_secret(self):
        if not self.otp_secret:
            self.otp_secret = pyotp.random_base32()
            self.save(update_fields=["otp_secret"])
        return self.otp_secret

    def verify_totp(self, code):
        secret = self.get_or_create_otp_secret()
        totp = pyotp.TOTP(secret)
        return totp.verify(code, valid_window=1)

    def __str__(self):
        return self.username


class PasswordResetToken(models.Model):
    """Token à usage unique pour la réinitialisation de mot de passe."""
    utilisateur = models.ForeignKey(Utilisateur, on_delete=models.CASCADE, related_name="reset_tokens")
    token = models.CharField(max_length=64, unique=True, default=secrets.token_urlsafe)
    created_at = models.DateTimeField(auto_now_add=True)
    used = models.BooleanField(default=False)
    EXPIRATION_MINUTES = 30

    def is_valid(self):
        expired = timezone.now() > self.created_at + timedelta(minutes=self.EXPIRATION_MINUTES)
        return (not self.used) and (not expired)

    def __str__(self):
        return f"Token reset pour {self.utilisateur.username} ({'utilisé' if self.used else 'actif'})"
