from django.contrib.auth import authenticate
from django.core import signing
from django.core.mail import send_mail
from django.conf import settings
from rest_framework import viewsets, permissions, status
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Utilisateur, PasswordResetToken
from .serializers import (
    UtilisateurSerializer, LoginSerializer, Verify2FASerializer,
    ForgotPasswordSerializer, ResetPasswordSerializer,
)

PRE_AUTH_SALT = "hydrowatt.pre-auth-2fa"
PRE_AUTH_MAX_AGE = 300  # 5 minutes pour saisir le code TOTP


class IsAdmin(permissions.BasePermission):
    """Seul un administrateur (is_staff) peut gérer le module Utilisateurs."""
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.is_staff)


def tokens_for_user(user):
    refresh = RefreshToken.for_user(user)
    return {"access": str(refresh.access_token), "refresh": str(refresh)}


class LoginView(APIView):
    """
    Étape 1 : email/identifiant + mot de passe (haché en base, jamais en clair).
    Si l'utilisateur a la 2FA activée, renvoie un pre_auth_token à usage unique
    (valide 5 min) à fournir avec le code TOTP sur /api/auth/2fa/verify/.
    Sinon, renvoie directement les jetons JWT.
    """
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = authenticate(
            username=serializer.validated_data["username"],
            password=serializer.validated_data["password"],
        )
        if user is None or not user.is_active:
            return Response({"detail": "Identifiants invalides."}, status=status.HTTP_401_UNAUTHORIZED)

        if user.is_2fa_enabled:
            pre_auth_token = signing.dumps({"user_id": user.id}, salt=PRE_AUTH_SALT)
            return Response({"requires_2fa": True, "pre_auth_token": pre_auth_token})

        return Response({"requires_2fa": False, **tokens_for_user(user)})


class Verify2FAView(APIView):
    """Étape 2 : validation du code TOTP (type Google Authenticator)."""
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = Verify2FASerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            payload = signing.loads(
                serializer.validated_data["pre_auth_token"],
                salt=PRE_AUTH_SALT,
                max_age=PRE_AUTH_MAX_AGE,
            )
        except signing.BadSignature:
            return Response({"detail": "Jeton pré-authentification invalide ou expiré."}, status=400)

        user = Utilisateur.objects.filter(id=payload["user_id"]).first()
        if user is None or not user.verify_totp(serializer.validated_data["code"]):
            return Response({"detail": "Code 2FA invalide."}, status=status.HTTP_401_UNAUTHORIZED)

        return Response(tokens_for_user(user))


class ForgotPasswordView(APIView):
    """
    Génère un token à usage unique, envoyé par email (backend console en dev),
    avec expiration (30 min).
    """
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = ForgotPasswordSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data["email"]
        user = Utilisateur.objects.filter(email=email).first()

        # Réponse volontairement identique que l'email existe ou non (anti-énumération).
        if user:
            reset = PasswordResetToken.objects.create(utilisateur=user)
            reset_link = f"{settings.FRONTEND_RESET_URL}?token={reset.token}"
            send_mail(
                subject="HydroWatt — Réinitialisation de mot de passe",
                message=f"Voici votre lien de réinitialisation (valide 30 min) : {reset_link}",
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[email],
                fail_silently=True,
            )
        return Response({"detail": "Si ce compte existe, un email a été envoyé."})


class ResetPasswordView(APIView):
    """Formulaire de reset via le lien reçu ; invalide le token après usage."""
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = ResetPasswordSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        token = PasswordResetToken.objects.filter(token=serializer.validated_data["token"]).first()

        if token is None or not token.is_valid():
            return Response({"detail": "Token invalide ou expiré."}, status=400)

        user = token.utilisateur
        user.set_password(serializer.validated_data["new_password"])
        user.save()
        token.used = True
        token.save(update_fields=["used"])
        return Response({"detail": "Mot de passe réinitialisé avec succès."})


class MeView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        return Response(UtilisateurSerializer(request.user).data)


class UtilisateurViewSet(viewsets.ModelViewSet):
    """
    CRUD utilisateurs — réservé à l'administrateur.
    Aucune route d'auto-inscription publique n'existe : c'est la seule
    façon de créer un compte.
    """
    queryset = Utilisateur.objects.all().order_by("username")
    serializer_class = UtilisateurSerializer
    permission_classes = [IsAdmin]
    filter_backends = []
