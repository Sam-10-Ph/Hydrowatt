from django.db import models


class Barrage(models.Model):
    """Un barrage hydroélectrique gabonais (ex : Kinguélé, Grand Poubara)."""
    nom = models.CharField(max_length=150, unique=True)
    localisation = models.CharField(max_length=200)
    capacite_installee_mw = models.DecimalField(
        max_digits=8, decimal_places=2,
        help_text="Capacité installée en mégawatts (MW)"
    )
    date_creation = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["nom"]

    def __str__(self):
        return self.nom


class ReleveNiveauEau(models.Model):
    """Relevé ponctuel du niveau d'eau d'un barrage."""
    barrage = models.ForeignKey(Barrage, on_delete=models.CASCADE, related_name="releves_niveau")
    date = models.DateField()
    niveau_m = models.DecimalField(max_digits=6, decimal_places=2, help_text="Niveau d'eau en mètres")

    class Meta:
        ordering = ["-date"]
        constraints = [
            models.UniqueConstraint(fields=["barrage", "date"], name="unique_releve_par_jour")
        ]

    def __str__(self):
        return f"{self.barrage.nom} - {self.date} - {self.niveau_m} m"


class ProductionJournaliere(models.Model):
    """Production d'énergie journalière d'un barrage."""
    barrage = models.ForeignKey(Barrage, on_delete=models.CASCADE, related_name="productions")
    date = models.DateField()
    energie_produite_mwh = models.DecimalField(max_digits=10, decimal_places=2, help_text="Énergie produite en MWh")

    class Meta:
        ordering = ["-date"]
        constraints = [
            models.UniqueConstraint(fields=["barrage", "date"], name="unique_production_par_jour")
        ]

    def __str__(self):
        return f"{self.barrage.nom} - {self.date} - {self.energie_produite_mwh} MWh"
