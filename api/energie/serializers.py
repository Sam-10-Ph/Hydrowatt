from rest_framework import serializers
from .models import Barrage, ReleveNiveauEau, ProductionJournaliere


class BarrageSerializer(serializers.ModelSerializer):
    class Meta:
        model = Barrage
        fields = ["id", "nom", "localisation", "capacite_installee_mw", "date_creation"]


class ReleveNiveauEauSerializer(serializers.ModelSerializer):
    barrage_nom = serializers.CharField(source="barrage.nom", read_only=True)

    class Meta:
        model = ReleveNiveauEau
        fields = ["id", "barrage", "barrage_nom", "date", "niveau_m"]


class ProductionJournaliereSerializer(serializers.ModelSerializer):
    barrage_nom = serializers.CharField(source="barrage.nom", read_only=True)

    class Meta:
        model = ProductionJournaliere
        fields = ["id", "barrage", "barrage_nom", "date", "energie_produite_mwh"]


class CorrelationPointSerializer(serializers.Serializer):
    """Un point date/barrage avec niveau d'eau et production associée, pour le graphique de corrélation."""
    date = serializers.DateField()
    barrage = serializers.CharField()
    niveau_m = serializers.DecimalField(max_digits=6, decimal_places=2, allow_null=True)
    energie_produite_mwh = serializers.DecimalField(max_digits=10, decimal_places=2, allow_null=True)
