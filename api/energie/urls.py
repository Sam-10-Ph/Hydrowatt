from rest_framework.routers import DefaultRouter
from .views import BarrageViewSet, ReleveNiveauEauViewSet, ProductionJournaliereViewSet

router = DefaultRouter()
router.register("barrages", BarrageViewSet, basename="barrage")
router.register("releves-niveau", ReleveNiveauEauViewSet, basename="releve-niveau")
router.register("productions", ProductionJournaliereViewSet, basename="production")

urlpatterns = router.urls
