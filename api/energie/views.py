from rest_framework import viewsets, filters as drf_filters
from rest_framework.decorators import action
from rest_framework.response import Response
from django_filters.rest_framework import DjangoFilterBackend

from .models import Barrage, ReleveNiveauEau, ProductionJournaliere
from .serializers import (
    BarrageSerializer, ReleveNiveauEauSerializer, ProductionJournaliereSerializer,
    CorrelationPointSerializer,
)
from .filters import ReleveNiveauEauFilter, ProductionJournaliereFilter


class BarrageViewSet(viewsets.ModelViewSet):
    queryset = Barrage.objects.all()
    serializer_class = BarrageSerializer
    filter_backends = [DjangoFilterBackend, drf_filters.SearchFilter]
    filterset_fields = ["localisation"]
    search_fields = ["nom", "localisation"]

    @action(detail=True, methods=["get"])
    def correlation(self, request, pk=None):
        """
        Défi technique du sujet : met en évidence la corrélation entre le
        niveau d'eau et la production journalière pour ce barrage.
        """
        barrage = self.get_object()
        releves = {r.date: r.niveau_m for r in barrage.releves_niveau.all()}
        productions = {p.date: p.energie_produite_mwh for p in barrage.productions.all()}
        dates = sorted(set(releves) | set(productions))
        data = [
            {
                "date": d,
                "barrage": barrage.nom,
                "niveau_m": releves.get(d),
                "energie_produite_mwh": productions.get(d),
            }
            for d in dates
        ]
        serializer = CorrelationPointSerializer(data, many=True)
        return Response(serializer.data)


class ReleveNiveauEauViewSet(viewsets.ModelViewSet):
    queryset = ReleveNiveauEau.objects.select_related("barrage").all()
    serializer_class = ReleveNiveauEauSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_class = ReleveNiveauEauFilter


class ProductionJournaliereViewSet(viewsets.ModelViewSet):
    queryset = ProductionJournaliere.objects.select_related("barrage").all()
    serializer_class = ProductionJournaliereSerializer
    filter_backends = [DjangoFilterBackend]
    filterset_class = ProductionJournaliereFilter
