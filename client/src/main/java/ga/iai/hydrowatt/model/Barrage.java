package ga.iai.hydrowatt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Barrage {
    private Integer id;
    private String nom;
    private String localisation;
    private java.math.BigDecimal capaciteInstalleeMw;

    public Barrage() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    @com.fasterxml.jackson.annotation.JsonProperty("capacite_installee_mw")
    public java.math.BigDecimal getCapaciteInstalleeMw() { return capaciteInstalleeMw; }
    @com.fasterxml.jackson.annotation.JsonProperty("capacite_installee_mw")
    public void setCapaciteInstalleeMw(java.math.BigDecimal capaciteInstalleeMw) { this.capaciteInstalleeMw = capaciteInstalleeMw; }

    @Override
    public String toString() {
        return nom + " (" + localisation + ", " + capaciteInstalleeMw + " MW)";
    }
}
