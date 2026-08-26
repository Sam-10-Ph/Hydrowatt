from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import Utilisateur, PasswordResetToken

admin.site.register(Utilisateur, UserAdmin)
admin.site.register(PasswordResetToken)
