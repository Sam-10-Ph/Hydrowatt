import django_filters
from .models import ReleveNiveauEau, ProductionJournaliere


class ReleveNiveauEauFilter(django_filters.FilterSet):
    barrage = django_filters.NumberFilter(field_name="barrage__id")
    date_debut = django_filters.DateFilter(field_name="date", lookup_expr="gte")
    date_fin = django_filters.DateFilter(field_name="date", lookup_expr="lte")
    niveau_min = django_filters.NumberFilter(field_name="niveau_m", lookup_expr="gte")
    niveau_max = django_filters.NumberFilter(field_name="niveau_m", lookup_expr="lte")

    class Meta:
        model = ReleveNiveauEau
        fields = ["barrage", "date_debut", "date_fin", "niveau_min", "niveau_max"]


class ProductionJournaliereFilter(django_filters.FilterSet):
    barrage = django_filters.NumberFilter(field_name="barrage__id")
    date_debut = django_filters.DateFilter(field_name="date", lookup_expr="gte")
    date_fin = django_filters.DateFilter(field_name="date", lookup_expr="lte")
    production_min = django_filters.NumberFilter(field_name="energie_produite_mwh", lookup_expr="gte")

    class Meta:
        model = ProductionJournaliere
        fields = ["barrage", "date_debut", "date_fin", "production_min"]
