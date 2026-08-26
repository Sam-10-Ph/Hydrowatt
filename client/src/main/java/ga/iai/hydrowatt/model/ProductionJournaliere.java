package ga.iai.hydrowatt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionJournaliere {
    private Integer id;
    private Integer barrage;
    @JsonProperty("barrage_nom")
    private String barrageNom;
    private LocalDate date;
    @JsonProperty("energie_produite_mwh")
    private BigDecimal energieProduiteMwh;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getBarrage() { return barrage; }
    public void setBarrage(Integer barrage) { this.barrage = barrage; }
    public String getBarrageNom() { return barrageNom; }
    public void setBarrageNom(String barrageNom) { this.barrageNom = barrageNom; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getEnergieProduiteMwh() { return energieProduiteMwh; }
    public void setEnergieProduiteMwh(BigDecimal energieProduiteMwh) { this.energieProduiteMwh = energieProduiteMwh; }
}
