from django.contrib import admin
from .models import Barrage, ReleveNiveauEau, ProductionJournaliere

admin.site.register(Barrage)
admin.site.register(ReleveNiveauEau)
admin.site.register(ProductionJournaliere)
